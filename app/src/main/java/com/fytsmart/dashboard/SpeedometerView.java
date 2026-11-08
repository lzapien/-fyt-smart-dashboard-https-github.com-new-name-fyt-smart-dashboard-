package com.fytsmart.dashboard;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Real GPS dial: only the arc interpolates; the digital value is the last GPS measurement. */
final class SpeedometerView extends View {
    private static final float MAX=120,START=180,SWEEP=180;
    // The needle spans radii 163..193: the progress band fills that same radial span.
    private static final float PROGRESS_RADIUS=178,PROGRESS_WIDTH=30;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc=new RectF(35,10,405,380);
    private final Typeface digits=Typeface.create("sans-serif-light",Typeface.NORMAL);
    private final Typeface labels=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    private float gpsSpeed=Float.NaN,arcSpeed;
    private String display="--";
    private String status="ESPERANDO GPS";
    private boolean running;
    private DashboardPalette palette=DashboardPalette.DARK;
    void setPalette(DashboardPalette value) { palette=value;invalidate(); }
    private ValueAnimator animator;
    private boolean fillSparks;
    private float fillFraction;
    private final float[] neonHsv={205,.72f,1f};

    SpeedometerView(Context context) { super(context);setFocusable(true);updateDescription(); }
    void setRunning(boolean value) {
        running=value;
        if(!value){fillSparks=false;fillFraction=0;}
        if(!value && animator!=null) { animator.cancel();animator=null;arcSpeed=Float.isNaN(gpsSpeed)?0:Math.min(MAX,gpsSpeed);invalidate(); }
    }
    float getGpsSpeed() { return gpsSpeed; }
    float getArcSpeed() { return arcSpeed; }
    String getDisplayValue() { return display; }
    int getAccentColor() {
        float speed=Float.isFinite(gpsSpeed)?Math.min(MAX,Math.max(0,arcSpeed)):0;
        // Decorative progression from blue through cyan/amber to red, driven only by GPS.
        float hue=speed<=60?205-speed/60f*25:speed<=90?180-(speed-60)/30f*135:45-(speed-90)/30f*45;
        neonHsv[0]=hue;neonHsv[1]=palette.light?.95f:.72f;neonHsv[2]=palette.light?.66f:1f;return Color.HSVToColor(neonHsv);
    }
    void setUnknown(String reason) {
        if(animator!=null){animator.cancel();animator=null;}
        fillSparks=false;fillFraction=0;
        gpsSpeed=Float.NaN;arcSpeed=0;display="--";status=reason;updateDescription();invalidate();
    }
    void setGpsSpeed(float kmh,boolean animate) {
        if(Float.isNaN(kmh)||Float.isInfinite(kmh)||kmh<0){setUnknown("VELOCIDAD NO DISPONIBLE");return;}
        gpsSpeed=kmh;display=Integer.toString(Math.round(kmh));status="";updateDescription();
        float target=Math.min(MAX,kmh);
        if(animator!=null){animator.cancel();animator=null;}
        fillSparks=false;fillFraction=0;
        if(running && animate && ValueAnimator.areAnimatorsEnabled() && Math.abs(target-arcSpeed)>.1f) {
            fillSparks=target>arcSpeed+.1f;
            animator=ValueAnimator.ofFloat(arcSpeed,target);animator.setDuration(450);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(a->{arcSpeed=(float)a.getAnimatedValue();fillFraction=a.getAnimatedFraction();invalidate();});animator.start();
        } else {arcSpeed=target;invalidate();}
    }
    private void updateDescription() {
        setContentDescription("Velocímetro GPS, "+(Float.isNaN(gpsSpeed)?status:display+" kilómetros por hora")+", escala de 0 a 120");
    }
    private void stroke(Canvas c,float radius,float width,int color,float sweep) {
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setColor(color);paint.setStrokeCap(Paint.Cap.ROUND);
        arc.set(220-radius,195-radius,220+radius,195+radius);c.drawArc(arc,START,sweep,false,paint);
    }
    private void progressStroke(Canvas c,float width,int color,float sweep) {
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setColor(color);
        // Flat ends follow the radial needle; a 30-unit round cap would create a large dot at zero.
        paint.setStrokeCap(Paint.Cap.BUTT);
        arc.set(220-PROGRESS_RADIUS,195-PROGRESS_RADIUS,220+PROGRESS_RADIUS,195+PROGRESS_RADIUS);
        c.drawArc(arc,START,Math.max(.1f,sweep),false,paint);
    }
    private void text(Canvas c,String value,float x,float y,float size,int color,Typeface font) {
        paint.setStyle(Paint.Style.FILL);paint.setTypeface(font);paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);paint.setColor(color);c.drawText(value,x,y,paint);
    }
    private void drawFillSparks(Canvas c,float sweep,int accent) {
        if(!fillSparks||!running||animator==null||!animator.isRunning()||sweep<=1||!ValueAnimator.areAnimatorsEnabled())return;
        // Six small trailing glints share the existing 450 ms GPS interpolation; no extra clock or allocations.
        float envelope=(float)Math.sin(Math.PI*fillFraction);
        if(envelope<=.01f)return;
        int light=Color.rgb((Color.red(accent)+255)/2,(Color.green(accent)+255)/2,(Color.blue(accent)+255)/2);
        for(int i=0;i<6;i++) {
            float age=(fillFraction*1.6f+i*.13f)%1f;
            float strength=envelope*(1-age);
            float radius=168+(i*7%21)+(float)Math.sin((fillFraction+i)*Math.PI)*1.4f;
            float degrees=START+sweep-Math.min(5,sweep-.1f)*age;
            double angle=Math.toRadians(degrees),tail=Math.toRadians(Math.max(START,degrees-.9f*(1-age)));
            float x=220+(float)Math.cos(angle)*radius,y=195+(float)Math.sin(angle)*radius;
            sparkPaint.setStyle(Paint.Style.FILL);sparkPaint.setColor(accent);sparkPaint.setAlpha((int)(36*strength));
            c.drawCircle(x,y,3.6f,sparkPaint);
            sparkPaint.setStyle(Paint.Style.STROKE);sparkPaint.setStrokeCap(Paint.Cap.ROUND);sparkPaint.setStrokeWidth(1.2f);
            sparkPaint.setColor(light);sparkPaint.setAlpha((int)(190*strength));
            c.drawLine(x,y,220+(float)Math.cos(tail)*radius,195+(float)Math.sin(tail)*radius,sparkPaint);
            sparkPaint.setStyle(Paint.Style.FILL);sparkPaint.setAlpha((int)(220*strength));c.drawCircle(x,y,.8f,sparkPaint);
        }
        sparkPaint.setAlpha(255);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.save();float scale=Math.min(getWidth()/440f,getHeight()/245f);
        c.translate((getWidth()-440*scale)/2,(getHeight()-245*scale)/2);c.scale(scale,scale);
        stroke(c,185,16,palette.light?0xffcedde9:0x122c91fb,SWEEP);stroke(c,185,6,palette.light?0xff758da3:0xff112b46,SWEEP);
        stroke(c,185,1.5f,palette.light?0xff1b5f90:0xff3987c8,SWEEP);stroke(c,177,1,palette.light?0xff8196a9:0xff183249,SWEEP);
        if(!Float.isNaN(gpsSpeed)) {
            float sweep=SWEEP*arcSpeed/MAX;
            int accent=getAccentColor();
            // Keep the glow inside the existing view bounds and the scale legible above the band.
            progressStroke(c,PROGRESS_WIDTH+4,(accent&0x00ffffff)|0x25000000,sweep);
            progressStroke(c,PROGRESS_WIDTH+2,(accent&0x00ffffff)|0x77000000,sweep);
            progressStroke(c,PROGRESS_WIDTH,accent,sweep);
            drawFillSparks(c,sweep,accent);
        }
        paint.setStrokeCap(Paint.Cap.ROUND);
        for(int speed=0;speed<=120;speed+=10) {
            double angle=Math.toRadians(START+SWEEP*speed/MAX);
            boolean major=speed%20==0,medium=speed%10==0;
            float outer=172,inner=major?157:medium?162:166;
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(major?2:1);
            paint.setColor(palette.light?(major?0xff173b59:0xff3c5972):(major?0xff83b5d8:0xff36536a));
            c.drawLine(220+(float)Math.cos(angle)*inner,195+(float)Math.sin(angle)*inner,
                    220+(float)Math.cos(angle)*outer,195+(float)Math.sin(angle)*outer,paint);
            if(major) text(c,Integer.toString(speed),220+(float)Math.cos(angle)*140,
                    195+(float)Math.sin(angle)*140+4,13,palette.light?0xff1e3c56:0xff9fb6c9,labels);
        }
        if(!Float.isNaN(gpsSpeed)) {
            float sweep=SWEEP*arcSpeed/MAX;
            double angle=Math.toRadians(START+sweep);
            paint.setColor(palette.light?0xff082d48:0xffb4ecff);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);
            c.drawLine(220+(float)Math.cos(angle)*163,195+(float)Math.sin(angle)*163,
                    220+(float)Math.cos(angle)*193,195+(float)Math.sin(angle)*193,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(palette.light?0xff082d48:0xffe2f7ff);
            c.drawCircle(220+(float)Math.cos(angle)*185,195+(float)Math.sin(angle)*185,3.5f,paint);
        }
        text(c,display,220,168,82,Float.isNaN(gpsSpeed)?(palette.light?0xff3e566c:0xff7a8d9f):(palette.light?palette.text:Color.WHITE),digits);
        text(c,"km/h",220,198,17,palette.light?palette.secondary:0xff9fb5c8,labels);
        text(c,status,220,233,10.5f,Float.isNaN(gpsSpeed)?(palette.light?0xff815000:0xffd3ab65):(palette.light?palette.accent:0xff56bbeb),labels);
        c.restore();
    }
    @Override protected void onDetachedFromWindow(){setRunning(false);super.onDetachedFromWindow();}
}
