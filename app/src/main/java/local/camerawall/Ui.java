package local.camerawall;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;

/** Small shared style helpers for the native, dependency-free interface. */
final class Ui {
    static final int BACKGROUND = Color.rgb(11, 18, 32);
    static final int SURFACE = Color.rgb(17, 27, 45);
    static final int RAISED = Color.rgb(24, 38, 58);
    static final int SELECTED = Color.rgb(30, 58, 67);
    static final int OUTLINE = Color.rgb(43, 60, 82);
    static final int PRIMARY = Color.rgb(244, 247, 251);
    static final int SECONDARY = Color.rgb(155, 172, 191);
    static final int ACCENT = Color.rgb(84, 226, 192);
    static final int DANGER = Color.rgb(255, 122, 134);

    private Ui() { }

    static void button(Button button, boolean primary) {
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(dp(button.getContext(), 44));
        button.setPadding(dp(button.getContext(), 14), dp(button.getContext(), 8), dp(button.getContext(), 14), dp(button.getContext(), 8));
        button.setTextColor(primary ? BACKGROUND : PRIMARY);
        button.setBackground(shape(primary ? ACCENT : RAISED, primary ? ACCENT : OUTLINE, dp(button.getContext(), 14)));
    }

    static void panel(View view) {
        view.setBackground(shape(SURFACE, OUTLINE, dp(view.getContext(), 18)));
    }

    static GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, stroke);
        return drawable;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
