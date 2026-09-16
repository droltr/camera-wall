package local.camerawall;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/** Opens data-management screens directly, without starting camera playback. */
@RunWith(AndroidJUnit4.class)
public final class TabletUiSmokeTest {
    @Test public void testMainNavigationAndEmptyWall() throws Throwable {
        final android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final Activity main = launch(MainActivity.class);
        Activity cameras = null;
        Activity settings = null;
        try {
            instrumentation.waitForIdleSync();
            assertTrue("empty public build should show camera count without playback", findTextContaining(main.getWindow().getDecorView(), "0 kamera"));

            android.app.Instrumentation.ActivityMonitor camerasMonitor = instrumentation.addMonitor(CameraListActivity.class.getName(), null, false);
            clickDescription(main, "Kameralar, bölümü aç");
            cameras = instrumentation.waitForMonitorWithTimeout(camerasMonitor, 5000);
            instrumentation.removeMonitor(camerasMonitor);
            assertNotNull("camera navigation did not open", cameras);

            android.app.Instrumentation.ActivityMonitor settingsMonitor = instrumentation.addMonitor(SettingsActivity.class.getName(), null, false);
            clickDescription(cameras, "Ayarlar, bölümü aç");
            settings = instrumentation.waitForMonitorWithTimeout(settingsMonitor, 5000);
            instrumentation.removeMonitor(settingsMonitor);
            assertNotNull("settings navigation did not open", settings);
        } finally {
            final Activity closeSettings = settings;
            final Activity closeCameras = cameras;
            instrumentation.runOnMainSync(new Runnable() {
                @Override public void run() {
                    if (closeSettings != null && !closeSettings.isFinishing()) closeSettings.finish();
                    if (closeCameras != null && !closeCameras.isFinishing()) closeCameras.finish();
                    if (!main.isFinishing()) main.finish();
                }
            });
            instrumentation.waitForIdleSync();
        }
    }

    @Test public void testSettingsAndCameraListLayoutForCurrentOrientation() throws Throwable {
        verifyScreen(SettingsActivity.class, "settings");
        verifyScreen(CameraListActivity.class, "cameras");
    }

