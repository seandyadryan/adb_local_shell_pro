"""Render original store compositions from unmodified screenshots of the native app.

Requires Pillow and imageio-ffmpeg. Run after device_qa.py capture.
No network calls, synthetic app screens or external stock media.
"""
from pathlib import Path
import argparse
import json
import math
import subprocess
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
STORE = ROOT / 'store'
INK, PANEL, LIME, PAPER, MUTED, LINE = '#101410', '#1b231b', '#d5f58b', '#f1f3e9', '#a5afa1', '#354232'
FONT = ROOT / 'app/src/main/res/font/space_bold.ttf'
MEDIUM = ROOT / 'app/src/main/res/font/space_medium.ttf'

FEATURES = [
    ('shell', 'Your device.\nYour command.', 'Run a local ADB shell.\nSee useful answers as they arrive.', '01 / LOCAL SHELL'),
    ('commands', 'Good commands.\nAlways close.', 'A useful command library.\nRoom for your own shortcuts.', '02 / SAVED COMMANDS'),
    ('connect', 'No cables.\nJust Android.', 'Pair with Wireless debugging.\nConnect directly on your device.', '03 / WIRELESS PAIRING'),
    ('history', 'Pick up where\nyou left off.', 'Revisit results. Reuse commands.\nExport a record when you need one.', '04 / COMMAND HISTORY'),
    ('settings', 'A workspace\nthat stays yours.', 'Local history. Adjustable text.\nNo ads. No account.', '05 / YOUR WORKSPACE'),
]

def font(size, bold=True):
    return ImageFont.truetype(str(FONT if bold else MEDIUM), size)

def text(im, xy, value, size, fill=PAPER, bold=True, spacing=None):
    draw = ImageDraw.Draw(im)
    draw.multiline_text(xy, value, font=font(size, bold), fill=fill, spacing=spacing if spacing is not None else int(size * .13), stroke_width=0)

def line(im, xy, fill=LINE, width=1):
    ImageDraw.Draw(im).line(xy, fill=fill, width=width)

def mark(im, x, y, size, background=LIME, foreground=INK):
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((x, y, x+size, y+size), radius=int(size*.22), fill=background)
    s = size / 108
    def p(points): return [(round(x+a*s), round(y+b*s)) for a,b in points]
    d.polygon(p([(28,32),(38,32),(58,52),(58,56),(38,76),(28,76),(50,54)]), fill=foreground)
    d.rectangle((x+58*s,y+68*s,x+80*s,y+76*s), fill=foreground)
    d.rectangle((x+73*s,y+29*s,x+79*s,y+35*s), fill=foreground)

def brand(im, x, y, scale=1):
    mark(im,x,y,int(48*scale))
    text(im,(x+int(65*scale),y+int(3*scale)), 'LOCAL SHELL', int(24*scale))
    text(im,(x+int(65*scale),y+int(30*scale)), 'PRO / ANDROID WORKSPACE', int(10*scale), MUTED, False)

