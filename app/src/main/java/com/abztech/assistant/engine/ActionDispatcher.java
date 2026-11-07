package com.abztech.assistant.engine;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.AlarmClock;
import android.provider.Settings;

import com.abztech.assistant.data.Note;
import com.abztech.assistant.data.NotesDatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Executes on-device actions such as toggling hardware, launching apps,
 * scheduling alarms/timers, and reading local sensors/status.
 */
public class ActionDispatcher implements IActionDispatcher {

    private final Context context;
    private final NotesDatabaseHelper notesDb;
    private static boolean isFlashlightOn = false;

    public ActionDispatcher(Context context, NotesDatabaseHelper notesDb) {
        this.context = context.getApplicationContext();
        this.notesDb = notesDb;
    }

    public boolean setFlashlight(boolean enable) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) return false;

        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (hasFlash != null && hasFlash && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraManager.setTorchMode(id, enable);
                    isFlashlightOn = enable;
                    return true;
                }
            }
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean toggleFlashlight() {
        return setFlashlight(!isFlashlightOn);
    }

    public boolean isFlashlightOn() {
        return isFlashlightOn;
    }

    public void adjustVolume(int direction) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
        }
    }

    public void setVolumePercent(int percent) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int target = Math.max(0, Math.min(max, (int) Math.round((percent / 100.0) * max)));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI);
        }
    }

    public void muteVolume() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
        }
    }

    public void setMaxVolume() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI);
        }
    }

    public int getBatteryPercentage() {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return -1;
    }

    public boolean isDeviceCharging() {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            int status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        }
        return false;
    }

    public String getDeviceInfo() {
        long freeBytes = new StatFs(Environment.getDataDirectory().getPath()).getAvailableBytes();
        long freeGb = freeBytes / (1024 * 1024 * 1024);
        return String.format(Locale.getDefault(),
                "Device: %s %s\nAndroid Version: %s (API %d)\nFree Internal Storage: ~%d GB",
                Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, freeGb);
    }

    public boolean launchApp(String targetAppName) {
    private String findPackageForApp(String appName) {
        if (appName == null || appName.trim().isEmpty()) return null;
        PackageManager pm = context.getPackageManager();
        String target = appName.toLowerCase(Locale.ROOT).trim();
        target = target.replaceAll("^(the\\s+app\\s+|the\\s+application\\s+|app\\s+)", "").trim();

        // Common speech-to-text aliases
        if (target.equals("yt") || target.contains("you tube")) target = "youtube";
        if (target.equals("wa") || target.contains("what's app") || target.contains("whats app")) target = "whatsapp";
        if (target.equals("cam")) target = "camera";
        if (target.equals("calc")) target = "calculator";
        if (target.equals("browser") || target.equals("internet")) target = "chrome";
        if (target.equals("photos")) target = "gallery";
        if (target.equals("music")) target = "spotify";

        // 1. Check real user launcher activities first (guarantees launchable apps)
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchables = pm.queryIntentActivities(mainIntent, 0);

        ResolveInfo bestMatch = null;
        float highestScore = 0f;

        for (ResolveInfo info : launchables) {
            CharSequence labelSeq = info.loadLabel(pm);
            if (labelSeq == null) continue;
            String label = labelSeq.toString().toLowerCase(Locale.ROOT);
            String pkg = info.activityInfo.packageName.toLowerCase(Locale.ROOT);

            // Exact match
            if (label.equals(target) || pkg.endsWith("." + target) || pkg.equals(target)) {
                return info.activityInfo.packageName;
            }

            // Substring or fuzzy matching
            if (label.contains(target) || target.contains(label)) {
                bestMatch = info;
                highestScore = 0.9f;
            } else {
                float sim = FuzzyMatcher.similarity(label, target);
                if (sim > highestScore && sim > 0.60f) {
                    highestScore = sim;
                    bestMatch = info;
                }
            }
        }

        if (bestMatch != null && bestMatch.activityInfo != null) {
            return bestMatch.activityInfo.packageName;
        }

        // 2. Fallback: Search all installed applications
        try {
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo app : apps) {
                CharSequence labelSeq = pm.getApplicationLabel(app);
                if (labelSeq == null) continue;
                String label = labelSeq.toString().toLowerCase(Locale.ROOT);
                if (label.contains(target) || target.contains(label) || FuzzyMatcher.similarity(label, target) > 0.65f) {
                    return app.packageName;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    @Override
    public boolean launchApp(String targetAppName) {
        String pkg = findPackageForApp(targetAppName);
        if (pkg != null) {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean closeApp(String appName) {
        if (appName == null || appName.trim().isEmpty() || "this app".equalsIgnoreCase(appName.trim()) || "the app".equalsIgnoreCase(appName.trim()) || "current app".equalsIgnoreCase(appName.trim())) {
            goToHomeScreen();
            return true;
        }

        String pkg = findPackageForApp(appName);
        if (pkg != null) {
            try {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    am.killBackgroundProcesses(pkg);
                }
            } catch (Exception ignored) {}
            goToHomeScreen();
            return true;
        }

        goToHomeScreen();
        return false;
    }

    @Override
    public void goToHomeScreen() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(homeIntent);
        } catch (Exception ignored) {}
    }

    public boolean setTimer(int seconds) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
        intent.putExtra(AlarmClock.EXTRA_LENGTH, seconds);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, "Nova Siri Timer");
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    public boolean setAlarm(int hour, int minute, String message) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, message != null ? message : "Siri Alarm");
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    public void openSettings(String action) {
        Intent intent = new Intent(action != null ? action : Settings.ACTION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void dialNumber(String numberOrName) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + Uri.encode(numberOrName)));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void sendSms(String recipient, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + Uri.encode(recipient)));
        intent.putExtra("sms_body", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public String saveNote(String text) {
        if (notesDb != null) {
            long id = notesDb.addNote(text);
            return id > 0 ? "Saved note: \"" + text + "\"" : "Could not save note.";
        }
        return "Notes storage unavailable.";
    }

    public String getFormattedNotes() {
        if (notesDb == null) return "Notes storage is not initialized.";
        List<Note> notes = notesDb.getAllNotes();
        if (notes.isEmpty()) {
            return "You don't have any saved notes yet.";
        }

        StringBuilder sb = new StringBuilder("Here are your notes:\n");
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
        int count = 1;
        for (Note n : notes) {
            sb.append(count++).append(". ")
                    .append(n.getContent())
                    .append(" (").append(sdf.format(new Date(n.getTimestamp()))).append(")\n");
            if (count > 5) break; // Limit top 5 in voice readout
        }
        return sb.toString().trim();
    }

    public String clearNotes() {
        if (notesDb != null) {
            notesDb.clearAllNotes();
            return "I've cleared all your notes.";
        }
        return "Could not clear notes.";
    }
}
