package com.fytsmart.dashboard;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
/** GPS course rose; no sensor or invented heading when stationary. */
final class CompassIndicatorView extends View {
    private DashboardPalette palette=DashboardPalette.DARK;
    void setPalette(DashboardPalette value){palette=value;invalidate();}
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private float bearing=Float.NaN;
    CompassIndicatorView(Context c){super(c);setContentDescription("Brújula GPS sin rumbo fiable");}
    void setBearing(float value){bearing=value;setContentDescription(Float.isNaN(value)?"Brújula GPS sin rumbo fiable":"Rumbo GPS "+Math.round(value)+" grados");invalidate();}
    @Override protected void onDraw(Canvas c){
        float s=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-s)/2,(getHeight()-s)/2);c.scale(s/24,s/24);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setColor(palette.light?0xff364c61:0xff637d91);c.drawCircle(12,12,10,p);
        c.drawLine(12,1,12,4,p);c.drawLine(12,20,12,23,p);c.drawLine(1,12,4,12,p);c.drawLine(20,12,23,12,p);
        if(Float.isFinite(bearing)){c.rotate(bearing,12,12);p.setColor(palette.light?palette.accent:0xff63c7ed);p.setStrokeWidth(2);c.drawLine(12,5,12,18,p);c.drawLine(12,5,9,9,p);c.drawLine(12,5,15,9,p);}
        else {p.setColor(palette.light?palette.secondary:0xff8093a5);c.drawLine(8,12,16,12,p);}
        c.restore();
    }
}
