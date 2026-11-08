package com.fytsmart.dashboard;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.PowerManager;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Lightweight ambient weather. Static on low-RAM, power saver or disabled system animations. */
final class WeatherScene extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private DashboardPalette palette=DashboardPalette.DARK;
    void setPalette(DashboardPalette value) { palette=value;invalidate(); }
    private String mode = "unknown";
    private float phase;
    private boolean running;
    private ValueAnimator animator;
    WeatherScene(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    void setWeather(String icon) { mode = icon; updateAnimation(); invalidate(); }
    void setRunning(boolean value) { running = value; updateAnimation(); }
    private void updateAnimation() {
        ActivityManager am = (ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
        PowerManager pm = (PowerManager)getContext().getSystemService(Context.POWER_SERVICE);
        boolean allowed = running && !"unknown".equals(mode) && ValueAnimator.areAnimatorsEnabled()
                && am != null && !am.isLowRamDevice() && pm != null && !pm.isPowerSaveMode();
        if (animator != null) { animator.cancel(); animator = null; }
        if (!allowed) return;
        animator = ValueAnimator.ofFloat(0,1); animator.setDuration(18000);
        animator.setRepeatCount(ValueAnimator.INFINITE); animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> { phase=(float)a.getAnimatedValue(); invalidate(); });
        animator.start();
    }
    @Override protected void onDetachedFromWindow() { setRunning(false); super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float h=getHeight(), w=getWidth();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.4f*getResources().getDisplayMetrics().density);
        paint.setColor(palette.light?0x25005a91:0x2557cbe2);
        float x=w*.82f;
        if ("☀".equals(mode) || "☾".equals(mode)) {
            paint.setColor(palette.light?("☾".equals(mode)?0x25005a91:0x25865000):("☾".equals(mode)?0x254fa5d7:0x35eeb75a));
            float pulse=(float)Math.sin(phase*Math.PI*2)*h*.04f;
            canvas.drawCircle(x,h*.6f,h*.32f+pulse,paint);
            canvas.drawCircle(x,h*.6f,h*.56f+pulse,paint);
            canvas.drawCircle(x,h*.6f,h*.8f+pulse,paint);
        } else if ("☁".equals(mode) || "≋".equals(mode)) {
            for (int i=0;i<3;i++) {
                float offset=(phase-.5f)*h*.24f;
                canvas.drawRoundRect(x-h*.7f+offset+i*h*.24f,h*.25f+i*h*.17f,
                        x+h*.6f+offset+i*h*.24f,h*.35f+i*h*.17f,h*.05f,h*.05f,paint);
            }
        } else if ("☂".equals(mode) || "⚡".equals(mode) || "❄".equals(mode)) {
            for(int i=0;i<14;i++) {
                float rx=w*.68f+(i%7)*h*.34f;
                float ry=((i/7f+phase*2)%1f)*h;
                canvas.drawLine(rx,ry,rx-h*.04f,ry+h*.1f,paint);
            }
        } else {
            canvas.drawCircle(x,h*.6f,h*.7f,paint);
            canvas.drawCircle(x,h*.6f,h*.44f,paint);
        }
    }
}
