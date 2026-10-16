#!/usr/bin/env python3
"""Generates the Sura Engine launcher icons (RGBA PNG) with no external dependencies.

Run:  python3 tools/make_icons.py
"""
import os, struct, zlib, math

SS = 4  # supersampling factor
OUT = os.path.join(os.path.dirname(__file__), "..", "android", "app", "src", "main", "res")

BG_TOP = (30, 30, 46)      # #1E1E2E
BG_BOTTOM = (17, 17, 27)   # #11111B
FRAME = (69, 71, 90)       # #45475A
ACCENT_A = (137, 180, 250) # #89B4FA
ACCENT_B = (116, 199, 236) # #74C7EC
DOT_RED = (243, 139, 168)
DOT_YELLOW = (249, 226, 175)
DOT_GREEN = (166, 227, 161)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def rounded_rect_inside(x, y, left, top, right, bottom, radius):
    if x < left or x > right or y < top or y > bottom:
        return False
    cx = min(max(x, left + radius), right - radius)
    cy = min(max(y, top + radius), bottom - radius)
    dx = x - cx
    dy = y - cy
    return dx * dx + dy * dy <= radius * radius


def circle_inside(x, y, cx, cy, r):
    dx = x - cx
    dy = y - cy
    return dx * dx + dy * dy <= r * r


def triangle_inside(x, y, a, b, c):
    def sign(p1, p2, p3):
        return (p1[0] - p3[0]) * (p2[1] - p3[1]) - (p2[0] - p3[0]) * (p1[1] - p3[1])
    d1 = sign((x, y), a, b)
    d2 = sign((x, y), b, c)
    d3 = sign((x, y), c, a)
    has_neg = d1 < 0 or d2 < 0 or d3 < 0
    has_pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (has_neg and has_pos)


def render(size):
    N = size
    px = bytearray(N * N * 4)
    radius = N * 0.22
    frame = (N * 0.13, N * 0.16, N * 0.87, N * 0.84)
    frame_radius = N * 0.10
    stroke = max(2.0, N * 0.022)
    # play triangle geometry (centred)
    cx, cy = N * 0.5, N * 0.53
    tri = N * 0.20
    a = (cx - tri * 0.55, cy - tri)
    b = (cx - tri * 0.55, cy + tri)
    c = (cx + tri * 0.95, cy)
    dots_r = N * 0.032
    dots_y = N * 0.235
    dots = ((N * 0.215, DOT_RED), (N * 0.295, DOT_YELLOW), (N * 0.375, DOT_GREEN))

    for y in range(N):
        for x in range(N):
            fx, fy = x + 0.5, y + 0.5
            # background rounded square with vertical gradient
            if not rounded_rect_inside(fx, fy, 0, 0, N - 1, N - 1, radius):
                continue
            t = fy / N
            r, g, bl = lerp(BG_TOP, BG_BOTTOM, t)
            # subtle vignette
            dx = (fx - N / 2) / (N / 2)
            dy = (fy - N / 2) / (N / 2)
            d = min(1.0, math.sqrt(dx * dx + dy * dy))
            shade = 1.0 - 0.18 * d
            r = int(r * shade); g = int(g * shade); bl = int(bl * shade)
            alpha = 255

            # scene-view frame (outline)
            outer = rounded_rect_inside(fx, fy, frame[0] - stroke, frame[1] - stroke, frame[2] + stroke, frame[3] + stroke, frame_radius + stroke)
            inner = rounded_rect_inside(fx, fy, frame[0] + stroke, frame[1] + stroke, frame[2] - stroke, frame[3] - stroke, max(1.0, frame_radius - stroke))
            if outer and not inner:
                r, g, bl = FRAME
            elif inner:
                t2 = (fy - frame[1]) / (frame[3] - frame[1])
                r, g, bl = lerp((26, 26, 40), (17, 17, 27), max(0.0, min(1.0, t2)))

            # traffic light dots
            for dot_x, color in dots:
                if circle_inside(fx, fy, dot_x, dots_y, dots_r):
                    r, g, bl = color

            # play triangle with a soft accent gradient
            if triangle_inside(fx, fy, a, b, c):
                k = (fx - (cx - tri)) / (tri * 1.6)
                r, g, bl = lerp(ACCENT_A, ACCENT_B, max(0.0, min(1.0, k)))
            # glow ring behind the triangle
            else:
                dist = math.hypot(fx - cx, fy - cy)
                if abs(dist - tri * 1.25) < stroke * 1.2:
                    r = int(r * 0.75 + ACCENT_A[0] * 0.25)
                    g = int(g * 0.75 + ACCENT_A[1] * 0.25)
                    bl = int(bl * 0.75 + ACCENT_A[2] * 0.25)

            i = (y * N + x) * 4
            px[i] = r
            px[i + 1] = g
            px[i + 2] = bl
            px[i + 3] = alpha
    return px


def downsample(src, src_size, dst_size):
    factor = src_size // dst_size
    dst = bytearray(dst_size * dst_size * 4)
    for y in range(dst_size):
        for x in range(dst_size):
            r = g = b = 0
            for sy in range(factor):
                for sx in range(factor):
                    i = ((y * factor + sy) * src_size + (x * factor + sx)) * 4
                    r += src[i]; g += src[i + 1]; b += src[i + 2]
            n = factor * factor
            i = (y * dst_size + x) * 4
            dst[i] = r // n
            dst[i + 1] = g // n
            dst[i + 2] = b // n
            dst[i + 3] = 255
    return dst


def write_png(path, size, pixels):
    raw = b''.join(b'\x00' + bytes(pixels[y * size * 4:(y + 1) * size * 4]) for y in range(size))

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(raw, 9))
    png += chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(png)


def main():
    master = 1024
    print("rendering %dx%d master icon..." % (master, master))
    pixels = render(master)
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in densities.items():
        scaled = downsample(pixels, master, size)
        path = os.path.join(OUT, folder, "ic_launcher.png")
        write_png(path, size, scaled)
        print("wrote", path, size)
    # round variant for launchers that ask for it
    for folder, size in densities.items():
        scaled = downsample(pixels, master, size)
        path = os.path.join(OUT, folder, "ic_launcher_round.png")
        write_png(path, size, scaled)
        print("wrote", path, size)
    # a large copy for documentation / store listings
    write_png(os.path.join(OUT, "..", "..", "..", "..", "docs", "icon-512.png"), 512, downsample(pixels, master, 512))
    print("done")


if __name__ == "__main__":
    main()
