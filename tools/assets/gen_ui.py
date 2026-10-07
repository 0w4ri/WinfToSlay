#!/usr/bin/env python3
"""Erzeugt Oberflächen- und Effekttexturen für WinfToSlay (prozedural, ohne fremde Grafiken).

Alle Bilder werden mit doppelter Auflösung gezeichnet und im Spiel mit Faktor 0,5
skaliert, damit Kanten auch auf hochauflösenden Displays sauber bleiben.

Aufruf:  python3 tools/assets/gen_ui.py
Ziel:    view/src/main/resources/{ui,fx,dice}
"""
from __future__ import annotations

import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "view", "src", "main", "resources")
UI = os.path.join(RES, "ui")
ICONS = os.path.join(UI, "icons")
FX = os.path.join(RES, "fx")
DICE = os.path.join(RES, "dice")
TABLE = os.path.join(RES, "textures", "table")

# Farbpalette (passend zum Logo: Ziegelrot, Gold, Creme, dunkle Kontur)
OUTLINE = (30, 15, 9)
WOOD_DARK = (36, 22, 15)
WOOD = (58, 36, 24)
WOOD_LIGHT = (84, 54, 34)
GOLD = (242, 179, 61)
GOLD_DARK = (176, 112, 30)
RED = (170, 42, 32)
RED_DARK = (112, 24, 18)
GREEN = (62, 142, 65)
GREEN_DARK = (34, 88, 38)
CREAM = (244, 235, 217)

SS = 4  # Supersampling für weiche Kanten


def save(img: Image.Image, folder: str, name: str):
    os.makedirs(folder, exist_ok=True)
    img.save(os.path.join(folder, name), optimize=True)


def rounded_mask(w: int, h: int, r: float, inset: float = 0) -> Image.Image:
    """Antialiasierte Maske eines abgerundeten Rechtecks."""
    big = Image.new("L", (w * SS, h * SS), 0)
    ImageDraw.Draw(big).rounded_rectangle(
        (inset * SS, inset * SS, (w - inset) * SS - 1, (h - inset) * SS - 1), radius=max(0, r - inset) * SS, fill=255)
    return big.resize((w, h), Image.LANCZOS)


def vertical_gradient(w: int, h: int, top, bottom) -> Image.Image:
    t = np.linspace(0, 1, h)[:, None, None]
    arr = (np.array(top, float)[None, None, :] * (1 - t) + np.array(bottom, float)[None, None, :] * t)
    arr = np.repeat(arr, w, axis=1)
    return Image.fromarray(arr.clip(0, 255).astype(np.uint8), "RGB")


def noise(w: int, h: int, scale: float, seed: int = 1) -> np.ndarray:
    """Weiches Wertrauschen (mehrere Oktaven) im Bereich 0..1."""
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w))
    amp, total = 1.0, 0.0
    for octave in range(5):
        cells = max(2, int(scale * (2 ** octave)))
        grid = rng.random((cells + 1, cells + 1))
        img = Image.fromarray((grid * 255).astype(np.uint8), "L").resize((w, h), Image.BICUBIC)
        out += np.asarray(img, float) / 255 * amp
        total += amp
        amp *= 0.5
    return out / total


