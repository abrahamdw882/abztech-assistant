package com.abztech.assistant.engine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Main AI orchestrator. Parses queries, executes device actions, retrieves
 * offline knowledge, and crafts Siri-styled responses.
 */
public class AssistantBrain {

    public static class Response {
        public final String spokenText;
        public final String displayText;
        public final String actionTag;

        public Response(String spokenText, String displayText, String actionTag) {
            this.spokenText = spokenText;
            this.displayText = displayText;
            this.actionTag = actionTag;
        }

        public Response(String text) {
            this(text, text, null);
        }
    }

    private final IntentClassifier classifier;
    private final IActionDispatcher actionDispatcher;
    private final OfflineKnowledgeBase knowledgeBase;
    private final ContextMemory memory;

    public AssistantBrain(IActionDispatcher actionDispatcher) {
        this.actionDispatcher = actionDispatcher;
        this.knowledgeBase = new OfflineKnowledgeBase();
        this.classifier = new IntentClassifier(this.knowledgeBase);
        this.memory = new ContextMemory();
    }

    // Constructor for testing without full Android action dispatcher
    public AssistantBrain(IntentClassifier classifier, IActionDispatcher actionDispatcher, OfflineKnowledgeBase kb, ContextMemory memory) {
        this.classifier = classifier;
        this.actionDispatcher = actionDispatcher;
        this.knowledgeBase = kb;
        this.memory = memory;
    }

    public Response process(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return new Response("I'm listening. How can I help you?", "I'm listening...", null);
        }

        IntentResult intent = classifier.classify(userQuery, memory);

        switch (intent.getType()) {
            case GREETING:
                return handleGreeting();

            case FAREWELL:
                return new Response("Goodbye! Call me whenever you need me.");

            case THANKS:
                return new Response("You're very welcome! Always happy to assist.");

            case IDENTITY:
                return new Response("I'm your offline personal AI assistant, created by AbzTech. I live directly on your phone and don't need an internet connection to help you.");

            case CAPABILITIES:
                return new Response(
                        "Here's what I can do offline:\n" +
                        "• Control flashlight, volume & check battery\n" +
                        "• Open apps & settings\n" +
                        "• Solve math expressions & unit conversions\n" +
                        "• Set alarms & timers\n" +
                        "• Save & read voice notes\n" +
                        "• Answer world facts, history, science & jokes\n" +
                        "• Place phone calls and send text messages",
                        "Here is what I can do offline:\n\n" +
                        "🔦 **Device Controls**: Flashlight, Volume, Battery status\n" +
                        "🚀 **App Launcher**: Open any installed app\n" +
                        "🧮 **Math & Units**: Complex math, percentages, unit conversions\n" +
                        "⏰ **Timers & Alarms**: Set countdowns & wake-up alarms\n" +
                        "📝 **Offline Notes**: Dictate and view local notes\n" +
                        "🌍 **Knowledge Base**: World capitals, science, history, geography\n" +
                        "😂 **Entertainment**: Jokes, fun facts, easter eggs",
                        "CAPABILITIES"
                );

            case JOKE:
                String joke = knowledgeBase.getRandomJoke();
                return new Response(joke, "😂 " + joke, "JOKE");

            case FUN_FACT:
                String fact = knowledgeBase.getRandomFunFact();
                return new Response("Did you know? " + fact, "💡 " + fact, "FACT");

            case EASTER_EGG:
                return new Response("Boots and cats and boots and cats and boots and cats! That's my best beatbox.");

            // HARDWARE
            case FLASHLIGHT_ON:
                if (actionDispatcher != null && actionDispatcher.setFlashlight(true)) {
                    return new Response("Flashlight turned on.", "🔦 Flashlight turned ON", "TORCH_ON");
                }
                return new Response("Sorry, I couldn't access your flashlight.", "Could not enable flashlight.", "ERROR");

            case FLASHLIGHT_OFF:
                if (actionDispatcher != null && actionDispatcher.setFlashlight(false)) {
                    return new Response("Flashlight turned off.", "🔦 Flashlight turned OFF", "TORCH_OFF");
                }
                return new Response("Sorry, I couldn't access your flashlight.", "Could not disable flashlight.", "ERROR");

            case FLASHLIGHT_TOGGLE:
                if (actionDispatcher != null) {
                    boolean nowOn = actionDispatcher.toggleFlashlight();
                    String state = nowOn ? "on" : "off";
                    return new Response("Flashlight is now " + state + ".", "🔦 Flashlight toggled " + state.toUpperCase(), "TORCH_TOGGLE");
                }
                return new Response("Unable to toggle flashlight.", "Flashlight toggle failed.", "ERROR");

            case VOLUME_UP:
                if (actionDispatcher != null) actionDispatcher.adjustVolume(1);
                return new Response("Volume increased.", "🔊 Volume increased", "VOLUME");

            case VOLUME_DOWN:
                if (actionDispatcher != null) actionDispatcher.adjustVolume(-1);
                return new Response("Volume decreased.", "🔉 Volume decreased", "VOLUME");

            case VOLUME_MUTE:
                if (actionDispatcher != null) actionDispatcher.muteVolume();
                return new Response("Volume muted.", "🔇 Volume muted", "VOLUME");

            case VOLUME_MAX:
                if (actionDispatcher != null) actionDispatcher.setMaxVolume();
                return new Response("Volume set to maximum.", "🔊 Volume set to 100%", "VOLUME");

            case VOLUME_SET:
                int targetVol = Integer.parseInt(intent.getSlotOrDefault("level", "50"));
                if (actionDispatcher != null) actionDispatcher.setVolumePercent(targetVol);
                return new Response("Volume set to " + targetVol + " percent.", "🔊 Volume set to " + targetVol + "%", "VOLUME");

            case BATTERY_STATUS:
                if (actionDispatcher != null) {
                    int battery = actionDispatcher.getBatteryPercentage();
                    boolean charging = actionDispatcher.isDeviceCharging();
                    String chargingStr = charging ? " and it is currently charging." : " and it is not charging.";
                    if (battery >= 0) {
                        return new Response("Your battery is at " + battery + " percent" + chargingStr,
                                "🔋 Battery: " + battery + "%" + (charging ? " ⚡ (Charging)" : ""), "BATTERY");
                    }
                }
                return new Response("I couldn't read the battery level right now.");

            case DEVICE_INFO:
                if (actionDispatcher != null) {
                    String info = actionDispatcher.getDeviceInfo();
                    return new Response("Here is your device information:\n" + info, "📱 " + info, "DEVICE_INFO");
                }
                return new Response("Device info is unavailable.");

            // TIME & DATE
            case TIME:
                String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                return new Response("It's " + time + ".", "⏰ Current time: " + time, "TIME");

            case DATE:
                String date = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());
                return new Response("Today is " + date + ".", "📅 " + date, "DATE");

