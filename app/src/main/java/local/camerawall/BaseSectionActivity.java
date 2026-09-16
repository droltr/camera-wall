package local.camerawall;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

abstract class BaseSectionActivity extends Activity implements BottomNavigationBar.Listener {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BACKGROUND);

        root.addView(createContentView(), new LinearLayout.LayoutParams(-1, 0, 1f));

        BottomNavigationBar navigation = new BottomNavigationBar(this, destination());
        navigation.setListener(this);
        root.addView(navigation, new LinearLayout.LayoutParams(-1, dp(52)));
        setContentView(root);
    }

    abstract String sectionTitle();
    abstract BottomNavigationBar.Destination destination();

    View createContentView() {
        TextView placeholder = new TextView(this);
        placeholder.setText(sectionTitle() + "\n\nBu ekran sonraki alpha aşamasında tamamlanacak.");
        placeholder.setTextColor(Color.WHITE);
        placeholder.setTextSize(22);
        placeholder.setGravity(Gravity.CENTER);
        return placeholder;
    }

    @Override public void onDestinationSelected(BottomNavigationBar.Destination selected) {
        if (selected == destination()) return;
        Intent intent;
        if (selected == BottomNavigationBar.Destination.HOME) {
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else if (selected == BottomNavigationBar.Destination.CAMERAS) {
            intent = new Intent(this, CameraListActivity.class);
        } else {
            intent = new Intent(this, SettingsActivity.class);
        }
        startActivity(intent);
        finish();
    }

    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) hideSystemUi();
    }

    final int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }
}