def compose_panel(w, h, r, fill_top, fill_bottom, rim, rim_width, outline_width=3, grain=0.0, inner_glow=None,
                  highlight=0.18):
    """Allgemeiner Baustein für Panels und Buttons."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    outer = rounded_mask(w, h, r)
    dark = Image.new("RGBA", (w, h), OUTLINE + (255,))
    img.paste(dark, (0, 0), outer)
    rim_mask = rounded_mask(w, h, r, outline_width)
    img.paste(Image.new("RGBA", (w, h), rim + (255,)), (0, 0), rim_mask)
    inner = rounded_mask(w, h, r, outline_width + rim_width)
    body = vertical_gradient(w, h, fill_top, fill_bottom).convert("RGBA")
    if grain:
        n = noise(w, h, 3, seed=w * 7 + h)
        streaks = noise(w, max(2, h // 8), 1.5, seed=w + 3)
        streaks = np.asarray(Image.fromarray((streaks * 255).astype(np.uint8)).resize((w, h)), float) / 255
        mod = 1 + grain * ((n - 0.5) * 0.8 + (streaks - 0.5) * 1.2)
        arr = np.asarray(body, float)
        arr[..., :3] *= mod[..., None]
        body = Image.fromarray(arr.clip(0, 255).astype(np.uint8), "RGBA")
    img.paste(body, (0, 0), inner)
    # Glanzkante oben
    if highlight:
        hl = Image.new("L", (w, h), 0)
        d = ImageDraw.Draw(hl)
        top = outline_width + rim_width
        d.rounded_rectangle((top, top, w - top - 1, top + max(4, h * 0.45)), radius=max(1, r - top),
                            fill=int(255 * highlight))
        hl = hl.filter(ImageFilter.GaussianBlur(3))
        hl = Image.fromarray(np.minimum(np.asarray(hl), np.asarray(inner)).astype(np.uint8))
        img = Image.alpha_composite(img, Image.merge("RGBA", [Image.new("L", (w, h), 255)] * 3 + [hl]))
    if inner_glow:
        glow = Image.new("L", (w, h), 0)
        ImageDraw.Draw(glow).rounded_rectangle((0, 0, w - 1, h - 1), radius=r, outline=255,
                                               width=outline_width + rim_width + 6)
        glow = glow.filter(ImageFilter.GaussianBlur(6))
        glow = Image.fromarray(np.minimum(np.asarray(glow), np.asarray(inner)).astype(np.uint8))
        layer = Image.new("RGBA", (w, h), inner_glow[:3] + (0,))
        layer.putalpha(glow.point(lambda a: a * inner_glow[3] // 255))
        img = Image.alpha_composite(img, layer)
    return img


def drop_shadow(img: Image.Image, pad: int, blur: float, alpha: int, offset=(0, 4)) -> Image.Image:
    w, h = img.size
    out = Image.new("RGBA", (w + 2 * pad, h + 2 * pad), (0, 0, 0, 0))
    a = img.getchannel("A").point(lambda v: v * alpha // 255)
    sh = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sh.putalpha(a)
    out.paste(sh, (pad + offset[0], pad + offset[1]), sh)
    out = out.filter(ImageFilter.GaussianBlur(blur))
    out.alpha_composite(img, (pad, pad))
    return out


# ----------------------------------------------------------------------
# Oberfläche
# ----------------------------------------------------------------------

def gen_panels():
    panel = compose_panel(192, 192, 34, WOOD_LIGHT, WOOD_DARK, GOLD_DARK, 4, outline_width=4, grain=0.35,
                          inner_glow=(0, 0, 0, 160), highlight=0.06)
    # Innenlinie (goldene Zierlinie)
    d = ImageDraw.Draw(panel)
    d.rounded_rectangle((16, 16, 175, 175), radius=22, outline=GOLD + (70,), width=2)
    save(panel, UI, "panel.png")

    dark = compose_panel(128, 128, 26, (34, 24, 20), (20, 13, 10), (88, 62, 38), 3, outline_width=3, grain=0.15,
                         highlight=0.04)
    save(dark, UI, "panel_dark.png")

    glass = Image.new("RGBA", (96, 96), (0, 0, 0, 0))
    m = rounded_mask(96, 96, 22)
    glass.paste(Image.new("RGBA", (96, 96), (14, 9, 7, 205)), (0, 0), m)
    rim = Image.new("L", (96, 96), 0)
    ImageDraw.Draw(rim).rounded_rectangle((1, 1, 94, 94), radius=21, outline=255, width=2)
    layer = Image.new("RGBA", (96, 96), GOLD + (0,))
    layer.putalpha(rim.point(lambda a: a * 90 // 255))
    save(Image.alpha_composite(glass, layer), UI, "panel_glass.png")

    # Tooltip / Toast
    toast = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    toast.paste(Image.new("RGBA", (64, 64), (10, 6, 4, 225)), (0, 0), rounded_mask(64, 64, 16))
    save(toast, UI, "toast.png")


def gen_buttons():
    variants = {
        "primary": (RED, RED_DARK, GOLD),
        "secondary": (WOOD_LIGHT, WOOD_DARK, (150, 110, 60)),
        "success": (GREEN, GREEN_DARK, GOLD),
        "gold": (GOLD, GOLD_DARK, (255, 226, 150)),
    }
    for name, (top, bottom, rim) in variants.items():
        def shade(c, f):
            return tuple(min(255, int(v * f)) for v in c)

        normal = compose_panel(128, 96, 30, shade(top, 1.08), bottom, rim, 4, grain=0.12, highlight=0.22)
        hover = compose_panel(128, 96, 30, shade(top, 1.3), shade(bottom, 1.25), shade(rim, 1.15), 4, grain=0.12,
                              highlight=0.3, inner_glow=(255, 230, 160, 120))
        pressed = compose_panel(128, 96, 30, shade(bottom, 0.95), shade(top, 0.9), shade(rim, 0.85), 4, grain=0.12,
                                highlight=0.0, inner_glow=(0, 0, 0, 150))
        save(normal, UI, f"button_{name}.png")
        save(hover, UI, f"button_{name}_hover.png")
        save(pressed, UI, f"button_{name}_pressed.png")
    disabled = compose_panel(128, 96, 30, (74, 66, 60), (48, 42, 38), (96, 88, 80), 4, highlight=0.08)
    save(disabled, UI, "button_disabled.png")

    # Rundes Symbol-Button
    for name, (top, bottom, rim) in {"round": (WOOD_LIGHT, WOOD_DARK, GOLD_DARK),
                                     "round_hover": ((120, 80, 50), (70, 44, 28), GOLD)}.items():
        save(compose_panel(96, 96, 48, top, bottom, rim, 4, highlight=0.2), UI, f"button_{name}.png")


def gen_fields():
    field = Image.new("RGBA", (96, 80), (0, 0, 0, 0))
    field.paste(Image.new("RGBA", (96, 80), OUTLINE + (255,)), (0, 0), rounded_mask(96, 80, 18))
    field.paste(Image.new("RGBA", (96, 80), (110, 80, 50, 255)), (0, 0), rounded_mask(96, 80, 18, 3))
    body = vertical_gradient(96, 80, (16, 10, 8), (32, 22, 16)).convert("RGBA")
    field.paste(body, (0, 0), rounded_mask(96, 80, 18, 5))
    save(field, UI, "field.png")
    focus = field.copy()
    ImageDraw.Draw(focus).rounded_rectangle((3, 3, 92, 76), radius=16, outline=GOLD + (255,), width=3)
    save(focus, UI, "field_focus.png")

    # Schieberegler
    track = Image.new("RGBA", (64, 28), (0, 0, 0, 0))
    track.paste(Image.new("RGBA", (64, 28), OUTLINE + (255,)), (0, 0), rounded_mask(64, 28, 14))
    track.paste(Image.new("RGBA", (64, 28), (20, 13, 10, 255)), (0, 0), rounded_mask(64, 28, 14, 3))
    save(track, UI, "slider_track.png")
    fill = Image.new("RGBA", (64, 28), (0, 0, 0, 0))
    fill.paste(vertical_gradient(64, 28, (255, 205, 100), GOLD_DARK).convert("RGBA"), (0, 0),
               rounded_mask(64, 28, 14, 3))
    save(fill, UI, "slider_fill.png")
    thumb = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    big = Image.new("RGBA", (64 * SS, 64 * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    d.ellipse((4 * SS, 6 * SS, 60 * SS, 62 * SS), fill=(0, 0, 0, 120))
    d.ellipse((4 * SS, 2 * SS, 60 * SS, 58 * SS), fill=OUTLINE + (255,))
    d.ellipse((8 * SS, 6 * SS, 56 * SS, 54 * SS), fill=GOLD + (255,))
    d.ellipse((14 * SS, 10 * SS, 46 * SS, 30 * SS), fill=(255, 235, 180, 160))
    thumb = big.resize((64, 64), Image.LANCZOS)
    save(thumb, UI, "slider_thumb.png")

    # Checkbox
    for name, checked in (("check_off", False), ("check_on", True)):
        box = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        box.paste(Image.new("RGBA", (64, 64), OUTLINE + (255,)), (0, 0), rounded_mask(64, 64, 14))
        box.paste(Image.new("RGBA", (64, 64), (110, 80, 50, 255)), (0, 0), rounded_mask(64, 64, 14, 3))
        box.paste(Image.new("RGBA", (64, 64), (20, 13, 10, 255) if not checked else GREEN + (255,)), (0, 0),
                  rounded_mask(64, 64, 14, 6))
        if checked:
            big = Image.new("RGBA", (64 * SS, 64 * SS), (0, 0, 0, 0))
            ImageDraw.Draw(big).line([(16 * SS, 33 * SS), (28 * SS, 45 * SS), (49 * SS, 20 * SS)],
                                     fill=CREAM + (255,), width=7 * SS, joint="curve")
            box.alpha_composite(big.resize((64, 64), Image.LANCZOS))
        save(box, UI, f"{name}.png")


def gen_decor():
    # Goldene Trennlinie mit Raute
    w, h = 512, 32
    big = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    cy = h * SS // 2
    for x in range(0, w * SS):
        t = abs(x / (w * SS) - 0.5) * 2
        a = int(255 * max(0.0, 1 - t ** 1.6))
        d.line([(x, cy - SS), (x, cy + SS)], fill=GOLD + (a,))
    s = 9 * SS
    d.polygon([(w * SS // 2, cy - s), (w * SS // 2 + s, cy), (w * SS // 2, cy + s), (w * SS // 2 - s, cy)],
              fill=GOLD + (255,), outline=OUTLINE + (255,))
    save(big.resize((w, h), Image.LANCZOS), UI, "divider.png")

    # Rotes Band für Banner (Zugwechsel, Titel)
    w, h = 1024, 200
    big = Image.new("RGBA", (w * 2, h * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    W, H = w * 2, h * 2
    fold = 70
    tail = [(0, 70), (fold * 2 + 40, 70), (fold * 2 + 40, H - 30), (0, H - 30), (70, (H + 40) // 2)]
    d.polygon(tail, fill=RED_DARK + (255,), outline=OUTLINE + (255,), width=10)
    d.polygon([(W - x, y) for x, y in tail], fill=RED_DARK + (255,), outline=OUTLINE + (255,), width=10)
    d.polygon([(fold * 2, 20), (W - fold * 2, 20), (W - fold * 2, H - 80), (fold * 2, H - 80)], fill=RED + (255,),
              outline=OUTLINE + (255,), width=10)
    d.line([(fold * 2 + 20, 48), (W - fold * 2 - 20, 48)], fill=GOLD + (200,), width=6)
    d.line([(fold * 2 + 20, H - 108), (W - fold * 2 - 20, H - 108)], fill=GOLD + (200,), width=6)
    save(big.resize((w, h), Image.LANCZOS), UI, "ribbon.png")

    # Aktionspunkt-Edelstein
    for name, filled in (("ap_full", True), ("ap_empty", False)):
        big = Image.new("RGBA", (64 * SS, 64 * SS), (0, 0, 0, 0))
        d = ImageDraw.Draw(big)
        c = 32 * SS
        pts = [(c, 3 * SS), (61 * SS, c), (c, 61 * SS), (3 * SS, c)]
        d.polygon(pts, fill=OUTLINE + (255,))
        inner = [(c, 9 * SS), (55 * SS, c), (c, 55 * SS), (9 * SS, c)]
        if filled:
            d.polygon(inner, fill=GOLD + (255,))
            d.polygon([(c, 9 * SS), (55 * SS, c), (c, c)], fill=(255, 220, 130, 255))
            d.polygon([(c, 55 * SS), (9 * SS, c), (c, c)], fill=GOLD_DARK + (255,))
            d.ellipse((22 * SS, 16 * SS, 32 * SS, 26 * SS), fill=(255, 255, 240, 220))
        else:
            d.polygon(inner, fill=(46, 34, 26, 255))
            d.polygon([(c, 55 * SS), (9 * SS, c), (c, c)], fill=(30, 22, 16, 255))
        save(big.resize((64, 64), Image.LANCZOS), UI, f"{name}.png")

    # Vignette für den Bildschirmrand
    n = 512
    y, x = np.mgrid[0:n, 0:n] / (n - 1) * 2 - 1
    r = np.sqrt(x ** 2 + y ** 2) / math.sqrt(2)
    a = np.clip((r - 0.45) / 0.55, 0, 1) ** 1.8 * 235
    vig = np.zeros((n, n, 4), np.uint8)
    vig[..., 3] = a.astype(np.uint8)
    save(Image.fromarray(vig, "RGBA"), UI, "vignette.png")

    # Weißer Pixel (für einfarbige Flächen)
    save(Image.new("RGBA", (4, 4), (255, 255, 255, 255)), UI, "white.png")


def gen_icons():
    codepoints = {}
    with open(os.path.join(HERE, "fonts", "MaterialIcons-Regular.codepoints"), encoding="utf-8") as f:
        for line in f:
            name, cp = line.split()
            codepoints.setdefault(name, cp)
    wanted = ["settings", "help_outline", "close", "person", "smart_toy", "star", "add", "remove", "delete",
              "play_arrow", "group", "lan", "logout", "volume_up", "music_note", "speed", "casino", "style",
              "shield", "bolt", "military_tech", "emoji_events", "local_fire_department", "auto_awesome",
              "visibility", "refresh", "arrow_back", "check", "content_copy", "mouse", "menu_book", "favorite",
              "pets", "back_hand", "layers", "flag", "hourglass_empty", "school", "account_circle",
              "sports_martial_arts", "flash_on", "block", "home", "info", "warning", "lightbulb", "skip_next",
              "autorenew", "touch_app", "computer", "public", "schedule", "replay", "exit_to_app", "tune",
              "videogame_asset", "wifi_tethering", "arrow_forward", "chevron_left", "chevron_right"]
    font = ImageFont.truetype(os.path.join(HERE, "fonts", "MaterialIcons-Regular.ttf"), 96)
    for name in wanted:
        cp = codepoints.get(name)
        if cp is None:
            print("Symbol fehlt:", name)
            continue
        img = Image.new("RGBA", (112, 112), (0, 0, 0, 0))
        ImageDraw.Draw(img).text((56, 56), chr(int(cp, 16)), font=font, fill=(255, 255, 255, 255), anchor="mm")
        save(img, ICONS, name + ".png")


# ----------------------------------------------------------------------
# Effekte
# ----------------------------------------------------------------------

def radial(n, power=2.0, core=0.0):
    y, x = np.mgrid[0:n, 0:n] / (n - 1) * 2 - 1
    r = np.sqrt(x ** 2 + y ** 2)
    a = np.clip(1 - r, 0, 1) ** power
    if core:
        a = np.maximum(a, np.clip(1 - r / core, 0, 1))
    return a


def gray_alpha(alpha: np.ndarray, color=(255, 255, 255)) -> Image.Image:
    h, w = alpha.shape
    arr = np.zeros((h, w, 4), np.uint8)
    arr[..., 0], arr[..., 1], arr[..., 2] = color
    arr[..., 3] = (np.clip(alpha, 0, 1) * 255).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def sheet(images, cols, rows):
    w, h = images[0].size
    out = Image.new("RGBA", (w * cols, h * rows), (0, 0, 0, 0))
    for i, im in enumerate(images):
        out.paste(im, ((i % cols) * w, (i // cols) * h))
    return out


def gen_fx():
    save(gray_alpha(radial(128, 2.2)), FX, "glow.png")
    save(gray_alpha(radial(128, 1.0, core=0.15) * 0.9), FX, "soft.png")

    # Funke: Stern mit vier Strahlen
    n = 128
    y, x = np.mgrid[0:n, 0:n] / (n - 1) * 2 - 1
    r = np.sqrt(x ** 2 + y ** 2)
    rays = np.exp(-np.abs(x) * 26) * np.clip(1 - np.abs(y), 0, 1) ** 2 + \
           np.exp(-np.abs(y) * 26) * np.clip(1 - np.abs(x), 0, 1) ** 2
    spark = np.clip(rays * 0.9 + np.clip(1 - r * 3.2, 0, 1) ** 1.5, 0, 1)
    save(gray_alpha(spark), FX, "spark.png")

    # Rauch (2x2 Atlas)
    puffs = []
    for i in range(4):
        n = 128
        base = radial(n, 1.4)
        nz = noise(n, n, 3, seed=11 + i)
        a = np.clip(base * (0.55 + nz * 0.9) - 0.08, 0, 1)
        puffs.append(gray_alpha(a))
    save(sheet(puffs, 2, 2), FX, "smoke.png")

    # Flammen (2x2 Atlas)
    flames = []
    for i in range(4):
        n = 128
        y, x = np.mgrid[0:n, 0:n] / (n - 1)
        nz = noise(n, n, 4, seed=31 + i)
        cx = 0.5 + (nz - 0.5) * 0.25
        width = 0.32 * (1 - y) ** 0.5 + 0.02
        shape = np.clip(1 - np.abs(x - cx) / np.maximum(width, 1e-3), 0, 1)
        vertical = np.clip(y * 1.4, 0, 1) * np.clip((1 - y) * 4, 0, 1)
        a = np.clip(shape ** 1.2 * vertical * (0.7 + nz * 0.6), 0, 1)
        flames.append(gray_alpha(a[::-1]))
    save(sheet(flames, 2, 2), FX, "flame.png")

    # Schockwellen-Ring
    n = 256
    y, x = np.mgrid[0:n, 0:n] / (n - 1) * 2 - 1
    r = np.sqrt(x ** 2 + y ** 2)
    ring = np.exp(-((r - 0.82) / 0.07) ** 2) + 0.35 * np.exp(-((r - 0.7) / 0.18) ** 2)
    save(gray_alpha(np.clip(ring * (r < 1), 0, 1)), FX, "ring.png")

    # Trümmer (2x2 Atlas): kantige Splitter
    rng = random.Random(5)
    chunks = []
    for i in range(4):
        big = Image.new("RGBA", (64 * SS, 64 * SS), (0, 0, 0, 0))
        d = ImageDraw.Draw(big)
        pts = []
        k = rng.randint(5, 7)
        for j in range(k):
            ang = j / k * math.tau + rng.uniform(-0.3, 0.3)
            rad = rng.uniform(14, 28) * SS
            pts.append((32 * SS + math.cos(ang) * rad, 32 * SS + math.sin(ang) * rad))
        d.polygon(pts, fill=(255, 255, 255, 255))
        chunks.append(big.resize((64, 64), Image.LANCZOS))
    save(sheet(chunks, 2, 2), FX, "debris.png")

    # Konfetti (2x2 Atlas)
    conf = []
    for i in range(4):
        big = Image.new("RGBA", (64 * SS, 64 * SS), (0, 0, 0, 0))
        d = ImageDraw.Draw(big)
        if i == 0:
            d.rectangle((18 * SS, 26 * SS, 46 * SS, 38 * SS), fill=(255, 255, 255, 255))
        elif i == 1:
            d.ellipse((20 * SS, 20 * SS, 44 * SS, 44 * SS), fill=(255, 255, 255, 255))
        elif i == 2:
            d.polygon([(32 * SS, 12 * SS), (52 * SS, 50 * SS), (12 * SS, 50 * SS)], fill=(255, 255, 255, 255))
        else:
            d.arc((8 * SS, 16 * SS, 56 * SS, 64 * SS), 200, 340, fill=(255, 255, 255, 255), width=8 * SS)
        conf.append(big.resize((64, 64), Image.LANCZOS))
    save(sheet(conf, 2, 2), FX, "confetti.png")

    # Magischer Kreis (für Heldeneffekte)
    n = 512
    big = Image.new("RGBA", (n * 2, n * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    c = n
    for rad, wd in ((n - 20, 10), (n - 70, 5), (n * 0.55, 6), (n * 0.5, 3)):
        d.ellipse((c - rad, c - rad, c + rad, c + rad), outline=(255, 255, 255, 255), width=wd)
    # Rauten auf dem äußeren Ring, Speichen zwischen den inneren Ringen
    for i in range(24):
        ang = i / 24 * math.tau
        px, py = c + math.cos(ang) * (n - 45), c + math.sin(ang) * (n - 45)
        s = 16 if i % 2 == 0 else 9
        d.polygon([(px + math.cos(ang) * s, py + math.sin(ang) * s),
                   (px + math.cos(ang + math.pi / 2) * s * 0.55, py + math.sin(ang + math.pi / 2) * s * 0.55),
                   (px - math.cos(ang) * s, py - math.sin(ang) * s),
                   (px - math.cos(ang + math.pi / 2) * s * 0.55, py - math.sin(ang + math.pi / 2) * s * 0.55)],
                  fill=(255, 255, 255, 255))
    for i in range(12):
        ang = i / 12 * math.tau
        r0, r1 = n * 0.55, n - 70
        d.line([(c + math.cos(ang) * r0, c + math.sin(ang) * r0), (c + math.cos(ang) * r1, c + math.sin(ang) * r1)],
               fill=(255, 255, 255, 200), width=4)
    for i in range(3):
        ang = i / 3 * math.tau - math.pi / 2
        px, py = c + math.cos(ang) * n * 0.3, c + math.sin(ang) * n * 0.3
        d.ellipse((px - 26, py - 26, px + 26, py + 26), outline=(255, 255, 255, 255), width=5)
    d.ellipse((c - 34, c - 34, c + 34, c + 34), fill=(255, 255, 255, 255))
    img = big.resize((n, n), Image.LANCZOS)
    glow = img.filter(ImageFilter.GaussianBlur(6))
    save(Image.alpha_composite(glow, img), FX, "magic_circle.png")

    # Kratzspuren (Monsterangriff)
    w, h = 256, 256
    big = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    for k in range(3):
        off = (k - 1) * 46 * SS
        pts = []
        for t in np.linspace(0, 1, 40):
            x = (40 + t * 170) * SS + off * 0.3
            y = (30 + t * 196) * SS + off + math.sin(t * math.pi) * 14 * SS
            pts.append((x, y))
        for i in range(len(pts) - 1):
            t = i / (len(pts) - 1)
            wd = int((2 + 18 * math.sin(t * math.pi)) * SS)
            d.line([pts[i], pts[i + 1]], fill=(255, 255, 255, 255), width=max(1, wd))
    img = big.resize((w, h), Image.LANCZOS)
    save(Image.alpha_composite(img.filter(ImageFilter.GaussianBlur(5)), img), FX, "slash.png")

    # Lichtstrahl (vertikal, für Auswahl und Blitz)
    n = 128
    y, x = np.mgrid[0:n, 0:n] / (n - 1)
    beam = np.exp(-((x - 0.5) / 0.16) ** 2) * np.clip(np.sin(y * math.pi) * 1.4, 0, 1)
    save(gray_alpha(beam), FX, "beam.png")

    # Leuchtender Ring unter dem aktiven Spieler (elliptisch gedacht, im Spiel gestreckt)
    n = 256
    y, x = np.mgrid[0:n, 0:n] / (n - 1) * 2 - 1
    r = np.sqrt(x ** 2 + y ** 2)
    seat = np.exp(-((r - 0.86) / 0.06) ** 2) + 0.25 * np.clip(1 - r, 0, 1) ** 0.6 * (r < 0.9)
    save(gray_alpha(np.clip(seat, 0, 1)), FX, "seat_glow.png")

    # Platzhalter für Kartenplätze (gestrichelter Rahmen)
    w, h = 140, 196
    big = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    d.rounded_rectangle((6 * SS, 6 * SS, (w - 6) * SS, (h - 6) * SS), radius=12 * SS, fill=(255, 255, 255, 36))
    dash = 14 * SS
    per = []
    x0, y0, x1, y1 = 8 * SS, 8 * SS, (w - 8) * SS, (h - 8) * SS
    for x in range(x0 + 12 * SS, x1 - 12 * SS, dash * 2):
        per += [((x, y0), (min(x + dash, x1 - 12 * SS), y0)), ((x, y1), (min(x + dash, x1 - 12 * SS), y1))]
    for y in range(y0 + 12 * SS, y1 - 12 * SS, dash * 2):
        per += [((x0, y), (x0, min(y + dash, y1 - 12 * SS))), ((x1, y), (x1, min(y + dash, y1 - 12 * SS)))]
    for a, b in per:
        d.line([a, b], fill=(255, 255, 255, 170), width=3 * SS)
    for cx, cy, s, e in ((x0 + 12 * SS, y0 + 12 * SS, 180, 270), (x1 - 12 * SS, y0 + 12 * SS, 270, 360),
                         (x1 - 12 * SS, y1 - 12 * SS, 0, 90), (x0 + 12 * SS, y1 - 12 * SS, 90, 180)):
        d.arc((cx - 12 * SS, cy - 12 * SS, cx + 12 * SS, cy + 12 * SS), s, e, fill=(255, 255, 255, 170),
              width=3 * SS)
    save(big.resize((w, h), Image.LANCZOS), FX, "slot.png")

    # Kartenglühen (Rahmen, der hinter einer Karte leuchtet)
    w, h = 200, 256
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle((30, 30, w - 30, h - 30), radius=18, fill=255)
    m = m.filter(ImageFilter.GaussianBlur(13))
    save(gray_alpha(np.asarray(m, float) / 255), FX, "card_glow.png")

    # Pfeil (Zielauswahl)
    w, h = 128, 128
    big = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    d.polygon([(64 * SS, 120 * SS), (16 * SS, 60 * SS), (44 * SS, 60 * SS), (44 * SS, 8 * SS), (84 * SS, 8 * SS),
               (84 * SS, 60 * SS), (112 * SS, 60 * SS)], fill=(255, 255, 255, 255), outline=OUTLINE + (255,),
              width=5 * SS)
    save(big.resize((w, h), Image.LANCZOS), FX, "arrow.png")

    # Mündungsfeuer (Hubschrauber)
    n = 128
    y, x = np.mgrid[0:n, 0:n] / (n - 1) * 2 - 1
    ang = np.arctan2(y, x)
    r = np.sqrt(x ** 2 + y ** 2)
    star = np.clip(1 - r / (0.35 + 0.65 * np.abs(np.cos(ang * 4)) ** 6), 0, 1)
    save(gray_alpha(np.clip(star ** 0.8 + radial(n, 3) * 0.6, 0, 1)), FX, "muzzle.png")

    # Leuchtspur
    w, h = 32, 256
    yy, xx = np.mgrid[0:h, 0:w]
    tracer = np.exp(-((xx / (w - 1) - 0.5) / 0.18) ** 2) * (1 - yy / (h - 1)) ** 1.5
    save(gray_alpha(tracer), FX, "tracer.png")


# ----------------------------------------------------------------------
# Würfel
# ----------------------------------------------------------------------

PIPS = {
    1: [(0.5, 0.5)],
    2: [(0.27, 0.27), (0.73, 0.73)],
    3: [(0.27, 0.27), (0.5, 0.5), (0.73, 0.73)],
    4: [(0.27, 0.27), (0.73, 0.27), (0.27, 0.73), (0.73, 0.73)],
    5: [(0.27, 0.27), (0.73, 0.27), (0.5, 0.5), (0.27, 0.73), (0.73, 0.73)],
    6: [(0.27, 0.25), (0.73, 0.25), (0.27, 0.5), (0.73, 0.5), (0.27, 0.75), (0.73, 0.75)],
}


def gen_dice():
    """Würfelatlas 3x2: Zeile oben 1-3, unten 4-6."""
    n = 256
    faces = []
    for v in range(1, 7):
        big = Image.new("RGBA", (n * SS, n * SS), (0, 0, 0, 0))
        d = ImageDraw.Draw(big)
        d.rectangle((0, 0, n * SS, n * SS), fill=(210, 196, 170, 255))
        d.rounded_rectangle((6 * SS, 6 * SS, (n - 6) * SS, (n - 6) * SS), radius=40 * SS, fill=CREAM + (255,))
        rad = 22 * SS
        for px, py in PIPS[v]:
            cx, cy = px * n * SS, py * n * SS
            d.ellipse((cx - rad, cy - rad + 3 * SS, cx + rad, cy + rad + 3 * SS), fill=(255, 255, 255, 255))
            d.ellipse((cx - rad, cy - rad, cx + rad, cy + rad), fill=RED_DARK + (255,) if v != 1 else RED + (255,))
        faces.append(big.resize((n, n), Image.LANCZOS))
    atlas = sheet(faces, 3, 2)
    save(atlas.convert("RGB"), DICE, "dice.png")


# ----------------------------------------------------------------------
# Spieltisch
# ----------------------------------------------------------------------

def gen_table():
    """Filzmatte für die Tischmitte und Ledermatten für die Spielerplätze."""
    w, h = 1536, 960
    nz = noise(w // 2, h // 2, 18, seed=77)
    nz = np.asarray(Image.fromarray((nz * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), float) / 255
    fine = np.random.default_rng(3).random((h, w))
    base = np.array((92, 22, 24), float)
    shade = 0.82 + nz[..., None] * 0.3 + (fine[..., None] - 0.5) * 0.08
    yy, xx = np.mgrid[0:h, 0:w]
    ex = (xx - w / 2) / (w / 2 - 8)
    ey = (yy - h / 2) / (h / 2 - 8)
    r = np.sqrt(ex ** 2 + ey ** 2)
    shade *= (1 - 0.35 * np.clip(r, 0, 1) ** 3)[..., None]
    rgb = (base * shade).clip(0, 255).astype(np.uint8)
    img = Image.fromarray(rgb, "RGB").convert("RGBA")
    # Wasserzeichen: Logo sehr dezent in der Mitte
    logo_path = os.path.join(RES, "images", "logo.png")
    if os.path.exists(logo_path):
        logo = Image.open(logo_path).convert("RGBA").resize((400, 398), Image.LANCZOS)
        logo.putalpha(logo.getchannel("A").point(lambda v: v * 34 // 255))
        img.alpha_composite(logo, ((w - 400) // 2, (h - 398) // 2))
    d = ImageDraw.Draw(img)
    for inset, width, alpha in ((26, 6, 230), (46, 2, 160)):
        d.ellipse((inset, inset, w - inset, h - inset), outline=GOLD + (alpha,), width=width)
    mask = Image.new("L", (w * 2, h * 2), 0)
    ImageDraw.Draw(mask).ellipse((4, 4, w * 2 - 4, h * 2 - 4), fill=255)
    img.putalpha(mask.resize((w, h), Image.LANCZOS))
    save(img.convert("RGBA"), TABLE, "felt.png")

    # Ledermatte für einen Spielerplatz
    w, h = 1024, 512
    nz = noise(w, h, 40, seed=91)
    leather = np.array((54, 34, 24), float) * (0.85 + nz[..., None] * 0.3)
    mat = Image.fromarray(leather.clip(0, 255).astype(np.uint8), "RGB").convert("RGBA")
    mat.putalpha(rounded_mask(w, h, 60, 4))
    d = ImageDraw.Draw(mat)
    # Ziernaht
    inset = 22
    step = 18
    for x in range(inset + 30, w - inset - 30, step):
        d.line([(x, inset), (x + 9, inset)], fill=(214, 168, 96, 200), width=3)
        d.line([(x, h - inset), (x + 9, h - inset)], fill=(214, 168, 96, 200), width=3)
    for y in range(inset + 30, h - inset - 30, step):
        d.line([(inset, y), (inset, y + 9)], fill=(214, 168, 96, 200), width=3)
        d.line([(w - inset, y), (w - inset, y + 9)], fill=(214, 168, 96, 200), width=3)
    save(mat, TABLE, "seat_mat.png")


if __name__ == "__main__":
    gen_panels()
    gen_buttons()
    gen_fields()
    gen_decor()
    gen_icons()
    gen_fx()
    gen_dice()
    gen_table()
    print("Texturen erzeugt.")
