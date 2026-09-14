package local.camerawall;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

final class BottomNavigationBar extends LinearLayout {
    enum Destination { HOME, CAMERAS, SETTINGS }

    interface Listener {
        void onDestinationSelected(Destination destination);
    }

    private Listener listener;

    BottomNavigationBar(Context context, Destination selected) {
        super(context);
        setOrientation(HORIZONTAL);
        setBackgroundColor(Color.rgb(18, 18, 18));
        addDestination("Ana", Destination.HOME, selected);
        addDestination("Kameralar", Destination.CAMERAS, selected);
        addDestination("Ayarlar", Destination.SETTINGS, selected);
    }

    void setListener(Listener value) {
        listener = value;
    }

    private void addDestination(String label, final Destination destination, Destination selected) {
        TextView item = new TextView(getContext());
        item.setText(label);
        item.setTextSize(15);
        item.setGravity(Gravity.CENTER);
        item.setTextColor(destination == selected ? Color.WHITE : Color.rgb(170, 170, 170));
        item.setBackgroundColor(destination == selected ? Color.rgb(42, 58, 66) : Color.TRANSPARENT);
        item.setContentDescription(label);
        item.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                if (listener != null) listener.onDestinationSelected(destination);
            }
        });
        addView(item, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
    }
}
