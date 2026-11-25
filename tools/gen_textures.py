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
    # M1c: copper cable.
    ("src/main/resources/assets/ironworks/textures/block/copper_cable.png", (0xb8, 0x73, 0x33)),
    # M1d: tin ore, machine casing, crusher, electric furnace, and processed materials.
    ("src/main/resources/assets/ironworks/textures/block/tin_ore.png", (0x8f, 0x96, 0x9c)),
    ("src/main/resources/assets/ironworks/textures/block/deepslate_tin_ore.png", (0x5a, 0x60, 0x66)),
    ("src/main/resources/assets/ironworks/textures/block/machine_casing.png", (0x7d, 0x86, 0x8c)),
    ("src/main/resources/assets/ironworks/textures/block/crusher_front.png", (0x4d, 0x53, 0x58)),
    ("src/main/resources/assets/ironworks/textures/block/crusher_front_lit.png", (0xd0, 0x8c, 0x2a)),
    ("src/main/resources/assets/ironworks/textures/block/crusher_side.png", (0x6a, 0x72, 0x78)),
    ("src/main/resources/assets/ironworks/textures/block/crusher_top.png", (0x8a, 0x92, 0x98)),
    ("src/main/resources/assets/ironworks/textures/block/electric_furnace_front.png", (0x3c, 0x3c, 0x3c)),
    ("src/main/resources/assets/ironworks/textures/block/electric_furnace_front_lit.png", (0xe0, 0x4a, 0x1a)),
    ("src/main/resources/assets/ironworks/textures/block/electric_furnace_side.png", (0x55, 0x55, 0x55)),
    ("src/main/resources/assets/ironworks/textures/block/electric_furnace_top.png", (0x6e, 0x6e, 0x6e)),
    ("src/main/resources/assets/ironworks/textures/item/raw_tin.png", (0xc2, 0xa0, 0x84)),
    ("src/main/resources/assets/ironworks/textures/item/tin_ingot.png", (0xc9, 0xd1, 0xd6)),
    ("src/main/resources/assets/ironworks/textures/item/tin_dust.png", (0xb4, 0xbc, 0xc2)),
    ("src/main/resources/assets/ironworks/textures/item/iron_dust.png", (0xa8, 0xa8, 0xa8)),
    ("src/main/resources/assets/ironworks/textures/item/copper_dust.png", (0xd0, 0x7a, 0x4a)),
    ("src/main/resources/assets/ironworks/textures/item/gold_dust.png", (0xe6, 0xc2, 0x3c)),
    ("src/main/resources/assets/ironworks/textures/item/bronze_ingot.png", (0xb0, 0x7a, 0x3a)),
    # M1e: silicon ore, alloy furnace, compressor, circuit assembler, and processed materials.
    ("src/main/resources/assets/ironworks/textures/block/silicon_ore.png", (0x9a, 0x9f, 0xa6)),
    ("src/main/resources/assets/ironworks/textures/block/deepslate_silicon_ore.png", (0x62, 0x67, 0x6e)),
    ("src/main/resources/assets/ironworks/textures/block/alloy_furnace_front.png", (0x6a, 0x4a, 0x3a)),
    ("src/main/resources/assets/ironworks/textures/block/alloy_furnace_front_lit.png", (0xe8, 0x7a, 0x2a)),
    ("src/main/resources/assets/ironworks/textures/block/alloy_furnace_side.png", (0x5e, 0x5a, 0x58)),
    ("src/main/resources/assets/ironworks/textures/block/alloy_furnace_top.png", (0x7a, 0x76, 0x72)),
    ("src/main/resources/assets/ironworks/textures/block/compressor_front.png", (0x4a, 0x50, 0x58)),
    ("src/main/resources/assets/ironworks/textures/block/compressor_front_lit.png", (0x5f, 0xb0, 0xe0)),
    ("src/main/resources/assets/ironworks/textures/block/compressor_side.png", (0x66, 0x6e, 0x76)),
    ("src/main/resources/assets/ironworks/textures/block/compressor_top.png", (0x80, 0x88, 0x90)),
    ("src/main/resources/assets/ironworks/textures/block/circuit_assembler_front.png", (0x3e, 0x5a, 0x3e)),
    ("src/main/resources/assets/ironworks/textures/block/circuit_assembler_front_lit.png", (0x7a, 0xe0, 0x5a)),
    ("src/main/resources/assets/ironworks/textures/block/circuit_assembler_side.png", (0x58, 0x6e, 0x58)),
    ("src/main/resources/assets/ironworks/textures/block/circuit_assembler_top.png", (0x6e, 0x84, 0x6e)),
    ("src/main/resources/assets/ironworks/textures/item/steel_ingot.png", (0x8e, 0x94, 0x9a)),
    ("src/main/resources/assets/ironworks/textures/item/iron_plate.png", (0xbc, 0xbc, 0xc0)),
    ("src/main/resources/assets/ironworks/textures/item/copper_plate.png", (0xe0, 0x8a, 0x5a)),
    ("src/main/resources/assets/ironworks/textures/item/steel_plate.png", (0x9a, 0xa2, 0xaa)),
    ("src/main/resources/assets/ironworks/textures/item/gold_foil.png", (0xf2, 0xd4, 0x5a)),
    ("src/main/resources/assets/ironworks/textures/item/plastic_sheet.png", (0xe8, 0xe8, 0xe0)),
    ("src/main/resources/assets/ironworks/textures/item/silicon.png", (0x3a, 0x3f, 0x47)),
    ("src/main/resources/assets/ironworks/textures/item/silicon_wafer.png", (0x4f, 0x6a, 0x8a)),
    ("src/main/resources/assets/ironworks/textures/item/basic_circuit.png", (0x3c, 0x9a, 0x4c)),
    ("src/main/resources/assets/ironworks/textures/item/advanced_circuit.png", (0x9a, 0x4c, 0xc8)),
    # M1f: electronic components (resistor, diode, transistor, IC chips).
    ("src/main/resources/assets/ironworks/textures/item/resistor.png", (0xc4, 0x8a, 0x3e)),
    ("src/main/resources/assets/ironworks/textures/item/diode.png", (0x2a, 0x2a, 0x30)),
    ("src/main/resources/assets/ironworks/textures/item/transistor.png", (0x6a, 0x7a, 0x8a)),
    ("src/main/resources/assets/ironworks/textures/item/ic_chip_basic.png", (0x2e, 0x5a, 0x8a)),
    ("src/main/resources/assets/ironworks/textures/item/ic_chip_advanced.png", (0x6a, 0x2e, 0x8a)),
    # M2: conveyor, splitter and sorter sides (solid placeholders).
    ("src/main/resources/assets/ironworks/textures/block/conveyor_side.png", (0x4a, 0x4f, 0x55)),
    ("src/main/resources/assets/ironworks/textures/block/splitter_top.png", (0x4f, 0x6e, 0x58)),
    ("src/main/resources/assets/ironworks/textures/block/splitter_side.png", (0x3e, 0x58, 0x46)),
    ("src/main/resources/assets/ironworks/textures/block/sorter_top.png", (0x6e, 0x58, 0x7a)),
    ("src/main/resources/assets/ironworks/textures/block/sorter_side.png", (0x52, 0x40, 0x5c)),
    # M3a: server rack (front_lit shown while powered), network cable, storage port, and drive icons per tier.
    ("src/main/resources/assets/ironworks/textures/block/server_rack_front.png", (0x33, 0x3a, 0x44)),
    ("src/main/resources/assets/ironworks/textures/block/server_rack_front_lit.png", (0x3f, 0xc0, 0x6a)),
    ("src/main/resources/assets/ironworks/textures/block/server_rack_side.png", (0x4a, 0x52, 0x5c)),
    ("src/main/resources/assets/ironworks/textures/block/server_rack_top.png", (0x5c, 0x64, 0x6e)),
    ("src/main/resources/assets/ironworks/textures/block/network_cable.png", (0x3a, 0xa8, 0xd8)),
    ("src/main/resources/assets/ironworks/textures/block/storage_port.png", (0x6e, 0x4f, 0x9a)),
    ("src/main/resources/assets/ironworks/textures/item/drive_1k.png", (0x9a, 0xc8, 0xe8)),
    ("src/main/resources/assets/ironworks/textures/item/drive_4k.png", (0x4a, 0x9a, 0xd8)),
    ("src/main/resources/assets/ironworks/textures/item/drive_16k.png", (0x1e, 0x5f, 0xa8)),
]

