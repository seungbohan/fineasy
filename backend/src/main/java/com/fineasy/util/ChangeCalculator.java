package com.fineasy.util;

public final class ChangeCalculator {

    private ChangeCalculator() {}

    public record Change(Double changeAmount, Double changeRate) {
        public static final Change EMPTY = new Change(null, null);
    }

    public static Change calculate(double currentValue, Double previousValue) {
        if (previousValue == null || previousValue == 0.0) {
            return Change.EMPTY;
        }
        double amount = Math.round((currentValue - previousValue) * 100.0) / 100.0;
        double rate = Math.round(((currentValue - previousValue) / Math.abs(previousValue)) * 10000.0) / 100.0;
        return new Change(amount, rate);
    }
}
