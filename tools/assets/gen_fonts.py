#!/usr/bin/env python3
"""Erzeugt die Bitmap-Schriften (AngelCode-BMFont, Textformat) für jMonkeyEngine.

Die Glyphen werden weiß gerendert und von jME über die Textfarbe eingefärbt.
Kontur und Schatten sind dunkel eingebrannt und bleiben deshalb beim Einfärben
erhalten – so bleibt Text auch über der 3D-Szene gut lesbar.

Aufruf:  python3 tools/assets/gen_fonts.py
Quellen: tools/assets/fonts (Lilita One, Nunito – SIL Open Font License 1.1)
Ziel:    view/src/main/resources/fonts
"""
from __future__ import annotations

import os
from dataclasses import dataclass

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(HERE, "fonts")
OUT = os.path.join(ROOT, "view", "src", "main", "resources", "fonts")

CHARSET = (
        [chr(c) for c in range(32, 127)]
        + [chr(c) for c in range(160, 256)]
        + list("–—‘’‚“”„…•€™←→↑↓✓✗★·")
)


@dataclass
class FontSpec:
    name: str
    file: str
    size: int
    variation: str | None = None
    outline: int = 0
    shadow: int = 0
    padding: int = 2


SPECS = [
    FontSpec("title", "LilitaOne-Regular.ttf", 96, outline=7, shadow=6),
    FontSpec("heading", "LilitaOne-Regular.ttf", 52, outline=4, shadow=3),
    FontSpec("ui", "Nunito-Variable.ttf", 34, variation="ExtraBold", outline=3, shadow=2),
    FontSpec("body", "Nunito-Variable.ttf", 30, variation="SemiBold"),
]

OUTLINE_COLOR = (34, 16, 10, 255)
SHADOW_COLOR = (0, 0, 0, 150)


def load_font(spec: FontSpec) -> ImageFont.FreeTypeFont:
    font = ImageFont.truetype(os.path.join(SRC, spec.file), spec.size)
    if spec.variation:
        font.set_variation_by_name(spec.variation)
    return font


def missing_glyph_signature(font: ImageFont.FreeTypeFont) -> bytes:
    """Bitmap der .notdef-Glyphe, um fehlende Zeichen zu erkennen."""
    return bytes(font.getmask(""))


