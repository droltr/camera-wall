package local.camerawall;

public final class SettingsActivity extends BaseSectionActivity {
    @Override String sectionTitle() { return "Ayarlar"; }
    @Override BottomNavigationBar.Destination destination() { return BottomNavigationBar.Destination.SETTINGS; }
}
