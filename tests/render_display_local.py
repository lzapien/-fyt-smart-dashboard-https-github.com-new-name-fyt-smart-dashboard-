"""Local render of the production Canvas gauge via Java2D, not an Android/device screenshot.

Synthetic speeds exist only in this test process. No ADB, installation, GPS or application data.
An available pre-change backup enables a pixel regression check of the unchanged dark gauge.
"""
from pathlib import Path
import ast
import json
import subprocess
import zipfile

root=Path(__file__).resolve().parents[1]
out=root/'artifacts/display-modes'
out.mkdir(parents=True,exist_ok=True)
work=root/'build/display-local-render'
# Reuse the existing drawing bridge without running its separate seasonal renderer.
module=ast.parse((root/'tests/render_appearance_local.py').read_text())
base=next(ast.literal_eval(node.value) for node in module.body if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='sources' for t in node.targets))
sources={k:v for k,v in base.items() if k.startswith('android/graphics/')}
sources['android/graphics/Paint.java']=sources['android/graphics/Paint.java'].replace('public Paint(int flags){}', '''
 public enum Align {LEFT,CENTER,RIGHT} Align align=Align.LEFT; Typeface typeface; float textSize=12;
 public Paint(int flags){} public void setTextAlign(Align value){align=value;}
 public void setTypeface(Typeface value){typeface=value;} public void setTextSize(float value){textSize=value;}
 public void setAlpha(int alpha){color=(color&0x00ffffff)|(alpha<<24);}
''')
sources['android/graphics/Canvas.java']=sources['android/graphics/Canvas.java'].replace(' public void translate(', '''
 public void drawArc(RectF r,float start,float sweep,boolean center,Paint p){draw(new Arc2D.Float(r.l,r.t,r.r-r.l,r.b-r.t,-start,-sweep,Arc2D.OPEN),p);}
 public void drawText(String value,float x,float y,Paint p){
  graphics.setColor(new Color(p.color,true));graphics.setComposite(AlphaComposite.SrcOver);
  graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12).deriveFont(p.textSize));
  float width=(float)graphics.getFont().getStringBounds(value,graphics.getFontRenderContext()).getWidth();
  graphics.drawString(value,x-(p.align==Paint.Align.CENTER?width/2:p.align==Paint.Align.RIGHT?width:0),y);
 }
 public void translate(''')
