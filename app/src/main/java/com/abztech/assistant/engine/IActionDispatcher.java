package com.abztech.assistant.engine;

/**
 * Interface defining all platform hardware and system capabilities
 * executed by the assistant.
 */
public interface IActionDispatcher {
    boolean setFlashlight(boolean enable);
    boolean toggleFlashlight();
    boolean isFlashlightOn();

    void adjustVolume(int direction);
    void setVolumePercent(int percent);
    void muteVolume();
    void setMaxVolume();

    int getBatteryPercentage();
    boolean isDeviceCharging();
    String getDeviceInfo();

    boolean launchApp(String appName);
    boolean setTimer(int seconds);
    boolean setAlarm(int hour, int minute, String message);
    void openSettings(String action);

    void dialNumber(String numberOrName);
    void sendSms(String recipient, String message);

    String saveNote(String text);
    String getFormattedNotes();
    String clearNotes();
}
