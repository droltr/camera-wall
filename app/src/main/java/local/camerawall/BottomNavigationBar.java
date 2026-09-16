package local.camerawall;

import android.content.Context;
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
        setBackgroundColor(Ui.BACKGROUND);
        setPadding(Ui.dp(context, 8), Ui.dp(context, 4), Ui.dp(context, 8), Ui.dp(context, 6));
        addDestination("Ana", Destination.HOME, selected);
        addDestination("Kameralar", Destination.CAMERAS, selected);
        addDestination("Ayarlar", Destination.SETTINGS, selected);
    }

    void setListener(Listener value) {
        listener = value;
    }

    private void addDestination(String label, final Destination destination, Destination selected) {
        LinearLayout item = new LinearLayout(getContext());
        item.setOrientation(VERTICAL);
        item.setGravity(Gravity.CENTER);
        boolean active = destination == selected;
        item.setBackground(Ui.shape(active ? Ui.SELECTED : Ui.BACKGROUND, active ? Ui.ACCENT : Ui.BACKGROUND, Ui.dp(getContext(), 14)));
        String symbol = destination == Destination.HOME ? "⌂" : destination == Destination.CAMERAS ? "▦" : "⚙";
        TextView icon = new TextView(getContext());
        icon.setText(symbol);
        icon.setTextSize(18);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(active ? Ui.ACCENT : Ui.SECONDARY);
        TextView title = new TextView(getContext());
        title.setText(label);
        title.setTextSize(11);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(active ? Ui.PRIMARY : Ui.SECONDARY);
        item.addView(icon);
        item.addView(title);
        item.setContentDescription(label);
        item.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                if (listener != null) listener.onDestinationSelected(destination);
            }
        });
        LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(Ui.dp(getContext(), 3), 0, Ui.dp(getContext(), 3), 0);
        addView(item, params);
    }
}
