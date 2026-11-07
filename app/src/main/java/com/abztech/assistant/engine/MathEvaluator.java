package com.abztech.assistant.engine;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fast, offline recursive-descent math expression evaluator and unit converter.
 */
public class MathEvaluator {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.######");

    /**
     * Attempts to evaluate a math or conversion query. Returns null if not a valid math expression.
     */
    public static String evaluate(String query, Double previousAnswer) {
        if (query == null) return null;
        String clean = query.toLowerCase(Locale.ROOT).trim();

        // 1. Check for Unit Conversions first
        String conversionResult = evaluateConversion(clean);
        if (conversionResult != null) {
            return conversionResult;
        }

        // 2. Pre-process natural language speech into math symbols
        clean = preprocessNaturalMath(clean, previousAnswer);

        // 3. Try parsing percentage: "X percent of Y" or "X % of Y"
        Pattern percentPattern = Pattern.compile("([0-9.]+)\\s*(?:%|percent)\\s+of\\s+([0-9.]+)");
        Matcher percentMatcher = percentPattern.matcher(clean);
        if (percentMatcher.find()) {
            try {
                double pct = Double.parseDouble(percentMatcher.group(1));
                double total = Double.parseDouble(percentMatcher.group(2));
                double result = (pct / 100.0) * total;
                return FORMAT.format(result);
            } catch (Exception ignored) {}
        }

        // 4. Try parsing square root: "sqrt X" or "sqrt(X)"
        Pattern sqrtPattern = Pattern.compile("sqrt\\s*\\(?([0-9.]+)\\)?");
        Matcher sqrtMatcher = sqrtPattern.matcher(clean);
        if (sqrtMatcher.find()) {
            try {
                double val = Double.parseDouble(sqrtMatcher.group(1));
                if (val < 0) return "Error: Cannot take the square root of a negative number.";
                return FORMAT.format(Math.sqrt(val));
            } catch (Exception ignored) {}
        }

        // 5. Check if string contains arithmetic symbols or numbers
        if (!clean.matches(".*[0-9].*") || !clean.matches(".*[+\\-*/^%].*")) {
            return null;
        }

        // Filter out non-math characters
        String mathOnly = clean.replaceAll("[^0-9.+\\-*/^%() ]", "").trim();
        if (mathOnly.isEmpty()) return null;

        try {
            double val = new ExpressionParser(mathOnly).parse();
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                return "Cannot divide by zero.";
            }
            return FORMAT.format(val);
        } catch (Exception e) {
            return null;
        }
    }

    private static String preprocessNaturalMath(String input, Double previousAnswer) {
        String s = input;

        // Contextual memory replacements
        if (previousAnswer != null) {
            s = s.replaceAll("\\b(that|it|previous answer|ans)\\b", String.valueOf(previousAnswer));
        }

        // Conversational math phrases: "add X to Y", "subtract X from Y", etc.
        s = s.replaceAll("\\badd\\s+([0-9.]+)\\s+to\\s+([0-9.]+)\\b", "$2 + $1")
                .replaceAll("\\bsubtract\\s+([0-9.]+)\\s+from\\s+([0-9.]+)\\b", "$2 - $1")
                .replaceAll("\\badd\\s+([0-9.]+)\\b", "+ $1")
                .replaceAll("\\bsubtract\\s+([0-9.]+)\\b", "- $1")
                .replaceAll("\\bmultiply\\s+by\\s+([0-9.]+)\\b", "* $1")
                .replaceAll("\\bdivide\\s+by\\s+([0-9.]+)\\b", "/ $1");

        // Common speech-to-math replacements
        s = s.replaceAll("\\bplus\\b", "+")
                .replaceAll("\\bminus\\b", "-")
                .replaceAll("\\btimes\\b", "*")
                .replaceAll("\\bmultiplied by\\b", "*")
                .replaceAll("\\bx\\b", "*")
                .replaceAll("\\bdivided by\\b", "/")
                .replaceAll("\\bover\\b", "/")
                .replaceAll("\\bmodulo\\b", "%")
                .replaceAll("\\bpower\\b", "^")
                .replaceAll("\\bsquared\\b", "^2")
                .replaceAll("\\bcubed\\b", "^3")
                .replaceAll("\\bsquare root of\\b", "sqrt")
                .replaceAll("\\bsqrt of\\b", "sqrt")
                .replaceAll("\\bcalculate\\b", "")
                .replaceAll("\\bwhat is\\b", "")
                .replaceAll("\\bhow much is\\b", "")
                .replaceAll("\\bequals\\b", "")
                .trim();

        // If leading with operator and we have previousAnswer, prepend it
        if (previousAnswer != null && s.matches("^[+\\-*/^%].*")) {
            s = previousAnswer + " " + s;
        }

        return s;
    }

    private static String evaluateConversion(String query) {
        // Temperature: C to F
        Pattern cToF = Pattern.compile("([0-9.]+)\\s*(?:degrees?\\s*)?(?:c|celsius)\\s+(?:in|to)\\s+(?:f|fahrenheit)");
        Matcher m1 = cToF.matcher(query);
        if (m1.find()) {
            double c = Double.parseDouble(m1.group(1));
            double f = (c * 9.0 / 5.0) + 32.0;
            return FORMAT.format(c) + "°C is " + FORMAT.format(f) + "°F";
        }

        // Temperature: F to C
        Pattern fToC = Pattern.compile("([0-9.]+)\\s*(?:degrees?\\s*)?(?:f|fahrenheit)\\s+(?:in|to)\\s+(?:c|celsius)");
        Matcher m2 = fToC.matcher(query);
        if (m2.find()) {
            double f = Double.parseDouble(m2.group(1));
            double c = (f - 32.0) * 5.0 / 9.0;
            return FORMAT.format(f) + "°F is " + FORMAT.format(c) + "°C";
        }

        // Distance: km to miles
        Pattern kmToMiles = Pattern.compile("([0-9.]+)\\s*(?:km|kilometers?)\\s+(?:in|to)\\s+(?:miles?|mi)");
        Matcher m3 = kmToMiles.matcher(query);
        if (m3.find()) {
            double km = Double.parseDouble(m3.group(1));
            double miles = km * 0.621371;
            return FORMAT.format(km) + " km is " + FORMAT.format(miles) + " miles";
        }

        // Distance: miles to km
        Pattern milesToKm = Pattern.compile("([0-9.]+)\\s*(?:miles?|mi)\\s+(?:in|to)\\s+(?:km|kilometers?)");
        Matcher m4 = milesToKm.matcher(query);
        if (m4.find()) {
            double miles = Double.parseDouble(m4.group(1));
            double km = miles * 1.60934;
            return FORMAT.format(miles) + " miles is " + FORMAT.format(km) + " km";
        }

        // Weight: kg to lbs
        Pattern kgToLbs = Pattern.compile("([0-9.]+)\\s*(?:kg|kilograms?)\\s+(?:in|to)\\s+(?:lbs|pounds?)");
        Matcher m5 = kgToLbs.matcher(query);
        if (m5.find()) {
            double kg = Double.parseDouble(m5.group(1));
            double lbs = kg * 2.20462;
            return FORMAT.format(kg) + " kg is " + FORMAT.format(lbs) + " lbs";
        }

        // Weight: lbs to kg
        Pattern lbsToKg = Pattern.compile("([0-9.]+)\\s*(?:lbs|pounds?)\\s+(?:in|to)\\s+(?:kg|kilograms?)");
        Matcher m6 = lbsToKg.matcher(query);
        if (m6.find()) {
            double lbs = Double.parseDouble(m6.group(1));
            double kg = lbs / 2.20462;
            return FORMAT.format(lbs) + " lbs is " + FORMAT.format(kg) + " kg";
        }

        return null;
    }

    /**
     * Recursive descent expression parser for +, -, *, /, ^, % and parentheses.
     */
    private static class ExpressionParser {
        private final String str;
        private int pos = -1, ch;

        ExpressionParser(String str) {
            this.str = str;
        }

        void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
            return x;
        }

        double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else if (eat('%')) x %= parseFactor();
                else return x;
            }
        }

        double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                eat(')');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(str.substring(startPos, this.pos));
            } else {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }

            if (eat('^')) x = Math.pow(x, parseFactor());

            return x;
        }
    }
}
