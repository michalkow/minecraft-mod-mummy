#!/usr/bin/env python3
"""Generates every texture the three mods ship with (Mummy, Verity, Zombie Kingdom).

All art is drawn procedurally onto the vanilla zombie UV layouts (adult 64x64
HumanoidModel and the 26.x BabyZombieModel), so the mobs can reuse the zombie
models unchanged. Loose cloth (bandages, cape, skirt) and crowns use the free
lower half of the textures or separate regalia textures. Run from the
repository root:

    python3 tools/generate_textures.py

Requires Pillow. Output is deterministic (fixed random seeds).
"""
from __future__ import annotations

import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def assets(project, mod_id):
    return os.path.join(ROOT, project, "src", "main", "resources", "assets", mod_id)


MUMMY_ASSETS = assets("mummy", "mummy")
VERITY_ASSETS = assets("verity", "verity")
KINGDOM_ASSETS = assets("zombie-kingdom", "zombie_kingdom")
CURSEFORGE = os.path.join(ROOT, "curseforge")

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

# Mirrors ADULT_STRIPS / BABY_STRIPS in mummy/.../MummyRenderer.java:
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


def shade_egg(img):
    """Shades the egg silhouette: light upper-left rim, dark lower-right rim."""
    shaded = img.copy()
    for y, (a, b) in EGG_ROWS.items():
        for x in range(a, b + 1):
            r, g, bl, _ = img.getpixel((x, y))
            edge_r = x == b or y == 14 or (y + 1 in EGG_ROWS and x > EGG_ROWS[y + 1][1])
            edge_l = x == a or y == 1 or (y - 1 in EGG_ROWS and x < EGG_ROWS[y - 1][0])
            if edge_r:
                shaded.putpixel((x, y), mix((r, g, bl), (60, 48, 36), 0.5) + (255,))
            elif edge_l:
                shaded.putpixel((x, y), mix((r, g, bl), (255, 255, 245), 0.35) + (255,))
    return shaded


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
    return shade_egg(img)


# --------------------------------------------------------- zombie king ----

K_SKIN = (118, 166, 94)
K_SKIN_LIGHT = (140, 186, 112)
K_SKIN_SHADE = (96, 140, 76)
K_HAIR = (74, 50, 34)
K_EYE = (40, 30, 24)
ROBE = (160, 30, 42)
ROBE_LIGHT = (190, 52, 60)
ROBE_DARK = (116, 18, 30)
LINING = (92, 22, 52)
GOLD = (238, 192, 62)
GOLD_LIGHT = (255, 232, 140)
GOLD_DARK = (186, 136, 34)
ERMINE = (244, 242, 236)
SPECK = (34, 30, 30)
PANTS = (62, 48, 92)
PANTS_DARK = (46, 34, 70)
BOOT = (86, 58, 36)
BOOT_DARK = (62, 40, 24)
RUBY = (220, 30, 60)
SAPPHIRE = (60, 110, 230)
EMERALD = (40, 190, 110)

# Mirrors the crown boxes and capes in ZombieKingRenderer.java (all in zombie_king_regalia.png):
# Box(u, v, w, h, d) per crown cube; capes as (u, v, width, segment_length, segments).
ADULT_CROWN = [Box(0, 40, 10, 2, 1), Box(24, 40, 1, 2, 8), Box(44, 40, 1, 2, 1), Box(48, 40, 1, 3, 1)]
BABY_CROWN = [Box(0, 52, 8, 2, 1), Box(20, 52, 1, 2, 6), Box(36, 52, 1, 1, 1)]
ADULT_CAPE = (0, 0, 10, 4, 4)
BABY_CAPE = (24, 0, 5, 2, 3)


def fill(img, rect, rng, color, amount=3):
    x0, y0, w, h = rect
    for x in range(w):
        for y in range(h):
            c = color(x, y) if callable(color) else color
            img.putpixel((x0 + x, y0 + y), jitter(c, rng, amount) + (255,))


def box_faces(box):
    """All six face rectangles of a cube's UV layout."""
    u, v, w, h, d = box.u, box.v, box.w, box.h, box.d
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "west": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h),
    }


def robe(x, y):
    return ROBE_DARK if x % 4 == 3 else ROBE_LIGHT if x % 4 == 0 and y % 3 == 0 else ROBE


def ermine(x, y):
    return SPECK if (x * 3 + y * 5) % 7 == 0 else ERMINE


def paint_king_face(img, fx, fy, size, rng):
    """Cheerful royal face: hair fringe, brows, sparkly eyes and a big smile."""
    fill(img, (fx, fy, size, size), rng, lambda x, y: K_SKIN_LIGHT if y == 1 else K_SKIN)
    fill(img, (fx, fy, size, 1), rng, K_HAIR)
    if size == 8:
        put(img, [(fx, fy + 1), (fx + 7, fy + 1)], K_HAIR)
        put(img, [(fx + 1, fy + 2), (fx + 2, fy + 2), (fx + 5, fy + 2), (fx + 6, fy + 2)], K_HAIR)
        for ex in (1, 5):
            put(img, [(fx + ex + 1, fy + 3), (fx + ex, fy + 4), (fx + ex + 1, fy + 4)], K_EYE)
            put(img, [(fx + ex, fy + 3)], SPARKLE)
        put(img, [(fx + 2, fy + 6), (fx + 5, fy + 6), (fx + 3, fy + 7), (fx + 4, fy + 7)], MOUTH)  # smile
    else:  # baby, 6x6
        for ex in (1, 4):
            put(img, [(fx + ex, fy + 1)], SPARKLE)
            put(img, [(fx + ex, fy + 2)], K_EYE)
        put(img, [(fx + 1, fy + 4), (fx + 4, fy + 4), (fx + 2, fy + 5), (fx + 3, fy + 5)], MOUTH)  # smile


