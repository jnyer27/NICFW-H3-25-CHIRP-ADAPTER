"""
Convert UserGuide.md -> UserGuide.pdf using markdown + fpdf2.
Run from the repo root:
    python tools/md_to_pdf.py
"""

import re
from pathlib import Path
import markdown
from fpdf import FPDF

# ── Paths ─────────────────────────────────────────────────────────────────────
REPO_ROOT = Path(__file__).parent.parent
MD_FILE   = REPO_ROOT / "UserGuide.md"
PDF_FILE  = REPO_ROOT / "UserGuide.pdf"

# Windows system fonts
FONT_DIR   = Path("C:/Windows/Fonts")
FONT_REG   = FONT_DIR / "calibri.ttf"
FONT_BOLD  = FONT_DIR / "calibrib.ttf"
FONT_ITAL  = FONT_DIR / "calibrii.ttf"
FONT_BOLDI = FONT_DIR / "calibriz.ttf"   # bold+italic

if not FONT_REG.exists():
    FONT_REG   = FONT_DIR / "arial.ttf"
    FONT_BOLD  = FONT_DIR / "arialbd.ttf"
    FONT_ITAL  = FONT_DIR / "ariali.ttf"
    FONT_BOLDI = FONT_DIR / "arialbi.ttf"

# ── Unicode -> ASCII sanitiser ─────────────────────────────────────────────────
REPLACEMENTS = {
    # Emoji
    "\U0001F50D": "[Search]",
    # Box-drawing
    "\u250C": "+", "\u2510": "+", "\u2514": "+", "\u2518": "+",
    "\u251C": "+", "\u2524": "+", "\u252C": "+", "\u2534": "+", "\u253C": "+",
    "\u2500": "-", "\u2502": "|",
    "\u2550": "=", "\u2551": "|",
    "\u2554": "+", "\u2557": "+", "\u255A": "+", "\u255D": "+",
    "\u2560": "+", "\u2563": "+", "\u2566": "+", "\u2569": "+", "\u256C": "+",
    # Arrows & misc symbols
    "\u2192": "->", "\u2190": "<-", "\u21B5": "<-",
    "\u22EE": "|",
    "\u2022": "*",
    "\u00B7": ".",
    "\u2026": "...",
    # Smart quotes & dashes
    "\u2018": "'", "\u2019": "'",
    "\u201C": '"',  "\u201D": '"',
    "\u2013": "-",  "\u2014": "--",
    # Symbols
    "\u2714": "[OK]", "\u2718": "[X]", "\u2713": "[OK]",
    "\u26A0": "[!]",
    "\u00A9": "(c)", "\u00AE": "(R)",
    "\u00BD": "1/2",
}

def sanitise(text: str) -> str:
    for char, replacement in REPLACEMENTS.items():
        text = text.replace(char, replacement)
    # Replace any remaining non-latin-1 characters with '?'
    return text.encode("latin-1", errors="replace").decode("latin-1")

# ── HTML post-processing ──────────────────────────────────────────────────────
def clean_html(html: str) -> str:
    """
    fpdf2's HTML writer doesn't support:
    - <code> inside <td>/<th>  -> strip the <code> tags, keep text
    - <a href> with non-latin URLs -> strip hrefs
    - <blockquote> -> fpdf2 may not handle it; convert to <p><em>
    """
    # Strip ALL inline tags inside table cells (fpdf2 limitation: no nested tags in td/th)
    INLINE_TAGS = r"code|strong|b|em|i|a|span|s|del|ins|mark|sup|sub"
    def strip_inline_in_td(m):
        inner = re.sub(rf"</?(?:{INLINE_TAGS})[^>]*>", "", m.group(0), flags=re.IGNORECASE)
        return inner
    html = re.sub(r"<t[dh][^>]*>.*?</t[dh]>", strip_inline_in_td,
                  html, flags=re.DOTALL | re.IGNORECASE)

    # Strip hrefs from <a> tags — fpdf2 can render links but URLs with
    # non-latin chars cause encoding errors; just keep the link text
    html = re.sub(r'<a\s[^>]*>', "", html, flags=re.IGNORECASE)
    html = re.sub(r"</a>", "", html, flags=re.IGNORECASE)

    # Convert <blockquote> to indented italic paragraph (fpdf2 ignores blockquote)
    html = re.sub(r"<blockquote>", "<p><em>", html, flags=re.IGNORECASE)
    html = re.sub(r"</blockquote>", "</em></p>", html, flags=re.IGNORECASE)

    return html

# ── Markdown -> HTML ───────────────────────────────────────────────────────────
md_text = sanitise(MD_FILE.read_text(encoding="utf-8"))

html_body = markdown.markdown(
    md_text,
    extensions=["tables", "fenced_code", "nl2br"],
)

html_body = clean_html(html_body)

HTML = f"""<!DOCTYPE html>
<html><head><meta charset="utf-8"/></head>
<body>
{html_body}
</body></html>"""

# ── PDF class ─────────────────────────────────────────────────────────────────
class PDF(FPDF):
    def header(self):
        self.set_font("calibri", "B", 9)
        self.set_text_color(100, 100, 100)
        self.cell(0, 8, "NICFW TD-H3 25 Channel Editor -- User Guide", align="L")
        self.ln(2)
        self.set_draw_color(180, 180, 180)
        self.line(self.l_margin, self.get_y(), self.w - self.r_margin, self.get_y())
        self.ln(4)
        self.set_text_color(0, 0, 0)

    def footer(self):
        self.set_y(-13)
        self.set_font("calibri", "I", 8)
        self.set_text_color(150, 150, 150)
        self.cell(0, 8, f"Page {self.page_no()}", align="C")


pdf = PDF(format="A4")
pdf.set_auto_page_break(auto=True, margin=18)
pdf.set_margins(left=18, top=20, right=18)

pdf.add_font("calibri", style="",   fname=str(FONT_REG))
pdf.add_font("calibri", style="B",  fname=str(FONT_BOLD))
pdf.add_font("calibri", style="I",  fname=str(FONT_ITAL))
pdf.add_font("calibri", style="BI", fname=str(FONT_BOLDI))

pdf.set_font("calibri", size=10)
pdf.add_page()

pdf.write_html(HTML)

pdf.output(str(PDF_FILE))
size_kb = PDF_FILE.stat().st_size // 1024
print(f"Written: {PDF_FILE}  ({size_kb} KB)")
