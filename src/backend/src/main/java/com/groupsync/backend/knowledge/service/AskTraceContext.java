package com.groupsync.backend.knowledge.service;

import java.util.function.BiConsumer;
import com.groupsync.backend.knowledge.dto.*;

/** Request-scoped bridge used only while an Ask attempt is executing. */
public final class AskTraceContext {
    private static final ThreadLocal<BiConsumer<AskTraceStage, AskTraceTechnicalDetails>> CURRENT = new ThreadLocal<>();
    private AskTraceContext() { }
    public static Scope open(BiConsumer<AskTraceStage, AskTraceTechnicalDetails> sink) {
        BiConsumer<AskTraceStage, AskTraceTechnicalDetails> previous = CURRENT.get();
        CURRENT.set(sink);
        return () -> { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); };
    }
    public static void emit(AskTraceStage stage, AskTraceTechnicalDetails details) {
        BiConsumer<AskTraceStage, AskTraceTechnicalDetails> sink = CURRENT.get();
        if (sink != null) sink.accept(stage, details);
    }
    @FunctionalInterface public interface Scope extends AutoCloseable { @Override void close(); }
}
