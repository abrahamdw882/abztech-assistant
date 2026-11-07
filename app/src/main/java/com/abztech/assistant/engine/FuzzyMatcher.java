package com.abztech.assistant.engine;

import java.util.Locale;

public class FuzzyMatcher {

    /**
     * Calculates the Levenshtein distance between two strings.
     */
    public static int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;
        s1 = s1.toLowerCase(Locale.ROOT).trim();
        s2 = s2.toLowerCase(Locale.ROOT).trim();

        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    /**
     * Calculates normalized similarity between 0.0 (completely different) and 1.0 (identical).
     */
    public static float similarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0f;
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0f;
        int distance = levenshteinDistance(s1, s2);
        return 1.0f - ((float) distance / maxLen);
    }

    /**
     * Checks if target is contained in source, or closely matches within distance threshold.
     */
    public static boolean containsOrSimilar(String text, String target, float threshold) {
        if (text == null || target == null) return false;
        text = text.toLowerCase(Locale.ROOT);
        target = target.toLowerCase(Locale.ROOT);

        if (text.contains(target)) return true;

        String[] words = text.split("\\s+");
        for (String word : words) {
            if (similarity(word, target) >= threshold) {
                return true;
            }
        }
        return false;
    }
}
