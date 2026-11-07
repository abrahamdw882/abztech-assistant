package com.abztech.assistant.engine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Main AI orchestrator. Parses queries, executes device actions, retrieves
 * offline knowledge, and crafts natural, human-like responses without robotic emojis.
 */
public class AssistantBrain {

    public static class Response {
        public final String spokenText;
        public final String displayText;
        public final String actionTag;
        public final IntentType intentType;

        public Response(String spokenText, String displayText, String actionTag, IntentType intentType) {
            this.spokenText = spokenText;
            this.displayText = displayText;
            this.actionTag = actionTag;
            this.intentType = intentType;
        }

        public Response(String spokenText, String displayText, String actionTag) {
            this(spokenText, displayText, actionTag, null);
        }

        public Response(String text) {
            this(text, text, null, null);
        }
    }

    private final IntentClassifier classifier;
    private final IActionDispatcher actionDispatcher;
    private final OfflineKnowledgeBase knowledgeBase;
    private final ContextMemory memory;
    private final Random random = new Random();

    public AssistantBrain(IActionDispatcher actionDispatcher) {
        this.actionDispatcher = actionDispatcher;
        this.knowledgeBase = new OfflineKnowledgeBase();
        this.classifier = new IntentClassifier(this.knowledgeBase);
        this.memory = new ContextMemory();
    }

    public AssistantBrain(IntentClassifier classifier, IActionDispatcher actionDispatcher, OfflineKnowledgeBase kb, ContextMemory memory) {
        this.classifier = classifier;
        this.actionDispatcher = actionDispatcher;
        this.knowledgeBase = kb;
        this.memory = memory;
    }

    public Response process(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return new Response("I'm right here. What's on your mind?", "I'm listening. What's on your mind?", null);
        }

        IntentResult intent = classifier.classify(userQuery, memory);

