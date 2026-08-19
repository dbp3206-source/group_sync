package com.groupsync.backend.knowledge.chunking;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.ingestion.BlockType;
import com.groupsync.backend.knowledge.ingestion.ParsedBlock;
import com.groupsync.backend.knowledge.ingestion.ParsedDocument;
import com.groupsync.backend.knowledge.model.ChunkLevel;

/**
 * Structure-aware chunking strategy for RAG v2.
 * Preserves structural document boundaries (headings, sections, paragraphs, page numbers)
 * and constructs hierarchical Parent (~1500 chars) and Child (~500 chars) chunks.
 */
@Component
public class StructureAwareChunkingStrategy implements ChunkingStrategy {

    private final int parentTargetSize;
    private final int childTargetSize;
    private final int childOverlap;
    private final RecursiveChunkingStrategy recursiveFallback;

    public StructureAwareChunkingStrategy(
            @Value("${rag.chunking.parent-target-size:1500}") int parentTargetSize,
            @Value("${rag.chunking.child-target-size:500}") int childTargetSize,
            @Value("${rag.chunking.child-overlap:80}") int childOverlap,
            RecursiveChunkingStrategy recursiveFallback) {
        this.parentTargetSize = parentTargetSize > 0 ? parentTargetSize : 1500;
        this.childTargetSize = childTargetSize > 0 ? childTargetSize : 500;
        this.childOverlap = childOverlap >= 0 ? childOverlap : 80;
        this.recursiveFallback = recursiveFallback;
    }

    public record HierarchicalChunk(
            int index,
            ChunkLevel level,
            Integer parentIndex,
            Integer pageNumber,
            String sectionTitle,
            String content
    ) {}

    public List<HierarchicalChunk> chunkDocument(ParsedDocument document) {
        if (document == null || document.fullText().isBlank()) {
            return List.of();
        }

        List<ParsedBlock> blocks = document.blocks();
        if (blocks.isEmpty()) {
            // Fallback to simple paragraph / text splitting
            return chunkPlainText(document.fullText());
        }

        List<HierarchicalChunk> results = new ArrayList<>();
        int currentIndex = 0;

        // Group blocks into coherent Section/Parent units
        List<ParentBlockGroup> parentGroups = groupBlocksIntoParents(blocks);

        for (ParentBlockGroup pGroup : parentGroups) {
            int parentIdx = currentIndex++;
            String parentContent = pGroup.content().trim();
            if (parentContent.isBlank()) continue;

            HierarchicalChunk parentChunk = new HierarchicalChunk(
                    parentIdx,
                    ChunkLevel.PARENT,
                    null,
                    pGroup.pageNumber(),
                    pGroup.sectionTitle(),
                    parentContent
            );
            results.add(parentChunk);

            // Subdivide parent into precision child chunks
            List<String> childTexts = splitIntoChildTexts(pGroup);
            for (String cText : childTexts) {
                if (cText.isBlank()) continue;
                HierarchicalChunk childChunk = new HierarchicalChunk(
                        currentIndex++,
                        ChunkLevel.CHILD,
                        parentIdx,
                        pGroup.pageNumber(),
                        pGroup.sectionTitle(),
                        cText.trim()
                );
                results.add(childChunk);
            }
        }

        return results;
    }

    @Override
    public List<String> chunk(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<HierarchicalChunk> hierarchical = chunkDocument(new ParsedDocument(null, content, List.of()));
        return hierarchical.stream()
                .filter(c -> c.level() == ChunkLevel.CHILD)
                .map(HierarchicalChunk::content)
                .toList();
    }

    private record ParentBlockGroup(
            String sectionTitle,
            Integer pageNumber,
            String content,
            List<ParsedBlock> blocks
    ) {}

