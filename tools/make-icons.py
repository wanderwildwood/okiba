#!/usr/bin/env python3
"""
Three candidate launcher marks in the Kompakt's stock style (81 grid, ring 2.5 wide, glyph
stroke 2.5, black on transparent), as SVG, PNG at 512 and 72, and a vector drawable for
whichever one is chosen.

    python3 tools/make-icons.py [out_dir] [letter_for_the_drawable]

Nothing here ships by itself: the drawable written to app/src/main/res/drawable is a
placeholder until the owner has looked at the candidates.
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "icons")
PICK = sys.argv[2] if len(sys.argv) > 2 else "A"

RING = ("M19.25,2.5C9.999,2.5 2.5,9.999 2.5,19.25V61.75C2.5,71.001 9.999,78.5 19.25,78.5H61.75"
        "C71.001,78.5 78.5,71.001 78.5,61.75V19.25C78.5,9.999 71.001,2.5 61.75,2.5H19.25Z"
        "M0,19.25C0,8.619 8.619,0 19.25,0H61.75C72.382,0 81,8.619 81,19.25V61.75C81,72.382 72.382,81 61.75,81"
        "H19.25C8.619,81 0,72.382 0,61.75V19.25Z")

# Each glyph is a list of (path data, fill). Stroke only, one weight, drawn to survive 48 px.
GLYPHS = {
    # A: a map pin standing on a short ground line.
    "A": [
        ("M40.5,57 C40.5,57 27,46 27,35.5 A13.5,13.5 0 0 1 54,35.5 C54,46 40.5,57 40.5,57 Z", None),
        ("M35.5,35.5 A5,5 0 1 0 45.5,35.5 A5,5 0 1 0 35.5,35.5 Z", None),
        ("M31,63 H50", None),
    ],
    # B: a car from the side: body, window, two wheels.
    "B": [
        ("M20,49 V43 Q20,40.5 22.5,40 L28.5,38.5 L34,31 Q35,30 37,30 H46 Q48,30 49,31 L54.5,38.5 L58.5,40 Q61,40.5 61,43 V49 H56 M45,49 H36 M25,49 H20", None),
        ("M33.5,38 L37.5,32.5 H44.5 L48.5,38 Z", None),
        ("M25.5,49.5 A5,5 0 1 0 35.5,49.5 A5,5 0 1 0 25.5,49.5 Z", None),
        ("M45.5,49.5 A5,5 0 1 0 55.5,49.5 A5,5 0 1 0 45.5,49.5 Z", None),
    ],
    # C: the letter P in a circle, as the sign on a car park.
    "C": [
        ("M25.5,40.5 A15,15 0 1 0 55.5,40.5 A15,15 0 1 0 25.5,40.5 Z", None),
        ("M36,50 V31 M36,31 H42.5 A5.5,5.5 0 0 1 42.5,42 H36", None),
    ],
}

def svg(letter):
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 81 81" width="81" height="81">',
             '<rect width="81" height="81" fill="#fff"/>',
             f'<path d="{RING}" fill="#000" fill-rule="evenodd"/>']
    for d, _ in GLYPHS[letter]:
        parts.append(f'<path d="{d}" fill="none" stroke="#000" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>')
    parts.append("</svg>")
    return "\n".join(parts)

def vector(letter):
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           '<!-- Parked: launcher icon in the Kompakt stock style (81 grid, ring, glyph stroke 2.5). PLACEHOLDER until chosen. -->',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    android:width="81dp" android:height="81dp" android:viewportWidth="81" android:viewportHeight="81">',
           f'    <path android:pathData="{RING}" android:fillColor="#FF000000" android:fillType="evenOdd"/>']
    for d, _ in GLYPHS[letter]:
        out.append(f'    <path android:pathData="{d}" android:fillColor="#00000000" android:strokeColor="#FF000000" '
                   'android:strokeWidth="2.5" android:strokeLineCap="round" android:strokeLineJoin="round"/>')
    out.append("</vector>")
    return "\n".join(out) + "\n"

os.makedirs(OUT, exist_ok=True)
for k in GLYPHS:
    p = os.path.join(OUT, f"icon-{k}.svg")
    open(p, "w").write(svg(k))
    for px in (512, 72):
        subprocess.run(["rsvg-convert", "-w", str(px), "-h", str(px), p, "-o", os.path.join(OUT, f"icon-{k}-{px}.png")], check=True)
res = os.path.join(HERE, "..", "app", "src", "main", "res", "drawable", "ic_launcher_mudita.xml")
open(res, "w").write(vector(PICK))
