package local.camerawall;

public final class CameraListActivity extends BaseSectionActivity {
    @Override String sectionTitle() { return "Kameralar"; }
    @Override BottomNavigationBar.Destination destination() { return BottomNavigationBar.Destination.CAMERAS; }
}