def render_glyph(font: ImageFont.FreeTypeFont, spec: FontSpec, ch: str):
    """Rendert eine Glyphe mit Kontur und Schatten.

    @return (Bild, x-Versatz, y-Versatz) relativ zum Zeilenursprung oder None für Leerzeichen
    """
    bbox = font.getbbox(ch, stroke_width=spec.outline)
    if bbox[2] - bbox[0] <= 0 or bbox[3] - bbox[1] <= 0:
        return None
    extra = spec.outline + spec.shadow + spec.padding + (spec.shadow // 2 + 1 if spec.shadow else 0)
    w = bbox[2] - bbox[0] + 2 * extra
    h = bbox[3] - bbox[1] + 2 * extra
    ox, oy = extra - bbox[0], extra - bbox[1]

    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    if spec.shadow:
        shadow = Image.new("L", (w, h), 0)
        ImageDraw.Draw(shadow).text((ox + spec.shadow * 0.6, oy + spec.shadow), ch, font=font, fill=255,
                                    stroke_width=spec.outline, stroke_fill=255)
        shadow = shadow.filter(ImageFilter.GaussianBlur(spec.shadow * 0.45))
        layer = Image.new("RGBA", (w, h), SHADOW_COLOR[:3] + (0,))
        layer.putalpha(shadow.point(lambda a: a * SHADOW_COLOR[3] // 255))
        img = Image.alpha_composite(img, layer)
    if spec.outline:
        outline = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        ImageDraw.Draw(outline).text((ox, oy), ch, font=font, fill=OUTLINE_COLOR,
                                     stroke_width=spec.outline, stroke_fill=OUTLINE_COLOR)
        img = Image.alpha_composite(img, outline)
    fill = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(fill).text((ox, oy), ch, font=font, fill=(255, 255, 255, 255))
    img = Image.alpha_composite(img, fill)
    # Ränder ohne Inhalt abschneiden (spart Atlasfläche)
    alpha_box = img.getchannel("A").getbbox()
    if alpha_box is None:
        return None
    img = img.crop(alpha_box)
    return img, alpha_box[0] - ox, alpha_box[1] - oy


def pack(glyphs: dict[str, tuple[Image.Image, int, int]], spacing: int = 2):
    """Einfacher Regal-Packer; liefert Atlasgröße und Positionen."""
    order = sorted(glyphs, key=lambda c: -glyphs[c][0].height)
    for size in [(256, 256), (512, 256), (512, 512), (1024, 512), (1024, 1024), (2048, 1024), (2048, 2048)]:
        W, H = size
        x = y = shelf = 0
        pos = {}
        ok = True
        for c in order:
            g = glyphs[c][0]
            if x + g.width + spacing > W:
                x, y, shelf = 0, y + shelf + spacing, 0
            if y + g.height > H:
                ok = False
                break
            pos[c] = (x, y)
            x += g.width + spacing
            shelf = max(shelf, g.height)
        if ok:
            return size, pos
    raise RuntimeError("Atlas zu klein")


def kerning_pairs(font: ImageFont.FreeTypeFont, chars: list[str]):
    letters = [c for c in chars if c.isalnum() and ord(c) < 128] + list(".,:;!?-'")
    pairs = []
    widths = {c: font.getlength(c) for c in letters}
    for a in letters:
        for b in letters:
            amount = round(font.getlength(a + b) - widths[a] - widths[b])
            if amount != 0:
                pairs.append((ord(a), ord(b), amount))
    return pairs


def generate(spec: FontSpec):
    font = load_font(spec)
    missing = missing_glyph_signature(font)
    ascent, descent = font.getmetrics()
    glyphs = {}
    advances = {}
    for ch in CHARSET:
        if ch != " " and ord(ch) != 160 and bytes(font.getmask(ch)) == missing:
            continue
        advances[ch] = round(font.getlength(ch)) + spec.outline
        rendered = render_glyph(font, spec, ch)
        if rendered is not None:
            glyphs[ch] = rendered
    (W, H), pos = pack(glyphs)
    atlas = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for ch, (img, _, _) in glyphs.items():
        atlas.paste(img, pos[ch])
    os.makedirs(OUT, exist_ok=True)
    atlas.save(os.path.join(OUT, spec.name + ".png"), optimize=True)

    line_height = ascent + descent + spec.outline
    lines = [
        f'info face="{spec.name}" size={spec.size} bold=0 italic=0 charset="" unicode=1 stretchH=100 '
        f'smooth=1 aa=1 padding=0,0,0,0 spacing=2,2',
        f"common lineHeight={line_height} base={ascent} scaleW={W} scaleH={H} pages=1 packed=0",
        f'page id=0 file="{spec.name}.png"',
        f"chars count={len(advances)}",
    ]
    for ch, adv in advances.items():
        if ch in glyphs:
            img, dx, dy = glyphs[ch]
            x, y = pos[ch]
            lines.append(f"char id={ord(ch)} x={x} y={y} width={img.width} height={img.height} "
                         f"xoffset={dx} yoffset={dy} xadvance={adv} page=0 chnl=15")
        else:
            lines.append(f"char id={ord(ch)} x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 "
                         f"xadvance={adv} page=0 chnl=15")
    kern = kerning_pairs(font, list(advances))
    lines.append(f"kernings count={len(kern)}")
    lines += [f"kerning first={a} second={b} amount={k}" for a, b, k in kern]
    with open(os.path.join(OUT, spec.name + ".fnt"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"{spec.name}: {len(advances)} Zeichen, Atlas {W}x{H}, {len(kern)} Kerning-Paare")


if __name__ == "__main__":
    for s in SPECS:
        generate(s)
