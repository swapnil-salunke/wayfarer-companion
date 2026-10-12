#!/usr/bin/env python3
"""Turns the canvas sources (canvas/*.dc.html) into standalone HTML + PNG previews (preview/).

The .dc.html files use the Design canvas runtime (support.js), so they only render inside the canvas.
Their markup is plain HTML/CSS inside <x-dc>, so we unwrap it, hoist <helmet> into <head>, drop the runtime
script, and screenshot each file with headless Chrome (needs network for Google Fonts).

    python3 design/mockups/build_previews.py
"""
import json, pathlib, re, subprocess, shutil, sys

root = pathlib.Path(__file__).parent
canvas = json.loads((root / "canvas" / "canvas.json").read_text())
out = root / "preview"; out.mkdir(exist_ok=True)
chrome = next((p for p in ["/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", shutil.which("google-chrome") or "", shutil.which("chromium") or ""] if p and pathlib.Path(p).exists()), None)

for name, box in canvas["boards"].items():
    src = (root / "canvas" / name).read_text()
    title = re.search(r"<title>(.*?)</title>", src).group(1)
    helmet = re.search(r"<helmet>(.*?)</helmet>", src, re.S).group(1)
    body = re.search(r"</helmet>(.*?)</x-dc>", src, re.S).group(1)
    stem = name.removesuffix(".dc.html")
    html = (f'<!doctype html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n<meta name="viewport" content="width=device-width, initial-scale=1">\n'
            f'<title>{title}</title>\n{helmet}\n</head>\n<body>\n{body}\n</body>\n</html>\n')
    page = out / f"{stem}.html"; page.write_text(html)
    if chrome:
        subprocess.run([chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=2",
                        f"--window-size={box['w']},{box['h']}", "--virtual-time-budget=8000",
                        f"--screenshot={out / (stem + '.png')}", page.as_uri()], check=True, capture_output=True)
        print("wrote", out / f"{stem}.png")
    else:
        print("Chrome not found; wrote HTML only", file=sys.stderr)