        switch (intent.getType()) {
            case WAKE_WORD:
                return handleWakeWord();

            case GREETING:
                return handleGreeting();

            case HOW_ARE_YOU:
                return handleHowAreYou();

            case WHATS_UP:
                return handleWhatsUp();

            case USER_STATUS:
                return handleUserStatus();

            case FAREWELL:
                return handleFarewell();

            case THANKS:
                return handleThanks();

            case IDENTITY:
                return new Response(
                        "I'm your personal offline assistant, built by AbzTech. I live directly on your phone and handle things without needing any internet connection.",
                        "I'm your personal offline assistant, built by AbzTech. I live directly on your phone and run 100% offline.",
                        "IDENTITY"
                );

            case CAPABILITIES:
                return new Response(
                        "Here is what I can do offline. I can control your flashlight, adjust volume, check battery status, open any app on your phone, calculate math and conversions, set timers and alarms, save voice notes, and answer world facts and trivia.",
                        "Here is what I can do offline:\n\n" +
                        "- Device Controls: Flashlight, Volume, Battery status\n" +
                        "- App Launcher: Open any installed app\n" +
                        "- Math & Conversions: Arithmetic, percentages, unit conversions\n" +
                        "- Timers & Alarms: Countdown timers and alarms\n" +
                        "- Offline Notes: Save and review local notes\n" +
                        "- Knowledge Base: World facts, science, history, geography\n" +
                        "- Conversation: Small talk, jokes, fun facts",
                        "CAPABILITIES"
                );

            case JOKE:
                String joke = knowledgeBase.getRandomJoke();
                return new Response(joke, joke, "JOKE");

            case FUN_FACT:
                String fact = knowledgeBase.getRandomFunFact();
                return new Response("Here is an interesting fact: " + fact, fact, "FACT");

            case EASTER_EGG:
                return new Response("Boots and cats and boots and cats and boots and cats! That's my best beatbox.");

            // HARDWARE CONTROLS
            case FLASHLIGHT_ON:
                if (actionDispatcher != null && actionDispatcher.setFlashlight(true)) {
                    return new Response("Flashlight turned on.", "[FLASHLIGHT: ON]", "TORCH_ON");
                }
                return new Response("I couldn't access the flashlight right now.", "[FLASHLIGHT ERROR]", "ERROR");

            case FLASHLIGHT_OFF:
                if (actionDispatcher != null && actionDispatcher.setFlashlight(false)) {
                    return new Response("Flashlight turned off.", "[FLASHLIGHT: OFF]", "TORCH_OFF");
                }
                return new Response("I couldn't turn off the flashlight.", "[FLASHLIGHT ERROR]", "ERROR");

            case FLASHLIGHT_TOGGLE:
                if (actionDispatcher != null) {
                    boolean nowOn = actionDispatcher.toggleFlashlight();
                    String state = nowOn ? "on" : "off";
                    return new Response("Flashlight is now " + state + ".", "[FLASHLIGHT: " + state.toUpperCase() + "]", "TORCH_TOGGLE");
                }
                return new Response("Unable to toggle flashlight.", "[FLASHLIGHT ERROR]", "ERROR");

            case VOLUME_UP:
                if (actionDispatcher != null) actionDispatcher.adjustVolume(1);
                return new Response("Volume turned up.", "[VOLUME: INCREASED]", "VOLUME");

            case VOLUME_DOWN:
                if (actionDispatcher != null) actionDispatcher.adjustVolume(-1);
                return new Response("Volume turned down.", "[VOLUME: DECREASED]", "VOLUME");

            case VOLUME_MUTE:
                if (actionDispatcher != null) actionDispatcher.muteVolume();
                return new Response("Volume muted.", "[VOLUME: MUTED]", "VOLUME");

            case VOLUME_MAX:
                if (actionDispatcher != null) actionDispatcher.setMaxVolume();
                return new Response("Volume set to maximum.", "[VOLUME: 100%]", "VOLUME");

            case VOLUME_SET:
                int targetVol = Integer.parseInt(intent.getSlotOrDefault("level", "50"));
                if (actionDispatcher != null) actionDispatcher.setVolumePercent(targetVol);
                return new Response("Volume set to " + targetVol + " percent.", "[VOLUME: " + targetVol + "%]", "VOLUME");

            case BATTERY_STATUS:
                if (actionDispatcher != null) {
                    int battery = actionDispatcher.getBatteryPercentage();
                    boolean charging = actionDispatcher.isDeviceCharging();
                    String chargingStr = charging ? ", and it's currently charging." : ". Not charging.";
                    if (battery >= 0) {
                        return new Response("Your battery is at " + battery + " percent" + chargingStr,
                                "[BATTERY: " + battery + "%" + (charging ? " - CHARGING]" : "]"), "BATTERY");
                    }
                }
                return new Response("I couldn't read the battery level right now.");

            case DEVICE_INFO:
                if (actionDispatcher != null) {
                    String info = actionDispatcher.getDeviceInfo();
                    return new Response("Here is your device information:\n" + info, info, "DEVICE_INFO");
                }
                return new Response("Device information is unavailable.");

            // TIME & DATE
            case TIME:
                String time = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
                return new Response("It's " + time + ".", "[TIME: " + time + "]", "TIME");

            case DATE:
                String date = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());
                return new Response("Today is " + date + ".", "[DATE: " + date + "]", "DATE");

            case DAY_OF_WEEK:
                String day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
                return new Response("Today is " + day + ".", "[DAY: " + day + "]", "DAY");

            // TIMER & ALARMS
            case SET_TIMER:
                int val = Integer.parseInt(intent.getSlotOrDefault("value", "1"));
                String unit = intent.getSlotOrDefault("unit", "minutes").toLowerCase(Locale.ROOT);
                int totalSeconds = val * (unit.startsWith("sec") ? 1 : unit.startsWith("hr") || unit.startsWith("hour") ? 3600 : 60);
                if (actionDispatcher != null && actionDispatcher.setTimer(totalSeconds)) {
                    return new Response("Timer set for " + val + " " + unit + ".", "[TIMER: " + val + " " + unit + "]", "TIMER");
                }
                return new Response("Setting timer for " + val + " " + unit + ".", "[TIMER: " + val + " " + unit + "]", "TIMER");

