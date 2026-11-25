#!/usr/bin/env python3
"""Generate placeholder 16x16 solid-color PNG textures for Ironworks.

Uses only the Python standard library (zlib, struct).
Run from anywhere: output paths are resolved relative to this script's project root.
"""
import os
import struct
import zlib

SIZE = 16

# (relative output path under project root, RGB color)
TEXTURES = [
    ("src/main/resources/assets/ironworks/textures/block/test_block.png", (0xB4, 0xB4, 0xB4)),
]


def png_chunk(tag: bytes, data: bytes) -> bytes:
    crc = zlib.crc32(tag + data) & 0xFFFFFFFF
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)


def solid_png(width: int, height: int, rgb: tuple) -> bytes:
    signature = b"\x89PNG\r\n\x1a\n"
    # bit depth 8, color type 2 (RGB), compression 0, filter 0, interlace 0
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    row = b"\x00" + bytes(rgb) * width  # filter type 0 (None) + pixels
    raw = row * height
    idat = zlib.compress(raw, 9)
    return signature + png_chunk(b"IHDR", ihdr) + png_chunk(b"IDAT", idat) + png_chunk(b"IEND", b"")


def main() -> None:
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    for rel_path, rgb in TEXTURES:
        out_path = os.path.join(project_root, rel_path)
        os.makedirs(os.path.dirname(out_path), exist_ok=True)
        with open(out_path, "wb") as f:
            f.write(solid_png(SIZE, SIZE, rgb))
        print(f"wrote {rel_path} ({SIZE}x{SIZE}, rgb={rgb})")


if __name__ == "__main__":
    main()
