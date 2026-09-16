package local.camerawall;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Clear, large touch targets for the app's three main sections. */
final class BottomNavigationBar extends LinearLayout {
    static final int HEIGHT_DP = 68;

    enum Destination { HOME, CAMERAS, SETTINGS }

    interface Listener {
        void onDestinationSelected(Destination destination);
    }

    private Listener listener;

    BottomNavigationBar(Context context, Destination selected) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundColor(Ui.SURFACE);
        setPadding(Ui.dp(context, 10), Ui.dp(context, 7), Ui.dp(context, 10), Ui.dp(context, 7));
        addDestination("Canlı", Destination.HOME, selected);
        addDestination("Kameralar", Destination.CAMERAS, selected);
        addDestination("Ayarlar", Destination.SETTINGS, selected);
    }

    void setListener(Listener value) {
        listener = value;
    }

    private void addDestination(String label, final Destination destination, Destination selected) {
        final boolean active = destination == selected;
        LinearLayout item = new LinearLayout(getContext());
        item.setOrientation(HORIZONTAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(Ui.dp(getContext(), 12), 0, Ui.dp(getContext(), 12), 0);
        item.setBackground(Ui.shape(active ? Ui.SELECTED : Ui.SURFACE,
            active ? Ui.ACCENT : Ui.OUTLINE, Ui.dp(getContext(), 18)));
        item.setClickable(true);
        item.setFocusable(true);

        NavIcon icon = new NavIcon(getContext(), destination, active ? Ui.ACCENT : Ui.SECONDARY);
        item.addView(icon, new LinearLayout.LayoutParams(Ui.dp(getContext(), 23), Ui.dp(getContext(), 23)));

        TextView title = new TextView(getContext());
        title.setText(label);
        title.setTextSize(14);
        title.setTextColor(active ? Ui.PRIMARY : Ui.SECONDARY);
        title.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        title.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-2, -1);
        titleParams.leftMargin = Ui.dp(getContext(), 9);
        item.addView(title, titleParams);

        item.setContentDescription(label + (active ? ", açık bölüm" : ", bölümü aç"));
        item.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                if (listener != null) listener.onDestinationSelected(destination);
            }
        });

        LayoutParams params = new LayoutParams(0, -1, 1f);
        params.setMargins(Ui.dp(getContext(), 4), 0, Ui.dp(getContext(), 4), 0);
        addView(item, params);
    }

    private static final class NavIcon extends View {
        private final Destination destination;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        NavIcon(Context context, Destination destination, int color) {
            super(context);
            this.destination = destination;
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Ui.dp(context, 2));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scale = getResources().getDisplayMetrics().density;
            canvas.save();
            canvas.translate((getWidth() - 24 * scale) / 2, (getHeight() - 24 * scale) / 2);
            canvas.scale(scale, scale);
            if (destination == Destination.HOME) drawHome(canvas);
            else if (destination == Destination.CAMERAS) drawCameras(canvas);
            else drawSettings(canvas);
            canvas.restore();
        }

        private void drawHome(Canvas canvas) {
            Path roof = new Path();
            roof.moveTo(3, 11); roof.lineTo(12, 4); roof.lineTo(21, 11);
            canvas.drawPath(roof, paint);
            canvas.drawRoundRect(new RectF(5, 10, 19, 21), 2, 2, paint);
            canvas.drawLine(10, 21, 10, 15, paint);
            canvas.drawLine(14, 21, 14, 15, paint);
        }

        private void drawCameras(Canvas canvas) {
            canvas.drawRoundRect(new RectF(2.5f, 5, 21.5f, 20), 3, 3, paint);
            canvas.drawCircle(12, 12.5f, 4.5f, paint);
            canvas.drawCircle(12, 12.5f, 1, paint);
            canvas.drawLine(6, 3, 18, 3, paint);
        }

        private void drawSettings(Canvas canvas) {
            canvas.drawCircle(12, 12, 3.5f, paint);
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                float innerX = 12 + (float) Math.cos(radians) * 6;
                float innerY = 12 + (float) Math.sin(radians) * 6;
                float outerX = 12 + (float) Math.cos(radians) * 9;
                float outerY = 12 + (float) Math.sin(radians) * 9;
                canvas.drawLine(innerX, innerY, outerX, outerY, paint);
            }
            canvas.drawCircle(12, 12, 9, paint);
        }
    }
}