    private List<ParentBlockGroup> groupBlocksIntoParents(List<ParsedBlock> blocks) {
        List<ParentBlockGroup> groups = new ArrayList<>();

        String currentSection = null;
        Integer currentPage = null;
        StringBuilder currentText = new StringBuilder();
        List<ParsedBlock> currentBlocks = new ArrayList<>();

        for (ParsedBlock block : blocks) {
            if (block.heading() != null && !block.heading().isBlank()) {
                currentSection = block.heading();
            }
            if (block.pageNumber() != null) {
                currentPage = block.pageNumber();
            }

            // If adding this block exceeds parentTargetSize and we already have content, flush
            if (currentText.length() + block.content().length() > parentTargetSize && !currentText.isEmpty()) {
                groups.add(new ParentBlockGroup(currentSection, currentPage, currentText.toString(), new ArrayList<>(currentBlocks)));
                currentText.setLength(0);
                currentBlocks.clear();
            }

            if (!currentText.isEmpty()) {
                currentText.append("\n\n");
            }
            currentText.append(block.content());
            currentBlocks.add(block);
        }

        if (!currentText.isEmpty()) {
            groups.add(new ParentBlockGroup(currentSection, currentPage, currentText.toString(), currentBlocks));
        }

        return groups;
    }

    private List<String> splitIntoChildTexts(ParentBlockGroup pGroup) {
        String content = pGroup.content();
        if (content.length() <= childTargetSize) {
            return List.of(content);
        }

        List<String> children = new ArrayList<>();

        // Try paragraph-based splitting first
        String[] paragraphs = content.split("(?:\\r?\\n){2,}");
        StringBuilder currentChild = new StringBuilder();

        for (String para : paragraphs) {
            String p = para.trim();
            if (p.isBlank()) continue;

            if (p.length() > childTargetSize) {
                // If this single paragraph is oversized, flush current child and recursive-split the paragraph
                if (!currentChild.isEmpty()) {
                    children.add(currentChild.toString().trim());
                    currentChild.setLength(0);
                }
                List<String> subChunks = recursiveFallback.chunk(p);
                children.addAll(subChunks);
            } else if (currentChild.length() + p.length() + 2 > childTargetSize) {
                // Exceeds childTargetSize, flush current child with overlap
                children.add(currentChild.toString().trim());

                // Start next child with overlap if possible
                String prev = currentChild.toString();
                currentChild.setLength(0);
                if (prev.length() > childOverlap) {
                    currentChild.append(prev.substring(prev.length() - childOverlap).trim()).append(" ");
                }
                currentChild.append(p);
            } else {
                if (!currentChild.isEmpty()) currentChild.append("\n\n");
                currentChild.append(p);
            }
        }

        if (!currentChild.isEmpty()) {
            children.add(currentChild.toString().trim());
        }

        return children;
    }

    private List<HierarchicalChunk> chunkPlainText(String text) {
        List<HierarchicalChunk> results = new ArrayList<>();
        int index = 0;

        List<String> paragraphs = List.of(text.split("(?:\\r?\\n){2,}"));
        StringBuilder parentBuf = new StringBuilder();
        int parentIdx = index++;

        for (String para : paragraphs) {
            if (parentBuf.length() + para.length() > parentTargetSize && !parentBuf.isEmpty()) {
                results.add(new HierarchicalChunk(parentIdx, ChunkLevel.PARENT, null, null, null, parentBuf.toString().trim()));
                parentBuf.setLength(0);
                parentIdx = index++;
            }
            if (!parentBuf.isEmpty()) parentBuf.append("\n\n");
            parentBuf.append(para);
        }

        if (!parentBuf.isEmpty()) {
            results.add(new HierarchicalChunk(parentIdx, ChunkLevel.PARENT, null, null, null, parentBuf.toString().trim()));
        }

        // Generate children
        List<String> childPieces = recursiveFallback.chunk(text);
        for (String piece : childPieces) {
            results.add(new HierarchicalChunk(index++, ChunkLevel.CHILD, parentIdx, null, null, piece));
        }

        return results;
    }
}
