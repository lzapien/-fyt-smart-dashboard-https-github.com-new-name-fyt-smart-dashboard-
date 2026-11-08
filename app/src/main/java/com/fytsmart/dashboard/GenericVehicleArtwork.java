package com.fytsmart.dashboard;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/** Original rear-view vehicle geometry, without a manufacturer, model or emblem.
 * Coordinates share SuvRearView's 400 x 260 design space and editable plate area.
 */
final class GenericVehicleArtwork {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path body = new Path(), glass = new Path(), bumper = new Path();
    private final Shader silver = new LinearGradient(100, 0, 300, 0,
            new int[]{0xff697785, 0xffc5cdd4, 0xfff1f3f5, 0xffaeb9c3, 0xff5a6775},
            new float[]{0, .23f, .5f, .77f, 1}, Shader.TileMode.CLAMP);
    private final Shader window = new LinearGradient(0, 45, 0, 95,
            new int[]{0xff334959, 0xff101e2b}, null, Shader.TileMode.CLAMP);

    GenericVehicleArtwork() {
        body.moveTo(139, 31); body.quadTo(200, 22, 261, 31);
        body.quadTo(272, 34, 277, 48); body.lineTo(291, 92);
        body.quadTo(303, 107, 304, 129); body.lineTo(304, 192);
        body.quadTo(301, 207, 284, 209); body.lineTo(116, 209);
        body.quadTo(99, 207, 96, 192); body.lineTo(96, 129);
        body.quadTo(97, 107, 109, 92); body.lineTo(123, 48);
        body.quadTo(128, 34, 139, 31); body.close();
        glass.moveTo(140, 45); glass.quadTo(200, 39, 260, 45);
        glass.lineTo(276, 91); glass.quadTo(200, 96, 124, 91);
        glass.close();
        bumper.moveTo(100, 176); bumper.quadTo(200, 187, 300, 176);
        bumper.lineTo(299, 195); bumper.quadTo(200, 210, 101, 195);
        bumper.close();
    }

    void draw(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL); paint.setShader(null);
        paint.setColor(0x60000000); canvas.drawOval(89, 210, 311, 235, paint);
        paint.setColor(0xff151b23);
        canvas.drawRect(105, 178, 130, 225, paint);
        canvas.drawRect(270, 178, 295, 225, paint);
        // Mirrors and tires are deliberately independent of any manufacturer's silhouette.
        paint.setColor(0xff7b8995);
        canvas.drawOval(88, 89, 111, 105, paint);
        canvas.drawOval(289, 89, 312, 105, paint);
        paint.setShader(silver); canvas.drawPath(body, paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.6f);
        paint.setColor(0xff738391); canvas.drawPath(body, paint);
        paint.setStyle(Paint.Style.FILL); paint.setShader(window);
        canvas.drawPath(glass, paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setColor(0xffd2dce3);
        paint.setStrokeWidth(1.2f); canvas.drawPath(glass, paint);
        paint.setColor(0x55758a9a); canvas.drawLine(147, 49, 130, 88, paint);
        paint.setColor(0xff273743); paint.setStrokeWidth(2.4f);
        canvas.drawLine(190, 90, 231, 87, paint);
        paint.setStrokeWidth(1); paint.setColor(0xff74828e);
        canvas.drawLine(114, 145, 119, 169, paint);
        canvas.drawLine(119, 169, 281, 169, paint);
        canvas.drawLine(281, 169, 286, 145, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xff293640);
        canvas.drawPath(bumper, paint);
        paint.setColor(0xff8998a5); canvas.drawRect(137, 192, 263, 198, paint);
        // Short rectangular rear lamps: no model-specific vertical signature.
        paint.setColor(0xff782d39);
        canvas.drawRect(108, 101, 150, 115, paint);
        canvas.drawRect(250, 101, 292, 115, paint);
        paint.setColor(0xffd85563);
        canvas.drawRect(111, 105, 147, 109, paint);
        canvas.drawRect(253, 105, 289, 109, paint);
        paint.setColor(0xff687985); canvas.drawRect(172, 109, 228, 137, paint);
        paint.setColor(0xff17212b); canvas.drawRect(176.5f, 113, 223.5f, 133, paint);
        paint.setColor(0xff8e9da9); canvas.drawRect(186, 103, 214, 105, paint);
        paint.setColor(0xffb44751);
        canvas.drawRect(111, 185, 130, 189, paint);
        canvas.drawRect(270, 185, 289, 189, paint);
    }
}