            case DAY_OF_WEEK:
                String day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
                return new Response("Today is " + day + ".", "📅 Today is " + day, "DAY");

            // TIMER & ALARMS
            case SET_TIMER:
                int val = Integer.parseInt(intent.getSlotOrDefault("value", "1"));
                String unit = intent.getSlotOrDefault("unit", "minutes").toLowerCase(Locale.ROOT);
                int totalSeconds = val * (unit.startsWith("sec") ? 1 : unit.startsWith("hr") || unit.startsWith("hour") ? 3600 : 60);
                if (actionDispatcher != null && actionDispatcher.setTimer(totalSeconds)) {
                    return new Response("Timer set for " + val + " " + unit + ".", "⏳ Timer set: " + val + " " + unit, "TIMER");
                }
                return new Response("Setting timer for " + val + " " + unit + ".", "⏳ Set timer: " + val + " " + unit, "TIMER");

            case SET_ALARM:
                String alarmTime = intent.getSlot("time");
                return parseAndSetAlarm(alarmTime);

            // APP LAUNCHER
            case OPEN_APP:
                String appName = intent.getSlot("appName");
                if (actionDispatcher != null && actionDispatcher.launchApp(appName)) {
                    return new Response("Opening " + appName + "...", "🚀 Opening " + appName, "LAUNCH_APP");
                }
                return new Response("I couldn't find an installed app named " + appName + " on this device.",
                        "⚠️ App not found: \"" + appName + "\"", "APP_NOT_FOUND");

            case SETTINGS_WIFI:
                if (actionDispatcher != null) actionDispatcher.openSettings("android.settings.WIFI_SETTINGS");
                return new Response("Opening Wi-Fi settings...", "📶 Opening Wi-Fi Settings", "SETTINGS");

            case SETTINGS_BLUETOOTH:
                if (actionDispatcher != null) actionDispatcher.openSettings("android.settings.BLUETOOTH_SETTINGS");
                return new Response("Opening Bluetooth settings...", "📶 Opening Bluetooth Settings", "SETTINGS");

            case SETTINGS_GENERAL:
                if (actionDispatcher != null) actionDispatcher.openSettings("android.settings.SETTINGS");
                return new Response("Opening Settings...", "⚙️ Opening Settings", "SETTINGS");

