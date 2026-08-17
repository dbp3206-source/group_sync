import asyncio
import os
import re
import sys
from playwright.async_api import async_playwright

def markdown_to_html(md_text):
    lines = md_text.split('\n')
    html_lines = []
    in_table = False
    table_lines = []
    in_code = False
    code_lang = ""
    code_lines = []
    in_list = False
    
    def flush_table():
        nonlocal in_table, table_lines, html_lines
        if not table_lines:
            in_table = False
            return
        
        html = ['<table class="styled-table">']
        header_parsed = False
        for line in table_lines:
            line = line.strip()
            if not line.startswith('|'):
                continue
            cells = [c.strip() for c in line.strip('|').split('|')]
            if all(set(c).issubset({'-', ':', ' '}) for c in cells):
                header_parsed = True
                continue
            
            if not header_parsed:
                html.append('  <thead><tr>' + ''.join(f'<th>{format_inline(c)}</th>' for c in cells) + '</tr></thead><tbody>')
                header_parsed = True
            else:
                html.append('  <tr>' + ''.join(f'<td>{format_inline(c)}</td>' for c in cells) + '</tr>')
        html.append('</tbody></table>')
        html_lines.extend(html)
        table_lines = []
        in_table = False

    def format_inline(text):
        text = text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
        text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
        text = re.sub(r'\*(.+?)\*', r'<em>\1</em>', text)
        text = re.sub(r'`(.+?)`', r'<code>\1</code>', text)
        return text

    for line in lines:
        stripped = line.strip()
        
        # Code blocks
        if stripped.startswith('```'):
            if in_code:
                in_code = False
                raw_code = '\n'.join(code_lines)
                if code_lang == 'mermaid':
                    html_lines.append(f'<div class="mermaid-container"><pre class="mermaid">\n{raw_code}\n</pre></div>')
                else:
                    escaped_code = raw_code.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
                    html_lines.append(f'<pre class="code-block language-{code_lang}"><code>{escaped_code}</code></pre>')
                code_lines = []
            else:
                if in_table:
                    flush_table()
                if in_list:
                    html_lines.append('</ul>')
                    in_list = False
                in_code = True
                code_lang = stripped[3:].strip()
            continue
        
        if in_code:
            code_lines.append(line)
            continue
        
        # Tables
        if stripped.startswith('|') and stripped.endswith('|'):
            if not in_table:
                if in_list:
                    html_lines.append('</ul>')
                    in_list = False
                in_table = True
                table_lines = []
            table_lines.append(stripped)
            continue
        elif in_table:
            flush_table()
            
        # Lists
        if re.match(r'^\s*[-*]\s+', line):
            if not in_list:
                html_lines.append('<ul class="styled-list">')
                in_list = True
            item_text = re.sub(r'^\s*[-*]\s+', '', line)
            html_lines.append(f'  <li>{format_inline(item_text)}</li>')
            continue
        elif in_list and (stripped == '' or not re.match(r'^\s+', line)):
            html_lines.append('</ul>')
            in_list = False
            
        # Numbered lists
        if re.match(r'^\s*\d+\.\s+', line):
            item_text = re.sub(r'^\s*\d+\.\s+', '', line)
            num_part = line.split(".")[0].strip()
            html_lines.append(f'<div class="numbered-item"><strong class="item-num">{num_part}.</strong> {format_inline(item_text)}</div>')
            continue

        # Headings
        if stripped.startswith('# '):
            html_lines.append(f'<h1 class="title-h1">{format_inline(stripped[2:])}</h1>')
        elif stripped.startswith('## '):
            html_lines.append(f'<h2 class="title-h2">{format_inline(stripped[3:])}</h2>')
        elif stripped.startswith('### '):
            html_lines.append(f'<h3 class="title-h3">{format_inline(stripped[4:])}</h3>')
        elif stripped.startswith('#### '):
            html_lines.append(f'<h4 class="title-h4">{format_inline(stripped[5:])}</h4>')
        elif stripped.startswith('> '):
            html_lines.append(f'<blockquote class="callout">{format_inline(stripped[2:])}</blockquote>')
        elif stripped.startswith('---'):
            html_lines.append('<hr class="divider" />')
        elif stripped == '':
            html_lines.append('<div class="spacer"></div>')
        else:
            html_lines.append(f'<p class="body-p">{format_inline(stripped)}</p>')

    if in_table:
        flush_table()
    if in_list:
        html_lines.append('</ul>')

    return '\n'.join(html_lines)