def paint_king_head(img, head, rng):
    sx, sy, sw, sh = head.sides
    hair_rows = sh // 2
    fill(img, (sx, sy, sw, sh), rng, lambda x, y: K_HAIR if y < hair_rows or x >= sw - head.w else K_SKIN_SHADE if y == sh - 1 else K_SKIN)
    fill(img, head.top, rng, K_HAIR)
    fill(img, head.bottom, rng, K_SKIN_SHADE)
    fx, fy, _, _ = head.front
    paint_king_face(img, fx, fy, head.w, rng)


def paint_king_body(img, body, rng, collar_rows, belt_row):
    sx, sy, sw, sh = body.sides
    fill(img, (sx, sy, sw, sh), rng, lambda x, y: robe(x, y))
    fill(img, (sx, sy, sw, collar_rows), rng, ermine, 2)
    fill(img, body.top, rng, ermine, 2)
    fill(img, body.bottom, rng, ROBE_DARK)
    fill(img, (sx, sy + belt_row, sw, 1), rng, GOLD)                                # gold belt all round
    fx, fy, fw, fh = body.front
    mid = fw // 2
    fill(img, (fx + mid - 1, fy + collar_rows, 2 if fw > 4 else 1, fh - collar_rows), rng, GOLD)  # trim down the front
    put(img, [(fx + mid - 1, fy + belt_row)], GOLD_LIGHT)                          # buckle
    if fw > 4:
        put(img, [(fx + mid, fy + belt_row)], RUBY)


def paint_king_arm(img, arm, rng, hand_rows, cuff_row, collar_rows):
    sx, sy, sw, sh = arm.sides
    fill(img, (sx, sy, sw, sh), rng, lambda x, y: robe(x, y))
    fill(img, (sx, sy, sw, collar_rows), rng, ermine, 2)
    fill(img, (sx, sy + cuff_row, sw, 1), rng, GOLD)
    fill(img, (sx, sy + sh - hand_rows, sw, hand_rows), rng, K_SKIN)
    fill(img, arm.top, rng, ermine, 2)
    fill(img, arm.bottom, rng, K_SKIN)


def paint_king_leg(img, leg, rng, boot_rows):
    sx, sy, sw, sh = leg.sides
    fill(img, (sx, sy, sw, sh), rng, lambda x, y: PANTS_DARK if x % 4 == 0 else PANTS)
    fill(img, (sx, sy + sh - boot_rows, sw, boot_rows), rng, lambda x, y: BOOT_DARK if y == boot_rows - 1 else BOOT)
    fill(img, (sx, sy + sh - boot_rows, sw, 1), rng, GOLD_DARK)                     # boot cuff
    fill(img, leg.top, rng, PANTS)
    fill(img, leg.bottom, rng, BOOT_DARK)


def king_textures(rng):
    img = Image.new("RGBA", (64, 64), CLEAR)
    paint_king_head(img, Box(0, 0, 8, 8, 8), rng)
    paint_king_body(img, Box(16, 16, 8, 12, 4), rng, collar_rows=2, belt_row=7)
    paint_king_arm(img, Box(40, 16, 4, 12, 4), rng, hand_rows=2, cuff_row=9, collar_rows=2)
    paint_king_leg(img, Box(0, 16, 4, 12, 4), rng, boot_rows=3)

    baby = Image.new("RGBA", (64, 64), CLEAR)
    paint_king_head(baby, Box(3, 3, 6, 6, 6), rng)
    paint_king_body(baby, Box(16, 16, 4, 5, 2), rng, collar_rows=1, belt_row=3)
    for arm in (Box(36, 16, 2, 5, 2), Box(28, 16, 2, 5, 2)):
        paint_king_arm(baby, arm, rng, hand_rows=1, cuff_row=3, collar_rows=1)
    for leg in (Box(8, 16, 2, 4, 2), Box(0, 16, 2, 4, 2)):
        paint_king_leg(baby, leg, rng, boot_rows=1)
    return img, baby


def paint_cape(img, cape, rng):
    """A 1-pixel-thick cape: red outside with fold shading and gold edges, dark lining inside,
    an ermine collar along the shoulders and a gold hem along the bottom."""
    u, v, w, seg_len, segments = cape
    for i in range(segments):
        faces = box_faces(Box(u, v + i * (1 + seg_len), w, seg_len, 1))
        last = i == segments - 1
        fill(img, faces["top"], rng, ROBE_DARK)
        fill(img, faces["bottom"], rng, GOLD_DARK if last else ROBE_DARK)  # inner joints stay red
        fill(img, faces["west"], rng, GOLD)
        fill(img, faces["east"], rng, GOLD)
        fill(img, faces["north"], rng, lambda x, y: LINING)                         # faces the body
        hem = lambda x, y, last=last: GOLD if x in (0, w - 1) or (last and y == seg_len - 1) else robe(x, y)
        fill(img, faces["south"], rng, hem)                                         # faces away
        if i == 0:
            ox, oy, ow, _ = faces["south"]
            fill(img, (ox, oy, ow, 1), rng, ermine, 2)


