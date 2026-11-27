package com.example.splitmate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ยอดสุทธิ = เงินที่จ่ายจริง - ส่วนที่ต้องรับผิดชอบ + เงินที่คืนไปแล้ว - เงินที่ได้รับคืนแล้ว
// ค่าบวก = มีคนต้องคืนเงินให้, ค่าลบ = ต้องคืนเงินให้คนอื่น
// คิดเป็นสตางค์ (long) ทั้งหมดเพื่อไม่ให้มีเศษทศนิยมเพี้ยน
public final class BalanceCalculator {

    private BalanceCalculator() {
    }

    public static Map<String, Long> calculateNetSatang(
            List<String> members,
            List<Expense> expenses,
            Map<Integer, List<ExpenseSplit>> splitsByExpense,
            List<Settlement> settlements) {

        Map<String, Long> net = new LinkedHashMap<>();

        if (members != null) {
            for (String member : members) {
                net.put(member, 0L);
            }
        }

        if (expenses != null) {
            for (Expense expense : expenses) {
                List<ExpenseSplit> splits = splitsByExpense == null
                        ? null : splitsByExpense.get(expense.getExpenseId());

                // ค่าใช้จ่ายที่ยังไม่ได้หาร (หรือหารไม่ครบ) ยังไม่นำมาคิด
                if (!isSplitComplete(expense, splits)) {
                    continue;
                }

                add(net, expense.getPaidBy(), SplitCalculator.toSatang(expense.getAmount()));

                for (ExpenseSplit split : splits) {
                    add(net, split.getMemberName(),
                            -SplitCalculator.toSatang(split.getShareAmount()));
                }
            }
        }

        if (settlements != null) {
            for (Settlement settlement : settlements) {
                if (!settlement.isPaid()) {
                    continue;
                }
                long amount = SplitCalculator.toSatang(settlement.getAmount());
                add(net, settlement.getFromMember(), amount);
                add(net, settlement.getToMember(), -amount);
            }
        }

        return net;
    }

    // ดึงข้อมูลจาก Store ของกลุ่มแล้วคำนวณ
    public static Map<String, Long> calculateNetSatang(int groupId) {
        List<Expense> expenses = ExpenseStore.getExpenses(groupId);
        Map<Integer, List<ExpenseSplit>> splitsByExpense = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            splitsByExpense.put(expense.getExpenseId(),
                    SplitStore.getSplits(expense.getExpenseId()));
        }

        return calculateNetSatang(
                GroupStore.getMembers(groupId),
                expenses,
                splitsByExpense,
                SettlementStore.getSettlements(groupId, Settlement.STATUS_PAID));
    }

    // สรุปของสมาชิกแต่ละคน: ออกเงินไปเท่าไร และส่วนของตัวเองเท่าไร (หน่วยสตางค์)
    public static class MemberSummary {
        public final String name;
        public long paid;
        public long share;

        MemberSummary(String name) {
            this.name = name;
        }
    }

    public static Map<String, MemberSummary> summarize(int groupId) {
        Map<String, MemberSummary> result = new LinkedHashMap<>();
        for (String member : GroupStore.getMembers(groupId)) {
            result.put(member, new MemberSummary(member));
        }

        for (Expense expense : ExpenseStore.getExpenses(groupId)) {
            List<ExpenseSplit> splits = SplitStore.getSplits(expense.getExpenseId());
            if (!isSplitComplete(expense, splits)) {
                continue;
            }
            summaryOf(result, expense.getPaidBy()).paid +=
                    SplitCalculator.toSatang(expense.getAmount());
            for (ExpenseSplit split : splits) {
                summaryOf(result, split.getMemberName()).share +=
                        SplitCalculator.toSatang(split.getShareAmount());
            }
        }

        return result;
    }

    private static MemberSummary summaryOf(Map<String, MemberSummary> map, String name) {
        MemberSummary summary = map.get(name);
        if (summary == null) {
            summary = new MemberSummary(name);
            map.put(name, summary);
        }
        return summary;
    }

    public static Map<String, Double> calculateNet(int groupId) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : calculateNetSatang(groupId).entrySet()) {
            result.put(entry.getKey(), SplitCalculator.toBaht(entry.getValue()));
        }
        return result;
    }

    public static boolean isAllSettled(Map<String, Long> net) {
        for (long value : net.values()) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSplitComplete(Expense expense, List<ExpenseSplit> splits) {
        if (splits == null || splits.isEmpty()) {
            return false;
        }
        long total = 0;
        for (ExpenseSplit split : splits) {
            total += SplitCalculator.toSatang(split.getShareAmount());
        }
        return total == SplitCalculator.toSatang(expense.getAmount());
    }

    private static void add(Map<String, Long> net, String member, long amount) {
        if (member == null) {
            return;
        }
        Long current = net.get(member);
        net.put(member, (current == null ? 0 : current) + amount);
    }
}
