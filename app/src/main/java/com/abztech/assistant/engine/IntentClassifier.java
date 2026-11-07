package com.abztech.assistant.engine;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-speed, deterministic Intent and Entity Extractor.
 * Operates offline in < 1ms without requiring heavy neural networks.
 */
public class IntentClassifier {

    private final OfflineKnowledgeBase knowledgeBase;

    public IntentClassifier(OfflineKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public IntentResult classify(String rawText, ContextMemory memory) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return new IntentResult(IntentType.UNKNOWN, 0.0f, "");
        }

        String text = rawText.trim().toLowerCase(Locale.ROOT);
        text = text.replaceAll("[?!.,]+$", "").trim();

        // Normalize Siri / Nova / Jarvis hotwords or prefixes (e.g. "hello nova", "hey jarvis", "jarvis", "wake up")
        text = text.replaceAll("^(?:hey\\s+|ok\\s+|hello\\s+|hi\\s+)?(?:siri|nova|jarvis|assistant|computer)\\s*,?\\s*", "").trim();
        text = text.replaceAll("^(?:wake\\s+up|listen\\s+up|are\\s+you\\s+there)\\s*,?\\s*", "").trim();
        text = text.replaceAll("[?!.,]+$", "").trim();
        if (text.isEmpty() || matchesAny(text, "^(jarvis|nova|siri)$")) {
            return new IntentResult(IntentType.GREETING, 1.0f, rawText);
        }

