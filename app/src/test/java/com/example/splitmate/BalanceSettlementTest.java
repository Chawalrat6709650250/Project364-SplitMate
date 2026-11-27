package com.example.splitmate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BalanceSettlementTest {

    private final List<String> members = Arrays.asList("A", "B", "C", "D");

    // ตัวอย่างในเอกสาร: A จ่าย 1,000 หาร A,C / B จ่าย 500 หาร B,D
    private Map<String, Long> exampleNet(List<Settlement> settlements) {
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 1, "อาหาร", 1000, "A", "Food", "2026-01-01"),
                new Expense(2, 1, "เดินทาง", 500, "B", "Travel", "2026-01-01"));

        Map<Integer, List<ExpenseSplit>> splits = new HashMap<>();
        splits.put(1, Arrays.asList(
                new ExpenseSplit(1, 1, "A", 500), new ExpenseSplit(2, 1, "C", 500)));
        splits.put(2, Arrays.asList(
                new ExpenseSplit(3, 2, "B", 250), new ExpenseSplit(4, 2, "D", 250)));

        return BalanceCalculator.calculateNetSatang(members, expenses, splits, settlements);
    }

    @Test
    public void netBalance_matchesBriefExample() {
        Map<String, Long> net = exampleNet(new ArrayList<>());

        assertEquals(50000L, (long) net.get("A"));
        assertEquals(25000L, (long) net.get("B"));
        assertEquals(-50000L, (long) net.get("C"));
        assertEquals(-25000L, (long) net.get("D"));
    }

    @Test
    public void suggest_matchesBriefExample() {
        List<SettlementCalculator.Transfer> transfers =
                SettlementCalculator.suggest(exampleNet(new ArrayList<>()));

        assertEquals(2, transfers.size());
        assertEquals("C", transfers.get(0).from);
        assertEquals("A", transfers.get(0).to);
        assertEquals(50000L, transfers.get(0).amountSatang);
        assertEquals("D", transfers.get(1).from);
        assertEquals("B", transfers.get(1).to);
        assertEquals(25000L, transfers.get(1).amountSatang);
    }

    @Test
    public void paidSettlements_bringBalancesToZero() {
        List<Settlement> paid = Arrays.asList(
                new Settlement(1, 1, "C", "A", 500, Settlement.STATUS_PAID),
                new Settlement(2, 1, "D", "B", 250, Settlement.STATUS_PAID));

        Map<String, Long> net = exampleNet(paid);

        assertTrue(BalanceCalculator.isAllSettled(net));
        assertTrue(SettlementCalculator.suggest(net).isEmpty());
    }

    @Test
    public void pendingSettlements_areIgnored() {
        List<Settlement> pending = Arrays.asList(
                new Settlement(1, 1, "C", "A", 500, Settlement.STATUS_PENDING));

        assertEquals(-50000L, (long) exampleNet(pending).get("C"));
    }

    @Test
    public void unsplitExpense_isIgnored() {
        List<Expense> expenses = Arrays.asList(
                new Expense(1, 1, "x", 300, "A", "Food", "2026-01-01"));

        Map<String, Long> net = BalanceCalculator.calculateNetSatang(
                members, expenses, new HashMap<>(), new ArrayList<>());

        assertTrue(BalanceCalculator.isAllSettled(net));
    }

    @Test
    public void suggest_usesAtMostNMinusOneTransfers() {
        Map<String, Long> net = new LinkedHashMap<>();
        net.put("A", 30000L);
        net.put("B", -10000L);
        net.put("C", -10000L);
        net.put("D", -10001L);
        net.put("E", 10001L);

        List<SettlementCalculator.Transfer> transfers = SettlementCalculator.suggest(net);

        assertTrue(transfers.size() <= 4);
        long total = 0;
        for (SettlementCalculator.Transfer t : transfers) {
            total += t.amountSatang;
        }
        assertEquals(30001L, total);
    }
}
