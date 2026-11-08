package com.fytsmart.dashboard;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.List;

/** Original cockpit symbols, drawn as cached vector paths. No app icons or bitmap assets. */
final class DockIconDrawable extends Drawable {
    private static final class Stroke {
        final Path path;
        final int color;
        Stroke(Path path, int color) { this.path=path;this.color=color; }
    }
    private final List<Stroke> strokes=new ArrayList<>();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int alpha=255;
    private final boolean daylight;

    DockIconDrawable(String symbol,int color) { this(symbol,color,false); }
    DockIconDrawable(String symbol, int color, boolean daylight) {
        this.daylight=daylight;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        switch(symbol) {
            case "radio": radio(color);break;
            case "navigation": navigation(color);break;
            case "maps": maps();break;
            case "equalizer": equalizer(color);break;
            case "play": play(color);break;
            default: throw new IllegalArgumentException("Unknown dock symbol: "+symbol);
        }
    }
    private void add(Path path,int color) { strokes.add(new Stroke(path,color)); }
    private void line(float x1,float y1,float x2,float y2,int color) {
        Path p=new Path();p.moveTo(x1,y1);p.lineTo(x2,y2);add(p,color);
    }
    private void circle(float x,float y,float r,int color) {
        Path p=new Path();p.addCircle(x,y,r,Path.Direction.CW);add(p,color);
    }
    private void radio(int color) {
        Path body=new Path();body.addRoundRect(10,23,54,53,6,6,Path.Direction.CW);add(body,color);
        line(17,22,45,11,color);circle(23,38,6,color);
        line(36,33,46,33,color);line(36,39,46,39,color);line(36,45,42,45,color);
        line(16,57,23,57,color);line(41,57,48,57,color);
    }
    private void navigation(int color) {
        Path arrow=new Path();arrow.moveTo(32,8);arrow.lineTo(51,53);arrow.lineTo(32,44);
        arrow.lineTo(13,53);arrow.close();add(arrow,color);
        line(32,19,32,35,color);
        Path orbit=new Path();orbit.addArc(4,4,60,60,195,50);add(orbit,0xff376ba0);
        Path orbit2=new Path();orbit2.addArc(4,4,60,60,295,50);add(orbit2,0xff376ba0);
    }
    private void maps() {
        // A custom segmented location outline, not the application's branded pin artwork.
        Path left=new Path();left.moveTo(32,56);left.cubicTo(25,47,13,35,13,25);
        left.cubicTo(13,15,21,8,32,8);add(left,daylight?0xff005caa:0xff55aaff);
        Path top=new Path();top.moveTo(32,8);top.cubicTo(43,8,51,15,51,25);add(top,daylight?0xffb71930:0xffff646e);
        Path right=new Path();right.moveTo(51,25);right.cubicTo(51,35,39,47,32,56);add(right,daylight?0xff865000:0xffffc766);
        Path ring1=new Path();ring1.addArc(24,17,40,33,180,180);add(ring1,daylight?0xff006b50:0xff5bdfc0);
        Path ring2=new Path();ring2.addArc(24,17,40,33,0,180);add(ring2,daylight?0xff006574:0xff55cfff);
    }
    private void equalizer(int color) {
        line(13,30,13,44,color);line(22,21,22,44,color);line(32,11,32,44,color);
        line(42,24,42,44,color);line(51,31,51,44,color);
        Path base=new Path();base.moveTo(13,53);base.cubicTo(25,57,39,57,51,53);add(base,color);
    }
    private void play(int color) {
        circle(32,31,23,color);
        Path triangle=new Path();triangle.moveTo(27,20);triangle.lineTo(43,31);triangle.lineTo(27,42);triangle.close();add(triangle,color);
        line(24,59,24,60,color);line(32,57,32,61,color);line(40,59,40,60,color);
    }
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds();float size=Math.min(b.width(),b.height());
        canvas.save();canvas.translate(b.left+(b.width()-size)/2,b.top+(b.height()-size)/2);canvas.scale(size/64f,size/64f);
        // Two broad, low-opacity strokes form a cheap static halo without blur layers or animation.
        for(Stroke stroke:strokes) {
            paint.setColor(stroke.color);paint.setAlpha(alpha*6/255);paint.setStrokeWidth(9);canvas.drawPath(stroke.path,paint);
            paint.setAlpha(alpha*18/255);paint.setStrokeWidth(5);canvas.drawPath(stroke.path,paint);
            paint.setAlpha(alpha);paint.setStrokeWidth(daylight?2.4f:1.9f);canvas.drawPath(stroke.path,paint);
        }
        canvas.restore();
    }
    @Override public void setAlpha(int value) { alpha=Math.max(0,Math.min(255,value));invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter);invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
