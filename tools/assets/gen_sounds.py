#!/usr/bin/env python3
"""Synthetisiert die zusätzlichen Soundeffekte von WinfToSlay.

Alle Klänge entstehen rechnerisch (Rauschen, Sinus, Filter) – es werden keine
fremden Aufnahmen verwendet. Ausgabe als Ogg Vorbis (benötigt ffmpeg).

Aufruf:  python3 tools/assets/gen_sounds.py
Ziel:    view/src/main/resources/sounds
"""
from __future__ import annotations

import os
import subprocess
import tempfile
import wave

import numpy as np
from scipy import signal

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "view", "src", "main", "resources", "sounds")
SR = 44100
rng = np.random.default_rng(42)


def t(seconds: float) -> np.ndarray:
    return np.arange(int(seconds * SR)) / SR


def noise(seconds: float) -> np.ndarray:
    return rng.uniform(-1, 1, int(seconds * SR))


def band(x, low, high, order=2):
    sos = signal.butter(order, [low, high], btype="bandpass", fs=SR, output="sos")
    return signal.sosfilt(sos, x)


def lowpass(x, cutoff, order=2):
    return signal.sosfilt(signal.butter(order, cutoff, btype="lowpass", fs=SR, output="sos"), x)


def highpass(x, cutoff, order=2):
    return signal.sosfilt(signal.butter(order, cutoff, btype="highpass", fs=SR, output="sos"), x)


