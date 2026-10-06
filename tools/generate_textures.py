#!/usr/bin/env python3
"""Generates every texture the Mummy mod ships with.

All art is drawn procedurally onto the vanilla zombie UV layouts (adult 64x64
HumanoidModel and the 26.x BabyZombieModel), so the mummy can reuse the zombie
models unchanged. The loose, animated bandage strips (MummyBandagesModel) use
the free lower half of each texture. Run from the repository root:

    python3 tools/generate_textures.py

Requires Pillow. Output is deterministic (fixed random seeds).
"""
from __future__ import annotations

import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "mummy")
ENTITY_DIR = os.path.join(ASSETS, "textures", "entity", "mummy")
ITEM_DIR = os.path.join(ASSETS, "textures", "item")
EXTRA_DIR = os.path.join(ROOT, "curseforge")

# Clean, bright linen (light to dark) and the soft shadow between wraps.
LINEN = [(246, 242, 228), (232, 225, 204), (214, 205, 180), (190, 179, 151)]
GAP = (150, 136, 110)
# Friendly sage-green skin.
SKIN = (163, 191, 122)
SKIN_LIGHT = (184, 208, 142)
SKIN_SHADE = (138, 166, 102)
BLUSH = (232, 152, 140)
MOUTH = (78, 50, 46)
EYE = (110, 62, 28)        # base texture: warm brown, the glow layer adds amber on top
EYE_GLOW = (255, 170, 60)
SPARKLE = (255, 250, 236)
CLEAR = (0, 0, 0, 0)

# Mirrors ADULT_STRIPS / BABY_STRIPS in MummyBandagesModel.java:
# (u, v, width, segment_length, segments, sideways)
ADULT_STRIPS = [
    (0, 32, 3, 3, 3, False),   # right arm
    (6, 32, 3, 4, 2, False),   # left arm
    (12, 32, 4, 4, 3, False),  # back
    (20, 32, 2, 3, 2, True),   # hip
    (24, 32, 3, 3, 3, False),  # head
    (30, 32, 3, 3, 2, False),  # front
]
BABY_STRIPS = [
    (0, 32, 1, 2, 2, False),
    (2, 32, 1, 2, 2, False),
    (4, 32, 2, 2, 3, False),
    (8, 32, 1, 2, 2, True),
    (10, 32, 1, 2, 2, False),
    (12, 32, 1, 2, 2, False),
]


def clamp(c: int) -> int:
    return max(0, min(255, c))


def jitter(rgb, rng, amount=4):
    d = rng.randint(-amount, amount)
    return tuple(clamp(c + d) for c in rgb)


def mix(a, b, t: float):
    return tuple(clamp(round(x + (y - x) * t)) for x, y in zip(a, b))


class Box:
    """UV layout of a Minecraft model cube (texOffs u,v; size w,h,d)."""

    def __init__(self, u, v, w, h, d):
        self.u, self.v, self.w, self.h, self.d = u, v, w, h, d

    @property
    def top(self):
        return self.u + self.d, self.v, self.w, self.d

    @property
    def bottom(self):
        return self.u + self.d + self.w, self.v, self.w, self.d

    @property
    def front(self):
        return self.u + self.d, self.v + self.d, self.w, self.h

    @property
    def sides(self):
        """The four side faces are laid out contiguously: right, front, left, back."""
        return self.u, self.v + self.d, 2 * self.d + 2 * self.w, self.h


def put(img, pts, color):
    for x, y in pts:
        img.putpixel((x, y), color + (255,) if len(color) == 3 else color)


def paint_wraps(img, rect, rng, band_min=2, band_max=3, slope=0.25, dirt_bottom=0.0, gap_chance=0.5):
    """Fills a rectangle with diagonal bandage wraps that continue around the cube.

    Each wrap is one strip of linen: a seam row (soft shadow where it overlaps the
    wrap below), a highlight row, then the strip's own tone.
    """
    x0, y0, w, h = rect
    span = h + int(w * slope) + band_max * 2
    bands = []  # (start, thickness, tone, deep_seam)
    pos = -band_max
    while pos < span:
        t = rng.randint(band_min, band_max)
        bands.append((pos, t, rng.choice([1, 1, 2, 0]), rng.random() < gap_chance))
        pos += t
    for x in range(w):
        shift = int(x * slope)
        for y in range(h):
            s = y + shift
            start, t, tone, deep = next(b for b in bands if b[0] <= s < b[0] + b[1])
            r = s - start
            if r == 0:
                c = GAP if deep and rng.random() < 0.8 else LINEN[3]
            elif r == 1 and t > 2:
                c = LINEN[max(0, tone - 1)]
            else:
                c = LINEN[tone]
            c = jitter(c, rng)
            if dirt_bottom and h > 1:
                c = mix(c, (196, 170, 120), dirt_bottom * (y / (h - 1)) ** 2)
            img.putpixel((x0 + x, y0 + y), c + (255,))