async def build_pdf_async():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.dirname(os.path.dirname(script_dir))
    md_path = os.path.join(root_dir, 'docs', '01_guides', 'KNOWLEDGEOS_GUIDE.md')
    html_path = os.path.join(root_dir, 'docs', '01_guides', 'KNOWLEDGEOS_GUIDE.html')
    pdf_path = os.path.join(root_dir, 'docs', '01_guides', 'KNOWLEDGEOS_GUIDE.pdf')

    with open(md_path, 'r', encoding='utf-8') as f:
        md_content = f.read()

    body_html = markdown_to_html(md_content)

    full_html = f"""<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<title>KnowledgeOS — Complete Product Guide & User Manual</title>
<style>
@page {{
    size: A4;
    margin: 20mm 18mm 20mm 18mm;
}}

body {{
    font-family: "Times New Roman", Times, "DejaVu Serif", "Noto Serif", serif;
    font-size: 10.5pt;
    line-height: 1.55;
    color: #1a202c;
    background: #ffffff;
    margin: 0;
    padding: 0;
}}

.title-h1 {{
    font-size: 19pt;
    font-weight: bold;
    color: #0f2d4a;
    border-bottom: 2px solid #0f2d4a;
    padding-bottom: 5px;
    margin-top: 22pt;
    margin-bottom: 10pt;
    page-break-after: avoid;
}}

.title-h2 {{
    font-size: 13.5pt;
    font-weight: bold;
    color: #1a4971;
    border-bottom: 1px solid #cbd5e1;
    padding-bottom: 4px;
    margin-top: 16pt;
    margin-bottom: 8pt;
    page-break-after: avoid;
}}

.title-h3 {{
    font-size: 11.5pt;
    font-weight: bold;
    color: #2b6cb0;
    margin-top: 12pt;
    margin-bottom: 5pt;
    page-break-after: avoid;
}}

.title-h4 {{
    font-size: 10.5pt;
    font-weight: bold;
    color: #2d3748;
    margin-top: 9pt;
    margin-bottom: 4pt;
    page-break-after: avoid;
}}

.body-p {{
    margin-top: 0;
    margin-bottom: 7pt;
    text-align: justify;
}}

.callout {{
    border-left: 3.5px solid #2b6cb0;
    background-color: #f7fafc;
    margin: 8pt 0;
    padding: 7pt 11pt;
    font-style: italic;
    color: #4a5568;
    page-break-inside: avoid;
}}

.styled-table {{
    width: 100%;
    border-collapse: collapse;
    margin: 10pt 0;
    font-size: 9pt;
    page-break-inside: avoid;
}}

.styled-table th, .styled-table td {{
    border: 1px solid #cbd5e1;
    padding: 5pt 7pt;
    text-align: left;
    vertical-align: top;
}}

.styled-table th {{
    background-color: #edf2f7;
    font-weight: bold;
    color: #1a202c;
}}

.styled-table tr:nth-child(even) td {{
    background-color: #f8fafc;
}}

.code-block {{
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 4px;
    padding: 7pt 9pt;
    font-family: "Courier New", Courier, monospace;
    font-size: 8.5pt;
    line-height: 1.4;
    white-space: pre-wrap;
    word-break: break-word;
    page-break-inside: avoid;
    margin: 8pt 0;
}}

.mermaid-container {{
    margin: 12pt 0;
    text-align: center;
    page-break-inside: avoid;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 4px;
    padding: 10pt;
}}

code {{
    font-family: "Courier New", Courier, monospace;
    font-size: 9pt;
    background-color: #f1f5f9;
    padding: 1px 3px;
    border-radius: 3px;
}}

.styled-list {{
    margin: 4pt 0 8pt 16pt;
    padding: 0;
}}

.styled-list li {{
    margin-bottom: 3pt;
}}

.numbered-item {{
    margin-left: 10pt;
    margin-bottom: 4pt;
}}

.item-num {{
    color: #1a4971;
}}

.divider {{
    border: 0;
    border-top: 1px solid #e2e8f0;
    margin: 14pt 0;
}}

.spacer {{
    height: 3pt;
}}

.cover-page {{
    text-align: center;
    padding-top: 90pt;
    padding-bottom: 130pt;
    page-break-after: always;
}}

.cover-title {{
    font-size: 30pt;
    font-weight: bold;
    color: #0f2d4a;
    margin-bottom: 12pt;
}}

.cover-subtitle {{
    font-size: 15pt;
    color: #4a5568;
    margin-bottom: 30pt;
}}

.cover-meta {{
    font-size: 11pt;
    color: #718096;
    line-height: 1.8;
}}
</style>
</head>
<body>

<div class="cover-page">
    <div class="cover-title">KnowledgeOS</div>
    <div class="cover-subtitle">Product Guide & Complete User Manual</div>
    <hr style="width: 120px; border-top: 2px solid #2b6cb0; margin: 24pt auto;" />
    <div class="cover-meta">
        <strong>Third-Year University Computer Science Project</strong><br>
        Java 21 • Spring Boot 4 • PostgreSQL (pgvector & FTS) • React 19<br>
        Hybrid Retrieval-Augmented Generation Architecture<br><br>
        <em>Author: Dinh Bao Phuc</em><br>
        Version 1.0 (Production Release)
    </div>
</div>

{body_html}

</body>
</html>
"""

    with open(html_path, 'w', encoding='utf-8') as f:
        f.write(full_html)
    print(f"Generated HTML at {html_path}")

    async with async_playwright() as p:
        browser = await p.chromium.launch()
        page = await browser.new_page()
        file_url = f"file:///{html_path.replace(os.sep, '/')}"
        await page.goto(file_url, wait_until="networkidle")
        
        # Render PDF
        await page.pdf(
            path=pdf_path,
            format="A4",
            margin={"top": "20mm", "bottom": "20mm", "left": "18mm", "right": "18mm"},
            print_background=True
        )
        print(f"Successfully generated PDF via Playwright: {pdf_path}")
        size = os.path.getsize(pdf_path)
        print(f"PDF Size: {size} bytes ({size / 1024:.2f} KB)")
        await browser.close()

if __name__ == '__main__':
    asyncio.run(build_pdf_async())