def paint_crown(img, boxes, rng):
    gold = lambda x, y, h: GOLD_LIGHT if y == 0 else GOLD_DARK if y == h - 1 and h > 1 else GOLD
    for box in boxes:
        for name, (x0, y0, w, h) in box_faces(box).items():
            if w and h:
                fill(img, (x0, y0, w, h), rng, lambda x, y, h=h: gold(x, y, h), 2)
    # Gems: a ruby at the centre of the front/back band, sapphires either side, emeralds on the sides.
    band = box_faces(boxes[0])
    for face in ("north", "south"):
        x0, y0, w, h = band[face]
        c = w // 2
        put(img, [(x0 + c - 1, y0 + h - 1), (x0 + c, y0 + h - 1)], RUBY)
        if w >= 10:
            put(img, [(x0 + 2, y0 + h - 1), (x0 + w - 3, y0 + h - 1)], SAPPHIRE)
    side = box_faces(boxes[1])
    for face in ("west", "east"):
        x0, y0, w, h = side[face]
        put(img, [(x0 + w // 2, y0 + h - 1)], EMERALD)


def regalia_texture(rng):
    img = Image.new("RGBA", (64, 64), CLEAR)
    paint_cape(img, ADULT_CAPE, rng)
    paint_cape(img, BABY_CAPE, rng)
    paint_crown(img, ADULT_CROWN, rng)
    paint_crown(img, BABY_CROWN, rng)
    return img


def king_spawn_egg(rng):
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, (a, b) in EGG_ROWS.items():
        for x in range(a, b + 1):
            c = K_SKIN if y < 10 else ERMINE if y == 10 else robe(x, y)
            if y == 10 and (x * 3) % 7 == 0:
                c = SPECK
            img.putpixel((x, y), jitter(c, rng, 3) + (255,))
    # Crown: a gold band with a ruby, and points.
    for x in range(4, 12):
        put(img, [(x, 3), (x, 4)], GOLD)
    put(img, [(7, 4), (8, 4)], RUBY)
    put(img, [(5, 2), (7, 2), (8, 2), (10, 2), (7, 1), (8, 1)], GOLD_LIGHT)
    put(img, [(6, 2), (9, 2)], K_HAIR)
    # Face.
    for ex in (5, 10):
        put(img, [(ex, 6)], SPARKLE)
        put(img, [(ex, 7)], K_EYE)
    put(img, [(6, 8), (9, 8), (7, 9), (8, 9)], MOUTH)  # smile
    return shade_egg(img)


# ------------------------------------------------------ zombie princess ----

P_SKIN = (150, 202, 132)
P_SKIN_LIGHT = (172, 216, 152)
P_SKIN_SHADE = (126, 180, 110)
P_HAIR = (246, 208, 104)
P_HAIR_SHADE = (214, 170, 70)
P_EYE = (72, 40, 62)
P_BLUSH = (250, 150, 170)
P_MOUTH = (196, 72, 104)
PINK = (244, 146, 186)
PINK_LIGHT = (252, 190, 214)
PINK_DARK = (214, 100, 152)
PINK_DEEP = (176, 58, 118)
LACE = (255, 246, 250)
ROSE = (255, 84, 156)
PEARL = (250, 248, 240)

# Mirrors ZombiePrincessRenderer.java (zombie_princess_regalia.png):
# skirt panels as (u, v, width, segment_length, segments, sideways, outer_face); crown cubes as Box(u, v, w, h, d).
ADULT_SKIRT = [(0, 0, 10, 3, 2, False, "north"), (0, 8, 10, 3, 2, False, "south"),
               (24, 0, 6, 3, 2, True, "west"), (40, 0, 6, 3, 2, True, "east")]
BABY_SKIRT = [(0, 20, 5, 2, 2, False, "north"), (0, 26, 5, 2, 2, False, "south"),
              (24, 20, 3, 2, 2, True, "west"), (32, 20, 3, 2, 2, True, "east")]
ADULT_TIARA = [Box(0, 40, 6, 1, 1), Box(16, 40, 1, 1, 4), Box(28, 40, 1, 1, 1), Box(34, 40, 1, 2, 1)]
BABY_TIARA = [Box(0, 48, 4, 1, 1), Box(12, 48, 1, 1, 2), Box(20, 48, 1, 1, 1), Box(26, 48, 1, 2, 1)]


def pleats(x, y):
    return PINK_DARK if x % 3 == 0 else PINK_LIGHT if x % 3 == 1 else PINK


def paint_princess_head(img, head, rng):
    sx, sy, sw, sh = head.sides
    fill(img, (sx, sy, sw, sh), rng, lambda x, y: P_HAIR_SHADE if x % 3 == 0 else P_HAIR)  # long hair all round
    fill(img, head.top, rng, lambda x, y: P_HAIR_SHADE if (x + y) % 4 == 0 else P_HAIR)
    fill(img, head.bottom, rng, P_SKIN_SHADE)
    fx, fy, w, _ = head.front
    fill(img, (fx, fy, w, w), rng, lambda x, y: P_SKIN_LIGHT if y == 1 else P_SKIN)
    fill(img, (fx, fy, w, 1), rng, P_HAIR)                                          # fringe
    fill(img, (fx, fy + 1, 1, w - 1), rng, P_HAIR)                                  # hair framing the face
    fill(img, (fx + w - 1, fy + 1, 1, w - 1), rng, P_HAIR)
    if w == 8:
        put(img, [(fx + 1, fy + 1), (fx + 6, fy + 1)], P_HAIR)                      # side bangs
        for ex in (1, 5):                                                           # big, tall, sparkly eyes
            put(img, [(fx + ex, fy + 2), (fx + ex + 1, fy + 2), (fx + ex + 1, fy + 3), (fx + ex, fy + 4), (fx + ex + 1, fy + 4)], P_EYE)
            put(img, [(fx + ex, fy + 3)], SPARKLE)
        put(img, [(fx + 1, fy + 5), (fx + 6, fy + 5)], P_BLUSH)
        put(img, [(fx + 3, fy + 6), (fx + 4, fy + 6)], P_MOUTH)
    else:  # baby, 6x6
        for ex in (1, 4):
            put(img, [(fx + ex, fy + 2)], SPARKLE)
            put(img, [(fx + ex, fy + 3)], P_EYE)
        put(img, [(fx + 1, fy + 4), (fx + 4, fy + 4)], P_BLUSH)
        put(img, [(fx + 2, fy + 5), (fx + 3, fy + 5)], P_MOUTH)


def paint_princess_body(img, body, rng, sash_row):
    sx, sy, sw, sh = body.sides
    fill(img, (sx, sy, sw, sh), rng, lambda x, y: PINK_LIGHT if x % 4 == 1 else PINK)
    fill(img, (sx, sy, sw, 1), rng, LACE)                                           # lace neckline
    fill(img, (sx, sy + sash_row, sw, 2 if sh > 6 else 1), rng, PINK_DARK)          # sash
    fill(img, body.top, rng, PINK)
    fill(img, body.bottom, rng, PINK_DARK)
    fx, fy, fw, _ = body.front
    if fw == 8:                                                                     # a bow on the chest
        put(img, [(fx + 2, fy + 2), (fx + 5, fy + 2), (fx + 2, fy + 3), (fx + 5, fy + 3)], PINK_DEEP)
        put(img, [(fx + 3, fy + 3), (fx + 4, fy + 3)], ROSE)
    else:
        put(img, [(fx + 1, fy + 1), (fx + 2, fy + 1)], ROSE)


def paint_princess_arm(img, arm, rng, sleeve_rows):
    sx, sy, sw, sh = arm.sides
    fill(img, (sx, sy, sw, sh), rng, lambda x, y: P_SKIN_SHADE if y == sh - 1 else P_SKIN)
    fill(img, (sx, sy, sw, sleeve_rows), rng, lambda x, y: PINK_LIGHT if x % 2 else PINK)  # puffy sleeve
    fill(img, (sx, sy + sleeve_rows, sw, 1), rng, LACE)
    fill(img, arm.top, rng, PINK_LIGHT)
    fill(img, arm.bottom, rng, P_SKIN)


def paint_princess_leg(img, leg, rng, stocking_rows):
    sx, sy, sw, sh = leg.sides
    fill(img, (sx, sy, sw, sh), rng, PINK_DARK)                                     # under the skirt
    fill(img, (sx, sy + sh - 1 - stocking_rows, sw, stocking_rows), rng, LACE)      # white stockings
    fill(img, (sx, sy + sh - 1, sw, 1), rng, PINK_DEEP)                             # shoes
    fill(img, leg.top, rng, PINK_DARK)
    fill(img, leg.bottom, rng, PINK_DEEP)


def princess_textures(rng):
    img = Image.new("RGBA", (64, 64), CLEAR)
    paint_princess_head(img, Box(0, 0, 8, 8, 8), rng)
    paint_princess_body(img, Box(16, 16, 8, 12, 4), rng, sash_row=9)
    paint_princess_arm(img, Box(40, 16, 4, 12, 4), rng, sleeve_rows=3)
    paint_princess_leg(img, Box(0, 16, 4, 12, 4), rng, stocking_rows=3)

    baby = Image.new("RGBA", (64, 64), CLEAR)
    paint_princess_head(baby, Box(3, 3, 6, 6, 6), rng)
    paint_princess_body(baby, Box(16, 16, 4, 5, 2), rng, sash_row=3)
    for arm in (Box(36, 16, 2, 5, 2), Box(28, 16, 2, 5, 2)):
        paint_princess_arm(baby, arm, rng, sleeve_rows=1)
    for leg in (Box(8, 16, 2, 4, 2), Box(0, 16, 2, 4, 2)):
        paint_princess_leg(baby, leg, rng, stocking_rows=1)
    return img, baby


def paint_skirt(img, panels, rng):
    """Flat skirt panels: pleated pink outside, a darker inside, a sash along the waist and a lace hem."""
    for u, v, width, seg_len, segments, sideways, outer in panels:
        for i in range(segments):
            faces = box_faces(Box(u, v + i * ((width if sideways else 0) + seg_len), 0 if sideways else width, seg_len, width if sideways else 0))
            for name in (("west", "east") if sideways else ("north", "south")):
                x0, y0, w, h = faces[name]
                inner = name != outer
                last = i == segments - 1

                def colour(x, y, inner=inner, last=last, h=h):
                    if i == 0 and y == 0:
                        return PINK_DEEP
                    if last and y == h - 1:
                        return LACE if x % 2 == 0 else PINK_LIGHT
                    c = pleats(x, y)
                    return mix(c, PINK_DEEP, 0.35) if inner else c

                fill(img, (x0, y0, w, h), rng, colour, 2)


def paint_tiara(img, boxes, rng):
    gold = lambda x, y, h: GOLD_LIGHT if y == 0 else GOLD_DARK if y == h - 1 and h > 1 else GOLD
    band, side, point, centre = boxes
    for box, colour in ((band, None), (side, None), (point, PEARL), (centre, None)):
        for _, (x0, y0, w, h) in box_faces(box).items():
            if w and h:
                fill(img, (x0, y0, w, h), rng, colour if colour else (lambda x, y, h=h: gold(x, y, h)), 2)
    for name, (x0, y0, w, h) in box_faces(centre).items():                          # a pink jewel on the tall point
        if w and h:
            put(img, [(x0 + x, y0) for x in range(w)], ROSE)
    x0, y0, w, h = box_faces(band)["north"]
    put(img, [(x0 + w // 2 - 1, y0), (x0 + w // 2, y0)], ROSE)


def princess_regalia_texture(rng):
    img = Image.new("RGBA", (64, 64), CLEAR)
    paint_skirt(img, ADULT_SKIRT, rng)
    paint_skirt(img, BABY_SKIRT, rng)
    paint_tiara(img, ADULT_TIARA, rng)
    paint_tiara(img, BABY_TIARA, rng)
    return img


def princess_spawn_egg(rng):
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, (a, b) in EGG_ROWS.items():
        for x in range(a, b + 1):
            if y <= 4 or x in (a, b) and y < 10:
                c = P_HAIR
            elif y < 10:
                c = P_SKIN
            elif y == 10:
                c = LACE
            else:
                c = pleats(x, y)
            img.putpixel((x, y), jitter(c, rng, 3) + (255,))
    put(img, [(x, 2) for x in range(6, 10)], GOLD)                                  # little crown
    put(img, [(6, 1), (9, 1)], PEARL)
    put(img, [(7, 1), (8, 1)], ROSE)
    for ex in (5, 9):                                                                # big eyes
        put(img, [(ex + 1, 6), (ex, 7), (ex + 1, 7)], P_EYE)
        put(img, [(ex, 6)], SPARKLE)
    put(img, [(4, 8), (11, 8)], P_BLUSH)
    put(img, [(7, 9), (8, 9)], P_MOUTH)
    return shade_egg(img)


# --------------------------------------------------------------- verity ----

SUN = (255, 214, 10)
SUN_LIGHT = (255, 236, 96)
SUN_SHADE = (236, 182, 0)
SUN_DEEP = (210, 150, 0)
INK = (26, 22, 16)


def sunshine(rows):
    """Yellow shaded like a ball: light at the top of a face, deeper towards the bottom."""
    def colour(x, y):
        t = y / max(1, rows - 1)
        return SUN_LIGHT if t < 0.2 else SUN if t < 0.6 else SUN_SHADE if t < 0.9 else SUN_DEEP
    return colour


def paint_sunny_box(img, box, rng):
    sx, sy, sw, sh = box.sides
    fill(img, (sx, sy, sw, sh), rng, sunshine(sh), 2)
    fill(img, box.top, rng, SUN_LIGHT, 2)
    fill(img, box.bottom, rng, SUN_DEEP, 2)


def verity_textures(rng):
    """The smiley face: two black oval eyes and a wide curved smile."""
    img = Image.new("RGBA", (64, 64), CLEAR)
    head = Box(0, 0, 8, 8, 8)
    for box in (head, Box(16, 16, 8, 12, 4), Box(40, 16, 4, 12, 4), Box(0, 16, 4, 12, 4)):
        paint_sunny_box(img, box, rng)
    fx, fy, _, _ = head.front
    put(img, [(fx + 2, fy + 2), (fx + 2, fy + 3), (fx + 5, fy + 2), (fx + 5, fy + 3)], INK)
    put(img, [(fx + 1, fy + 5), (fx + 6, fy + 5), (fx + 2, fy + 6), (fx + 3, fy + 6), (fx + 4, fy + 6), (fx + 5, fy + 6)], INK)

    baby = Image.new("RGBA", (64, 64), CLEAR)
    head = Box(3, 3, 6, 6, 6)
    for box in (head, Box(16, 16, 4, 5, 2), Box(36, 16, 2, 5, 2), Box(28, 16, 2, 5, 2), Box(8, 16, 2, 4, 2), Box(0, 16, 2, 4, 2)):
        paint_sunny_box(baby, box, rng)
    fx, fy, _, _ = head.front
    put(baby, [(fx + 1, fy + 1), (fx + 1, fy + 2), (fx + 4, fy + 1), (fx + 4, fy + 2)], INK)
    put(baby, [(fx, fy + 4), (fx + 5, fy + 4), (fx + 1, fy + 5), (fx + 2, fy + 5), (fx + 3, fy + 5), (fx + 4, fy + 5)], INK)
    return img, baby


def verity_spawn_egg(rng):
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, (a, b) in EGG_ROWS.items():
        for x in range(a, b + 1):
            img.putpixel((x, y), jitter(sunshine(16)(x, y), rng, 2) + (255,))
    put(img, [(6, 5), (6, 6), (9, 5), (9, 6)], INK)
    put(img, [(4, 8), (11, 8), (5, 9), (10, 9), (6, 10), (7, 10), (8, 10), (9, 10)], INK)
    return shade_egg(img)


# ---------------------------------------------------------- royal guards ----

CHAIN = (150, 154, 160)
CHAIN_DARK = (112, 116, 124)
IRON = (210, 214, 220)
TABARD = (176, 30, 44)
LEATHER = (130, 86, 50)
LEATHER_DARK = (96, 60, 34)
HOOD = (64, 128, 60)
HOOD_DARK = (46, 98, 44)
HOOD_LIGHT = (86, 152, 78)
FLETCH = (240, 240, 240)


def paint_guard_face(img, head, rng, hair, hair_rows):
    """Zombie-green face with friendly sparkly eyes and a small smile; hair or hood around it."""
    sx, sy, sw, sh = head.sides
    fill(img, (sx, sy, sw, sh), rng, lambda x, y: hair if y < hair_rows or x >= sw - head.w else K_SKIN)
    fill(img, head.top, rng, hair)
    fill(img, head.bottom, rng, K_SKIN_SHADE)
    fx, fy, w, _ = head.front
    fill(img, (fx, fy, w, w), rng, K_SKIN)
    fill(img, (fx, fy, w, 1), rng, hair)
    if w == 8:
        for ex in (1, 5):
            put(img, [(fx + ex + 1, fy + 3), (fx + ex, fy + 4), (fx + ex + 1, fy + 4)], K_EYE)
            put(img, [(fx + ex, fy + 3)], SPARKLE)
        put(img, [(fx + 1, fy + 2), (fx + 2, fy + 2), (fx + 5, fy + 2), (fx + 6, fy + 2)], K_SKIN_SHADE)  # brows
        put(img, [(fx + 2, fy + 6), (fx + 5, fy + 6), (fx + 3, fy + 7), (fx + 4, fy + 7)], MOUTH)
    else:
        for ex in (1, 4):
            put(img, [(fx + ex, fy + 1)], SPARKLE)
            put(img, [(fx + ex, fy + 2)], K_EYE)
        put(img, [(fx + 1, fy + 4), (fx + 4, fy + 4), (fx + 2, fy + 5), (fx + 3, fy + 5)], MOUTH)


def chainmail(x, y):
    return CHAIN_DARK if (x + y) % 2 else CHAIN


def paint_knight(img, head, body, arms, legs, rng, adult):
    paint_guard_face(img, head, rng, K_HAIR, head.h // 2)
    sx, sy, sw, sh = body.sides
    fill(img, (sx, sy, sw, sh), rng, chainmail)                                       # mail shirt
    fx, fy, fw, fh = body.front
    fill(img, (fx + 1, fy + 1, fw - 2, fh - 1), rng, TABARD)                           # red tabard
    bx = body.u + 2 * body.d + body.w
    fill(img, (bx + 1, fy + 1, fw - 2, fh - 1), rng, TABARD)
    if adult:                                                                          # gold cross
        fill(img, (fx + 3, fy + 2, 2, 7), rng, GOLD)
        fill(img, (fx + 1, fy + 4, 6, 2), rng, GOLD)
    else:
        put(img, [(fx + 1, fy + 2), (fx + 2, fy + 2), (fx + 1, fy + 1), (fx + 1, fy + 3)], GOLD)
    fill(img, body.top, rng, chainmail)
    fill(img, body.bottom, rng, TABARD)
    for arm in arms:
        ax, ay, aw, ah = arm.sides
        fill(img, (ax, ay, aw, ah), rng, chainmail)
        fill(img, (ax, ay + ah - (2 if adult else 1), aw, 2 if adult else 1), rng, LEATHER_DARK)  # gauntlets
        fill(img, arm.top, rng, chainmail)
        fill(img, arm.bottom, rng, LEATHER_DARK)
    for leg in legs:
        lx, ly, lw, lh = leg.sides
        fill(img, (lx, ly, lw, lh), rng, chainmail)
        fill(img, (lx, ly + lh - (3 if adult else 1), lw, 3 if adult else 1), rng, BOOT)
        fill(img, leg.top, rng, chainmail)
        fill(img, leg.bottom, rng, BOOT_DARK)


def paint_archer(img, head, hat, body, arms, legs, rng, adult):
    paint_guard_face(img, head, rng, HOOD, head.h)
    # The hood (hat layer): sides, top and back, with an open front framing the face.
    hx, hy, hw, hh = hat.sides
    fill(img, (hx, hy, hat.d, hh), rng, lambda x, y: HOOD_DARK if x % 3 == 0 else HOOD)
    fill(img, (hx + hat.d + hat.w, hy, hat.d + hat.w, hh), rng, lambda x, y: HOOD_DARK if x % 3 == 0 else HOOD)
    fill(img, hat.top, rng, HOOD_LIGHT)
    fx, fy, fw, fh = hat.front
    fill(img, (fx, fy, fw, 1 if fw < 8 else 2), rng, HOOD_LIGHT)
    fill(img, (fx, fy, 1, fh), rng, HOOD)
    fill(img, (fx + fw - 1, fy, 1, fh), rng, HOOD)
    # Leather tunic with a belt and a quiver strap; the quiver itself on the back.
    sx, sy, sw, sh = body.sides
    fill(img, (sx, sy, sw, sh), rng, lambda x, y: LEATHER_DARK if x % 4 == 0 else LEATHER)
    fill(img, (sx, sy + sh * 2 // 3, sw, 1), rng, LEATHER_DARK)
    fill(img, body.top, rng, HOOD)
    fill(img, body.bottom, rng, LEATHER_DARK)
    bx, by, bw, bh = body.front
    for i in range(min(bw, bh)):
        put(img, [(bx + bw - 1 - i, by + i)], LEATHER_DARK)                            # strap
    back_x = body.u + 2 * body.d + body.w
    if adult:
        fill(img, (back_x + 4, by + 1, 3, 9), rng, LEATHER_DARK)                       # quiver
        put(img, [(back_x + 4, by), (back_x + 6, by)], FLETCH)
        put(img, [(back_x + 5, by)], TABARD)
    for arm in arms:
        ax, ay, aw, ah = arm.sides
        fill(img, (ax, ay, aw, ah), rng, K_SKIN)
        fill(img, (ax, ay, aw, ah // 2), rng, HOOD)                                    # green sleeves
        fill(img, (ax, ay + ah // 2, aw, 1), rng, LEATHER_DARK)                        # bracer
        fill(img, arm.top, rng, HOOD)
        fill(img, arm.bottom, rng, K_SKIN)
    for leg in legs:
        lx, ly, lw, lh = leg.sides
        fill(img, (lx, ly, lw, lh), rng, lambda x, y: HOOD_DARK if x % 4 == 0 else HOOD)
        fill(img, (lx, ly + lh - (3 if adult else 1), lw, 3 if adult else 1), rng, BOOT)
        fill(img, leg.top, rng, HOOD)
        fill(img, leg.bottom, rng, BOOT_DARK)


ADULT_PARTS = dict(head=Box(0, 0, 8, 8, 8), hat=Box(32, 0, 8, 8, 8), body=Box(16, 16, 8, 12, 4),
                   arms=[Box(40, 16, 4, 12, 4)], legs=[Box(0, 16, 4, 12, 4)])
BABY_PARTS = dict(head=Box(3, 3, 6, 6, 6), hat=Box(35, 3, 6, 6, 6), body=Box(16, 16, 4, 5, 2),
                  arms=[Box(36, 16, 2, 5, 2), Box(28, 16, 2, 5, 2)], legs=[Box(8, 16, 2, 4, 2), Box(0, 16, 2, 4, 2)])


def guard_textures(rng):
    out = {}
    for name in ("knight", "archer"):
        for adult, parts in ((True, ADULT_PARTS), (False, BABY_PARTS)):
            img = Image.new("RGBA", (64, 64), CLEAR)
            if name == "knight":
                paint_knight(img, parts["head"], parts["body"], parts["arms"], parts["legs"], rng, adult)
            else:
                paint_archer(img, parts["head"], parts["hat"], parts["body"], parts["arms"], parts["legs"], rng, adult)
            out[(name, adult)] = img
    return out


def knight_spawn_egg(rng):
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, (a, b) in EGG_ROWS.items():
        for x in range(a, b + 1):
            c = IRON if y < 9 else TABARD
            img.putpixel((x, y), jitter(c, rng, 3) + (255,))
    put(img, [(x, 6) for x in range(4, 12)], CHAIN_DARK)                                # visor slit
    put(img, [(7, y) for y in range(10, 14)] + [(8, y) for y in range(10, 14)], GOLD)  # cross
    put(img, [(6, 11), (9, 11)], GOLD)
    return shade_egg(img)


def archer_spawn_egg(rng):
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, (a, b) in EGG_ROWS.items():
        for x in range(a, b + 1):
            c = HOOD if y < 5 or (x in (a, b) and y < 9) else K_SKIN if y < 9 else LEATHER
            img.putpixel((x, y), jitter(c, rng, 3) + (255,))
    for ex in (5, 10):
        put(img, [(ex, 6)], SPARKLE)
        put(img, [(ex, 7)], K_EYE)
    put(img, [(6, 8), (9, 8)], MOUTH)
    put(img, [(4, 10), (5, 11), (6, 12), (7, 12), (8, 12), (9, 12), (10, 11), (11, 10)], (230, 190, 90))  # bow
    return shade_egg(img)


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


def backdrop(size, theme):
    """A square background for icons and logos: desert dusk, sunny meadow or castle wall."""
    bg = Image.new("RGBA", (size, size))
    horizon = int(size * 0.72)
    sky_top, sky_low, ground_top, ground_low, sun = {
        "desert": ((58, 40, 82), (238, 146, 74), (222, 180, 112), (184, 140, 80), (255, 214, 120)),
        "meadow": ((92, 160, 240), (186, 222, 255), (110, 186, 74), (78, 148, 52), (255, 236, 110)),
        "castle": ((70, 110, 200), (160, 200, 255), (128, 128, 128), (96, 96, 96), (255, 236, 160)),
    }[theme]
    rng = random.Random(size)
    for y in range(size):
        for x in range(size):
            if y < horizon:
                c = mix(sky_top, sky_low, y / horizon)
            elif theme == "castle":  # cobblestone wall
                block = max(1, size // 16)
                c = jitter(mix(ground_top, ground_low, ((x // block) * 7 + (y // block) * 3) % 5 / 5), random.Random(x // block * 977 + y // block), 10)
            else:
                c = mix(ground_top, ground_low, (y - horizon) / (size - horizon))
            bg.putpixel((x, y), c + (255,))
    cx, cy = {"desert": (0.72, 0.70), "meadow": (0.22, 0.2), "castle": (0.84, 0.16)}[theme]
    cx, cy, r = size * cx, size * cy, size * 0.12
    for y in range(horizon):
        for x in range(size):
            if (x - cx) ** 2 + (y - cy) ** 2 < r * r:
                bg.putpixel((x, y), sun + (255,))
    if theme == "castle":  # red banners on the wall
        w = max(2, size // 10)
        for bx in (int(size * 0.08), size - int(size * 0.08) - w):
            for y in range(horizon - size // 10, size):
                for x in range(bx, bx + w):
                    bg.putpixel((x, y), (176, 30, 44, 255) if (x - bx) not in (0, w - 1) else (238, 192, 62, 255))
    return bg


def portrait(skin, height):
    """Head and torso (16x20 pixels of the front view), scaled to the given height."""
    scale = max(1, height // 20)
    return front_view(skin).crop((0, 0, 16, 20)).resize((16 * scale, 20 * scale), Image.NEAREST)


def icon(skin, size, theme="desert"):
    bg = backdrop(size, theme)
    fig = portrait(skin, size * 9 // 10)
    bg.alpha_composite(fig, ((size - fig.width) // 2, size - fig.height))
    return bg


def kingdom_icon(king, princess, size):
    """The king and the princess side by side in front of their castle wall."""
    bg = backdrop(size, "castle")
    king_fig = portrait(king, size * 3 // 4)
    princess_fig = portrait(princess, size * 3 // 5)
    king_at = (size // 2 - king_fig.width + size // 16, size - king_fig.height)
    princess_at = (size // 2 + size // 12, size - princess_fig.height)
    bg.alpha_composite(king_fig, king_at)
    bg.alpha_composite(princess_fig, princess_at)
    # The crowns are 3D model parts in game, so paint them onto the portraits.
    king_crown = [(x, -2, GOLD) for x in range(3, 13)] + [(x, -1, GOLD_DARK) for x in range(3, 13)] \
        + [(x, -3, GOLD_LIGHT) for x in (3, 12)] + [(x, y, GOLD_LIGHT) for x in (7, 8) for y in (-4, -3)] \
        + [(7, -2, RUBY), (8, -2, RUBY), (5, -2, SAPPHIRE), (10, -2, SAPPHIRE)]
    princess_crown = [(x, -1, GOLD) for x in range(5, 11)] + [(5, -2, PEARL), (10, -2, PEARL)] \
        + [(7, -2, GOLD), (8, -2, GOLD), (7, -3, ROSE), (8, -3, ROSE)]
    for crown, fig, (fx, fy) in ((king_crown, king_fig, king_at), (princess_crown, princess_fig, princess_at)):
        unit = fig.width // 16
        for px, py, colour in crown:
            for dx in range(unit):
                for dy in range(unit):
                    bg.putpixel((fx + px * unit + dx, fy + py * unit + dy), colour + (255,))
    return bg


def save(img, *path):
    os.makedirs(os.path.dirname(os.path.join(*path)), exist_ok=True)
    img.save(os.path.join(*path))


def main():
    # ---- Mummy ----
    skin, eyes = adult_textures(random.Random(1922))  # Tutankhamun's tomb, 1922
    baby, baby_eyes = baby_textures(random.Random(1923))
    mummy_entity = os.path.join(MUMMY_ASSETS, "textures", "entity", "mummy")
    save(skin, mummy_entity, "mummy.png")
    save(eyes, mummy_entity, "mummy_eyes.png")
    save(baby, mummy_entity, "mummy_baby.png")
    save(baby_eyes, mummy_entity, "mummy_baby_eyes.png")
    save(spawn_egg(random.Random(7)), MUMMY_ASSETS, "textures", "item", "mummy_spawn_egg.png")
    save(icon(skin, 128), MUMMY_ASSETS, "icon.png")
    save(icon(skin, 400), CURSEFORGE, "mummy", "logo.png")

    # ---- Verity ----
    verity, verity_baby = verity_textures(random.Random(1963))
    save(verity, VERITY_ASSETS, "textures", "entity", "verity", "verity.png")
    save(verity_baby, VERITY_ASSETS, "textures", "entity", "verity", "verity_baby.png")
    save(verity_spawn_egg(random.Random(10)), VERITY_ASSETS, "textures", "item", "verity_spawn_egg.png")
    save(icon(verity, 128, "meadow"), VERITY_ASSETS, "icon.png")
    save(icon(verity, 400, "meadow"), CURSEFORGE, "verity", "logo.png")

    # ---- Zombie Kingdom ----
    entity = os.path.join(KINGDOM_ASSETS, "textures", "entity")
    item = os.path.join(KINGDOM_ASSETS, "textures", "item")
    king, king_baby = king_textures(random.Random(1066))
    save(king, entity, "zombie_king", "zombie_king.png")
    save(king_baby, entity, "zombie_king", "zombie_king_baby.png")
    save(regalia_texture(random.Random(1067)), entity, "zombie_king", "zombie_king_regalia.png")
    save(king_spawn_egg(random.Random(8)), item, "zombie_king_spawn_egg.png")
    princess, princess_baby = princess_textures(random.Random(1533))
    save(princess, entity, "zombie_princess", "zombie_princess.png")
    save(princess_baby, entity, "zombie_princess", "zombie_princess_baby.png")
    save(princess_regalia_texture(random.Random(1534)), entity, "zombie_princess", "zombie_princess_regalia.png")
    save(princess_spawn_egg(random.Random(9)), item, "zombie_princess_spawn_egg.png")
    for (name, adult), img in guard_textures(random.Random(1215)).items():
        save(img, entity, "zombie_" + name, "zombie_" + name + ("" if adult else "_baby") + ".png")
    save(knight_spawn_egg(random.Random(11)), item, "zombie_knight_spawn_egg.png")
    save(archer_spawn_egg(random.Random(12)), item, "zombie_archer_spawn_egg.png")
    save(kingdom_icon(king, princess, 128), KINGDOM_ASSETS, "icon.png")
    save(kingdom_icon(king, princess, 400), CURSEFORGE, "zombie-kingdom", "logo.png")
    print("Textures written for mummy, verity and zombie-kingdom")


if __name__ == "__main__":
    main()
