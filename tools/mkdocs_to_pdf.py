"""
Build MkDocs site and print to PDF using Playwright (headless Chromium).
Run from repo root:
    python tools/mkdocs_to_pdf.py
"""

import subprocess
from pathlib import Path

REPO_ROOT = Path(__file__).parent.parent
SITE_DIR = REPO_ROOT / "site"
INDEX_HTML = SITE_DIR / "index.html"
PDF_FILE = REPO_ROOT / "UserGuide.pdf"

# Build the site
print("Building MkDocs site...")
subprocess.run(["mkdocs", "build", "--clean", "--quiet"], cwd=REPO_ROOT, check=True)

# Print to PDF with Playwright
from playwright.sync_api import sync_playwright

CHROMIUM = (
    Path.home() / ".cache/ms-playwright/chromium-1194"
    / "chrome-linux/chrome"
)

print("Launching headless browser...")
with sync_playwright() as p:
    browser = p.chromium.launch(
        executable_path=str(CHROMIUM) if CHROMIUM.exists() else None,
        args=["--no-sandbox", "--disable-setuid-sandbox"],
    )
    page = browser.new_page()
    page.goto(INDEX_HTML.as_uri(), timeout=60000, wait_until="domcontentloaded")
    page.wait_for_timeout(2000)
    page.pdf(
        path=str(PDF_FILE),
        format="A4",
        print_background=True,
        margin={"top": "20mm", "bottom": "20mm", "left": "15mm", "right": "15mm"},
    )
    browser.close()

size_kb = PDF_FILE.stat().st_size // 1024
print(f"Written: {PDF_FILE}  ({size_kb} KB)")
