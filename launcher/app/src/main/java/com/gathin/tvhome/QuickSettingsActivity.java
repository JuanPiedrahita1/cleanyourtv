package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The panel the remote's Settings button opens when {@code settings_key_panel} is on (HomeKeyService), laid out
 * like the tvOS control center: power, network, picture, Bluetooth and screensaver tiles over Home or whatever app
 * is playing. Translucent and in a task of its own, so the app behind keeps running and Back returns to it.
 * Pressing Settings again closes the panel.
 */
public final class QuickSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }


    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_quick_settings);
        if (getResources().getBoolean(R.bool.rtl_menu_bar)) {
            View panel = findViewById(R.id.panel);
            android.widget.FrameLayout.LayoutParams position =
                    (android.widget.FrameLayout.LayoutParams) panel.getLayoutParams();
            position.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;
            int inset = position.getMarginEnd();
            position.setMarginEnd(0);
            position.setMargins(inset, position.topMargin, 0, position.bottomMargin);
            panel.setLayoutParams(position);
        }
        findViewById(R.id.scrim).setOnClickListener(v -> finish());
        // The system power menu (Power off, Restart) through the Home key helper; it is the only one allowed to open it.
        bind(R.id.ccPower, () -> {
            if (HomeKeyService.canOpenPowerMenu()) HomeKeyService.openPowerMenu();
            else Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        });
        bind(R.id.ccWifi, SystemSettings.intent(this, Settings.ACTION_WIFI_SETTINGS));
        bind(R.id.ccPicture, SystemSettings.intent(this, Settings.ACTION_DISPLAY_SETTINGS));
        bind(R.id.ccBluetooth, SystemSettings.intent(this, Settings.ACTION_BLUETOOTH_SETTINGS));
        // Sleep timer: each press steps 15, 30, 60, 90 minutes and off; the panel stays open to keep stepping.
        TextView sleepText = findViewById(R.id.ccSleepText);
        showSleep(sleepText, SleepTimer.minutesLeft(this));
        findViewById(R.id.ccSleep).setOnClickListener(v -> {
            int minutes = SleepTimer.nextStep(SleepTimer.minutesLeft(this));
            SleepTimer.set(this, minutes);
            showSleep(sleepText, minutes);
        });
        bind(R.id.ccScreensaver, () -> RemoteKeys.runAction(this, "screensaver"));
        bind(R.id.ccSettings, SystemSettings.intent(this, Settings.ACTION_SETTINGS));
        bind(R.id.ccApps, SystemSettings.intent(this, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        bind(R.id.ccMute, () -> RemoteKeys.runAction(this, "mute"));
        // Through Home, so it rebuilds with the new choices when Launcher settings closes.
        bind(R.id.ccLauncher, new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS));
        findViewById(R.id.ccPower).requestFocus();
    }

    /** Settings pressed again while the panel is up. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    /** Anything else taking the screen (Home, an app, a screensaver) closes the panel. */
    @Override
    protected void onPause() {
        super.onPause();
        finish();
        overridePendingTransition(0, 0);
    }

    private void showSleep(TextView label, int minutes) {
        if (minutes > 0) label.setText(getString(R.string.cc_sleep_on, minutes));
        else label.setText(R.string.cc_sleep);
    }

    private void bind(int id, Intent target) {
        bind(id, () -> {
            try {
                startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bind(int id, Runnable action) {
        findViewById(id).setOnClickListener(v -> {
            action.run();
            finish();
        });
    }
}
