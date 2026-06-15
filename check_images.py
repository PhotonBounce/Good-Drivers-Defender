import hashlib
import glob
import os

images = glob.glob(r"d:\good-drivers-defender\real_screenshots\*.png")
print(f"Checking {len(images)} images...")

for img in sorted(images):
    with open(img, "rb") as f:
        data = f.read()
        md5 = hashlib.md5(data).hexdigest()
        print(f"{os.path.basename(img)}: size={len(data)} bytes, md5={md5}")
