"""
Remove backgrounds from all PNG images in the drawable folder.
Keeps object interiors intact by using rembg's U2NET model.
Overwrites originals with transparent-background versions.
"""
import os
from pathlib import Path
from PIL import Image
from rembg import remove, new_session

DRAWABLE = Path("/Users/ponepuck/Library/CloudStorage/OneDrive-Personal/workSpace/HEl/app/src/main/res/drawable")

# Create a session once (downloads model on first run)
session = new_session("u2net")

png_files = sorted(DRAWABLE.glob("*.png"))
print(f"Found {len(png_files)} PNG files to process.\n")

for png in png_files:
    print(f"Processing: {png.name} ... ", end="", flush=True)
    try:
        img = Image.open(png).convert("RGBA")
        result = remove(
            img,
            session=session,
            alpha_matting=True,           # refine edges
            alpha_matting_foreground_threshold=240,
            alpha_matting_background_threshold=10,
            alpha_matting_erode_size=10,
        )
        result.save(png, "PNG")
        print("✅ done")
    except Exception as e:
        print(f"❌ error: {e}")

print("\nAll done!")