        // 1. GREETINGS & SMALL TALK
        if (matchesAny(text, "^(hi|hello|hey|good\\s+(morning|afternoon|evening|day)|greetings)$")) {
            return new IntentResult(IntentType.GREETING, 1.0f, rawText);
        }
        if (matchesAny(text, "^(bye|goodbye|see\\s+ya|exit|quit|close)$")) {
            return new IntentResult(IntentType.FAREWELL, 1.0f, rawText);
        }
        if (matchesAny(text, "^(thanks|thank\\s+you|appreciate\\s+it)$")) {
            return new IntentResult(IntentType.THANKS, 1.0f, rawText);
        }
        if (matchesAny(text, "^(who\\s+are\\s+you|what\\s+is\\s+your\\s+name|who\\s+made\\s+you|what\\s+are\\s+you)$")) {
            return new IntentResult(IntentType.IDENTITY, 1.0f, rawText);
        }
        if (matchesAny(text, "^(what\\s+can\\s+you\\s+do|help|features|commands|capabilities)$")) {
            return new IntentResult(IntentType.CAPABILITIES, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(tell\\s+(me\\s+)?a\\s+joke|make\\s+me\\s+laugh|joke).*")) {
            return new IntentResult(IntentType.JOKE, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(fun\\s+fact|tell\\s+(me\\s+)?a\\s+fact|give\\s+me\\s+a\\s+fact).*")) {
            return new IntentResult(IntentType.FUN_FACT, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(beatbox|sing|rap).*")) {
            return new IntentResult(IntentType.EASTER_EGG, 1.0f, rawText);
        }

        // 2. HARDWARE CONTROLS - FLASHLIGHT
        if (matchesAny(text, ".*(turn\\s+on\\s+(the\\s+)?(flashlight|torch)|torch\\s+on|flashlight\\s+on|enable\\s+(flashlight|torch)).*")) {
            return new IntentResult(IntentType.FLASHLIGHT_ON, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(turn\\s+off\\s+(the\\s+)?(flashlight|torch)|torch\\s+off|flashlight\\s+off|disable\\s+(flashlight|torch)).*")) {
            return new IntentResult(IntentType.FLASHLIGHT_OFF, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(toggle\\s+(the\\s+)?(flashlight|torch)|flash\\s*light|torch).*") && text.contains("toggle")) {
            return new IntentResult(IntentType.FLASHLIGHT_TOGGLE, 1.0f, rawText);
        }

        // 3. HARDWARE CONTROLS - VOLUME
        Pattern volSetPattern = Pattern.compile("(?:set\\s+volume\\s+(?:to\\s+)?|volume\\s+to\\s+)([0-9]{1,3})%?");
        Matcher volSetMatcher = volSetPattern.matcher(text);
        if (volSetMatcher.find()) {
            return new IntentResult(IntentType.VOLUME_SET, 1.0f, rawText)
                    .putSlot("level", volSetMatcher.group(1));
        }
        if (matchesAny(text, ".*(volume\\s+up|increase\\s+volume|louder|turn\\s+it\\s+up).*")) {
            return new IntentResult(IntentType.VOLUME_UP, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(volume\\s+down|decrease\\s+volume|lower\\s+volume|quieter|turn\\s+it\\s+down).*")) {
            return new IntentResult(IntentType.VOLUME_DOWN, 1.0f, rawText);
        }
        if (matchesAny(text, "^(mute|silence|mute\\s+volume|mute\\s+sound)$")) {
            return new IntentResult(IntentType.VOLUME_MUTE, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(max\\s+volume|maximum\\s+volume|full\\s+volume).*")) {
            return new IntentResult(IntentType.VOLUME_MAX, 1.0f, rawText);
        }

        // 4. BATTERY & DEVICE
        if (matchesAny(text, ".*(battery\\s+level|battery\\s+status|how\\s+much\\s+battery|battery\\s+percentage|check\\s+battery|is\\s+it\\s+charging).*")) {
            return new IntentResult(IntentType.BATTERY_STATUS, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(device\\s+info|phone\\s+info|system\\s+info|what\\s+phone\\s+is\\s+this|android\\s+version|specifications).*")) {
            return new IntentResult(IntentType.DEVICE_INFO, 1.0f, rawText);
        }

        // 5. TIME & DATE
        if (matchesAny(text, ".*(what\\s+time\\s+is\\s+it|tell\\s+(me\\s+)?the\\s+time|current\\s+time|what's\\s+the\\s+time).*")) {
            return new IntentResult(IntentType.TIME, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(what\\s+day\\s+is\\s+it|what\\s+day\\s+is\\s+today|day\\s+of\\s+the\\s+week).*")) {
            return new IntentResult(IntentType.DAY_OF_WEEK, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(what\\s+is\\s+today's\\s+date|what\\s+date\\s+is\\s+it|current\\s+date|what's\\s+the\\s+date).*")) {
            return new IntentResult(IntentType.DATE, 1.0f, rawText);
        }

        // 6. TIMER & ALARM
        Pattern timerPattern = Pattern.compile("(?:set\\s+(?:a\\s+)?timer\\s+for\\s+)([0-9]+)\\s*(seconds?|sec|minutes?|min|hours?|hr)?");
        Matcher timerMatcher = timerPattern.matcher(text);
        if (timerMatcher.find()) {
            String value = timerMatcher.group(1);
            String unit = timerMatcher.group(2) != null ? timerMatcher.group(2) : "minutes";
            return new IntentResult(IntentType.SET_TIMER, 1.0f, rawText)
                    .putSlot("value", value)
                    .putSlot("unit", unit);
        }

        Pattern alarmPattern = Pattern.compile("(?:set\\s+(?:an?\\s+)?alarm\\s+(?:for\\s+|at\\s+))([0-9]{1,2}(?::[0-9]{2})?\\s*(?:am|pm)?)");
        Matcher alarmMatcher = alarmPattern.matcher(text);
        if (alarmMatcher.find()) {
            return new IntentResult(IntentType.SET_ALARM, 1.0f, rawText)
                    .putSlot("time", alarmMatcher.group(1).trim());
        }

        // 7. NOTES (Offline Local SQLite)
        Pattern takeNotePattern = Pattern.compile("^(?:take\\s+(?:a\\s+)?note|create\\s+(?:a\\s+)?note|write\\s+(?:a\\s+)?note|save\\s+(?:a\\s+)?note|remember\\s+that|note)\\s*[:-]?\\s*(.*)$");
        Matcher takeNoteMatcher = takeNotePattern.matcher(text);
        if (takeNoteMatcher.find()) {
            String noteContent = takeNoteMatcher.group(1).trim();
            if (!noteContent.isEmpty()) {
                return new IntentResult(IntentType.TAKE_NOTE, 1.0f, rawText)
                        .putSlot("content", noteContent);
            }
        }
        if (matchesAny(text, ".*(show\\s+(my\\s+)?notes|read\\s+(my\\s+)?notes|list\\s+(my\\s+)?notes|what\\s+are\\s+my\\s+notes).*")) {
            return new IntentResult(IntentType.SHOW_NOTES, 1.0f, rawText);
        }
        if (matchesAny(text, ".*(clear\\s+(all\\s+)?notes|delete\\s+all\\s+notes).*")) {
            return new IntentResult(IntentType.CLEAR_NOTES, 1.0f, rawText);
        }

        // 8. APP LAUNCHER
        Pattern openAppPattern = Pattern.compile("^(?:please\\s+|can\\s+you\\s+|could\\s+you\\s+)?(?:open\\s+up|open|launch|start|run|play)\\s+(?:the\\s+app\\s+|the\\s+application\\s+|app\\s+)?([a-zA-Z0-9 ._'-]+)$");
        Matcher openAppMatcher = openAppPattern.matcher(text);
        if (openAppMatcher.find()) {
            String targetAppLower = openAppMatcher.group(1).trim();
            if (targetAppLower.equalsIgnoreCase("wifi") || targetAppLower.contains("wifi settings")) {
                return new IntentResult(IntentType.SETTINGS_WIFI, 1.0f, rawText);
            }
            if (targetAppLower.equalsIgnoreCase("bluetooth") || targetAppLower.contains("bluetooth settings")) {
                return new IntentResult(IntentType.SETTINGS_BLUETOOTH, 1.0f, rawText);
            }
            if (targetAppLower.equalsIgnoreCase("settings")) {
                return new IntentResult(IntentType.SETTINGS_GENERAL, 1.0f, rawText);
            }

            // Extract original casing from rawText if possible
            Pattern rawAppPattern = Pattern.compile("(?i)^(?:hey\\s+|ok\\s+|hello\\s+|hi\\s+)?(?:siri|nova|jarvis|assistant)?\\s*,?\\s*(?:please\\s+|can\\s+you\\s+|could\\s+you\\s+)?(?:open\\s+up|open|launch|start|run|play)\\s+(?:the\\s+app\\s+|the\\s+application\\s+|app\\s+)?([a-zA-Z0-9 ._'-]+)[?!.,]*$");
            Matcher rawAppMatcher = rawAppPattern.matcher(rawText.trim());
            String targetApp = (rawAppMatcher.find()) ? rawAppMatcher.group(1).trim() : targetAppLower;

            return new IntentResult(IntentType.OPEN_APP, 0.95f, rawText)
                    .putSlot("appName", targetApp);
        }

        // 9. TELEPHONY & MESSAGING
        Pattern callPattern = Pattern.compile("^(?:call|dial|phone)\\s+([a-zA-Z0-9 +]+)$");
        Matcher callMatcher = callPattern.matcher(text);
        if (callMatcher.find()) {
            return new IntentResult(IntentType.MAKE_CALL, 0.95f, rawText)
                    .putSlot("target", callMatcher.group(1).trim());
        }

        Pattern smsPattern = Pattern.compile("^(?:send\\s+(?:an?\\s+)?(?:sms|message|text)\\s+to\\s+)([a-zA-Z0-9 +]+)(?:\\s+saying\\s+(.*))?$");
        Matcher smsMatcher = smsPattern.matcher(text);
        if (smsMatcher.find()) {
            String recipient = smsMatcher.group(1).trim();
            String msg = smsMatcher.group(2) != null ? smsMatcher.group(2).trim() : "";
            return new IntentResult(IntentType.SEND_SMS, 0.95f, rawText)
                    .putSlot("recipient", recipient)
                    .putSlot("message", msg);
        }

        // 10. MATH & UNIT CONVERSIONS
        Double previousAns = (memory != null) ? memory.getLastMathResult() : null;
        String mathResult = MathEvaluator.evaluate(text, previousAns);
        if (mathResult != null) {
            return new IntentResult(IntentType.MATH_CALCULATION, 0.98f, rawText)
                    .putSlot("mathResult", mathResult);
        }

        // 11. OFFLINE ENCYCLOPEDIC KNOWLEDGE
        String knowledgeAnswer = knowledgeBase.findAnswer(text);
        if (knowledgeAnswer != null) {
            return new IntentResult(IntentType.OFFLINE_KNOWLEDGE, 0.95f, rawText)
                    .putSlot("answer", knowledgeAnswer);
        }

        // 12. FALLBACK WITH FUZZY APPS OR UNKNOWN
        return new IntentResult(IntentType.UNKNOWN, 0.0f, rawText);
    }

    private boolean matchesAny(String text, String... patterns) {
        for (String p : patterns) {
            if (text.matches(p)) return true;
        }
        return false;
    }
}
