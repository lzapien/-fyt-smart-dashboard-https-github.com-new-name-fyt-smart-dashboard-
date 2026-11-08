package com.fytsmart.dashboard;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/** Cached Canvas artwork in the SUV's existing 400x260 coordinate system. No touch surface. */
final class DecorationRenderer {
    static final int SNOW_COUNT = 18;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path safeSides = new Path(), snowBank = new Path(), pine = new Path(), pineFrost = new Path();
    private final Path deer = new Path(), deerLegs = new Path(), antlers = new Path(), web = new Path();
    private final Path spiderLegs = new Path(), ghost = new Path();
    private final float[] snowX = new float[SNOW_COUNT], snowOffset = new float[SNOW_COUNT], snowSize = new float[SNOW_COUNT];
    private final int[] snowCycles = new int[SNOW_COUNT];
    private boolean daylight;
    private final Shader purpleMist = new RadialGradient(22, 178, 85,
            new int[]{0x303f235c, 0x003f235c}, null, Shader.TileMode.CLAMP);
    private final Shader daylightMist = new RadialGradient(22, 178, 85,
            new int[]{0x3061477d, 0x0061477d}, null, Shader.TileMode.CLAMP);

    DecorationRenderer() {
        // The full car rectangle is x=50..350, y=20..245. Decorations never enter it.
        safeSides.addRect(0, 0, 49, 260, Path.Direction.CW);
        safeSides.addRect(351, 0, 400, 260, Path.Direction.CW);
        snowBank.moveTo(-45, 260); snowBank.lineTo(102, 0);
        snowBank.moveTo(445, 260); snowBank.lineTo(298, 0);
        for (int i = 0; i < SNOW_COUNT; i++) {
            snowX[i] = i % 2 == 0 ? 7 + i * 13 % 36 : 357 + i * 17 % 36;
            snowOffset[i] = (i * 73 % 257) / 257f;
            snowSize[i] = 1 + (i % 4) * .35f;
            snowCycles[i] = 1 + i % 3;
        }
        pine.moveTo(0, -42); pine.lineTo(-6, -28); pine.lineTo(-3, -28);
        pine.lineTo(-10, -15); pine.lineTo(-6, -15); pine.lineTo(-14, 0);
        pine.lineTo(14, 0); pine.lineTo(6, -15); pine.lineTo(10, -15);
        pine.lineTo(3, -28); pine.lineTo(6, -28); pine.close();
        pineFrost.moveTo(-5, -28); pineFrost.lineTo(4, -28);
        pineFrost.moveTo(-9, -15); pineFrost.lineTo(7, -15);
        pineFrost.moveTo(-12, 0); pineFrost.lineTo(11, 0);

        deer.addOval(2, -6, 22, 4, Path.Direction.CW);
        deer.moveTo(17, -1); deer.lineTo(21, -17); deer.lineTo(25, -16); deer.lineTo(24, 2); deer.close();
        deer.addOval(22, -21, 31, -14, Path.Direction.CW);
        deer.moveTo(23, -18); deer.lineTo(20, -24); deer.lineTo(26, -21); deer.close();
        deer.moveTo(3, -2); deer.lineTo(-1, -7); deer.lineTo(0, 0); deer.close();
        for (int x : new int[]{5, 9, 17, 21}) { deerLegs.moveTo(x, 1); deerLegs.lineTo(x - 1, 13); }
        antlers.moveTo(26, -20); antlers.lineTo(25, -29); antlers.lineTo(21, -33);
        antlers.moveTo(25, -27); antlers.lineTo(29, -32);
        antlers.moveTo(23, -30); antlers.lineTo(23, -34);

        for (int i = 0; i <= 5; i++) {
            double angle = Math.PI * i / 10;
            web.moveTo(2, 3); web.lineTo(2 + (float)Math.cos(angle) * 39, 3 + (float)Math.sin(angle) * 39);
        }
        for (int r : new int[]{12, 24, 36}) {
            web.moveTo(2 + r, 3);
            for (int i = 1; i <= 5; i++) {
                double a = Math.PI * (i - .5) / 10, end = Math.PI * i / 10;
                web.quadTo(2 + (float)Math.cos(a) * (r - 2), 3 + (float)Math.sin(a) * (r - 2),
                        2 + (float)Math.cos(end) * r, 3 + (float)Math.sin(end) * r);
            }
        }
        for (int side : new int[]{-1, 1}) for (int i = 0; i < 4; i++) {
            float y = -3 + i * 2;
            spiderLegs.moveTo(side * 2, y); spiderLegs.lineTo(side * 6, y - 2);
            spiderLegs.lineTo(side * 9, y + (i < 2 ? -4 : 3));
        }
        ghost.moveTo(-8, 10); ghost.lineTo(-8, -1);
        ghost.cubicTo(-8, -15, 8, -15, 8, -1); ghost.lineTo(8, 10);
        ghost.quadTo(5, 5, 3, 10); ghost.quadTo(0, 5, -3, 10); ghost.quadTo(-6, 5, -8, 10); ghost.close();
    }
    void draw(Canvas canvas, ThemeConfig theme, float phase) {
        draw(canvas, theme, phase, false);
    }
    void draw(Canvas canvas, ThemeConfig theme, float phase, boolean daylight) {
        if (theme == ThemeConfig.DEFAULT) return;
        this.daylight = daylight;
        canvas.save(); canvas.clipRect(0, 0, 400, 260); canvas.clipPath(safeSides);
        paint.setShader(null); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
        if (theme == ThemeConfig.CHRISTMAS) christmas(canvas, phase);
        else halloween(canvas, phase);
        paint.setShader(null); canvas.restore();
    }
    private void fill(int color) { paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color); }
    private void stroke(int color, float width) { paint.setShader(null); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(width); paint.setColor(color); }
    private int contrast(int day, int night) { return daylight ? day : night; }
    private void christmas(Canvas c, float phase) {
        stroke(contrast(0x55405f78, 0x557babc0), 11); c.drawPath(snowBank, paint);
        fill(contrast(0x55557588, 0x559bb9c8)); c.drawOval(0, 239, 48, 258, paint); c.drawOval(353, 226, 400, 249, paint);
        tree(c, 20, 159, .75f); tree(c, 34, 104, .43f);
        tree(c, 377, 144, .65f); tree(c, 384, 225, .8f);
        reindeer(c, 10, 231, .8f);
        c.save(); c.translate(400, 0); c.scale(-1, 1); reindeer(c, 13, 194, .72f); c.restore();
        for (int i = 0; i < SNOW_COUNT; i++) {
            float travel = (snowOffset[i] + phase * snowCycles[i]) % 1f;
            float x = snowX[i] + (float)Math.sin((phase + snowOffset[i]) * Math.PI * 2) * 1.6f;
            float y = travel * 284 - 12, size = snowSize[i];
            if (i % 3 == 0) {
                stroke(contrast(0xff304b63, 0xffe9f8ff), .7f);
                c.drawLine(x, y - size, x, y + size, paint);
                c.drawLine(x - size * .86f, y - size * .5f, x + size * .86f, y + size * .5f, paint);
                c.drawLine(x - size * .86f, y + size * .5f, x + size * .86f, y - size * .5f, paint);
            } else { fill(contrast(0xff304b63, 0xffe9f8ff)); c.drawCircle(x, y, size * .6f, paint); }
        }
    }
    private void tree(Canvas c, float x, float y, float size) {
        c.save(); c.translate(x, y); c.scale(size, size);
        fill(contrast(0xff3b3026, 0xffc2b6a5)); c.drawRect(-2, 0, 2, 7, paint);
        fill(contrast(0xff16523d, 0xff88cdb4)); c.drawPath(pine, paint);
        stroke(contrast(0xffd8e8ed, 0xffe5f5ff), 1.2f); c.drawPath(pineFrost, paint); c.restore();
    }
    private void reindeer(Canvas c, float x, float y, float size) {
        c.save(); c.translate(x, y); c.scale(size, size);
        fill(contrast(0xff3a5163, 0xffd2e4ee)); c.drawPath(deer, paint);
        stroke(contrast(0xff3a5163, 0xffd2e4ee), 1.6f); c.drawPath(deerLegs, paint); c.drawPath(antlers, paint); c.restore();
    }
    private void halloween(Canvas c, float phase) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xffffffff); paint.setShader(daylight ? daylightMist : purpleMist);
        c.drawRect(0, 80, 49, 260, paint);
        c.save(); c.translate(400, 0); c.scale(-1, 1); c.drawRect(0, 80, 49, 260, paint); c.restore(); paint.setShader(null);
        stroke(contrast(0xff39415c, 0xffbdc9dc), .75f); c.drawPath(web, paint);
        c.save(); c.translate(400, 0); c.scale(-1, 1); c.drawPath(web, paint); c.restore();
        float drift = (float)Math.sin(phase * Math.PI * 2);
        spider(c, 20 + drift * 2, 64 + drift * 5, 1);
        spider(c, 378 - drift * 2, 79 - drift * 5, .85f);
        specter(c, 22 + drift * 2, 139 + drift * 4, .95f);
        specter(c, 378 - drift * 2, 194 - drift * 4, 1.1f);
        fill(contrast(0x285e3d12, 0x28aa703d)); c.drawOval(2, 236, 43, 251, paint); c.drawOval(360, 244, 397, 259, paint);
    }
    private void spider(Canvas c, float x, float y, float size) {
        stroke(contrast(0xff4e3c65, 0xffb7a1d0), .7f); c.drawLine(x, 35, x, y - 4 * size, paint);
        c.save(); c.translate(x, y); c.scale(size, size);
        stroke(contrast(0xff754214, 0xffe8b882), .9f); c.drawPath(spiderLegs, paint);
        fill(contrast(0xff4f365d, 0xffcfbce3)); c.drawOval(-3, -4, 3, 4, paint); c.drawCircle(0, -5, 2, paint); c.restore();
    }
    private void specter(Canvas c, float x, float y, float size) {
        c.save(); c.translate(x, y); c.scale(size, size);
        fill(contrast(0xff443451, 0xffc9bde6)); c.drawPath(ghost, paint);
        fill(contrast(0xfffbf3ff, 0xff2c223c)); c.drawOval(-4, -3, -1, 1, paint); c.drawOval(1, -3, 4, 1, paint); c.restore();
    }
}
