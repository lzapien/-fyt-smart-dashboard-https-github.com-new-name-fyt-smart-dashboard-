package com.fytsmart.dashboard;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import android.view.Choreographer;
import android.view.animation.LinearInterpolator;

/** Brand-neutral Canvas vehicle with decorative lamps, projection and pavement reflections. */
final class SuvRearView extends View implements AutoCloseable {
    private final GenericVehicleArtwork artwork=new GenericVehicleArtwork();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path roadLeft=new Path(),roadRight=new Path(),leftLamp=new Path(),rightLamp=new Path(),leftBeam=new Path(),rightBeam=new Path();
    // Keep the animated marks at their existing offset. Move only the gray rails:
    // Rails stay clear of the generic vehicle's silhouette.
    private static final float LANE_OFFSET=8f;
    private static final float STATIC_LANE_OFFSET=48f;
    private DashboardPalette palette=DashboardPalette.DARK;
    void setPalette(DashboardPalette value) { palette=value;invalidate(); }
    private String plateText=VehiclePlateConfig.DEFAULT;
    private final RectF plateRect=new RectF(176.5f,113,223.5f,133);
    private final Typeface plateFont=Typeface.create("sans-serif-condensed",Typeface.BOLD);
    private final Paint.FontMetrics plateMetrics=new Paint.FontMetrics();
    private ThemeManager.Selection appearance=ThemeManager.DEFAULT;
    private DecorationRenderer decorations;
    private Choreographer choreographer;
    private boolean decorationScheduled;
    private float decorationPhase;
    private long decorationAnchor;
    private final Choreographer.FrameCallback decorationFrame=this::animateDecorations;
    private final Shader blueGround=new RadialGradient(200,239,140,new int[]{0x282976b3,0x00000000},null,Shader.TileMode.CLAMP);
    private final Shader projection=new LinearGradient(0,0,0,120,new int[]{0x00cde9ff,0x30cde9ff},null,Shader.TileMode.CLAMP);
    private final Shader reflection=new LinearGradient(0,220,0,260,new int[]{0x55ff445c,0x00ff445c},null,Shader.TileMode.CLAMP);
    private ValueAnimator motion,lightFade;
    private boolean running,animationAllowed=true,moving,closed;
    private float phase,lightLevel;
    SuvRearView(Context context) {
        super(context);setContentDescription("Vehículo genérico desde atrás, iluminación virtual por horario");
        roadLeft.moveTo(8-STATIC_LANE_OFFSET,260);roadLeft.lineTo(155-STATIC_LANE_OFFSET,0);
        roadRight.moveTo(392+STATIC_LANE_OFFSET,260);roadRight.lineTo(245+STATIC_LANE_OFFSET,0);
        leftBeam.moveTo(100,110);leftBeam.lineTo(0,0);leftBeam.lineTo(165,0);leftBeam.lineTo(155,110);leftBeam.close();
        rightBeam.moveTo(300,110);rightBeam.lineTo(400,0);rightBeam.lineTo(235,0);rightBeam.lineTo(245,110);rightBeam.close();
        leftLamp.moveTo(112,107);leftLamp.lineTo(146,107);
        rightLamp.moveTo(254,107);rightLamp.lineTo(288,107);
    }
    boolean hasDetailedArtwork(){return true;}
    float getLightingLevel(){return lightLevel;}
    void applyAppearance(ThemeManager.Selection selection) {
        if(selection==null)selection=ThemeManager.DEFAULT;
        if(appearance.theme!=selection.theme||appearance.animations!=selection.animations) {
            stopDecorationFrames();decorationPhase=0;
        }
        appearance=selection;plateText=selection.plate;
        if(selection.theme!=ThemeConfig.DEFAULT&&decorations==null)decorations=new DecorationRenderer();
        updateDecorationMotion();invalidate();
    }
    void setLighting(VehicleLightingPolicy.State state,boolean animate) {
        setContentDescription("Vehículo genérico desde atrás, "+(state.source==VehicleLightingPolicy.Source.SCHEDULE_DECORATIVE?"iluminación virtual decorativa por horario":"iluminación virtual de fuente CANBUS verificada"));
        setLightingLevel(state.on?1:0,animate);
    }
    void setLightingLevel(float value,boolean animate) {
        float target=Math.max(0,Math.min(1,value));
        if(lightFade!=null){lightFade.cancel();lightFade=null;}
        if(Math.abs(target-lightLevel)<.001f)return;
        if(running&&animate&&animationAllowed&&ValueAnimator.areAnimatorsEnabled()) {
            lightFade=ValueAnimator.ofFloat(lightLevel,target);lightFade.setDuration(500);
            lightFade.addUpdateListener(a->{lightLevel=(float)a.getAnimatedValue();invalidate();});lightFade.start();
        } else {lightLevel=target;invalidate();}
    }
    void setRunning(boolean value,boolean allowAnimation) {
        if(running==value&&animationAllowed==allowAnimation){updateDecorationMotion();return;}
        running=value;animationAllowed=allowAnimation;
        if(!value&&lightFade!=null){lightFade.end();lightFade=null;}updateMotion();
        updateDecorationMotion();
    }
    private boolean canAnimateDecorations() {
        return !closed&&running&&animationAllowed&&appearance.animations&&appearance.theme!=ThemeConfig.DEFAULT
                &&isAttachedToWindow()&&getWindowVisibility()==VISIBLE&&isShown()&&ValueAnimator.areAnimatorsEnabled();
    }
    private void updateDecorationMotion() {
        if(!canAnimateDecorations()){stopDecorationFrames();return;}
        if(decorationScheduled)return;
        if(choreographer==null)choreographer=Choreographer.getInstance();
        decorationAnchor=System.nanoTime()-(long)(decorationPhase*30_000_000_000L);
        decorationScheduled=true;choreographer.postFrameCallback(decorationFrame);
    }
    private void animateDecorations(long frameNanos) {
        decorationScheduled=false;
        if(!canAnimateDecorations())return;
        decorationPhase=(Math.max(0,frameNanos-decorationAnchor)%30_000_000_000L)/30_000_000_000f;
        invalidate();decorationScheduled=true;choreographer.postFrameCallbackDelayed(decorationFrame,40);
    }
    private void stopDecorationFrames() {
        if(decorationScheduled&&choreographer!=null)choreographer.removeFrameCallback(decorationFrame);
        decorationScheduled=false;
    }
    void setGpsSpeed(float kmh){boolean next=Float.isFinite(kmh)&&kmh>1;if(next!=moving){moving=next;updateMotion();}}
    private void updateMotion() {
        if(motion!=null){motion.cancel();motion=null;}
        if(!running||!animationAllowed||!moving||!ValueAnimator.areAnimatorsEnabled()){invalidate();return;}
        motion=ValueAnimator.ofFloat(0,1);motion.setDuration(2400);motion.setRepeatCount(ValueAnimator.INFINITE);motion.setInterpolator(new LinearInterpolator());
        motion.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});motion.start();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);c.save();float s=Math.min(getWidth()/400f,getHeight()/260f);
        c.translate((getWidth()-400*s)/2,(getHeight()-260*s)/2);c.scale(s,s);
        paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setShader(blueGround);c.drawOval(0,183,400,260,paint);paint.setShader(null);
        if(decorations!=null)decorations.draw(c,appearance.theme,decorationPhase,palette.light);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.8f);paint.setColor(palette.light?0xff485d70:0xff7c858f);
        c.drawPath(roadLeft,paint);c.drawPath(roadRight,paint);
        for(int i=0;i<3;i++) {
            float t=(i+phase)/3f,y=5+t*t*252;paint.setStrokeWidth(2.5f+t*2);paint.setColor(palette.light?0xff896500:0xe6ffed38);
            c.drawLine(25+(1-t)*134-LANE_OFFSET,y,25+(1-t)*134-4-t*8-LANE_OFFSET,y+5+t*12,paint);
            c.drawLine(375-(1-t)*134+LANE_OFFSET,y,375-(1-t)*134+4+t*8+LANE_OFFSET,y+5+t*12,paint);
        }
        paint.setStyle(Paint.Style.FILL);paint.setShader(projection);paint.setAlpha((int)(255*lightLevel));
        c.drawPath(leftBeam,paint);c.drawPath(rightBeam,paint);paint.setShader(null);paint.setAlpha(255);
        artwork.draw(c);drawPlate(c);
        if(lightLevel>0) {
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0xffff3a52);paint.setAlpha((int)(28*lightLevel));paint.setStrokeWidth(14);c.drawPath(leftLamp,paint);c.drawPath(rightLamp,paint);
            paint.setAlpha((int)(75*lightLevel));paint.setStrokeWidth(7);c.drawPath(leftLamp,paint);c.drawPath(rightLamp,paint);
            paint.setAlpha((int)(220*lightLevel));paint.setStrokeWidth(2.8f);c.drawPath(leftLamp,paint);c.drawPath(rightLamp,paint);
            paint.setStyle(Paint.Style.FILL);paint.setShader(reflection);paint.setAlpha((int)(180*lightLevel));
            c.drawOval(77,229,135,259,paint);c.drawOval(265,229,323,259,paint);paint.setShader(null);
        }
        paint.setAlpha(255);c.restore();
    }
    private void drawPlate(Canvas canvas) {
        canvas.save();canvas.clipRect(plateRect);
        paint.setShader(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(plateFont);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(10.5f);
        float width=paint.measureText(plateText),available=plateRect.width()-6;
        if(width>available)paint.setTextSize(paint.getTextSize()*available/width);
        paint.getFontMetrics(plateMetrics);paint.setColor(0xffe2e5e8);
        canvas.drawText(plateText,plateRect.centerX(),plateRect.centerY()-(plateMetrics.ascent+plateMetrics.descent)/2,paint);
        canvas.restore();
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();updateDecorationMotion();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(appearance!=null)updateDecorationMotion();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(appearance!=null)updateDecorationMotion();}
    @Override protected void onDetachedFromWindow(){setRunning(false,false);super.onDetachedFromWindow();}
    @Override public void close(){closed=true;setRunning(false,false);}
}