sources.update({
 'android/graphics/RectF.java':'''package android.graphics;public class RectF {float l,t,r,b;public RectF(float l,float t,float r,float b){set(l,t,r,b);}public void set(float l,float t,float r,float b){this.l=l;this.t=t;this.r=r;this.b=b;}}''',
 'android/graphics/Typeface.java':'''package android.graphics;public class Typeface {public static final int NORMAL=0;public static Typeface create(String family,int style){return new Typeface();}}''',
 'android/graphics/Color.java':'''package android.graphics;public class Color {public static final int WHITE=0xffffffff;public static int red(int c){return c>>16&255;}public static int green(int c){return c>>8&255;}public static int blue(int c){return c&255;}public static int rgb(int r,int g,int b){return 0xff000000|r<<16|g<<8|b;}public static int HSVToColor(float[] hsv){return java.awt.Color.HSBtoRGB(hsv[0]/360,hsv[1],hsv[2]);}}''',
 'android/content/Context.java':'''package android.content;public class Context {}''',
 'android/view/View.java':'''package android.view;import android.graphics.Canvas;import android.content.Context;public class View {public View(Context c){}public void setFocusable(boolean b){}public void setContentDescription(String s){}public void invalidate(){}public int getWidth(){return 880;}public int getHeight(){return 490;}protected void onDraw(Canvas c){}protected void onDetachedFromWindow(){}}''',
 'android/view/animation/DecelerateInterpolator.java':'''package android.view.animation;public class DecelerateInterpolator {}''',
 'android/animation/ValueAnimator.java':'''package android.animation;public class ValueAnimator {public interface AnimatorUpdateListener{void onAnimationUpdate(ValueAnimator a);}public static boolean areAnimatorsEnabled(){return false;}public static ValueAnimator ofFloat(float... values){return new ValueAnimator();}public void cancel(){}public void setDuration(long ms){}public void setInterpolator(Object o){}public void addUpdateListener(AnimatorUpdateListener l){}public void start(){}public Object getAnimatedValue(){return 0f;}public float getAnimatedFraction(){return 0;}public boolean isRunning(){return false;}}''',
 'com/fytsmart/dashboard/DisplayRender.java':r'''
package com.fytsmart.dashboard;
import android.graphics.Canvas;import java.awt.*;import java.awt.image.*;import java.io.*;import javax.imageio.ImageIO;
public class DisplayRender {
 public static void main(String[] args)throws Exception {
  File output=new File(args[0]);boolean baseline=args.length>1;
  for(boolean light:new boolean[]{false,true}){
   if(baseline&&light)continue;
   DashboardPalette palette=DashboardPalette.forLight(light);
   for(int speed:new int[]{-1,0,30,60,90,120}){
    SpeedometerView gauge=new SpeedometerView(new android.content.Context());
    if(!baseline)SpeedometerView.class.getDeclaredMethod("setPalette",DashboardPalette.class).invoke(gauge,palette);
    if(speed>=0)gauge.setGpsSpeed(speed,false);
    BufferedImage image=new BufferedImage(880,490,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
    g.setPaint(new GradientPaint(0,0,new Color(palette.panelTop,true),880,490,new Color(palette.panelBottom,true)));g.fillRect(0,0,880,490);g.dispose();
    gauge.onDraw(new Canvas(image));
    if(speed>=0&&!gauge.getDisplayValue().equals(Integer.toString(speed)))throw new AssertionError("Rendering must not change speed");
    ImageIO.write(image,"png",new File(output,(baseline?"baseline-":"")+(light?"light":"dark")+"-"+speed+".png"));
   }
  }
  if(!baseline){
   BufferedImage gallery=new BufferedImage(1200,1120,BufferedImage.TYPE_INT_RGB);Graphics2D g=gallery.createGraphics();g.setColor(new Color(0xffaebdca));g.fillRect(0,0,1200,1120);
   for(int column=0;column<2;column++){
    boolean light=column==0;g.setColor(new Color(0xff101e2d));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,24));g.drawString(light?"CLARO":"OSCURO",column*600+30,35);
    int row=0;for(int speed:new int[]{-1,60,120}){BufferedImage image=ImageIO.read(new File(output,(light?"light":"dark")+"-"+speed+".png"));g.drawImage(image,column*600+10,55+row*340,580,323,null);row++;}
   }
   g.setColor(new Color(0xff101e2d));g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));g.drawString("Render local del Canvas real · tipografía aproximada · velocidades solo de prueba",25,1100);g.dispose();ImageIO.write(gallery,"png",new File(output,"gauge-local-comparison.png"));
  }
 }
}
'''
})
paths=[]
for relative,source in sources.items():
 if relative in ('android/graphics/Canvas.java','android/graphics/RadialGradient.java'):
  source=source.replace('new Color(', 'new java.awt.Color(').replace('Color[]', 'java.awt.Color[]').replace('new Color[', 'new java.awt.Color[')
 p=work/'src'/relative;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(source);paths.append(str(p))
app=root/'app/src/main/java/com/fytsmart/dashboard'
classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
subprocess.run(['javac','-d',str(classes),*paths,str(app/'DashboardPalette.java'),str(app/'SpeedometerView.java')],check=True)
subprocess.run(['java','-Djava.awt.headless=true','-cp',str(classes),'com.fytsmart.dashboard.DisplayRender',str(out)],check=True)
# Compare the untouched dark drawing with the original source, using identical bridge/font rendering.
report=out/'backup.json'
if report.exists():
 archive=Path(json.loads(report.read_text())['path'])
 with zipfile.ZipFile(archive) as z: original=z.read(root.name+'/app/src/main/java/com/fytsmart/dashboard/SpeedometerView.java')
 old=work/'baseline-src/SpeedometerView.java';old.parent.mkdir(exist_ok=True);old.write_bytes(original)
 baseline=work/'baseline-classes';baseline.mkdir(exist_ok=True)
 subprocess.run(['javac','-d',str(baseline),*paths,str(app/'DashboardPalette.java'),str(old)],check=True)
 subprocess.run(['java','-Djava.awt.headless=true','-cp',str(baseline),'com.fytsmart.dashboard.DisplayRender',str(out),'baseline'],check=True)
 for speed in [-1,0,30,60,90,120]:
  assert (out/f'baseline-dark-{speed}.png').read_bytes()==(out/f'dark-{speed}.png').read_bytes(),f'Dark gauge changed at {speed}'
 print('PASS: 6 dark gauge renders exactly match the pre-change source; 12 day/night draws preserve GPS fixture readings.')
print('Preview uses a Java2D bridge, not Android rasterization, stereo visibility or live GPS evidence.')
