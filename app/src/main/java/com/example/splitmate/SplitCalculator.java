package com.example.splitmate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SplitCalculator {

    public static final String METHOD_EQUAL = "EQUAL";
    public static final String METHOD_CUSTOM = "CUSTOM";

    private SplitCalculator() {
    }

    public static class ValidationResult {
        public final boolean valid;
        public final String message;

        ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }

    public static long toSatang(double baht) {
        return Math.round(baht * 100.0);
    }

    public static double toBaht(long satang) {
        return satang / 100.0;
    }

    public static Map<String, Double> equalSplit(double amount, List<String> members) {
        Map<String, Double> result = new LinkedHashMap<>();

        if (members == null || members.isEmpty() || amount <= 0) {
            return result;
        }

        long total = toSatang(amount);
        int count = members.size();
        long base = total / count;
        long remainder = total % count;

        for (int i = 0; i < count; i++) {
            long share = base + (i < remainder ? 1 : 0);
            result.put(members.get(i), toBaht(share));
        }

        return result;
    }

    public static double sum(Map<String, Double> shares) {
        long total = 0;
        if (shares != null) {
            for (Double value : shares.values()) {
                if (value != null) {
                    total += toSatang(value);
                }
            }
        }
        return toBaht(total);
    }

    public static double remaining(double amount, Map<String, Double> shares) {
        return toBaht(toSatang(amount) - toSatang(sum(shares)));
    }

    public static ValidationResult validateSplit(double amount, Map<String, Double> shares) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return new ValidationResult(false, "Invalid expense amount");
        }

        if (shares == null || shares.isEmpty()) {
            return new ValidationResult(false, "Please select at least 1 member");
        }

        for (Map.Entry<String, Double> entry : shares.entrySet()) {
            Double value = entry.getValue();

            if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
                return new ValidationResult(false,
                        "Please enter an amount for " + entry.getKey());
            }

            if (toSatang(value) <= 0) {
                return new ValidationResult(false,
                        "Amount for " + entry.getKey() + " must be greater than 0");
            }
        }

        long diff = toSatang(amount) - toSatang(sum(shares));

        if (diff > 0) {
            return new ValidationResult(false, String.format(
                    java.util.Locale.US,
                    "Total is short by %.2f THB", toBaht(diff)));
        }

        if (diff < 0) {
            return new ValidationResult(false, String.format(
                    java.util.Locale.US,
                    "Total is over by %.2f THB", toBaht(-diff)));
        }

        return new ValidationResult(true, "OK");
    }
}
