import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

# Layered blocky voxel hills. Back layers = bigger blocks + more haze (LOD / far distance),
# front layers = small detailed blocks. Usage: voxy_art2.py out.png W H [banner] [text]

OUT = sys.argv[1]; W = int(sys.argv[2]); H = int(sys.argv[3])
BANNER = (len(sys.argv) > 4 and sys.argv[4] == "banner")
TEXT = (len(sys.argv) > 5 and sys.argv[5] == "text")

rng = np.random.default_rng(7)

# ---- sky ----
img = Image.new("RGB", (W, H))
px = img.load()
top = np.array([28, 46, 102]); mid = np.array([74, 138, 214]); hor = np.array([196, 224, 240])
for y in range(H):
    t = y/(H-1)
    c = top+(mid-top)*(t/0.66) if t < 0.66 else mid+(hor-mid)*((t-0.66)/0.34)
    row = (int(c[0]), int(c[1]), int(c[2]))
    for x in range(W):
        px[x, y] = row

# ---- sun glow ----
glow = Image.new("RGBA", (W, H), (0,0,0,0)); gd = ImageDraw.Draw(glow)
sx, sy = int(W*0.72), int(H*0.60)
for rad, a in [(int(W*0.34),26),(int(W*0.22),34),(int(W*0.13),54),(int(W*0.06),120)]:
    gd.ellipse([sx-rad, sy-rad, sx+rad, sy+rad], fill=(255,246,214,a))
glow = glow.filter(ImageFilter.GaussianBlur(W*0.02))
img = Image.alpha_composite(img.convert("RGBA"), glow).convert("RGB")
draw = ImageDraw.Draw(img)

def noise1d(n, octaves):
    h = np.zeros(n); tot = 0.0; amp = 1.0
    for o in range(octaves):
        res = 2*(2**o)
        g = rng.random(res+1)
        xs = np.linspace(0, res, n)
        i0 = np.floor(xs).astype(int); f = xs-i0
        f = f*f*(3-2*f)
        h += amp*(g[i0]*(1-f)+g[np.minimum(i0+1,res)]*f); tot += amp; amp *= 0.5
    h /= tot
    return (h-h.min())/(h.max()-h.min()+1e-9)

# layers: (block_size_px, base_y_frac, amp_frac, grass, haze)  back -> front
horizon = H*0.60
layers = [
    (max(6, int(W*0.060)), 0.62, 0.10, (150,178,170), 0.55),
    (max(5, int(W*0.044)), 0.66, 0.15, (118,162,128), 0.40),
    (max(4, int(W*0.030)), 0.72, 0.22, (92,158,96),  0.24),
    (max(3, int(W*0.020)), 0.80, 0.30, (74,150,78),  0.10),
    (max(2, int(W*0.013)), 0.90, 0.40, (60,138,66),  0.0),
]
sky_ref = np.array([196,224,240])

for bs, basef, ampf, grass, haze in layers:
    ncols = W//bs + 2
    hm = noise1d(ncols, 4)
    base_y = H*basef
    amp = H*ampf
    grass = np.array(grass, float)
    side = grass*0.66
    g = tuple(int(v) for v in (grass*(1-haze)+sky_ref*haze))
    s = tuple(int(v) for v in (side*(1-haze)+sky_ref*haze))
    for c in range(ncols):
        x0 = c*bs
        top_y = int(base_y - hm[c]*amp)
        top_y = (top_y//bs)*bs   # quantise to block grid for a voxel look
        # grass top block
        draw.rectangle([x0, top_y, x0+bs-1, top_y+bs-1], fill=g)
        # dirt/side body to bottom
        if top_y+bs < H:
            draw.rectangle([x0, top_y+bs, x0+bs-1, H], fill=s)
        # block separation + top highlight only on the larger (LOD) blocks, for a clean voxel read
        if bs >= int(W*0.028):
            draw.line([(x0, top_y),(x0, H)], fill=tuple(int(v*0.85) for v in s), width=1)
            draw.line([(x0, top_y),(x0+bs-1, top_y)], fill=tuple(min(255,int(v*1.18)) for v in g), width=1)

# vignette
vig = Image.new("L", (W, H), 0); vd = ImageDraw.Draw(vig)
vd.ellipse([-W*0.2, -H*0.2, W*1.2, H*1.2], fill=255)
vig = vig.filter(ImageFilter.GaussianBlur(W*0.12))
dark = Image.new("RGB", (W, H), (10,16,34))
img = Image.composite(img, dark, vig)

if TEXT:
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arialbd.ttf", int(H*0.34))
    except Exception:
        font = ImageFont.load_default()
    txt = "VOXY"
    bb = d.textbbox((0,0), txt, font=font)
    tw, th = bb[2]-bb[0], bb[3]-bb[1]
    tx, ty = (W-tw)//2 - bb[0], int(H*0.06)
    for dx in range(-3,4):
        for dy in range(-3,4):
            d.text((tx+dx, ty+dy), txt, font=font, fill=(8,18,30))
    d.text((tx, ty), txt, font=font, fill=(245,250,255))

img.save(OUT)
print("wrote", OUT, img.size)
