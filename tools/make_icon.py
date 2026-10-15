import cairosvg,io,os
from PIL import Image,ImageDraw,ImageFilter
R='#B3151C';CR='#FFF6E8';DK='#5A0E12'
def svg(mono=False):
    s=CR if not mono else '#fff'
    hd=DK if not mono else None
    a=[f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">']
    if mono:
        a.append('<defs><mask id="m"><rect width="108" height="108" fill="#fff"/>'
          '<rect x="28" y="40" width="52" height="3" fill="#000"/>'
          '<path d="M54 49 L67 62 L54 75 L41 62Z" fill="none" stroke="#000" stroke-width="4.5" stroke-linejoin="round"/>'
          '<path d="M54 57.5 L58.5 62 L54 66.5 L49.5 62Z" fill="#000"/>'
          '<path d="M35 59.4 L37.6 62 L35 64.6 L32.4 62Z M73 59.4 L75.6 62 L73 64.6 L70.4 62Z" fill="#000"/></mask></defs>'
          '<rect x="28" y="29" width="52" height="53" rx="7" fill="#fff" mask="url(#m)"/>'
          '<rect x="39.5" y="22" width="6" height="14" rx="3" fill="#fff" stroke="#000" stroke-opacity="0" />'
          '<rect x="62.5" y="22" width="6" height="14" rx="3" fill="#fff"/>')
    else:
        a.append(f'<rect x="28.5" y="31" width="52" height="53" rx="7" fill="#000" opacity=".18"/>')
        a.append(f'<rect x="28" y="29" width="52" height="53" rx="7" fill="{s}"/>')
        a.append(f'<path d="M28 41 V36 a7 7 0 0 1 7 -7 H73 a7 7 0 0 1 7 7 V41 Z" fill="{hd}"/>')
        a.append(f'<path d="M54 49 L67 62 L54 75 L41 62Z" fill="none" stroke="{R}" stroke-width="4.5" stroke-linejoin="round"/>')
        a.append(f'<path d="M54 57.5 L58.5 62 L54 66.5 L49.5 62Z" fill="{R}"/>')
        a.append(f'<path d="M35 59.4 L37.6 62 L35 64.6 L32.4 62Z M73 59.4 L75.6 62 L73 64.6 L70.4 62Z" fill="{R}"/>')
        for x in (39.5,62.5): a.append(f'<rect x="{x}" y="22" width="6" height="14" rx="3" fill="{CR}" stroke="{DK}" stroke-width="1.4"/>')
    a.append('</svg>');return ''.join(a)
def monopng(px):
    from PIL import ImageChops
    base=png('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><rect x="28" y="29" width="52" height="53" rx="7" fill="#fff"/><rect x="39.5" y="22" width="6" height="14" rx="3" fill="#fff"/><rect x="62.5" y="22" width="6" height="14" rx="3" fill="#fff"/></svg>',px)
    cut=png('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><rect x="28" y="40.5" width="52" height="3" fill="#fff"/><rect x="38" y="35" width="9" height="5" fill="#fff"/><rect x="61" y="35" width="9" height="5" fill="#fff"/><path d="M54 49 L67 62 L54 75 L41 62Z" fill="none" stroke="#fff" stroke-width="4.5" stroke-linejoin="round"/><path d="M54 57.5 L58.5 62 L54 66.5 L49.5 62Z M35 59.4 L37.6 62 L35 64.6 L32.4 62Z M73 59.4 L75.6 62 L73 64.6 L70.4 62Z" fill="#fff"/></svg>',px)
    a=ImageChops.subtract(base.split()[3],cut.split()[3]);o=Image.new('RGBA',(px,px),(255,255,255,0));o.putalpha(a);return o
def png(sv,px): return Image.open(io.BytesIO(cairosvg.svg2png(bytestring=sv.encode(),output_width=px,output_height=px))).convert('RGBA')
open('fg.svg','w').write(svg());open('mono.svg','w').write(svg(True))
def full(px):
    bg=Image.new('RGBA',(px,px),R);bg.alpha_composite(png(svg(),px));return bg
def masked(px,shape):
    big=full(px*108//72*1) # render 108 at scale then crop center 72
    s=px*108//72;im=full(s);o=(s-px)//2;im=im.crop((o,o,o+px,o+px))
    m=Image.new('L',(px*4,px*4),0);d=ImageDraw.Draw(m)
    if shape=='round':d.ellipse((0,0,px*4-1,px*4-1),fill=255)
    else:d.rounded_rectangle((0,0,px*4-1,px*4-1),radius=int(px*4*.30),fill=255)
    im.putalpha(m.resize((px,px),Image.LANCZOS));return im
import sys
res=sys.argv[1] if len(sys.argv)>1 else 'app/src/main/res'
for name,f in dict(mdpi=1,hdpi=1.5,xhdpi=2,xxhdpi=3,xxxhdpi=4).items():
    d=f'{res}/mipmap-{name}';
    png(svg(),int(108*f)).save(f'{d}/ic_launcher_foreground.png')
    monopng(int(108*f)).save(f'{d}/ic_launcher_monochrome.png')
    masked(int(48*f),'sq').save(f'{d}/ic_launcher.png');masked(int(48*f),'round').save(f'{d}/ic_launcher_round.png')
masked(512,'sq').save('icon_512.png');full(512).convert('RGB').save('icon_full_512.png')
# Play icon: full-bleed 512
