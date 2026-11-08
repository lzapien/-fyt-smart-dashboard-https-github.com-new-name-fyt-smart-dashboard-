package com.fytsmart.dashboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import java.util.function.Supplier;

/** Shared styling for settings and drawer. Media artwork overlays keep their own dark scrim. */
final class DisplayUi {
    static ArrayAdapter<String> spinnerAdapter(Context context,String[] labels,Supplier<DashboardPalette> palette) {
        return new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,labels) {
            { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); }
            private View styled(View view,boolean dropdown) {
                TextView text=(TextView)view;DashboardPalette p=palette.get();
                text.setTextColor(p.text);text.setTextSize(18);
                if(dropdown)text.setBackgroundColor(p.panelTop);
                return view;
            }
            @Override public View getView(int position,View recycled,ViewGroup parent) { return styled(super.getView(position,recycled,parent),false); }
            @Override public View getDropDownView(int position,View recycled,ViewGroup parent) { return styled(super.getDropDownView(position,recycled,parent),true); }
        };
    }
    static void apply(View view,DashboardPalette palette) {
        if(view instanceof TextView) {
            TextView text=(TextView)view;
            Object role=text.getTag(R.id.display_original_text);
            if(!(role instanceof Integer)) { role=text.getCurrentTextColor();text.setTag(R.id.display_original_text,role); }
            text.setTextColor((Integer)role==0xffa9bbd0?palette.secondary:palette.text);
            if(view instanceof EditText) {
                text.setHintTextColor(palette.secondary);text.setBackgroundTintList(ColorStateList.valueOf(palette.accent));
            } else if(view instanceof Button && !(view instanceof Switch)) {
                GradientDrawable bg=new GradientDrawable();bg.setColor(palette.button);
                bg.setCornerRadius(12*view.getResources().getDisplayMetrics().density);
                bg.setStroke(Math.round(view.getResources().getDisplayMetrics().density),palette.border);
                view.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(palette.pressed),bg,null));
            }
            if(view instanceof Switch) {
                Switch toggle=(Switch)view;
                toggle.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{palette.accent,palette.secondary}));
                toggle.setTrackTintList(ColorStateList.valueOf(palette.button));
            }
        }
        if(view instanceof Spinner) {
            view.setBackgroundTintList(ColorStateList.valueOf(palette.secondary));
            if(((Spinner)view).getAdapter() instanceof ArrayAdapter) ((ArrayAdapter<?>)((Spinner)view).getAdapter()).notifyDataSetChanged();
        } else if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)apply(group.getChildAt(i),palette);
        }
    }
    private DisplayUi() { }
}
