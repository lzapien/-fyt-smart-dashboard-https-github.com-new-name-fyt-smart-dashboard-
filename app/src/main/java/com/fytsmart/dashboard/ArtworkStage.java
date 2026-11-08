package com.fytsmart.dashboard;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;

/** The cover occupies .88 * .90 = 79.2% of the useful media body; metadata overlays it. */
final class ArtworkStage extends FrameLayout {
    ArtworkStage(Context context) { super(context); }
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() > 0) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)getChildAt(0).getLayoutParams();
            params.width = Math.round(MeasureSpec.getSize(widthMeasureSpec) * .88f);
            params.height = Math.round(MeasureSpec.getSize(heightMeasureSpec) * .90f);
            params.gravity = Gravity.CENTER;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
