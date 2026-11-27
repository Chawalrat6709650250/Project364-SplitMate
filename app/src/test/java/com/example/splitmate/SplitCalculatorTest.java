package com.example.splitmate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SplitCalculatorTest {

    private static final double DELTA = 0.001;

    @Test
    public void equalSplit_evenAmount() {
        // ตัวอย่างในเอกสาร: ค่าอาหาร 1,000 หาร A, C
        Map<String, Double> shares =
                SplitCalculator.equalSplit(1000, Arrays.asList("A", "C"));

        assertEquals(500.0, shares.get("A"), DELTA);
        assertEquals(500.0, shares.get("C"), DELTA);
    }

    @Test
    public void equalSplit_remainderSatangAddsUpExactly() {
        // 100 หาร 3 = 33.34 + 33.33 + 33.33
        Map<String, Double> shares =
                SplitCalculator.equalSplit(100, Arrays.asList("A", "B", "C"));

        assertEquals(33.34, shares.get("A"), DELTA);
        assertEquals(33.33, shares.get("B"), DELTA);
        assertEquals(33.33, shares.get("C"), DELTA);
        assertEquals(100.0, SplitCalculator.sum(shares), DELTA);
        assertTrue(SplitCalculator.validateSplit(100, shares).valid);
    }

    @Test
    public void equalSplit_noMembers_returnsEmpty() {
        assertTrue(SplitCalculator.equalSplit(100, Collections.emptyList()).isEmpty());
    }

    @Test
    public void customSplit_exactTotal_isValid() {
        Map<String, Double> shares = new LinkedHashMap<>();
        shares.put("A", 700.0);
        shares.put("B", 300.0);

        assertTrue(SplitCalculator.validateSplit(1000, shares).valid);
    }

    @Test
    public void customSplit_short_isInvalid() {
        Map<String, Double> shares = new LinkedHashMap<>();
        shares.put("A", 600.0);
        shares.put("B", 350.0);

        SplitCalculator.ValidationResult r = SplitCalculator.validateSplit(1000, shares);
        assertFalse(r.valid);
        assertEquals(50.0, SplitCalculator.remaining(1000, shares), DELTA);
    }

    @Test
    public void customSplit_over_isInvalid() {
        Map<String, Double> shares = new LinkedHashMap<>();
        shares.put("A", 600.0);
        shares.put("B", 500.0);

        assertFalse(SplitCalculator.validateSplit(1000, shares).valid);
        assertEquals(-100.0, SplitCalculator.remaining(1000, shares), DELTA);
    }

    @Test
    public void customSplit_emptyNegativeOrZero_isInvalid() {
        Map<String, Double> empty = new LinkedHashMap<>();
        empty.put("A", null);
        assertFalse(SplitCalculator.validateSplit(100, empty).valid);

        Map<String, Double> negative = new LinkedHashMap<>();
        negative.put("A", 150.0);
        negative.put("B", -50.0);
        assertFalse(SplitCalculator.validateSplit(100, negative).valid);

        Map<String, Double> zero = new LinkedHashMap<>();
        zero.put("A", 100.0);
        zero.put("B", 0.0);
        assertFalse(SplitCalculator.validateSplit(100, zero).valid);
    }

    @Test
    public void noMemberSelected_isInvalid() {
        assertFalse(SplitCalculator.validateSplit(100, new LinkedHashMap<>()).valid);
    }

    @Test
    public void decimalAmounts_noFloatingPointError() {
        // 0.1 + 0.2 ใน double ไม่เท่ากับ 0.3 พอดี แต่ระบบต้องผ่าน
        Map<String, Double> shares = new LinkedHashMap<>();
        shares.put("A", 0.1);
        shares.put("B", 0.2);

        assertTrue(SplitCalculator.validateSplit(0.3, shares).valid);
    }
}
