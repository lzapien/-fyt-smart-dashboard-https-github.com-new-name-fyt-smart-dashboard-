"""Render the production DecorationRenderer with a Java2D drawing bridge, without Android/ADB.

These previews validate geometry/clipping, not Android rasterization, lifecycle or vehicle performance.
Generated android.graphics bridge classes stay under build/ and are never packaged in the APK.
"""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
work = root / "build/appearance-local-render"
output = root / "artifacts/decor-contrast"
output.mkdir(parents=True, exist_ok=True)
work.mkdir(parents=True, exist_ok=True)
sources = {
    "android/graphics/Paint.java": r'''
package android.graphics;
public class Paint {
 public static final int ANTI_ALIAS_FLAG=1;
 public enum Style {FILL,STROKE} public enum Cap {BUTT,ROUND,SQUARE} public enum Join {MITER,ROUND,BEVEL}
 Style style=Style.FILL; Cap cap=Cap.BUTT; Join join=Join.MITER; int color=0xff000000; float width=1; Shader shader;
 public Paint(int flags){} public void setStyle(Style s){style=s;} public void setColor(int c){color=c;}
 public void setStrokeWidth(float w){width=w;} public void setStrokeCap(Cap c){cap=c;}
 public void setStrokeJoin(Join j){join=j;} public void setShader(Shader s){shader=s;}
}
''',
    "android/graphics/Path.java": r'''
package android.graphics;
import java.awt.geom.*;
public class Path {
 public enum Direction {CW,CCW} final Path2D.Float path=new Path2D.Float();
 public void moveTo(float x,float y){path.moveTo(x,y);} public void lineTo(float x,float y){path.lineTo(x,y);}
 public void quadTo(float a,float b,float c,float d){path.quadTo(a,b,c,d);}
 public void cubicTo(float a,float b,float c,float d,float e,float f){path.curveTo(a,b,c,d,e,f);}
 public void close(){path.closePath();}
 public void addRect(float l,float t,float r,float b,Direction d){path.append(new Rectangle2D.Float(l,t,r-l,b-t),false);}
 public void addOval(float l,float t,float r,float b,Direction d){path.append(new Ellipse2D.Float(l,t,r-l,b-t),false);}
}
''',
    "android/graphics/Shader.java": r'''
package android.graphics;
public abstract class Shader {public enum TileMode {CLAMP} java.awt.Paint nativePaint;}
''',
    "android/graphics/RadialGradient.java": r'''
package android.graphics;
import java.awt.*; import java.awt.geom.*;
public class RadialGradient extends Shader {
 public RadialGradient(float x,float y,float radius,int[] colors,float[] fractions,TileMode mode){
  Color[] nativeColors=new Color[colors.length];for(int i=0;i<colors.length;i++)nativeColors[i]=new Color(colors[i],true);
  if(fractions==null){fractions=new float[colors.length];for(int i=0;i<colors.length;i++)fractions[i]=i/(float)(colors.length-1);}
  nativePaint=new RadialGradientPaint(new Point2D.Float(x,y),radius,fractions,nativeColors);
 }
}
''',
    "android/graphics/LinearGradient.java": r'''
package android.graphics;
import java.awt.geom.Point2D;
public class LinearGradient extends Shader {
 public LinearGradient(float x,float y,float xx,float yy,int[] colors,float[] fractions,TileMode mode){
  java.awt.Color[] values=new java.awt.Color[colors.length];
  for(int i=0;i<colors.length;i++)values[i]=new java.awt.Color(colors[i],true);
  if(fractions==null){fractions=new float[colors.length];for(int i=0;i<colors.length;i++)fractions[i]=i/(float)(colors.length-1);}
  nativePaint=new java.awt.LinearGradientPaint(new Point2D.Float(x,y),new Point2D.Float(xx,yy),fractions,values);
 }
}
''',
    "android/graphics/Canvas.java": r'''
package android.graphics;
import java.awt.*; import java.awt.geom.*; import java.awt.image.*; import java.util.*;
public class Canvas {
 private Graphics2D graphics; private final ArrayDeque<Graphics2D> stack=new ArrayDeque<>(); public int operations;
 public Canvas(BufferedImage image){graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
 public Graphics2D nativeGraphics(){return graphics;}
 public int save(){stack.push(graphics);graphics=(Graphics2D)graphics.create();return stack.size();}
 public void restore(){graphics.dispose();graphics=stack.pop();}
 public void translate(float x,float y){graphics.translate(x,y);} public void scale(float x,float y){graphics.scale(x,y);}
 public void clipRect(float l,float t,float r,float b){graphics.clip(new Rectangle2D.Float(l,t,r-l,b-t));}
 public void clipPath(Path p){graphics.clip(p.path);}
 private void draw(Shape shape,Paint p){
  operations++; graphics.setComposite(AlphaComposite.SrcOver);
  graphics.setPaint(p.shader==null?new Color(p.color,true):p.shader.nativePaint);
  if(p.style==Paint.Style.FILL)graphics.fill(shape);
  else {int cap=p.cap==Paint.Cap.ROUND?BasicStroke.CAP_ROUND:p.cap==Paint.Cap.SQUARE?BasicStroke.CAP_SQUARE:BasicStroke.CAP_BUTT;
   int join=p.join==Paint.Join.ROUND?BasicStroke.JOIN_ROUND:p.join==Paint.Join.BEVEL?BasicStroke.JOIN_BEVEL:BasicStroke.JOIN_MITER;
   graphics.setStroke(new BasicStroke(Math.max(.01f,p.width),cap,join));graphics.draw(shape);}
 }
 public void drawPath(Path path,Paint p){draw(path.path,p);}
 public void drawRect(float l,float t,float r,float b,Paint p){draw(new Rectangle2D.Float(l,t,r-l,b-t),p);}
 public void drawOval(float l,float t,float r,float b,Paint p){draw(new Ellipse2D.Float(l,t,r-l,b-t),p);}
 public void drawCircle(float x,float y,float radius,Paint p){drawOval(x-radius,y-radius,x+radius,y+radius,p);}
 public void drawLine(float x,float y,float xx,float yy,Paint p){draw(new Line2D.Float(x,y,xx,yy),p);}
}
''',
    "com/fytsmart/dashboard/AppearanceRender.java": r'''
package com.fytsmart.dashboard;
import android.graphics.Canvas;
import java.awt.*;import java.awt.geom.*;import java.awt.image.*;import java.io.*;import javax.imageio.ImageIO;

public final class AppearanceRender {
 private static int checks;
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
 private static double luminance(int color){
  double result=0;for(int i=0;i<3;i++){double channel=((color>>(16-i*8))&255)/255d;channel=channel<=.04045?channel/12.92:Math.pow((channel+.055)/1.055,2.4);result+=channel*(i==0?.2126:i==1?.7152:.0722);}return result;
 }
 private static double contrast(int first,int second){double a=luminance(first),b=luminance(second);return (Math.max(a,b)+.05)/(Math.min(a,b)+.05);}
 private static void line(Graphics2D g,float x,float y,float xx,float yy,float width,int color){g.setColor(new Color(color,true));g.setStroke(new BasicStroke(width));g.draw(new Line2D.Float(x,y,xx,yy));}
 private static BufferedImage scene(DecorationRenderer renderer,ThemeConfig theme,GenericVehicleArtwork body,boolean light){
  BufferedImage result=new BufferedImage(800,520,BufferedImage.TYPE_INT_ARGB);Canvas canvas=new Canvas(result);Graphics2D g=canvas.nativeGraphics();g.scale(2,2);
  g.setColor(new Color(light?0xffedf2f7:0xff02060b,true));g.fillRect(0,0,400,260);
  g.setPaint(new RadialGradientPaint(new Point2D.Float(200,239),140,new float[]{0,1},new Color[]{new Color(0x282976b3,true),new Color(0,true)}));g.fill(new Ellipse2D.Float(0,183,400,77));
  renderer.draw(canvas,theme,.37f,light);
  line(g,-40,260,107,0,2.8f,light?0xff485d70:0xff7c858f);line(g,440,260,293,0,2.8f,light?0xff485d70:0xff7c858f);
  for(int i=0;i<3;i++){float t=i/3f,y=5+t*t*252;
   line(g,25+(1-t)*134-8,y,25+(1-t)*134-4-t*8-8,y+5+t*12,2.5f+t*2,light?0xff896500:0xe6ffed38);
   line(g,375-(1-t)*134+8,y,375-(1-t)*134+4+t*8+8,y+5+t*12,2.5f+t*2,light?0xff896500:0xe6ffed38);}
  body.draw(canvas);
  if(!light){line(g,112,107,146,107,7,0x55ff3a52);line(g,254,107,288,107,7,0x55ff3a52);
   line(g,112,107,146,107,2.8f,0xddff3a52);line(g,254,107,288,107,2.8f,0xddff3a52);}
  Shape oldClip=g.getClip();g.clip(new Rectangle2D.Float(176.5f,113,47,20));Font font=new Font(Font.SANS_SERIF,Font.BOLD,10).deriveFont(10.5f);g.setFont(font);
  String plate="TU-PLACA";float width=(float)font.getStringBounds(plate,g.getFontRenderContext()).getWidth();if(width>41){font=font.deriveFont(10.5f*41/width);g.setFont(font);}
  FontMetrics metrics=g.getFontMetrics();g.setColor(new Color(0xffe2e5e8,true));g.drawString(plate,200-metrics.stringWidth(plate)/2f,123+(metrics.getAscent()-metrics.getDescent())/2f);g.setClip(oldClip);
  g.dispose();return result;
 }
 public static void main(String[] args)throws Exception {
  File output=new File(args[0]);GenericVehicleArtwork body=new GenericVehicleArtwork();DecorationRenderer renderer=new DecorationRenderer();
  for(int width:new int[]{320,400,800}){
   int height=width*260/400;BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
   Canvas canvas=new Canvas(image);canvas.scale(width/400f,height/260f);body.draw(canvas);
   int pixels=0;boolean bounded=true;
   for(int y=0;y<height;y++)for(int x=0;x<width;x++)if((image.getRGB(x,y)>>>24)!=0){
    pixels++;float dx=(x+.5f)*400/width,dy=(y+.5f)*260/height;
    if(dx<86||dx>314||dy<22||dy>237)bounded=false;
   }
   check(bounded,"Generic car remains inside road/decorations at scale "+width);
   check(pixels>width*height*.25,"Generic car is visible without bitmap or loader");
   int plate=image.getRGB(width/2,Math.round(123*height/260f));
   check((plate&0x00ffffff)==0x17212b,"Editable plate recess remains aligned");
   canvas.nativeGraphics().dispose();
  }
  for(boolean light:new boolean[]{false,true})for(int width:new int[]{320,400,800})for(ThemeConfig theme:ThemeConfig.values())for(float phase:new float[]{0,.25f,.5f,.75f,.999f}){
   int height=width*260/400;BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);Canvas canvas=new Canvas(image);canvas.nativeGraphics().scale(width/400d,height/260d);renderer.draw(canvas,theme,phase,light);
   int pixels=0,left=0,right=0;boolean safe=true;
   for(int y=0;y<height;y++)for(int x=0;x<width;x++)if((image.getRGB(x,y)>>>24)!=0){pixels++;float design=(x+.5f)*400/width;if(design<50)left++;else if(design>=350)right++;else safe=false;}
   check(safe,"Decorations avoid the entire car rectangle, scale="+width+" theme="+theme.id);
   if(theme==ThemeConfig.DEFAULT){check(pixels==0&&canvas.operations==0,"Original has no drawing operations");}
   else check(left>0&&right>0,"Both seasonal side areas draw artwork");
   if(theme!=ThemeConfig.DEFAULT){
    int strongLeft=0,strongRight=0,bg=light?0xffedf2f7:0xff02060b;
    for(int y=0;y<height;y++)for(int x=0;x<width;x++){
     int pixel=image.getRGB(x,y),alpha=pixel>>>24;if(alpha==0)continue;
     int composite=0xff000000;
     for(int shift:new int[]{16,8,0})composite|=((((pixel>>shift)&255)*alpha+((bg>>shift)&255)*(255-alpha))/255)<<shift;
     if(contrast(composite,bg)>=4.5){if((x+.5f)*400/width<50)strongLeft++;else strongRight++;}
    }
    check(strongLeft>20&&strongRight>20,"Both sides have strongly contrasting decoration pixels, light="+light+" theme="+theme.id);
   }
   canvas.nativeGraphics().dispose();
  }
  // Changing only the palette must keep geometry/alpha masks identical for every animation phase.
  for(ThemeConfig theme:ThemeConfig.values())for(float phase:new float[]{0,.17f,.37f,.75f,.999f}){
   BufferedImage day=new BufferedImage(800,520,BufferedImage.TYPE_INT_ARGB),night=new BufferedImage(800,520,BufferedImage.TYPE_INT_ARGB);
   Canvas dc=new Canvas(day),nc=new Canvas(night);dc.scale(2,2);nc.scale(2,2);
   renderer.draw(dc,theme,phase,true);renderer.draw(nc,theme,phase,false);
   for(int y=0;y<520;y++)for(int x=0;x<800;x++)if((day.getRGB(x,y)>>>24)!=(night.getRGB(x,y)>>>24))throw new AssertionError("Palette changed geometry/opacity");
   check(true,"Day/night shape and alpha coverage identical");
  }
  check(DecorationRenderer.SNOW_COUNT==18,"Snow particle count is bounded");
  BufferedImage strip=new BufferedImage(1200,640,BufferedImage.TYPE_INT_RGB);Graphics2D gallery=strip.createGraphics();gallery.setColor(new Color(0xffaebdca));gallery.fillRect(0,0,1200,640);gallery.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  int row=0;
  for(boolean light:new boolean[]{true,false}){
   int index=0;
   for(ThemeConfig theme:ThemeConfig.values()){
    BufferedImage rendered=scene(renderer,theme,body,light);ImageIO.write(rendered,"png",new File(output,theme.id+(light?"-light":"-dark")+"-local.png"));gallery.drawImage(rendered,index*400,32+row*310,400,260,null);
    gallery.setFont(new Font(Font.SANS_SERIF,Font.BOLD,17));gallery.setColor(new Color(0xff101e2d));String label=theme.label+(light?" · Claro":" · Oscuro");gallery.drawString(label,index*400+200-gallery.getFontMetrics().stringWidth(label)/2,23+row*310);index++;
   }
   row++;
  }
  gallery.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));gallery.setColor(new Color(0xff101e2d));String caption="Vista previa local del dibujador; no es captura Android ni del estéreo.";gallery.drawString(caption,600-gallery.getFontMetrics().stringWidth(caption)/2,631);gallery.dispose();ImageIO.write(strip,"png",new File(output,"decor-contrast-local-comparison.png"));
  System.out.println("Local DecorationRenderer geometry: "+checks+" checks passed. Uses a Java2D bridge, not Android execution or device performance evidence.");
 }
}
''',
}
paths = []
for relative, source in sources.items():
    path = work / "src" / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(source)
    paths.append(str(path))
classes = work / "classes"
classes.mkdir(exist_ok=True)
app = root / "app/src/main/java/com/fytsmart/dashboard"
subprocess.run(["javac", "-d", str(classes), *paths, str(app / "ThemeConfig.java"), str(app / "DecorationRenderer.java"), str(app / "GenericVehicleArtwork.java")], check=True)
result = subprocess.run(["java", "-Djava.awt.headless=true", "-cp", str(classes), "com.fytsmart.dashboard.AppearanceRender", str(output)], capture_output=True, text=True)
(output / "local-render-checks.txt").write_text(result.stdout + result.stderr)
print(result.stdout + result.stderr)
result.check_returncode()
