from PIL import Image,ImageDraw,ImageFont,ImageFilter
import os,glob,shutil
import os; ROOT=os.environ.get('MK_ROOT', os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')); B=os.path.join(ROOT,'store'); F=os.path.join(ROOT,'app/src/main/assets/fonts/')
fb=lambda s:ImageFont.truetype(F+'NotoSerif-Bold.ttf',s); fr=lambda s:ImageFont.truetype(F+'NotoSans-Regular.ttf',s); fsb=lambda s:ImageFont.truetype(F+'NotoSans-Bold.ttf',s)
PAL={'light':dict(bg='#FBF3E8',fg='#2A1612',mut='#6F625C',red='#9E1C1C'),'dark':dict(bg='#17110F',fg='#EDE3D8',mut='#B0A298',red='#E0645C')}
CAP={'01_tache':('Таче — мо пайрем, кумалтыш, пале','Праздники и приметы дня'),
'02_tylze':('Тылзе — чыла кече ик ончалтышеш','Весь месяц одним взглядом'),
'03_holiday':('Пайрем: смысл, йӱла, налме вер','Смысл, обряды, источники'),
'04_rite':('Йӱла — кумалтыш радам','Обряды и моления'),
'05_kychalash':('Кычалаш — лӱм, пайрем, пале','Удобный поиск'),
'06_ushestarymash':('Ушештарымаш — эрдене','Напоминания по утрам'),
'07_share':('Ойлаш — сылне карточка','Красивые карточки'),
'08_notification':('Эрдене ушештара','Утреннее напоминание')}
def notif_mock(mode,src):
    p=PAL[mode]; im=Image.open(src).convert('RGB').filter(ImageFilter.GaussianBlur(18))
    ov=Image.new('RGB',im.size,'#000'); im=Image.blend(im,ov,0.45 if mode=='light' else 0.35); d=ImageDraw.Draw(im); W=im.size[0]
    d.text((W//2,330),'10:00',font=fr(190),fill='white',anchor='mm'); d.text((W//2,470),'Кушкыжмо, 14 Сӱрем',font=fr(46),fill='white',anchor='mm')
    x0,y0,x1=50,620,W-50; card='#FFFCF7' if mode=='light' else '#2A221E'
    lines=['• Пайрем: Сӱрем (Кӱсӧ)','• Кумалтыш: Сӱрем арня','• Калык пале: ваштар лышташ шырча — йӱр…']
    y1=y0+170+len(lines)*62+30; d.rounded_rectangle((x0,y0,x1,y1),40,fill=card)
    d.ellipse((x0+40,y0+40,x0+100,y0+100),fill=p['red']); cx,cy=x0+70,y0+70; d.polygon([(cx,cy-16),(cx+16,cy),(cx,cy+16),(cx-16,cy)],fill='white')
    d.text((x0+125,y0+70),'Марий кечышот · 10:00',font=fr(34),fill=p['mut'],anchor='lm')
    d.text((x0+40,y0+145),'Марий кечышот — Таче · 14 Сӱрем',font=fsb(42),fill=p['fg'],anchor='lm')
    for i,l in enumerate(lines): d.text((x0+40,y0+215+i*62),l,font=fr(38),fill=p['mut'],anchor='lm')
    return im
def frame(im,mode,key,W,H):
    p=PAL[mode if mode in PAL else 'light']; c=Image.new('RGB',(W,H),p['bg']); d=ImageDraw.Draw(c)
    top=int(H*0.13); m,r=CAP[key]
    fs=int(W*0.062)
    while d.textlength(r,font=fb(fs))>W*0.9: fs-=2
    d.text((W//2,int(top*0.55)),r,font=fb(fs),fill=p['red'],anchor='mm')
    sh=H-top-int(H*0.03); sw=int(im.size[0]*sh/im.size[1]); s=im.resize((sw,sh),Image.LANCZOS)
    mask=Image.new('L',s.size,0); ImageDraw.Draw(mask).rounded_rectangle((0,0,sw-1,sh-1),int(sw*0.05),fill=255)
    x=(W-sw)//2; sd=Image.new('L',(W,H),0); ImageDraw.Draw(sd).rounded_rectangle((x+6,top+14,x+sw+6,top+sh+14),int(sw*0.05),fill=110)
    c.paste(Image.new('RGB',(W,H),'#000'),(0,0),sd.filter(ImageFilter.GaussianBlur(18))); c.paste(s,(x,top),mask); return c
for mode in ['light','dark','tablet']:
    out=f'{B}/{mode}'; shutil.rmtree(out+'/play_rustore',ignore_errors=True); os.makedirs(out+'/play_rustore')
    pm='light' if mode=='tablet' else mode
    for f in sorted(glob.glob(f'{out}/raw/*.png')):
        k=os.path.basename(f)[:-4]; im=Image.open(f).convert('RGB')
        pass
        W,H=(1080,1920) if mode!='tablet' else (1440,2560)
        frame(im,pm,k,W,H).save(f'{out}/play_rustore/{k}.png')
    fs=sorted(glob.glob(f'{out}/play_rustore/*.png')); ims=[Image.open(x) for x in fs]; w=270; h=int(ims[0].size[1]*w/ims[0].size[0])
    mt=Image.new('RGB',(w*len(ims)+12*(len(ims)+1),h+24),'#555'); [mt.paste(i.resize((w,h)),(12+j*(w+12),12)) for j,i in enumerate(ims)]; mt.save(f'{B}/montage_{mode}.png')
# feature graphic
fg=Image.new('RGB',(1024,500),'#FBF3E8'); d=ImageDraw.Draw(fg)
d.rectangle((0,0,1024,500),fill='#FBF3E8'); d.rectangle((0,0,14,500),fill='#9E1C1C')
logo=Image.open(os.path.join(ROOT,'design/icon_512.png')).convert('RGBA').resize((330,330),Image.LANCZOS); fg.paste(logo,(50,85),logo)
d.text((430,170),'Марий кечышот',font=fb(62),fill='#2A1612',anchor='lm'); d.text((432,245),'Марийский календарь',font=fr(40),fill='#6F625C',anchor='lm')
d.text((432,320),'Праздники · Моления · Приметы · Обряды',font=fr(27),fill='#9E1C1C',anchor='lm')
for i in range(-3,4):
    cx=700+i*40; s=7 if i==0 else 4; d.polygon([(cx,395-s),(cx+s,395),(cx,395+s),(cx-s,395)],fill='#9E1C1C' if i==0 else '#2A1612')
d.line((432,395,640,395),fill='#CDBFAF',width=2); d.line((760,395,960,395),fill='#CDBFAF',width=2)
fg.save(f'{B}/feature_graphic_1024x500.png'); print('ok')