def env(n, attack=0.005, decay=None, release=0.05, curve=4.0):
    """Hüllkurve: schneller Anstieg, exponentielles Abklingen."""
    x = np.ones(n)
    a = max(1, int(attack * SR))
    x[:a] = np.linspace(0, 1, a)
    if decay:
        tt = np.arange(n - a) / SR
        x[a:] = np.exp(-tt / decay)
    r = max(1, min(n // 3, int(release * SR)))
    x[-r:] *= np.linspace(1, 0, r) ** 2
    return x


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def place(buf, x, at):
    i = int(at * SR)
    end = min(len(buf), i + len(x))
    buf[i:end] += x[:end - i]


def tone(freq, seconds, kind="sine", vibrato=0.0, vib_rate=5.0):
    tt = t(seconds)
    f = np.atleast_1d(freq)
    if f.size == 1:
        f = np.full(tt.size, float(f[0]))
    if vibrato:
        f = f * (1 + vibrato * np.sin(2 * np.pi * vib_rate * tt))
    phase = 2 * np.pi * np.cumsum(f) / SR
    if kind == "sine":
        return np.sin(phase)
    if kind == "saw":
        return 2 * ((phase / (2 * np.pi)) % 1) - 1
    if kind == "square":
        return np.sign(np.sin(phase))
    if kind == "tri":
        return 2 * np.abs(2 * ((phase / (2 * np.pi)) % 1) - 1) - 1
    raise ValueError(kind)


def write(name, x, peak=0.89):
    x = np.asarray(x, float)
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav_path = os.path.join(tmp, name + ".wav")
        with wave.open(wav_path, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((x * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "4",
                        os.path.join(OUT, name + ".ogg")], check=True)


# ----------------------------------------------------------------------

def whoosh(seconds=0.38, low=500, high=2800):
    n = noise(seconds)
    tt = t(seconds)
    # Bandpass, dessen Mitte nach oben und wieder nach unten wandert
    out = np.zeros_like(n)
    steps = 24
    for k in range(steps):
        a, b = k * len(n) // steps, (k + 1) * len(n) // steps
        center = low + (high - low) * np.sin(np.pi * (k + 0.5) / steps)
        seg = band(n, center * 0.6, min(center * 1.6, SR / 2 - 100))[a:b]
        out[a:b] = seg
    shape = np.sin(np.pi * tt / seconds) ** 2
    return out * shape


def card_place():
    thud = lowpass(noise(0.12), 400) * env(int(0.12 * SR), 0.001, 0.025)
    slap = highpass(noise(0.05), 2500) * env(int(0.05 * SR), 0.0005, 0.008)
    return mix(thud * 1.3, slap * 0.5)


def card_flip():
    out = np.zeros(int(0.16 * SR))
    for at, amp in ((0.0, 1.0), (0.06, 0.6)):
        flick = band(noise(0.05), 1800, 7000) * env(int(0.05 * SR), 0.001, 0.01)
        place(out, flick * amp, at)
    return out


def shuffle():
    out = np.zeros(int(0.9 * SR))
    for i in range(14):
        flick = band(noise(0.04), 1500, 6500) * env(int(0.04 * SR), 0.001, 0.008)
        place(out, flick * rng.uniform(0.4, 1.0), i * 0.055 + rng.uniform(0, 0.015))
    return out


def dice_roll():
    out = np.zeros(int(1.3 * SR))
    at, gap = 0.0, 0.16
    for i in range(11):
        f = rng.uniform(1700, 3200)
        click = (tone(f, 0.05) * 0.5 + band(noise(0.05), 1500, 6000)) * env(int(0.05 * SR), 0.0005, 0.007)
        thud = lowpass(noise(0.06), 300) * env(int(0.06 * SR), 0.001, 0.015)
        place(out, mix(click, thud * 0.8) * (1 - i / 14), at)
        place(out, click * 0.6 * (1 - i / 14), at + 0.012)
        at += gap
        gap *= 0.8
    return out


def explosion(seconds=1.8, big=False):
    n = noise(seconds)
    rumble = lowpass(n, 180 if big else 260, 4) * env(len(n), 0.004, 0.45 if big else 0.32)
    crack = highpass(noise(0.08), 900) * env(int(0.08 * SR), 0.0005, 0.02)
    body = band(noise(seconds), 200, 1800) * env(len(n), 0.002, 0.18)
    parts = [rumble * 3.0, crack * 0.8, body * 0.9]
    if big:
        sub = tone(np.linspace(70, 28, int(seconds * SR)), seconds) * env(int(seconds * SR), 0.01, 0.7)
        parts.append(sub * 1.4)
    out = mix(*parts)
    return np.tanh(out * 1.6)


def whistle():
    seconds = 1.0
    f = np.linspace(2100, 700, int(seconds * SR)) ** 1.0
    w = tone(f, seconds, vibrato=0.012, vib_rate=11) * np.linspace(0.3, 1, int(seconds * SR))
    air = band(noise(seconds), 1500, 5000) * 0.15
    return (w + air) * env(int(seconds * SR), 0.05, None, 0.03)


def thunder():
    seconds = 1.6
    crack = np.zeros(int(seconds * SR))
    for i in range(30):
        burst = highpass(noise(0.01), 1200) * rng.uniform(0.2, 1.0)
        place(crack, burst, rng.uniform(0, 0.25) ** 1.5)
    rumble = lowpass(noise(seconds), 220, 4) * env(int(seconds * SR), 0.03, 0.5)
    return np.tanh(mix(crack * 1.4, rumble * 3.5) * 1.3)


def bell(freq, seconds, partials=((1, 1), (2.01, 0.5), (3.02, 0.25), (4.2, 0.15)), decay=0.5):
    out = np.zeros(int(seconds * SR))
    for ratio, amp in partials:
        out += tone(freq * ratio, seconds) * amp * env(int(seconds * SR), 0.002, decay / ratio ** 0.5)
    return out


def magic():
    out = np.zeros(int(1.4 * SR))
    for i, f in enumerate((659.25, 783.99, 987.77, 1318.5, 1567.98)):
        place(out, bell(f, 0.9, decay=0.35) * 0.6, i * 0.07)
    shimmer = band(noise(1.4), 6000, 12000) * env(int(1.4 * SR), 0.2, 0.4) * 0.15
    return mix(out, shimmer)


def sparkle():
    out = np.zeros(int(0.9 * SR))
    for i in range(9):
        place(out, bell(rng.uniform(2200, 4200), 0.3, decay=0.12) * rng.uniform(0.3, 0.8), rng.uniform(0, 0.55))
    return out


def sword_clash():
    seconds = 1.2
    metal = np.zeros(int(seconds * SR))
    for f, amp, dec in ((1240, 1.0, 0.35), (2710, 0.7, 0.25), (3930, 0.5, 0.18), (5480, 0.35, 0.12),
                        (7150, 0.2, 0.08)):
        metal += tone(f, seconds) * amp * env(int(seconds * SR), 0.0005, dec)
    hit = highpass(noise(0.05), 2000) * env(int(0.05 * SR), 0.0003, 0.012)
    out = np.zeros(int(seconds * SR))
    place(out, mix(metal * 0.6, hit * 1.2), 0)
    place(out, metal[: int(0.5 * SR)] * 0.25, 0.09)
    return out


def chord(freqs, seconds, kind="saw", cutoff=2500):
    out = np.zeros(int(seconds * SR))
    for f in freqs:
        out += tone(f, seconds, kind, vibrato=0.004, vib_rate=5.5)
    return lowpass(out, cutoff)


def success():
    out = np.zeros(int(1.2 * SR))
    notes = (523.25, 659.25, 783.99)
    for i, f in enumerate(notes):
        place(out, chord([f, f * 2], 0.18, cutoff=3000) * env(int(0.18 * SR), 0.01, 0.12), i * 0.1)
    place(out, chord([523.25, 659.25, 783.99, 1046.5], 0.75, cutoff=3500) * env(int(0.75 * SR), 0.01, 0.35), 0.3)
    return out


def fail():
    out = np.zeros(int(1.0 * SR))
    place(out, chord([233.08, 277.18], 0.3, cutoff=1400) * env(int(0.3 * SR), 0.01, 0.2), 0)
    place(out, chord([207.65, 246.94], 0.6, cutoff=1100) * env(int(0.6 * SR), 0.01, 0.3), 0.28)
    return out


def lose_game():
    out = np.zeros(int(2.6 * SR))
    notes = ((392.0, 0.0, 0.35), (369.99, 0.4, 0.35), (349.23, 0.8, 0.35), (329.63, 1.2, 1.3))
    for f, at, dur in notes:
        vib = 0.02 if dur > 1 else 0.004
        x = lowpass(tone(f, dur, "saw", vibrato=vib, vib_rate=6) + tone(f / 2, dur, "saw") * 0.4, 1400)
        place(out, x * env(int(dur * SR), 0.02, None, 0.12), at)
    return out


def hover():
    return bell(2400, 0.08, partials=((1, 1),), decay=0.02) * 0.6


def burn():
    seconds = 1.4
    hiss = band(noise(seconds), 800, 5000) * env(int(seconds * SR), 0.15, 0.5) * 0.4
    pops = np.zeros(int(seconds * SR))
    for i in range(40):
        place(pops, highpass(noise(0.006), 1500) * rng.uniform(0.2, 1), rng.uniform(0, seconds - 0.05))
    roar = lowpass(noise(seconds), 300) * env(int(seconds * SR), 0.1, 0.6)
    return mix(hiss, pops * 0.8, roar * 1.5)


def banner():
    return mix(whoosh(0.6, 300, 1800) * 0.9, bell(880, 0.6, decay=0.2) * 0.25)


def ap_spend():
    return bell(1318.5, 0.25, decay=0.08) * 0.7


if __name__ == "__main__":
    write("whoosh", whoosh())
    write("card_place", card_place())
    write("card_flip", card_flip())
    write("shuffle", shuffle())
    write("dice_roll", dice_roll())
    write("explosion", explosion())
    write("explosion_big", explosion(3.0, big=True))
    write("whistle", whistle())
    write("thunder", thunder())
    write("magic", magic())
    write("sparkle", sparkle())
    write("sword_clash", sword_clash())
    write("success", success())
    write("fail", fail())
    write("lose_game", lose_game())
    write("hover", hover(), peak=0.5)
    write("burn", burn())
    write("banner", banner())
    write("ap_spend", ap_spend(), peak=0.6)
    print("Sounds erzeugt.")