def background(w,h):
    im = Image.new('RGB',(w,h),INK)
    d = ImageDraw.Draw(im)
    # Technical ruled-paper detail, restrained enough to keep the product central.
    for x in range(int(w*.66),w,max(30,w//24)):
        d.line((x,0,x,h), fill='#141b13', width=1)
    d.line((int(w*.66),0,int(w*.66),h),fill='#263222',width=1)
    return im

def device(path, target_h, kind='phone'):
    shot=Image.open(path).convert('RGB')
    ratio=shot.width/shot.height
    bezel=max(10,round(target_h*.012))
    hh=target_h-bezel*2
    ww=round(hh*ratio)
    corner=round(target_h*(.046 if kind=='phone' else .026))
    out=Image.new('RGBA',(ww+bezel*2,target_h),(0,0,0,0))
    d=ImageDraw.Draw(out)
    d.rounded_rectangle((0,0,out.width-1,out.height-1),radius=corner,fill='#050705',outline='#65705e',width=max(2,bezel//4))
    resized=shot.resize((ww,hh),Image.Resampling.LANCZOS)
    mask=Image.new('L',resized.size,0)
    ImageDraw.Draw(mask).rounded_rectangle((0,0,ww-1,hh-1),radius=max(8,corner-bezel),fill=255)
    out.paste(resized,(bezel,bezel),mask)
    # Only the bezel is illustrated; screen pixels are captured app pixels.
    if kind=='phone':
        radius=max(4,round(target_h*.005))
        cx=out.width//2; cy=bezel+int(hh*.016)
        d.ellipse((cx-radius,cy-radius,cx+radius,cy+radius),fill='#020302')
    return out

def paste_device(im, dev, x,y):
    shadow=Image.new('RGBA',im.size,(0,0,0,0))
    silhouette=Image.new('RGBA',dev.size,(0,0,0,0))
    silhouette.putalpha(dev.getchannel('A').point(lambda p:round(p*.55)))
    shadow.alpha_composite(silhouette,(int(x)+10,int(y)+20))
    shadow=shadow.filter(ImageFilter.GaussianBlur(22))
    base=im.convert('RGBA'); base.alpha_composite(shadow); base.alpha_composite(dev,(int(x),int(y)))
    return base.convert('RGB')

def phone_art(index, feature):
    name,title,desc,label=feature
    im=background(1080,1920)
    brand(im,76,68,1.25)
    text(im,(76,178),title,88,spacing=0)
    text(im,(80,403),desc,30,MUTED,False,spacing=9)
    dev=device(STORE/f'raw/phone-{name}.png',1230)
    im=paste_device(im,dev,(1080-dev.width)//2,574)
    line(im,(76,1850,1004,1850))
    text(im,(78,1870),label,18,LIME,False)
    text(im,(865,1870),f'{index:02d} / 05',18,MUTED,False)
    return im

def landscape_art(feature, w,h, prefix, label):
    name,title,desc,_=feature
    im=background(w,h); s=w/1920
    brand(im,int(88*s),int(72*s),1.35*s)
    text(im,(int(88*s),int(245*s)),title,int(79*s),spacing=2)
    text(im,(int(92*s),int(460*s)),desc,int(27*s),MUTED,False,spacing=int(10*s))
    line(im,(int(90*s),int(692*s),int(620*s),int(692*s)))
    text(im,(int(92*s),int(724*s)),label,int(18*s),LIME,False)
    dev=device(STORE/f'raw/{prefix}-{name}.png',int(h*.70),'tablet')
    maxw=int(w*.61)
    if dev.width>maxw: dev=dev.resize((maxw,round(dev.height*maxw/dev.width)),Image.Resampling.LANCZOS)
    im=paste_device(im,dev,int(w*.365),int((h-dev.height)/2))
    text(im,(int(90*s),h-int(65*s)),'ADB LOCAL SHELL PRO',int(15*s),MUTED,False)
    return im

def feature_graphic():
    im=background(1024,500)
    brand(im,42,34,.85)
    text(im,(43,125),'Android.\nAt your command.',52,spacing=1)
    text(im,(46,280),'A local shell. A considered workspace.',19,MUTED,False)
    text(im,(46,416),'WIRELESS ADB  /  SAVED COMMANDS  /  LOCAL HISTORY',11,LIME,False)
    dev=device(STORE/'raw/phone-shell.png',428)
    dev=dev.rotate(-7,Image.Resampling.BICUBIC,expand=True)
    im=paste_device(im,dev,697,35)
    return im

def make_video():
    import imageio_ffmpeg
    target=STORE/'video/adb-local-shell-pro-promo.mp4'
    width,height,fps=1920,1080,24
    scenes=[('shell','Android.\nAt your command.','A focused local ADB workspace.'),
            ('connect','A connection.\nWithout the cable.','Pair with Android Wireless debugging.'),
            ('commands','Keep the\ngood commands.','Find a starter. Save your own.'),
            ('history','Your last command.\nYour next step.','Revisit results. Export when you need to.'),
            ('settings','Built for\nthe curious.','No ads. No account. Local by design.')]
    cmd=[imageio_ffmpeg.get_ffmpeg_exe(),'-y','-f','rawvideo','-vcodec','rawvideo','-pix_fmt','rgb24','-s',f'{width}x{height}','-r',str(fps),'-i','-',
         '-an','-c:v','libx264','-preset','veryfast','-threads','2','-crf','20','-pix_fmt','yuv420p','-movflags','+faststart',str(target)]
    with (STORE/'video/render.log').open('w') as log:
        proc=subprocess.Popen(cmd,stdin=subprocess.PIPE,stderr=log)
        for scene,(name,title,subtitle) in enumerate(scenes):
            base=background(width,height); brand(base,100,74,1.4)
            text(base,(104,260),title,90,spacing=0)
            text(base,(108,505),subtitle,28,MUTED,False)
            text(base,(108,650),'ADB LOCAL SHELL PRO',23,LIME,False)
            text(base,(108,699),'Android 11+ · Wireless debugging required',18,MUTED,False)
            text(base,(108,973),'PT DEPLOY DULU PULANG NANTI',14,MUTED,False)
            dev=device(STORE/f'raw/phone-{name}.png',940)
            base=paste_device(base,dev,1300,72)
            if scene==0: base.save(STORE/'video/poster.jpg',quality=94)
            frames=6*fps
            zoomed_size = (width, height)
            zoomed = base
            for f in range(frames):
                t=f/(frames-1)
                zoom=1.0+.014*t
                size = (round(width*zoom), round(height*zoom))
                if size != zoomed_size:
                    zoomed = base.resize(size, Image.Resampling.BILINEAR).crop((0, 0, width, height))
                    zoomed_size = size
                frame = zoomed.copy()
                d=ImageDraw.Draw(frame)
                d.rectangle((100,1041,1820,1044),fill=LINE)
                progress=(scene+t)/len(scenes)
                d.rectangle((100,1041,100+int(1720*progress),1044),fill=LIME)
                # Gentle fade through charcoal between chapters.
                alpha=min(1.0,f/(fps*.35),(frames-1-f)/(fps*.35))
                if alpha<1: frame=Image.blend(Image.new('RGB',frame.size,INK),frame,max(0,alpha))
                proc.stdin.write(frame.tobytes())
        proc.stdin.close()
        if proc.wait()!=0: raise RuntimeError('Video encoding failed; inspect store/video/render.log')
    print('Created 30-second 1080p H.264 promo, 24 fps, silent.')

def validate():
    records=[]
    limits={'phone':(5,1080,1920),'tablet-7':(4,1920,1080),'tablet-10':(4,2560,1440),'desktop':(4,2560,1440)}
    for folder,(count,w,h) in limits.items():
        files=sorted((STORE/folder).glob('*.png')); assert len(files)==count,(folder,len(files))
        for path in files:
            with Image.open(path) as image:
                assert image.size==(w,h),(path,image.size)
                assert image.mode=='RGB'
            assert path.stat().st_size<=8*1024*1024
            records.append({'file':path.relative_to(STORE).as_posix(),'width':w,'height':h,'bytes':path.stat().st_size})
    artwork = [
        ('app-icon-512.png', (512, 512), 'RGBA', 1024 * 1024),
        ('feature-graphic-1024x500.png', (1024, 500), 'RGB', 15 * 1024 * 1024),
    ]
    for name, size, mode, max_bytes in artwork:
        p=STORE/'artwork'/name
        with Image.open(p) as image:
            assert image.size == size and image.mode == mode
        assert p.stat().st_size <= max_bytes
        records.append({'file':p.relative_to(STORE).as_posix(),'width':size[0],'height':size[1],'bytes':p.stat().st_size})
    short=(STORE/'listing/short-description.txt').read_text(encoding='utf-8').strip()
    full=(STORE/'listing/full-description.txt').read_text(encoding='utf-8').strip()
    assert len(short)<=80,len(short); assert len(full)<=4000,len(full)
    (STORE/'manifest.json').write_text(json.dumps({'short_description_characters':len(short),'full_description_characters':len(full),'images':records},indent=2),encoding='utf-8')
    print(f'Validated {len(records)} images; listing lengths {len(short)}/80 and {len(full)}/4000.')

def contact_sheet():
    thumbs=[]
    for p in sorted((STORE/'phone').glob('*.png')):
        im=Image.open(p).convert('RGB'); im.thumbnail((270,480)); thumbs.append(im)
    sheet=Image.new('RGB',(270*len(thumbs),480),INK)
    for i,im in enumerate(thumbs): sheet.paste(im,(i*270,0))
    sheet.save(STORE/'artwork/phone-contact-sheet.jpg',quality=94)

if __name__=='__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('--video',action='store_true'); args=parser.parse_args()
    for folder in ['artwork','phone','tablet-7','tablet-10','desktop','video']: (STORE/folder).mkdir(parents=True,exist_ok=True)
    icon=Image.new('RGBA',(512,512),INK); mark(icon,0,0,512,INK,LIME); icon.save(STORE/'artwork/app-icon-512.png')
    feature_graphic().save(STORE/'artwork/feature-graphic-1024x500.png',optimize=True)
    for i,feature in enumerate(FEATURES,1): phone_art(i,feature).save(STORE/f'phone/{i:02d}-{feature[0]}.png',optimize=True)
    for folder,w,h,prefix,label in [('tablet-7',1920,1080,'tablet7','MADE FOR MORE ROOM'),('tablet-10',2560,1440,'tablet','YOUR ANDROID WORKSPACE, EXPANDED'),('desktop',2560,1440,'desktop','A NATIVE ANDROID APP IN A WIDE WINDOW')]:
        for i,feature in enumerate(FEATURES[:4],1): landscape_art(feature,w,h,prefix,label).save(STORE/f'{folder}/{i:02d}-{feature[0]}.png',optimize=True)
    contact_sheet(); validate()
    if args.video: make_video()