    @Test public void testCameraSearchAndReorderPersistence() throws Throwable {
        final CameraRepository repository = new CameraRepository(InstrumentationRegistry.getInstrumentation().getTargetContext());
        final List<CameraSpec> original = repository.getCameras();
        if (original.isEmpty()) {
            // Public builds intentionally contain no camera endpoints. Use
            // non-routable test data so this test is repeatable on a clean AVD.
            java.util.ArrayList<CameraSpec> fixtures = new java.util.ArrayList<>();
            fixtures.add(new CameraSpec("Test camera A", "rtsp://camera-a.invalid/stream", "", ""));
            fixtures.add(new CameraSpec("Test camera B", "rtsp://camera-b.invalid/stream", "", ""));
            assertTrue("test fixtures should be saved", repository.replaceAll(fixtures));
        }
        final List<CameraSpec> cameras = repository.getCameras();
        assertTrue("at least two cameras are required for reorder test", cameras.size() >= 2);
        final CameraListActivity activity = (CameraListActivity) launch(CameraListActivity.class);
        try {
            final EditText search = findSearchField(activity.getWindow().getDecorView());
            assertNotNull("camera search field not found", search);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override public void run() { search.setText("query-with-no-match-92841"); }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertTrue("empty search result summary not shown", findTextContaining(activity.getWindow().getDecorView(), "0 / "));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override public void run() { search.setText(""); }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            View handle = findViewByDescription(activity.getWindow().getDecorView(), cameras.get(0).name + " sırasını değiştirmek için basılı tutup sürükleyin");
            View destination = findViewByDescription(activity.getWindow().getDecorView(), cameras.get(1).name + ", kayıtlı kamera");
            assertNotNull("first drag handle not found", handle);
            assertNotNull("second camera row not found", destination);
            assertTrue("drag handle should be long-clickable", handle.isLongClickable());
            List<CameraSpec> moved = new java.util.ArrayList<>(cameras);
            CameraSpec first = moved.remove(0);
            moved.add(1, first);
            assertTrue("camera reorder should persist", repository.replaceAll(moved));
            assertEquals(cameras.get(1).name, repository.getCameras().get(0).name);
            repository.replaceAll(cameras);
            assertEquals(cameras.get(0).name, repository.getCameras().get(0).name);
        } finally {
            repository.replaceAll(original);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override public void run() { if (!activity.isFinishing()) activity.finish(); }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }

    private void verifyScreen(Class<? extends Activity> screen, String name) throws Throwable {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final Activity activity = launch(screen);
        org.junit.Assert.assertNotNull(activity);
        instrumentation.waitForIdleSync();
        int orientation = activity.getResources().getConfiguration().orientation;
        assertTrue("activity should have a valid display orientation",
            orientation == Configuration.ORIENTATION_PORTRAIT || orientation == Configuration.ORIENTATION_LANDSCAPE);
        String orientationName = orientation == Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait";
        assertContentIsLaidOut(activity);
        capture(activity, name + "-" + orientationName + ".png");

        if (screen == SettingsActivity.class) {
            org.junit.Assert.assertEquals("three compact selectors expected", 3, countViews(activity.getWindow().getDecorView(), Spinner.class));
            View focus = activity.getCurrentFocus();
            assertTrue("keyboard field should not be focused on settings entry", focus == null || !(focus instanceof EditText));
        }
        scroll(activity, true);
        instrumentation.waitForIdleSync();
        capture(activity, name + "-bottom-" + orientationName + ".png");
        scroll(activity, false);
        instrumentation.runOnMainSync(new Runnable() {
            @Override public void run() { activity.finish(); }
        });
        instrumentation.waitForIdleSync();
    }

    private void assertContentIsLaidOut(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        org.junit.Assert.assertNotNull(content);
        org.junit.Assert.assertTrue("screen content has no size", content.getWidth() > 0 && content.getHeight() > 0);
    }

    private void capture(Activity activity, String filename) throws Exception {
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        org.junit.Assert.assertNotNull("screenshot capture failed", screenshot);
        boolean landscape = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        assertEquals("screenshot dimensions must match active display orientation", landscape,
            screenshot.getWidth() > screenshot.getHeight());
        File directory = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ui-test-captures");
        if (!directory.exists() && !directory.mkdirs()) throw new Exception("Could not create private capture directory");
        FileOutputStream output = new FileOutputStream(new File(directory, filename));
        try {
            if (!screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new Exception("Could not write screenshot");
        } finally {
            output.close();
            screenshot.recycle();
        }
    }

    private Activity launch(Class<? extends Activity> screen) {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), screen);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
    }

    private void scroll(final Activity activity, final boolean bottom) throws Throwable {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override public void run() {
                ViewGroup frame = (ViewGroup) activity.findViewById(android.R.id.content);
                ViewGroup root = (ViewGroup) frame.getChildAt(0);
                ScrollView scroll = (ScrollView) root.getChildAt(0);
                scroll.fullScroll(bottom ? View.FOCUS_DOWN : View.FOCUS_UP);
            }
        });
    }

    private int countViews(View root, Class<?> type) {
        int count = type.isInstance(root) ? 1 : 0;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) count += countViews(group.getChildAt(index), type);
        }
        return count;
    }

    private EditText findSearchField(View root) {
        if (root instanceof EditText && "Kameralarda ara".contentEquals(((EditText) root).getHint())) return (EditText) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) {
                EditText result = findSearchField(group.getChildAt(index));
                if (result != null) return result;
            }
        }
        return null;
    }

    private boolean findTextContaining(View root, String text) {
        if (root instanceof TextView && ((TextView) root).getText().toString().contains(text)) return true;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) if (findTextContaining(group.getChildAt(index), text)) return true;
        }
        return false;
    }

    private View findViewByDescription(View root, String description) {
        if (description.equals(root.getContentDescription())) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int index = 0; index < group.getChildCount(); index++) {
                View result = findViewByDescription(group.getChildAt(index), description);
                if (result != null) return result;
            }
        }
        return null;
    }

    private void clickDescription(final Activity activity, String description) {
        final View target = findViewByDescription(activity.getWindow().getDecorView(), description);
        assertNotNull("navigation target not found: " + description, target);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override public void run() { assertTrue("navigation item click was rejected", target.performClick()); }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
