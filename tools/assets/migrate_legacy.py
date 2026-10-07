#!/usr/bin/env python3
"""Einmalige Übernahme der Grafiken und Sounds aus dem alten Gruppenprojekt.

Die alten Karten lagen als glTF-Modelle vor, in die jeweils dieselbe 2,6-MB-Rückseite
eingebettet war (insgesamt ~270 MB). Dieses Skript zieht die Vorderseiten heraus,
speichert sie als JPEG in einheitlicher Größe und legt die Rückseiten nur einmal ab.
Die Kartengeometrie erzeugt das Spiel selbst (abgerundete Karten).

Aufruf:  python3 tools/assets/migrate_legacy.py <alter resources-Ordner>
         z. B. ../W2SBattleShips/winf2slay/view/src/main/resources
Ziel:    view/src/main/resources
"""
from __future__ import annotations

import glob
import io
import json
import os
import shutil
import struct
import subprocess
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "view", "src", "main", "resources")

CARD_SIZE = (560, 784)       # Seitenverhältnis 5:7
BACK_HASH_MIN_USES = 10      # Bilder, die in vielen Karten stecken, sind Rückseiten


def glb_images(path: str):
    """Liefert (Materialname, Bilddaten) aller Basisfarbtexturen eines .glb."""
    data = open(path, "rb").read()
    json_len = struct.unpack("<I", data[12:16])[0]
    gltf = json.loads(data[20:20 + json_len])
    bin_start = 20 + json_len + 8
    for material in gltf.get("materials", []):
        tex = material.get("pbrMetallicRoughness", {}).get("baseColorTexture")
        if tex is None:
            continue
        image = gltf["images"][gltf["textures"][tex["index"]]["source"]]
        view = gltf["bufferViews"][image["bufferView"]]
        offset = bin_start + view.get("byteOffset", 0)
        yield material.get("name"), data[offset:offset + view["byteLength"]]


def save_jpeg(raw: bytes, target: str, size=CARD_SIZE, quality=90):
    img = Image.open(io.BytesIO(raw)).convert("RGB").resize(size, Image.LANCZOS)
    os.makedirs(os.path.dirname(target), exist_ok=True)
    img.save(target, "JPEG", quality=quality, optimize=True, progressive=True)


def migrate_cards(src: str):
    usage: dict[bytes, int] = {}
    per_card: dict[str, list[bytes]] = {}
    for path in sorted(glob.glob(os.path.join(src, "cards", "*.glb"))):
        name = os.path.splitext(os.path.basename(path))[0]
        images = [raw for _, raw in glb_images(path)]
        per_card[name] = images
        for raw in images:
            usage[raw] = usage.get(raw, 0) + 1
    out = os.path.join(RES, "cards")
    backs = [raw for raw, n in usage.items() if n >= BACK_HASH_MIN_USES]
    back = max(backs, key=lambda r: Image.open(io.BytesIO(r)).size[0])  # das große Rückseitenbild
    save_jpeg(back, os.path.join(out, "back.jpg"))
    count = 0
    for name, images in per_card.items():
        fronts = [raw for raw in images if usage[raw] < BACK_HASH_MIN_USES]
        if not fronts:
            continue
        front = max(fronts, key=len)
        if name == "DiscardDeck":
            save_jpeg(front, os.path.join(out, "back_discard.jpg"))
        elif name in ("MonsterDeck", "SupportDeck", "bigCard", "smallCard"):
            continue
        else:
            save_jpeg(front, os.path.join(out, name + ".jpg"))
            count += 1
        if name == "Herausforderung":
            emblem(front)
    print(f"{count} Kartenvorderseiten übernommen")


def emblem(raw: bytes):
    """Schneidet das Schwert-Emblem der Herausforderungskarte für den Duell-Effekt aus."""
    img = Image.open(io.BytesIO(raw)).convert("RGBA")
    w, h = img.size
    cx, cy, r = w * 0.5, h * 0.384, w * 0.36
    crop = img.crop((int(cx - r), int(cy - r), int(cx + r), int(cy + r))).resize((384, 384), Image.LANCZOS)
    mask = Image.new("L", (384 * 4, 384 * 4), 0)
    ImageDraw.Draw(mask).ellipse((8, 8, 384 * 4 - 8, 384 * 4 - 8), fill=255)
    crop.putalpha(mask.resize((384, 384), Image.LANCZOS))
    crop.save(os.path.join(RES, "fx", "swords.png"), optimize=True)


def migrate_images(src: str):
    images = os.path.join(RES, "images")
    os.makedirs(images, exist_ok=True)
    logo = Image.open(os.path.join(src, "images", "logo.png")).convert("RGBA")
    logo.resize((900, 897), Image.LANCZOS).save(os.path.join(images, "logo.png"), optimize=True)
    for size in (16, 32, 64, 128):
        logo.resize((size, size), Image.LANCZOS).save(os.path.join(images, f"icon{size}.png"))
    heli = Image.open(os.path.join(src, "assets", "heli.png")).convert("RGBA")
    heli.resize((1600, 900), Image.LANCZOS).save(os.path.join(images, "heli.png"), optimize=True)
    flash = Image.open(os.path.join(src, "images", "muzzleflash.png")).convert("RGBA")
    box = flash.getchannel("A").getbbox()
    flash.crop(box).resize((256, 256), Image.LANCZOS).save(os.path.join(RES, "fx", "explosion.png"), optimize=True)

    sky = os.path.join(RES, "textures", "sky")
    os.makedirs(sky, exist_ok=True)
    for face in ("back", "down", "front", "left", "right", "up"):
        Image.open(os.path.join(src, "textures", "background", f"cube_{face}.png")).convert("RGB") \
            .save(os.path.join(sky, f"{face}.jpg"), "JPEG", quality=86, optimize=True)
    table = os.path.join(RES, "textures", "table")
    os.makedirs(table, exist_ok=True)
    for kind in ("diff", "nor_dx", "rough"):
        Image.open(os.path.join(src, "textures", "table", f"wood_table_worn_{kind}_2k.jpg")).convert("RGB") \
            .save(os.path.join(table, f"wood_{kind}.jpg"), "JPEG", quality=88, optimize=True)
    models = os.path.join(RES, "models")
    os.makedirs(models, exist_ok=True)
    shutil.copy(os.path.join(src, "models", "low_poly_knight.glb"), os.path.join(models, "knight.glb"))


def migrate_sounds(src: str):
    sounds = os.path.join(RES, "sounds")
    os.makedirs(sounds, exist_ok=True)
    music = os.path.join(RES, "music")
    os.makedirs(music, exist_ok=True)

    def ogg(source: str, target: str, quality: int = 4):
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", source, "-ac", "2" if "music" in target else "1",
                        "-c:a", "libvorbis", "-q:a", str(quality), target], check=True)

    ogg(os.path.join(src, "sounds", "medivaltavern.wav"), os.path.join(music, "tavern.ogg"), 3)
    for wav in glob.glob(os.path.join(src, "sounds", "soundeffects", "*.wav")):
        name = os.path.splitext(os.path.basename(wav))[0]
        ogg(wav, os.path.join(sounds, name + ".ogg"))
    print("Sounds konvertiert")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    source = sys.argv[1]
    migrate_cards(source)
    migrate_images(source)
    migrate_sounds(source)
