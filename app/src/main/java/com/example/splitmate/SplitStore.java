package com.example.splitmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SplitStore {

    private static final ArrayList<ExpenseSplit> splits = new ArrayList<>();

    private static final Map<Integer, String> splitMethods = new HashMap<>();

    private static int nextId = 1;

    public static void saveSplits(int expenseId, Map<String, Double> shares, String method) {
        deleteSplitsForExpense(expenseId);

        for (Map.Entry<String, Double> entry : shares.entrySet()) {
            splits.add(new ExpenseSplit(
                    nextId++,
                    expenseId,
                    entry.getKey(),
                    entry.getValue()
            ));
        }

        splitMethods.put(expenseId, method);
    }

    public static ArrayList<ExpenseSplit> getSplits(int expenseId) {
        ArrayList<ExpenseSplit> result = new ArrayList<>();

        for (ExpenseSplit split : splits) {
            if (split.getExpenseId() == expenseId) {
                result.add(split);
            }
        }

        return result;
    }

    public static ArrayList<ExpenseSplit> getSplitsByGroup(int groupId) {
        Set<Integer> expenseIds = new HashSet<>();

        for (Expense expense : ExpenseStore.getExpenses(groupId)) {
            expenseIds.add(expense.getExpenseId());
        }

        ArrayList<ExpenseSplit> result = new ArrayList<>();

        for (ExpenseSplit split : splits) {
            if (expenseIds.contains(split.getExpenseId())) {
                result.add(split);
            }
        }

        return result;
    }

    public static boolean hasSplits(int expenseId) {
        for (ExpenseSplit split : splits) {
            if (split.getExpenseId() == expenseId) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSplitValid(Expense expense) {
        if (expense == null) {
            return false;
        }

        ArrayList<ExpenseSplit> list = getSplits(expense.getExpenseId());

        if (list.isEmpty()) {
            return false;
        }

        long total = 0;
        for (ExpenseSplit split : list) {
            total += SplitCalculator.toSatang(split.getShareAmount());
        }

        return total == SplitCalculator.toSatang(expense.getAmount());
    }

    public static String getSplitMethod(int expenseId) {
        String method = splitMethods.get(expenseId);
        return method != null ? method : SplitCalculator.METHOD_EQUAL;
    }

    public static void deleteSplitsForExpense(int expenseId) {
        splits.removeIf(split -> split.getExpenseId() == expenseId);
        splitMethods.remove(expenseId);
    }

    public static boolean isMemberUsed(int groupId, String memberName) {
        for (ExpenseSplit split : getSplitsByGroup(groupId)) {
            if (split.getMemberName().equals(memberName)) {
                return true;
            }
        }
        return false;
    }

    public static int countExpensesInvolving(int groupId, String memberName) {
        int count = 0;
        for (Expense expense : ExpenseStore.getExpenses(groupId)) {
            boolean involved = memberName.equals(expense.getPaidBy());
            if (!involved) {
                for (ExpenseSplit split : getSplits(expense.getExpenseId())) {
                    if (split.getMemberName().equals(memberName)) {
                        involved = true;
                        break;
                    }
                }
            }
            if (involved) {
                count++;
            }
        }
        return count;
    }

    static void clearAll() {
        splits.clear();
        splitMethods.clear();
        nextId = 1;
    }

    public static Map<String, Double> getTotalShareByMember(int groupId) {
        Map<String, Long> satang = new HashMap<>();

        for (ExpenseSplit split : getSplitsByGroup(groupId)) {
            long value = SplitCalculator.toSatang(split.getShareAmount());
            Long current = satang.get(split.getMemberName());
            satang.put(split.getMemberName(), (current == null ? 0 : current) + value);
        }

        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Long> entry : satang.entrySet()) {
            result.put(entry.getKey(), SplitCalculator.toBaht(entry.getValue()));
        }

        return result;
    }

    public static List<Integer> getUnsplitExpenseIds(int groupId) {
        List<Integer> result = new ArrayList<>();
        for (Expense expense : ExpenseStore.getExpenses(groupId)) {
            if (!isSplitValid(expense)) {
                result.add(expense.getExpenseId());
            }
        }
        return result;
    }
}
