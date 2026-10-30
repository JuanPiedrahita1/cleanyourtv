package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The control center's sleep timer: each press steps through {@link #STEPS} minutes and back to off, and when
 * the time is up the Home key helper puts the TV in standby. The end time lives in preferences so the tile can
 * show what is left; the alarm survives the launcher's process being killed in between.
 */
final class SleepTimer {
    private static final String PREFS = "sleep_timer";
    private static final String KEY_END = "end";
    /** Minutes a press steps through; 0 is off. */
    static final int[] STEPS = {0, 15, 30, 60, 90};

    private SleepTimer() { }

    /** Minutes left, rounded up, or 0 when no timer is set. */
    static int minutesLeft(Context ctx) {
        long left = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_END, 0) - System.currentTimeMillis();
        return left > 0 ? (int) ((left + 59_999) / 60_000) : 0;
    }

    /** The next step after what is left now: off goes to 15, 15 to 30, the longest back to off. */
    static int nextStep(int minutesLeft) {
        for (int step : STEPS) if (step > minutesLeft) return step;
        return 0;
    }

    /** Sets the timer to this many minutes from now, or cancels it with 0. */
    static void set(Context ctx, int minutes) {
        AlarmManager alarms = ctx.getSystemService(AlarmManager.class);
        PendingIntent fire = PendingIntent.getBroadcast(ctx, 0, new Intent(ctx, Receiver.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarms != null) alarms.cancel(fire);
        if (minutes <= 0) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_END).apply();
            return;
        }
        long end = System.currentTimeMillis() + minutes * 60_000L;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_END, end).apply();
        // Inexact is fine for a sleep timer, and needs no exact-alarm permission.
        if (alarms != null) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, fire);
    }

    /** Time is up: clear the timer and put the TV in standby. */
    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_END).apply();
            HomeKeyService.sleepNow();
        }
    }
}
