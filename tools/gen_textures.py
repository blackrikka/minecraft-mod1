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
    ("src/main/resources/assets/ironworks/textures/block/test_block.png", (0xb4, 0xb4, 0xb4)),
    ("src/main/resources/assets/ironworks/textures/block/rubber_log_side.png", (0x3b, 0x2a, 0x1e)),
    ("src/main/resources/assets/ironworks/textures/block/rubber_log_side_sap.png", (0xb0, 0x6e, 0x1e)),
    ("src/main/resources/assets/ironworks/textures/block/rubber_log_top.png", (0x6e, 0x52, 0x36)),
    ("src/main/resources/assets/ironworks/textures/block/rubber_planks.png", (0x9c, 0x6b, 0x3e)),
    ("src/main/resources/assets/ironworks/textures/block/rubber_leaves.png", (0x2e, 0x6b, 0x2a)),
    ("src/main/resources/assets/ironworks/textures/block/rubber_sapling.png", (0x4c, 0x8f, 0x3a)),
    ("src/main/resources/assets/ironworks/textures/item/rubber_sap.png", (0xd9, 0x9a, 0x2b)),
    ("src/main/resources/assets/ironworks/textures/item/rubber.png", (0x22, 0x22, 0x22)),
    # M1b: combustion generator and development battery.
    ("src/main/resources/assets/ironworks/textures/block/combustion_generator_front.png", (0x5a, 0x5a, 0x5a)),
    ("src/main/resources/assets/ironworks/textures/block/combustion_generator_front_lit.png", (0xc0, 0x58, 0x0e)),
    ("src/main/resources/assets/ironworks/textures/block/combustion_generator_side.png", (0x6e, 0x6e, 0x6e)),
    ("src/main/resources/assets/ironworks/textures/block/combustion_generator_top.png", (0x8a, 0x8a, 0x8a)),
    ("src/main/resources/assets/ironworks/textures/block/creative_battery.png", (0x7a, 0x2f, 0xb0)),
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