            case SET_ALARM:
                String alarmTime = intent.getSlot("time");
                return parseAndSetAlarm(alarmTime);

            // APP LAUNCHER
            case OPEN_APP:
                String appName = intent.getSlot("appName");
                if (actionDispatcher != null && actionDispatcher.launchApp(appName)) {
                    return new Response("Opening " + appName + " now.", "[LAUNCHING: " + appName + "]", "LAUNCH_APP");
                }
                return new Response("I couldn't find an installed app named " + appName + " on this device.",
                        "[NOT FOUND: \"" + appName + "\"]", "APP_NOT_FOUND");

            case CLOSE_APP:
                String closeTarget = intent.getSlot("appName");
                if (actionDispatcher != null) {
                    boolean found = actionDispatcher.closeApp(closeTarget);
                    if (found && closeTarget != null && !closeTarget.equalsIgnoreCase("current app")) {
                        return new Response("Closing " + closeTarget + ".", "[CLOSED: " + closeTarget + "]", "CLOSE_APP");
                    } else {
                        return new Response("Closing app and returning to home screen.", "[CLOSED]", "CLOSE_APP");
                    }
                }
                return new Response("Closing app.", "[CLOSED]", "CLOSE_APP");

            case GO_HOME:
                if (actionDispatcher != null) {
                    actionDispatcher.goToHomeScreen();
                }
                return new Response("Going to home screen.", "[HOME]", "GO_HOME");

            case SETTINGS_WIFI:
                if (actionDispatcher != null) actionDispatcher.openSettings("android.settings.WIFI_SETTINGS");
                return new Response("Opening Wi-Fi settings.", "[SETTINGS: WI-FI]", "SETTINGS");

            case SETTINGS_BLUETOOTH:
                if (actionDispatcher != null) actionDispatcher.openSettings("android.settings.BLUETOOTH_SETTINGS");
                return new Response("Opening Bluetooth settings.", "[SETTINGS: BLUETOOTH]", "SETTINGS");

            case SETTINGS_GENERAL:
                if (actionDispatcher != null) actionDispatcher.openSettings("android.settings.SETTINGS");
                return new Response("Opening Settings.", "[SETTINGS]", "SETTINGS");

            case OVERLAY_MODE:
                return new Response(
                        "Switching to display over screen mode.",
                        "[OVERLAY: ACTIVE]",
                        "OVERLAY",
                        IntentType.OVERLAY_MODE
                );

            // NOTES
            case TAKE_NOTE:
                String noteContent = intent.getSlot("content");
                if (actionDispatcher != null) {
                    actionDispatcher.saveNote(noteContent);
                    return new Response("Got it. Saved note: " + noteContent, "[NOTE SAVED: " + noteContent + "]", "NOTE_SAVED");
                }
                return new Response("Saved note: " + noteContent, "[NOTE SAVED: " + noteContent + "]", "NOTE_SAVED");

            case SHOW_NOTES:
                if (actionDispatcher != null) {
                    String formatted = actionDispatcher.getFormattedNotes();
                    return new Response(formatted, formatted, "NOTES_LIST");
                }
                return new Response("No notes available.");

            case CLEAR_NOTES:
                if (actionDispatcher != null) {
                    actionDispatcher.clearNotes();
                    return new Response("I've cleared all your notes.", "[NOTES CLEARED]", "NOTES_CLEARED");
                }
                return new Response("Cleared all notes.");

            // CALL & SMS
            case MAKE_CALL:
                String target = intent.getSlot("target");
                if (actionDispatcher != null) actionDispatcher.dialNumber(target);
                return new Response("Calling " + target + " now.", "[CALLING: " + target + "]", "CALL");

            case SEND_SMS:
                String recipient = intent.getSlot("recipient");
                String message = intent.getSlot("message");
                if (actionDispatcher != null) actionDispatcher.sendSms(recipient, message);
                return new Response("Preparing message for " + recipient + ".", "[SMS TO: " + recipient + "]\n" + message, "SMS");

