package com.fytsmart.dashboard;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
/** Fixed shape and bounds; only its color changes with horizontal GPS accuracy. */
final class GpsIndicatorView extends View {
    private DashboardPalette palette=DashboardPalette.DARK;
    void setPalette(DashboardPalette value){palette=value;invalidate();}
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color=GpsTelemetry.RED;
    GpsIndicatorView(Context c){super(c);setContentDescription("Precisión horizontal GPS no disponible");}
    void setAccuracy(int value,String description){color=value;setContentDescription(description);invalidate();}
    int getIndicatorColor(){return color;}
    @Override protected void onDraw(Canvas c){
        float s=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-s)/2,(getHeight()-s)/2);c.scale(s/24,s/24);
        p.setColor(palette.light?android.graphics.Color.rgb(android.graphics.Color.red(color)*2/5,android.graphics.Color.green(color)*2/5,android.graphics.Color.blue(color)*2/5):color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);c.drawCircle(12,12,7,p);c.drawCircle(12,12,2,p);
        c.drawLine(12,1,12,6,p);c.drawLine(12,18,12,23,p);c.drawLine(1,12,6,12,p);c.drawLine(18,12,23,12,p);c.restore();
    }
}
