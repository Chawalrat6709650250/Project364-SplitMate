package com.example.splitmate;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitStore {

    private static SQLiteDatabase db() {
        return SplitMateApplication.getDatabase();
    }

    public static boolean saveSplits(int expenseId, Map<String, Double> shares, String method) {
        return saveSplits(db(), expenseId, shares, method);
    }

    // Package-private overload for isolated database tests.
    static boolean saveSplits(SQLiteDatabase database, int expenseId,
                              Map<String, Double> shares, String method) {
        if (!SplitCalculator.METHOD_EQUAL.equals(method)
                && !SplitCalculator.METHOD_CUSTOM.equals(method)) return false;
        if (shares == null || shares.isEmpty()) return false;

        database.beginTransaction();
        try {
            int groupId;
            long amountSatang;
            try (Cursor cursor = database.query("expenses",
                    new String[]{"group_id", "amount_satang"}, "expense_id = ?",
                    new String[]{String.valueOf(expenseId)}, null, null, null)) {
                if (!cursor.moveToFirst()) return false;
                groupId = cursor.getInt(0);
                amountSatang = cursor.getLong(1);
            }

            // Validate against the stored amount, avoiding overflow while summing.
            long remaining = amountSatang;
            for (Map.Entry<String, Double> entry : shares.entrySet()) {
                Double value = entry.getValue();
                if (entry.getKey() == null || value == null || !Double.isFinite(value)
                        || value * 100.0 >= (double) Long.MAX_VALUE) return false;
                long satang = SplitCalculator.toSatang(value);
                if (satang <= 0 || satang > remaining) return false;
                remaining -= satang;
            }
            if (remaining != 0) return false;

            String[] args = {String.valueOf(expenseId)};
            database.delete("expense_splits", "expense_id = ?", args);
            for (Map.Entry<String, Double> entry : shares.entrySet()) {
                ContentValues values = new ContentValues();
                values.put("expense_id", expenseId);
                values.put("group_id", groupId);
                values.put("member_name", entry.getKey());
                values.put("share_satang", SplitCalculator.toSatang(entry.getValue()));
                long id = database.insertOrThrow("expense_splits", null, values);
                if (id <= 0 || id > Integer.MAX_VALUE) return false;
            }
            ContentValues values = new ContentValues();
            values.put("split_method", method);
            if (database.update("expenses", values, "expense_id = ?", args) != 1) return false;
            database.setTransactionSuccessful();
            return true;
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot save splits", e);
            return false;
        } finally {
            database.endTransaction();
        }
    }

    private static ArrayList<ExpenseSplit> readSplits(String selection, String[] args) {
        ArrayList<ExpenseSplit> result = new ArrayList<>();
        try (Cursor cursor = db().query("expense_splits",
                new String[]{"split_id", "expense_id", "member_name", "share_satang"},
                selection, args, null, null, "split_id ASC")) {
            while (cursor.moveToNext()) {
                result.add(new ExpenseSplit(cursor.getInt(0), cursor.getInt(1),
                        cursor.getString(2), SplitCalculator.toBaht(cursor.getLong(3))));
            }
        }
        return result;
    }

    public static ArrayList<ExpenseSplit> getSplits(int expenseId) {
        return readSplits("expense_id = ?", new String[]{String.valueOf(expenseId)});
    }

    public static ArrayList<ExpenseSplit> getSplitsByGroup(int groupId) {
        return readSplits("group_id = ?", new String[]{String.valueOf(groupId)});
    }

    public static boolean hasSplits(int expenseId) {
        return !getSplits(expenseId).isEmpty();
    }

    public static boolean isSplitValid(Expense expense) {
        if (expense == null) return false;
        ArrayList<ExpenseSplit> list = getSplits(expense.getExpenseId());
        if (list.isEmpty()) return false;
        long remaining = SplitCalculator.toSatang(expense.getAmount());
        for (ExpenseSplit split : list) {
            long share = SplitCalculator.toSatang(split.getShareAmount());
            if (share <= 0 || share > remaining) return false;
            remaining -= share;
        }
        return remaining == 0;
    }

    public static String getSplitMethod(int expenseId) {
        try (Cursor cursor = db().query("expenses", new String[]{"split_method"},
                "expense_id = ?", new String[]{String.valueOf(expenseId)},
                null, null, null)) {
            return cursor.moveToFirst() ? cursor.getString(0) : SplitCalculator.METHOD_EQUAL;
        }
    }

    public static void deleteSplitsForExpense(int expenseId) {
        SQLiteDatabase database = db();
        String[] args = {String.valueOf(expenseId)};
        database.beginTransaction();
        try {
            database.delete("expense_splits", "expense_id = ?", args);
            ContentValues values = new ContentValues();
            values.put("split_method", SplitCalculator.METHOD_EQUAL);
            database.update("expenses", values, "expense_id = ?", args);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static boolean isMemberUsed(int groupId, String memberName) {
        try (Cursor cursor = db().query("expense_splits", new String[]{"split_id"},
                "group_id = ? AND member_name = ?",
                new String[]{String.valueOf(groupId), memberName},
                null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    public static int countExpensesInvolving(int groupId, String memberName) {
        try (Cursor cursor = db().rawQuery(
                "SELECT COUNT(*) FROM expenses e WHERE e.group_id = ? AND "
                        + "(e.paid_by = ? OR EXISTS (SELECT 1 FROM expense_splits s "
                        + "WHERE s.expense_id = e.expense_id AND s.member_name = ?))",
                new String[]{String.valueOf(groupId), memberName, memberName})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public static Map<String, Double> getTotalShareByMember(int groupId) {
        Map<String, Double> result = new HashMap<>();
        try (Cursor cursor = db().rawQuery(
                "SELECT member_name, SUM(share_satang) FROM expense_splits "
                        + "WHERE group_id = ? GROUP BY member_name",
                new String[]{String.valueOf(groupId)})) {
            while (cursor.moveToNext()) {
                result.put(cursor.getString(0), SplitCalculator.toBaht(cursor.getLong(1)));
            }
        }
        return result;
    }

    public static List<Integer> getUnsplitExpenseIds(int groupId) {
        List<Integer> result = new ArrayList<>();
        for (Expense expense : ExpenseStore.getExpenses(groupId)) {
            if (!isSplitValid(expense)) result.add(expense.getExpenseId());
        }
        return result;
    }
}
