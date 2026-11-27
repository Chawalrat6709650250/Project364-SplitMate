package com.example.splitmate;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SettlementStore {
    private static SQLiteDatabase db() {
        return SplitMateApplication.getDatabase();
    }

    private static boolean validStatus(String status) {
        return Settlement.STATUS_PENDING.equals(status) || Settlement.STATUS_PAID.equals(status);
    }

    private static int insert(SQLiteDatabase database, int groupId, String from,
                              String to, long amountSatang, String status) {
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("from_member", from);
        values.put("to_member", to);
        values.put("amount_satang", amountSatang);
        values.put("status", status);
        long id = database.insertOrThrow("settlements", null, values);
        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw new SQLiteException("Settlement ID out of range");
        }
        return (int) id;
    }

    public static int addSettlement(int groupId, String from, String to,
                                    double amount, String status) {
        if (!ExpenseStore.isValidAmount(amount) || !validStatus(status)) return -1;
        SQLiteDatabase database = db();
        database.beginTransaction();
        try {
            int id = insert(database, groupId, from, to,
                    SplitCalculator.toSatang(amount), status);
            database.setTransactionSuccessful();
            return id;
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot add settlement", e);
            return -1;
        } finally {
            database.endTransaction();
        }
    }

    private static ArrayList<Settlement> read(SQLiteDatabase database,
                                              String selection, String[] args) {
        ArrayList<Settlement> result = new ArrayList<>();
        try (Cursor cursor = database.query("settlements",
                new String[]{"settlement_id", "group_id", "from_member", "to_member",
                        "amount_satang", "status"}, selection, args,
                null, null, "settlement_id ASC")) {
            while (cursor.moveToNext()) {
                result.add(new Settlement(cursor.getInt(0), cursor.getInt(1),
                        cursor.getString(2), cursor.getString(3),
                        SplitCalculator.toBaht(cursor.getLong(4)), cursor.getString(5)));
            }
        }
        return result;
    }

    public static ArrayList<Settlement> getSettlements(int groupId) {
        return read(db(), "group_id = ?", new String[]{String.valueOf(groupId)});
    }

    public static ArrayList<Settlement> getSettlements(int groupId, String status) {
        return getSettlements(db(), groupId, status);
    }

    static ArrayList<Settlement> getSettlements(SQLiteDatabase database, int groupId, String status) {
        if (!validStatus(status)) return new ArrayList<>();
        return read(database, "group_id = ? AND status = ?",
                new String[]{String.valueOf(groupId), status});
    }

    public static Settlement getSettlement(int settlementId) {
        ArrayList<Settlement> rows = read(db(), "settlement_id = ?",
                new String[]{String.valueOf(settlementId)});
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static boolean updateStatus(int settlementId, String status) {
        return updateStatus(db(), settlementId, status);
    }

    static boolean updateStatus(SQLiteDatabase database, int settlementId, String status) {
        if (!validStatus(status)) return false;
        ContentValues values = new ContentValues();
        values.put("status", status);
        try {
            return database.update("settlements", values, "settlement_id = ?",
                    new String[]{String.valueOf(settlementId)}) == 1;
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot update settlement", e);
            return false;
        }
    }

    public static boolean deleteSettlement(int settlementId) {
        return deleteSettlement(db(), settlementId);
    }

    static boolean deleteSettlement(SQLiteDatabase database, int settlementId) {
        try {
            return database.delete("settlements", "settlement_id = ?",
                    new String[]{String.valueOf(settlementId)}) == 1;
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot delete settlement", e);
            return false;
        }
    }

    public static void deleteSettlementsForGroup(int groupId) {
        db().delete("settlements", "group_id = ?", new String[]{String.valueOf(groupId)});
    }

    public static ArrayList<Settlement> refreshPending(int groupId) {
        return refreshPending(db(), groupId, () -> BalanceCalculator.calculateNetSatang(groupId));
    }

    // Isolated tests can supply a net calculation without using the app database.
    static ArrayList<Settlement> refreshPending(SQLiteDatabase database, int groupId,
                                                Supplier<Map<String, Long>> calculateNet) {
        database.beginTransaction();
        try {
            List<SettlementCalculator.Transfer> transfers =
                    SettlementCalculator.suggest(calculateNet.get());
            database.delete("settlements", "group_id = ? AND status = ?",
                    new String[]{String.valueOf(groupId), Settlement.STATUS_PENDING});
            for (SettlementCalculator.Transfer transfer : transfers) {
                insert(database, groupId, transfer.from, transfer.to,
                        transfer.amountSatang, Settlement.STATUS_PENDING);
            }
            ArrayList<Settlement> result = getSettlements(database, groupId, Settlement.STATUS_PENDING);
            database.setTransactionSuccessful();
            return result;
        } finally {
            // Exceptions propagate to the UI; failed writes roll back, never appear as zero debt.
            database.endTransaction();
        }
    }
}
