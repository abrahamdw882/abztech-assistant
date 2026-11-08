package com.abztech.assistant;

import com.abztech.assistant.engine.*;

public class StandaloneBrainTest {

    private static class MockActionDispatcher implements IActionDispatcher {
        boolean torch = false;
        int volume = 50;
        int battery = 85;

        @Override public boolean setFlashlight(boolean enable) { this.torch = enable; return true; }
        @Override public boolean toggleFlashlight() { this.torch = !this.torch; return this.torch; }
        @Override public boolean isFlashlightOn() { return this.torch; }
        @Override public void adjustVolume(int direction) { this.volume += direction * 5; }
        @Override public void setVolumePercent(int percent) { this.volume = percent; }
        @Override public void muteVolume() { this.volume = 0; }
        @Override public void setMaxVolume() { this.volume = 100; }
        @Override public int getBatteryPercentage() { return this.battery; }
        @Override public boolean isDeviceCharging() { return false; }
        @Override public String getDeviceInfo() { return "Device: Nova Phone\nAndroid: 14"; }
        @Override public boolean launchApp(String appName) { return true; }
        @Override public boolean closeApp(String appName) { return true; }
        @Override public void goToHomeScreen() {}
        @Override public boolean setTimer(int seconds) { return true; }
        @Override public boolean setAlarm(int hour, int minute, String message) { return true; }
        @Override public void openSettings(String action) {}
        @Override public void dialNumber(String numberOrName) {}
        @Override public void sendSms(String recipient, String message) {}
        @Override public String saveNote(String text) { return "Saved note: \"" + text + "\""; }
        @Override public String getFormattedNotes() { return "1. Buy milk\n2. Call mom"; }
        @Override public String clearNotes() { return "Cleared all notes."; }
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    AMARA OFFLINE AI ASSISTANT - ENGINE TEST      ");
        System.out.println("==================================================");

        MockActionDispatcher mockActions = new MockActionDispatcher();
        AssistantBrain brain = new AssistantBrain(mockActions);
        OfflineKnowledgeBase kb = new OfflineKnowledgeBase();
        IntentClassifier classifier = new IntentClassifier(kb);
        ContextMemory memory = brain.getMemory();

        int passed = 0;
        int total = 0;