# Patterned textures: (relative output path, 16x16 rows of RGB tuples).
# The conveyor top shows an arrow pointing to the top edge of the image. The top face is drawn with the image's top
# edge toward north, so the arrow points north at facing=north and the blockstate rotates it for the other facings.
CONVEYOR_BG = (0x3a, 0x3f, 0x45)
CONVEYOR_ARROW = (0xf0, 0xc0, 0x30)


def arrow_rows(bg: tuple, fg: tuple) -> list:
    rows = [[bg] * SIZE for _ in range(SIZE)]
    # Head: rows 2..7, widening by one pixel per side per row (7..8 at the tip, 2..13 at the base).
    for y in range(2, 8):
        half = y - 2
        for x in range(7 - half, 9 + half):
            rows[y][x] = fg
    # Shaft: rows 8..13, columns 6..9.
    for y in range(8, 14):
        for x in range(6, 10):
            rows[y][x] = fg
    return rows


PATTERN_TEXTURES = [
    ("src/main/resources/assets/ironworks/textures/block/conveyor_top.png", arrow_rows(CONVEYOR_BG, CONVEYOR_ARROW)),
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


def pattern_png(rows: list) -> bytes:
    signature = b"\x89PNG\r\n\x1a\n"
    height = len(rows)
    width = len(rows[0])
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in rows)
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
    for rel_path, rows in PATTERN_TEXTURES:
        out_path = os.path.join(project_root, rel_path)
        os.makedirs(os.path.dirname(out_path), exist_ok=True)
        with open(out_path, "wb") as f:
            f.write(pattern_png(rows))
        print(f"wrote {rel_path} ({SIZE}x{SIZE}, arrow pattern)")


if __name__ == "__main__":
    main()