def paint_cap(img, rect, rng):
    """Top/bottom faces: two crossing layers of wraps."""
    x0, y0, w, h = rect
    for x in range(w):
        for y in range(h):
            if (x + y) % 4 == 0:
                c = LINEN[3]
            elif (x - y) % 6 == 0:
                c = GAP
            else:
                c = LINEN[1] if ((x - y) // 3) % 2 else LINEN[2]
            img.putpixel((x0 + x, y0 + y), jitter(c, rng) + (255,))


def paint_skin(img, rect, rng):
    x0, y0, w, h = rect
    for x in range(w):
        for y in range(h):
            c = SKIN_LIGHT if y == 0 else (SKIN_SHADE if y == h - 1 else SKIN)
            img.putpixel((x0 + x, y0 + y), jitter(c, rng, 3) + (255,))


def paint_box(img, box, rng, dirt=0.0, **wrap_args):
    paint_wraps(img, box.sides, rng, dirt_bottom=dirt, **wrap_args)
    paint_cap(img, box.top, rng)
    paint_cap(img, box.bottom, rng)


def paint_hands(img, box, rows, rng):
    """Bare hands: the bottom rows of the arm sides and the bottom face."""
    sx, sy, sw, sh = box.sides
    paint_skin(img, (sx, sy + sh - rows, sw, rows), rng)
    paint_skin(img, box.bottom, rng)


def paint_hood_frame(img, front, rng, forehead_rows, chin=True):
    """Hat overlay front: a raised bandage frame around the open face."""
    fx, fy, w, h = front
    for x in range(w):
        for y in range(h):
            edge = y < forehead_rows or x == 0 or x == w - 1
            chin_wrap = chin and y == h - 1 and (x < 2 or x >= w - 2)
            if edge or chin_wrap:
                c = LINEN[0] if y == 0 else (LINEN[3] if y == forehead_rows - 1 and 0 < x < w - 1 else LINEN[1])
                put(img, [(fx + x, fy + y)], jitter(c, rng))


def paint_strips(img, strips, rng):
    """Loose bandage strips: front and back faces of each zero-thickness segment."""
    for u, v, width, seg_len, segments, sideways in strips:
        rows = (width + seg_len) if sideways else seg_len
        for i in range(segments):
            top = v + i * rows + (width if sideways else 0)
            last = i == segments - 1
            face = []  # painted once, mirrored onto the back face so both sides line up
            for y in range(seg_len):
                row = []
                for x in range(width):
                    # Bright middle, darker hems: keeps the strips readable against the wraps behind them.
                    c = GAP if x == width - 1 and width > 1 else LINEN[2] if x == 0 and width > 2 else LINEN[0]
                    if (i * seg_len + y) % 4 == 3 and x != width - 1:
                        c = LINEN[2]  # creases along the strip
                    c = jitter(c, rng)
                    frayed = last and y == seg_len - 1 and (x % 2 == 1 or width == 1)
                    row.append(CLEAR if frayed and width > 1 else c + (255,))
                face.append(row)
            for y, row in enumerate(face):
                for x, c in enumerate(row):
                    img.putpixel((u + x, top + y), c)
                    img.putpixel((u + width + (width - 1 - x), top + y), c)


# ---------------------------------------------------------------- adult ----

def adult_textures(rng):
    img = Image.new("RGBA", (64, 64), CLEAR)
    eyes = Image.new("RGBA", (64, 64), CLEAR)

    head = Box(0, 0, 8, 8, 8)
    body = Box(16, 16, 8, 12, 4)
    arm = Box(40, 16, 4, 12, 4)   # shared (mirrored) by both arms
    leg = Box(0, 16, 4, 12, 4)    # shared (mirrored) by both legs

    paint_box(img, head, rng)
    paint_box(img, body, rng)
    paint_box(img, arm, rng)
    paint_hands(img, arm, 2, rng)
    paint_box(img, leg, rng, dirt=0.4)

    # Face: open in the bandage hood, round sparkly eyes, rosy cheeks and a little smile.
    fx, fy, _, _ = head.front
    paint_skin(img, (fx + 1, fy + 2, 6, 6), rng)
    for ex in (1, 5):
        put(img, [(fx + ex, fy + 3), (fx + ex + 1, fy + 3), (fx + ex, fy + 4), (fx + ex + 1, fy + 4)], EYE)
        put(img, [(fx + ex, fy + 3)], SPARKLE)
        put(eyes, [(fx + ex + 1, fy + 3), (fx + ex, fy + 4), (fx + ex + 1, fy + 4)], EYE_GLOW)
        put(eyes, [(fx + ex, fy + 3)], SPARKLE)
    put(img, [(fx + 1, fy + 5), (fx + 6, fy + 5)], BLUSH)
    put(img, [(fx + 2, fy + 5), (fx + 5, fy + 5), (fx + 3, fy + 6), (fx + 4, fy + 6)], MOUTH)

    # Chest: a small gap in the wraps showing skin.
    bx, by, _, _ = body.front
    paint_skin(img, (bx + 4, by + 4, 3, 2), rng)
    put(img, [(bx + 5, by + 6)], SKIN_SHADE)

    # Hat overlay (texOffs 32,0): the raised bandage hood around the face.
    hat = Box(32, 0, 8, 8, 8)
    paint_hood_frame(img, hat.front, rng, forehead_rows=2)
    sx, sy, _, sh = hat.sides
    paint_wraps(img, (sx, sy, 8, sh), rng, gap_chance=0.3)            # right side
    paint_wraps(img, (sx + 16, sy, 16, sh), rng, gap_chance=0.3)      # left side and back
    paint_cap(img, hat.top, rng)

    paint_strips(img, ADULT_STRIPS, rng)
    return img, eyes


# ----------------------------------------------------------------- baby ----

def baby_textures(rng):
    img = Image.new("RGBA", (64, 64), CLEAR)
    eyes = Image.new("RGBA", (64, 64), CLEAR)

    head = Box(3, 3, 6, 6, 6)
    body = Box(16, 16, 4, 5, 2)
    arms = (Box(36, 16, 2, 5, 2), Box(28, 16, 2, 5, 2))
    legs = (Box(8, 16, 2, 4, 2), Box(0, 16, 2, 4, 2))

    paint_box(img, head, rng)
    paint_box(img, body, rng, band_min=1, band_max=2, slope=0.5)
    for b in arms:
        paint_box(img, b, rng, band_min=1, band_max=2, slope=0.5)
        paint_hands(img, b, 1, rng)
    for b in legs:
        paint_box(img, b, rng, dirt=0.4, band_min=1, band_max=2, slope=0.5)

    fx, fy, _, _ = head.front
    paint_skin(img, (fx + 1, fy + 1, 4, 5), rng)
    for ex in (1, 4):
        put(img, [(fx + ex, fy + 2)], SPARKLE)
        put(img, [(fx + ex, fy + 3)], EYE)
        put(eyes, [(fx + ex, fy + 2)], SPARKLE)
        put(eyes, [(fx + ex, fy + 3)], EYE_GLOW)
    put(img, [(fx + 1, fy + 4), (fx + 4, fy + 4)], BLUSH)
    put(img, [(fx + 2, fy + 4), (fx + 3, fy + 4)], MOUTH)

    hat = Box(35, 3, 6, 6, 6)
    paint_hood_frame(img, hat.front, rng, forehead_rows=1, chin=False)
    sx, sy, _, sh = hat.sides
    paint_wraps(img, (sx, sy, 6, sh), rng, band_min=1, band_max=2, gap_chance=0.3)
    paint_wraps(img, (sx + 12, sy, 12, sh), rng, band_min=1, band_max=2, gap_chance=0.3)
    paint_cap(img, hat.top, rng)

    paint_strips(img, BABY_STRIPS, rng)
    return img, eyes


# ------------------------------------------------------------ spawn egg ----

EGG_ROWS = {  # y: (x_start, x_end) inclusive
    1: (6, 9), 2: (5, 10), 3: (4, 11), 4: (4, 11), 5: (3, 12), 6: (3, 12), 7: (3, 12),
    8: (3, 12), 9: (3, 12), 10: (3, 12), 11: (3, 12), 12: (4, 11), 13: (4, 11), 14: (5, 10),
}


def spawn_egg(rng):
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, (a, b) in EGG_ROWS.items():
        for x in range(a, b + 1):
            c = LINEN[(y + x // 4) % 3]
            if (y + x // 4) % 3 == 0 and x % 5 != 0:
                c = LINEN[2]
            img.putpixel((x, y), jitter(c, rng) + (255,))
    # A peek of the face between the wraps.
    for y in (7, 8, 9):
        for x in range(4, 12):
            img.putpixel((x, y), jitter(SKIN, rng, 3) + (255,))
    for x in (5, 10):
        img.putpixel((x, 7), SPARKLE + (255,))
        img.putpixel((x, 8), EYE_GLOW + (255,))
    for x in (7, 8):
        img.putpixel((x, 9), MOUTH + (255,))
    # Shade the silhouette: light upper-left, dark lower-right.
    shaded = img.copy()
    for y, (a, b) in EGG_ROWS.items():
        for x in range(a, b + 1):
            r, g, bl, _ = img.getpixel((x, y))
            edge_r = x == b or y == 14 or (y + 1 in EGG_ROWS and x > EGG_ROWS[y + 1][1])
            edge_l = x == a or y == 1 or (y - 1 in EGG_ROWS and x < EGG_ROWS[y - 1][0])
            if edge_r:
                shaded.putpixel((x, y), mix((r, g, bl), (110, 96, 70), 0.5) + (255,))
            elif edge_l:
                shaded.putpixel((x, y), mix((r, g, bl), (255, 255, 245), 0.35) + (255,))
    return shaded


# ----------------------------------------------------------------- icon ----

def front_view(skin: Image.Image) -> Image.Image:
    """Composes a flat front view (16x32) of the adult skin, hat overlay on top."""
    fig = Image.new("RGBA", (16, 32), CLEAR)

    def blit(src_rect, dst, flip=False):
        x, y, w, h = src_rect
        part = skin.crop((x, y, x + w, y + h))
        if flip:
            part = part.transpose(Image.FLIP_LEFT_RIGHT)
        fig.alpha_composite(part, dst)

    blit(Box(0, 0, 8, 8, 8).front, (4, 0))
    blit(Box(32, 0, 8, 8, 8).front, (4, 0))
    blit(Box(16, 16, 8, 12, 4).front, (4, 8))
    blit(Box(40, 16, 4, 12, 4).front, (0, 8))
    blit(Box(40, 16, 4, 12, 4).front, (12, 8), flip=True)
    blit(Box(0, 16, 4, 12, 4).front, (4, 20))
    blit(Box(0, 16, 4, 12, 4).front, (8, 20), flip=True)
    return fig


def icon(skin, size):
    bg = Image.new("RGBA", (size, size))
    horizon = int(size * 0.72)
    for y in range(size):
        if y < horizon:  # dusk sky
            c = mix((58, 40, 82), (238, 146, 74), y / horizon)
        else:            # dunes
            c = mix((222, 180, 112), (184, 140, 80), (y - horizon) / (size - horizon))
        for x in range(size):
            bg.putpixel((x, y), c + (255,))
    cx, cy, r = size * 0.72, horizon - size * 0.02, size * 0.16  # low sun
    for y in range(horizon):
        for x in range(size):
            if (x - cx) ** 2 + (y - cy) ** 2 < r * r:
                bg.putpixel((x, y), (255, 214, 120, 255))
    # Upper-body portrait (head and torso) so the face reads at small sizes.
    scale = max(1, (size * 9 // 10) // 20)
    fig = front_view(skin).crop((0, 0, 16, 20)).resize((16 * scale, 20 * scale), Image.NEAREST)
    bg.alpha_composite(fig, ((size - fig.width) // 2, size - fig.height))
    return bg


def main():
    os.makedirs(ENTITY_DIR, exist_ok=True)
    os.makedirs(ITEM_DIR, exist_ok=True)
    os.makedirs(EXTRA_DIR, exist_ok=True)

    skin, eyes = adult_textures(random.Random(1922))  # Tutankhamun's tomb, 1922
    skin.save(os.path.join(ENTITY_DIR, "mummy.png"))
    eyes.save(os.path.join(ENTITY_DIR, "mummy_eyes.png"))

    baby, baby_eyes = baby_textures(random.Random(1923))
    baby.save(os.path.join(ENTITY_DIR, "mummy_baby.png"))
    baby_eyes.save(os.path.join(ENTITY_DIR, "mummy_baby_eyes.png"))

    spawn_egg(random.Random(7)).save(os.path.join(ITEM_DIR, "mummy_spawn_egg.png"))

    icon(skin, 128).save(os.path.join(ASSETS, "icon.png"))
    icon(skin, 400).save(os.path.join(EXTRA_DIR, "logo.png"))
    print("Textures written to", ASSETS)


if __name__ == "__main__":
    main()