        // Test 1: Math Calculations
        total++;
        AssistantBrain.Response r1 = brain.process("what is 25 * 4");
        if (r1.spokenText.contains("100")) {
            System.out.println("✓ Test 1 Passed: 25 * 4 -> " + r1.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 1 Failed: Got " + r1.spokenText);
        }

        // Test 2: Percentage
        total++;
        AssistantBrain.Response r2 = brain.process("what is 15 percent of 240");
        if (r2.spokenText.contains("36")) {
            System.out.println("✓ Test 2 Passed: 15% of 240 -> " + r2.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 2 Failed: Got " + r2.spokenText);
        }

        // Test 3: Square Root
        total++;
        AssistantBrain.Response r3 = brain.process("square root of 144");
        if (r3.spokenText.contains("12")) {
            System.out.println("✓ Test 3 Passed: sqrt(144) -> " + r3.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 3 Failed: Got " + r3.spokenText);
        }

        // Test 4: Conversational Context Math ("add 50 to that")
        total++;
        AssistantBrain.Response r4 = brain.process("add 50 to that");
        if (r4.spokenText.contains("62")) { // 12 + 50 = 62
            System.out.println("✓ Test 4 Passed: Context memory math (12 + 50) -> " + r4.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 4 Failed: Expected 62, got " + r4.spokenText);
        }

        // Test 5: Unit Conversion
        total++;
        AssistantBrain.Response r5 = brain.process("100 celsius in fahrenheit");
        if (r5.displayText.contains("212")) {
            System.out.println("✓ Test 5 Passed: " + r5.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 5 Failed: Expected 212°F, got " + r5.displayText);
        }

        // Test 6: Hardware - Flashlight ON
        total++;
        AssistantBrain.Response r6 = brain.process("Hey Amara, turn on flashlight");
        if (mockActions.torch && "TORCH_ON".equals(r6.actionTag)) {
            System.out.println("✓ Test 6 Passed: " + r6.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 6 Failed: Torch state=" + mockActions.torch);
        }

        // Test 7: Hardware - Volume Set
        total++;
        AssistantBrain.Response r7 = brain.process("set volume to 80%");
        if (mockActions.volume == 80 && "VOLUME".equals(r7.actionTag)) {
            System.out.println("✓ Test 7 Passed: " + r7.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 7 Failed: Volume=" + mockActions.volume);
        }

        // Test 8: Battery Status
        total++;
        AssistantBrain.Response r8 = brain.process("how much battery is left?");
        if (r8.spokenText.contains("85 percent") && "BATTERY".equals(r8.actionTag)) {
            System.out.println("✓ Test 8 Passed: " + r8.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 8 Failed: Got " + r8.spokenText);
        }

        // Test 9: App Launcher
        total++;
        AssistantBrain.Response r9 = brain.process("open WhatsApp");
        if ("LAUNCH_APP".equals(r9.actionTag) && r9.displayText.contains("WhatsApp")) {
            System.out.println("✓ Test 9 Passed: " + r9.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 9 Failed: Got " + r9.displayText);
        }

        // Test 10: Alarms & Timers
        total++;
        AssistantBrain.Response r10 = brain.process("set timer for 10 minutes");
        if ("TIMER".equals(r10.actionTag) && r10.spokenText.contains("10 minutes")) {
            System.out.println("✓ Test 10 Passed: " + r10.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 10 Failed: Got " + r10.displayText);
        }

        // Test 11: Local Notes
        total++;
        AssistantBrain.Response r11 = brain.process("take a note: pick up dry cleaning");
        if ("NOTE_SAVED".equals(r11.actionTag) && r11.spokenText.contains("pick up dry cleaning")) {
            System.out.println("✓ Test 11 Passed: " + r11.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 11 Failed: Got " + r11.displayText);
        }

        // Test 12: Offline Knowledge Base (Capitals)
        total++;
        AssistantBrain.Response r12 = brain.process("what is the capital of France");
        if (r12.spokenText.contains("Paris")) {
            System.out.println("✓ Test 12 Passed: " + r12.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 12 Failed: Got " + r12.spokenText);
        }

        // Test 13: Joke
        total++;
        AssistantBrain.Response r13 = brain.process("tell me a joke");
        if ("JOKE".equals(r13.actionTag) && r13.spokenText.length() > 10) {
            System.out.println("✓ Test 13 Passed: Joke: " + r13.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 13 Failed: Got " + r13.displayText);
        }

        // Test 14: Amara Hotword Normalization
        total++;
        AssistantBrain.Response r14 = brain.process("Hey Amara, turn off torch");
        if (!mockActions.torch && "TORCH_OFF".equals(r14.actionTag)) {
            System.out.println("✓ Test 14 Passed: " + r14.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 14 Failed: Torch state=" + mockActions.torch);
        }

        // Test 15: Encyclopedic Fact (Speed of Light)
        total++;
        AssistantBrain.Response r15 = brain.process("how fast is light?");
        if (r15.spokenText.contains("299,792,458")) {
            System.out.println("✓ Test 15 Passed: " + r15.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 15 Failed: Got " + r15.spokenText);
        }

        // Test 16: Identity / Persona
        total++;
        AssistantBrain.Response r16 = brain.process("who are you?");
        if (r16.spokenText.contains("personal offline assistant")) {
            System.out.println("✓ Test 16 Passed: " + r16.spokenText);
            passed++;
        } else {
            System.err.println("✗ Test 16 Failed: Got " + r16.spokenText);
        }

        // Test 17: Wake word alone ("Hello Nova")
        total++;
        AssistantBrain.Response r17 = brain.process("Hello Nova");
        if (("LISTENING".equals(r17.actionTag) || "GREETING".equals(r17.actionTag)) && r17.spokenText.length() > 3) {
            System.out.println("✓ Test 17 Passed: 'Hello Nova' wake-word -> " + r17.spokenText);
            passed++;
        } else {
            System.err.println("✗ Test 17 Failed: Got " + r17.spokenText);
        }

        // Test 18: Hands-free App Launch ("Hello Nova open YouTube")
        total++;
        AssistantBrain.Response r18 = brain.process("Hello Nova open YouTube");
        if ("LAUNCH_APP".equals(r18.actionTag) && r18.displayText.contains("YouTube")) {
            System.out.println("✓ Test 18 Passed: 'Hello Nova open YouTube' -> " + r18.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 18 Failed: Got " + r18.displayText);
        }

        // Test 19: Hands-free Hardware ("Hello Nova turn on flashlight")
        total++;
        AssistantBrain.Response r19 = brain.process("Hello Nova turn on flashlight");
        if (mockActions.torch && "TORCH_ON".equals(r19.actionTag)) {
            System.out.println("✓ Test 19 Passed: 'Hello Nova turn on flashlight' -> " + r19.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 19 Failed: Got " + r19.displayText);
        }

        // Test 20: Jarvis Persona & Wake Word ("Hey Jarvis, open WhatsApp")
        total++;
        AssistantBrain.Response r20 = brain.process("Hey Jarvis, open WhatsApp");
        if ("LAUNCH_APP".equals(r20.actionTag) && r20.displayText.contains("WhatsApp")) {
            System.out.println("✓ Test 20 Passed: 'Hey Jarvis, open WhatsApp' -> " + r20.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 20 Failed: Got " + r20.displayText);
        }

        // Test 21: Human-like Small Talk ("How are you?")
        total++;
        AssistantBrain.Response r21 = brain.process("how are you doing today?");
        if ("HOW_ARE_YOU".equals(r21.actionTag) && r21.spokenText.length() > 10) {
            System.out.println("✓ Test 21 Passed: 'How are you' human reply -> " + r21.spokenText);
            passed++;
        } else {
            System.err.println("✗ Test 21 Failed: Got " + r21.spokenText);
        }

        // Test 22: Human-like Small Talk ("What's up?")
        total++;
        AssistantBrain.Response r22 = brain.process("what's up bro");
        if ("WHATS_UP".equals(r22.actionTag) && r22.spokenText.length() > 10) {
            System.out.println("✓ Test 22 Passed: 'What's up' human reply -> " + r22.spokenText);
            passed++;
        } else {
            System.err.println("✗ Test 22 Failed: Got " + r22.spokenText);
        }

        // Test 23: Display Over Other Apps / Floating Arc Reactor
        total++;
        AssistantBrain.Response r23 = brain.process("display over screen");
        if (r23.intentType == IntentType.OVERLAY_MODE && "OVERLAY".equals(r23.actionTag)) {
            System.out.println("✓ Test 23 Passed: 'Display over screen' -> " + r23.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 23 Failed: Got " + r23.displayText);
        }

        // Test 24: Date Query ("what's today's date")
        total++;
        AssistantBrain.Response r24 = brain.process("what's today's date");
        if ("DATE".equals(r24.actionTag) && r24.spokenText.contains("Today is")) {
            System.out.println("✓ Test 24 Passed: 'what's today's date' -> " + r24.spokenText);
            passed++;
        } else {
            System.err.println("✗ Test 24 Failed: Got " + r24.spokenText);
        }

        // Test 25: Close App ("close YouTube")
        total++;
        AssistantBrain.Response r25 = brain.process("close YouTube");
        if ("CLOSE_APP".equals(r25.actionTag) && r25.displayText.contains("YouTube")) {
            System.out.println("✓ Test 25 Passed: 'close YouTube' -> " + r25.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 25 Failed: Got " + r25.displayText);
        }

        // Test 26: Go Home / Close Current App ("go to home screen")
        total++;
        AssistantBrain.Response r26 = brain.process("go to home screen");
        if ("GO_HOME".equals(r26.actionTag) && "[HOME]".equals(r26.displayText)) {
            System.out.println("✓ Test 26 Passed: 'go to home screen' -> " + r26.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 26 Failed: Got " + r26.displayText);
        }

        // Test 27: Amara Wake Word ("Hey Amara")
        total++;
        AssistantBrain.Response r27 = brain.process("Hey Amara");
        if (r27.intentType == IntentType.WAKE_WORD && "LISTENING".equals(r27.actionTag)) {
            System.out.println("✓ Test 27 Passed: 'Hey Amara' wake-word -> " + r27.spokenText);
            passed++;
        } else {
            System.err.println("✗ Test 27 Failed: Got " + r27.spokenText);
        }

        // Test 28: Battery Percent ("whats my battery percent")
        total++;
        AssistantBrain.Response r28 = brain.process("whats my battery percent");
        if ("BATTERY".equals(r28.actionTag) && r28.spokenText.contains("85 percent")) {
            System.out.println("✓ Test 28 Passed: 'whats my battery percent' -> " + r28.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 28 Failed: Got " + r28.displayText);
        }

        // Test 29: Battery Percentage ("tell me the battery percentage")
        total++;
        AssistantBrain.Response r29 = brain.process("tell me the battery percentage");
        if ("BATTERY".equals(r29.actionTag) && r29.spokenText.contains("85 percent")) {
            System.out.println("✓ Test 29 Passed: 'tell me the battery percentage' -> " + r29.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 29 Failed: Got " + r29.displayText);
        }

        // Test 30: Amara Identity Persona
        total++;
        AssistantBrain.Response r30 = brain.process("who are you?");
        if (r30.spokenText.contains("Amara")) {
            System.out.println("✓ Test 30 Passed: Identity contains Amara -> " + r30.spokenText);
            passed++;
        } else {
            System.err.println("✗ Test 30 Failed: Got " + r30.spokenText);
        }

        // Test 31: Hands-free Amara Battery Query ("Hey Amara, what's my battery percent?")
        total++;
        AssistantBrain.Response r31 = brain.process("Hey Amara, what's my battery percent?");
        if ("BATTERY".equals(r31.actionTag) && r31.spokenText.contains("85 percent")) {
            System.out.println("✓ Test 31 Passed: 'Hey Amara, what's my battery percent?' -> " + r31.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 31 Failed: Got " + r31.displayText);
        }

        // Test 32: Suggestion Chip "Turn on Flashlight"
        total++;
        AssistantBrain.Response r32 = brain.process("Turn on Flashlight");
        if (mockActions.torch && "TORCH_ON".equals(r32.actionTag)) {
            System.out.println("✓ Test 32 Passed: Suggestion Chip 'Turn on Flashlight' -> " + r32.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 32 Failed: Got " + r32.displayText);
        }

        // Test 33: Suggestion Chip "Turn off Flashlight"
        total++;
        AssistantBrain.Response r33 = brain.process("Turn off Flashlight");
        if (!mockActions.torch && "TORCH_OFF".equals(r33.actionTag)) {
            System.out.println("✓ Test 33 Passed: Suggestion Chip 'Turn off Flashlight' -> " + r33.displayText);
            passed++;
        } else {
            System.err.println("✗ Test 33 Failed: Got " + r33.displayText);
        }

        System.out.println("==================================================");
        System.out.println("RESULTS: " + passed + " / " + total + " TESTS PASSED (100% OFFLINE)");
        System.out.println("==================================================");

        if (passed != total) {
            System.exit(1);
        }
    }
}
