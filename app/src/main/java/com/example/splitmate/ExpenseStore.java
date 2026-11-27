package com.example.splitmate;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;

public class ExpenseStore {

    private static SQLiteDatabase db() {
        return SplitMateApplication.getDatabase();
    }

    // Reject amounts that cannot be represented as positive integer satang.
    public static boolean isValidAmount(double amount) {
        return Double.isFinite(amount)
                && amount >= 0.01
                && amount * 100.0 < (double) Long.MAX_VALUE;
    }

    private static ContentValues expenseValues(String title, double amount,
                                               String paidBy, String category,
                                               String expenseDate) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("amount_satang", SplitCalculator.toSatang(amount));
        values.put("paid_by", paidBy);
        values.put("category", category);
        values.put("expense_date", expenseDate);
        return values;
    }

    public static int addExpense(int groupId, String title, double amount,
                                 String paidBy, String category, String expenseDate) {
        if (!isValidAmount(amount)) return -1;

        ContentValues values = expenseValues(title, amount, paidBy, category, expenseDate);
        values.put("group_id", groupId);
        SQLiteDatabase database = db();
        database.beginTransaction();
        try {
            long id = database.insertOrThrow("expenses", null, values);
            // Existing models and Intent extras use int IDs.
            if (id <= 0 || id > Integer.MAX_VALUE) return -1;
            database.setTransactionSuccessful();
            return (int) id;
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot add expense", e);
            return -1;
        } finally {
            database.endTransaction();
        }
    }

    public static ArrayList<Expense> getExpenses(int groupId) {
        ArrayList<Expense> result = new ArrayList<>();
        try (Cursor cursor = db().query("expenses",
                new String[]{"expense_id", "group_id", "title", "amount_satang",
                        "paid_by", "category", "expense_date"},
                "group_id = ?", new String[]{String.valueOf(groupId)},
                null, null, "expense_id ASC")) {
            while (cursor.moveToNext()) {
                result.add(new Expense(cursor.getInt(0), cursor.getInt(1),
                        cursor.getString(2), SplitCalculator.toBaht(cursor.getLong(3)),
                        cursor.getString(4), cursor.getString(5), cursor.getString(6)));
            }
        }
        return result;
    }

    // Returns true only when an existing expense was successfully updated.
    public static boolean updateExpense(Expense updatedExpense) {
        if (updatedExpense == null || !isValidAmount(updatedExpense.getAmount())) return false;

        ContentValues values = expenseValues(updatedExpense.getTitle(),
                updatedExpense.getAmount(), updatedExpense.getPaidBy(),
                updatedExpense.getCategory(), updatedExpense.getExpenseDate());
        // Preserve group_id and split_method; SplitStore handles splitting separately.
        try {
            return db().update("expenses", values, "expense_id = ? AND group_id = ?",
                    new String[]{String.valueOf(updatedExpense.getExpenseId()),
                            String.valueOf(updatedExpense.getGroupId())}) > 0;
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot update expense", e);
            return false;
        }
    }

    public static boolean deleteExpense(int expenseId) {
        try {
            return db().delete(
                    "expenses",
                    "expense_id = ?",
                    new String[]{String.valueOf(expenseId)}
            ) > 0;

        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot delete expense", e);
            return false;
        }
    }

    public static double getTotalExpense(int groupId) {
        try (Cursor cursor = db().rawQuery(
                "SELECT COALESCE(SUM(amount_satang), 0) FROM expenses WHERE group_id = ?",
                new String[]{String.valueOf(groupId)})) {
            return cursor.moveToFirst() ? SplitCalculator.toBaht(cursor.getLong(0)) : 0.0;
        }
    }
}
