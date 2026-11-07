package com.abztech.assistant.engine;

public enum IntentType {
    // Conversational & Persona
    GREETING,
    WAKE_WORD,
    HOW_ARE_YOU,
    WHATS_UP,
    USER_STATUS,
    FAREWELL,
    IDENTITY,
    CAPABILITIES,
    JOKE,
    FUN_FACT,
    PHILOSOPHY,
    EASTER_EGG,
    THANKS,

    // Time & Date
    TIME,
    DATE,
    DAY_OF_WEEK,

    // Math & Tools
    MATH_CALCULATION,
    UNIT_CONVERSION,

    // Hardware Controls
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    FLASHLIGHT_TOGGLE,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    VOLUME_MAX,
    VOLUME_SET,
    BATTERY_STATUS,
    DEVICE_INFO,

    // App & Navigation
    OPEN_APP,
    CLOSE_APP,
    GO_HOME,
    SETTINGS_WIFI,
    SETTINGS_BLUETOOTH,
    SETTINGS_GENERAL,
    OVERLAY_MODE,

    // Scheduling & Alarms
    SET_TIMER,
    SET_ALARM,

    // Notes
    TAKE_NOTE,
    SHOW_NOTES,
    CLEAR_NOTES,

    // Telephony / Comms
    MAKE_CALL,
    SEND_SMS,

    // Local Encyclopedic Q&A
    OFFLINE_KNOWLEDGE,

    // Fallback
    UNKNOWN
}