            // NOTES
            case TAKE_NOTE:
                String noteContent = intent.getSlot("content");
                if (actionDispatcher != null) {
                    String noteRes = actionDispatcher.saveNote(noteContent);
                    return new Response(noteRes, "📝 " + noteRes, "NOTE_SAVED");
                }
                return new Response("Saved note: " + noteContent, "📝 " + noteContent, "NOTE_SAVED");

            case SHOW_NOTES:
                if (actionDispatcher != null) {
                    String formatted = actionDispatcher.getFormattedNotes();
                    return new Response(formatted, formatted, "NOTES_LIST");
                }
                return new Response("No notes available.");

            case CLEAR_NOTES:
                if (actionDispatcher != null) {
                    String cleared = actionDispatcher.clearNotes();
                    return new Response(cleared, "🗑️ " + cleared, "NOTES_CLEARED");
                }
                return new Response("Cleared all notes.");

            // CALL & SMS
            case MAKE_CALL:
                String target = intent.getSlot("target");
                if (actionDispatcher != null) actionDispatcher.dialNumber(target);
                return new Response("Calling " + target + "...", "📞 Dialing " + target, "CALL");

            case SEND_SMS:
                String recipient = intent.getSlot("recipient");
                String message = intent.getSlot("message");
                if (actionDispatcher != null) actionDispatcher.sendSms(recipient, message);
                return new Response("Preparing message for " + recipient + "...", "✉️ SMS to " + recipient + ":\n" + message, "SMS");

            // MATH
            case MATH_CALCULATION:
                String mathRes = intent.getSlot("mathResult");
                try {
                    // Try to save numerical value to context memory for multi-step math
                    String cleanedNum = mathRes.replaceAll("[^0-9.-]", "");
                    if (!cleanedNum.isEmpty()) {
                        memory.setLastMathResult(Double.parseDouble(cleanedNum));
                    }
                } catch (Exception ignored) {}
                return new Response("The answer is " + mathRes + ".", "🧮 Result: " + mathRes, "MATH");

            // KNOWLEDGE
            case OFFLINE_KNOWLEDGE:
                String ans = intent.getSlot("answer");
                return new Response(ans, "💡 " + ans, "KNOWLEDGE");

            // UNKNOWN / FALLBACK
            case UNKNOWN:
            default:
                return new Response(
                        "I'm not quite sure about that offline. You can ask me to open apps, turn on the flashlight, solve math, set timers, take notes, or ask for facts.",
                        "🤔 I didn't catch that.\nTry saying:\n• \"Turn on flashlight\"\n• \"What is 15% of 240?\"\n• \"Set timer for 5 minutes\"\n• \"Open WhatsApp\"\n• \"Tell me a joke\"",
                        "UNKNOWN"
                );
        }
    }

    private Response handleGreeting() {
        int hour = Integer.parseInt(new SimpleDateFormat("H", Locale.getDefault()).format(new Date()));
        String greeting;
        if (hour < 12) greeting = "Good morning!";
        else if (hour < 18) greeting = "Good afternoon!";
        else greeting = "Good evening!";
        return new Response(greeting + " How can I help you?", greeting + " What can I do for you?", "GREETING");
    }

    private Response parseAndSetAlarm(String alarmTime) {
        if (alarmTime == null) return new Response("What time should I set the alarm for?");
        try {
            int hour = 7, min = 0;
            String lower = alarmTime.toLowerCase(Locale.ROOT);
            boolean isPm = lower.contains("pm");
            boolean isAm = lower.contains("am");
            String cleaned = lower.replaceAll("[^0-9:]", "").trim();

            if (cleaned.contains(":")) {
                String[] parts = cleaned.split(":");
                hour = Integer.parseInt(parts[0]);
                min = Integer.parseInt(parts[1]);
            } else if (!cleaned.isEmpty()) {
                hour = Integer.parseInt(cleaned);
            }

            if (isPm && hour < 12) hour += 12;
            if (isAm && hour == 12) hour = 0;

            if (actionDispatcher != null && actionDispatcher.setAlarm(hour, min, "Siri Alarm")) {
                return new Response(String.format(Locale.getDefault(), "Alarm set for %02d:%02d.", hour, min),
                        String.format(Locale.getDefault(), "⏰ Alarm set for %02d:%02d", hour, min), "ALARM");
            }
            return new Response("Setting alarm for " + alarmTime + ".");
        } catch (Exception e) {
            return new Response("Please specify the alarm time, for example: 'Set alarm for 7:30 AM'.");
        }
    }

    public ContextMemory getMemory() {
        return memory;
    }
}