            // MATH
            case MATH_CALCULATION:
                String mathRes = intent.getSlot("mathResult");
                try {
                    String cleanedNum = mathRes.replaceAll("[^0-9.-]", "");
                    if (!cleanedNum.isEmpty()) {
                        memory.setLastMathResult(Double.parseDouble(cleanedNum));
                    }
                } catch (Exception ignored) {}
                return new Response("The answer is " + mathRes + ".", "[RESULT: " + mathRes + "]", "MATH");

            // KNOWLEDGE
            case OFFLINE_KNOWLEDGE:
                String ans = intent.getSlot("answer");
                return new Response(ans, ans, "KNOWLEDGE");

            // UNKNOWN / FALLBACK
            case UNKNOWN:
            default:
                return new Response(
                        "I didn't quite catch that. You can ask me to open apps, turn on the flashlight, calculate math, set timers, or just chat.",
                        "I didn't catch that.\nTry saying:\n- Open YouTube\n- Turn on flashlight\n- What is 15% of 240?\n- Set timer for 5 minutes\n- How are you?",
                        "UNKNOWN"
                );
        }
    }

    private Response handleWakeWord() {
        String[] acknowledgements = new String[]{
                "I'm listening.",
                "Yes, I'm right here.",
                "Go ahead, I'm listening.",
                "Online. What do you need?",
                "Yes? What's on your mind?"
        };
        String pick = acknowledgements[random.nextInt(acknowledgements.length)];
        return new Response(pick, pick, "LISTENING", IntentType.WAKE_WORD);
    }

    private Response handleGreeting() {
        String[] greetings = new String[]{
                "Hey there! What's on your mind?",
                "Hello! Good to hear from you. What can I do for you today?",
                "Hey! I'm right here. How can I help?",
                "Hi there! What are we working on today?",
                "Hey! Hope you're having a great day. What's up?"
        };
        String pick = greetings[random.nextInt(greetings.length)];
        return new Response(pick, pick, "GREETING");
    }

    private Response handleHowAreYou() {
        String[] replies = new String[]{
                "I'm doing great, thanks for asking! How about yourself?",
                "All good on my end, feeling sharp and ready. How are you doing today?",
                "Pretty good! Just here waiting for you to give me something to do. How is your day going?",
                "Doing well! Always glad to hear from you. How are things with you?"
        };
        String pick = replies[random.nextInt(replies.length)];
        return new Response(pick, pick, "HOW_ARE_YOU");
    }

    private Response handleWhatsUp() {
        String[] replies = new String[]{
                "Not much, just hanging out on your phone ready to help. What's going on with you?",
                "Just staying ready for whatever you need. What's up with you?",
                "Not a whole lot, keeping things running smoothly. Anything interesting happening?"
        };
        String pick = replies[random.nextInt(replies.length)];
        return new Response(pick, pick, "WHATS_UP");
    }

    private Response handleUserStatus() {
        String[] replies = new String[]{
                "Glad to hear that! What are we getting into today?",
                "That's great! Let me know whenever you need a hand with anything.",
                "Awesome! Just say the word if you need anything opened, calculated, or set up."
        };
        String pick = replies[random.nextInt(replies.length)];
        return new Response(pick, pick, "USER_STATUS");
    }

    private Response handleFarewell() {
        String[] replies = new String[]{
                "Take care! I'll be right here whenever you need me.",
                "Catch you later! Call me anytime.",
                "Goodbye! Have a great one."
        };
        String pick = replies[random.nextInt(replies.length)];
        return new Response(pick, pick, "FAREWELL");
    }

    private Response handleThanks() {
        String[] replies = new String[]{
                "You're very welcome! Always happy to help.",
                "Anytime! Glad I could help out.",
                "No problem at all! Let me know if you need anything else."
        };
        String pick = replies[random.nextInt(replies.length)];
        return new Response(pick, pick, "THANKS");
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

            if (actionDispatcher != null && actionDispatcher.setAlarm(hour, min, "Nova Alarm")) {
                return new Response(String.format(Locale.getDefault(), "Alarm set for %02d:%02d.", hour, min),
                        String.format(Locale.getDefault(), "[ALARM: %02d:%02d]", hour, min), "ALARM");
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
