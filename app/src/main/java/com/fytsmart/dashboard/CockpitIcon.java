package com.fytsmart.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small, resolution-independent line icons; no bitmap decoding on the UI thread. */
final class CockpitIcon extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String kind;
    private int color;
    CockpitIcon(Context context, String kind, int color) {
        super(context); this.kind = kind; this.color = color;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.save();
        float size = Math.min(getWidth(), getHeight());
        c.translate((getWidth()-size)/2, (getHeight()-size)/2);
        c.scale(size/24f,size/24f);
        paint.setColor(color); paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
        Path p = new Path();
        switch (kind) {
            case "radio":
                c.drawRoundRect(3,7,21,21,3,3,paint); c.drawLine(5,7,18,2,paint);
                c.drawCircle(8,14,2.3f,paint); c.drawLine(14,12,18,12,paint); c.drawLine(14,16,18,16,paint); break;
            case "nav":
                p.moveTo(12,2);p.lineTo(21,21);p.lineTo(12,17);p.lineTo(3,21);p.close();c.drawPath(p,paint);break;
            case "pin":
                c.drawCircle(12,9,2.5f,paint);p.moveTo(12,22);p.cubicTo(8,17,4,13,4,9);p.cubicTo(4,-1,20,-1,20,9);p.cubicTo(20,13,16,17,12,22);c.drawPath(p,paint);break;
            case "video":
                c.drawRoundRect(2,5,22,19,4,4,paint);p.moveTo(10,9);p.lineTo(16,12);p.lineTo(10,15);p.close();c.drawPath(p,paint);break;
            case "apps":
                for(int y=0;y<2;y++)for(int x=0;x<2;x++)c.drawRoundRect(3+x*11,3+y*11,10+x*11,10+y*11,1.5f,1.5f,paint);break;
            default:
                p.moveTo(9,17);p.lineTo(9,5);p.lineTo(20,3);p.lineTo(20,15);c.drawPath(p,paint);
                c.drawLine(9,9,20,7,paint);c.drawOval(3,16,9,21,paint);c.drawOval(14,14,20,19,paint);break;
        }
        c.restore();
    }
}
