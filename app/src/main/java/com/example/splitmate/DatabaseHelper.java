package com.example.splitmate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteException;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "splitmate.db";
    public static final int DATABASE_VERSION = 1;

    private static DatabaseHelper instance;

    private DatabaseHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    // Constructor สำหรับแยกฐานข้อมูลในการทดสอบ
    DatabaseHelper(Context context, String databaseName) {
        super(context.getApplicationContext(),
                databaseName, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Table 1: Groups
        db.execSQL("CREATE TABLE groups ("
                + "group_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "group_name TEXT NOT NULL CHECK(length(trim(group_name)) > 0)"
                + ")");

        // Table 2: Members
        db.execSQL("CREATE TABLE members ("
                + "member_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "group_id INTEGER NOT NULL, "
                + "member_name TEXT NOT NULL CHECK(length(trim(member_name)) > 0), "
                + "UNIQUE(group_id, member_name), "
                + "FOREIGN KEY(group_id) REFERENCES groups(group_id) ON DELETE CASCADE"
                + ")");

        // Table 3: Expenses
        db.execSQL("CREATE TABLE expenses ("
                + "expense_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "group_id INTEGER NOT NULL, "
                + "title TEXT NOT NULL CHECK(length(trim(title)) > 0), "
                + "amount_satang INTEGER NOT NULL CHECK(typeof(amount_satang) = 'integer' AND amount_satang > 0), "
                + "paid_by TEXT NOT NULL, "
                + "category TEXT NOT NULL, "
                + "expense_date TEXT NOT NULL, "
                + "split_method TEXT NOT NULL DEFAULT 'EQUAL' CHECK(split_method IN ('EQUAL', 'CUSTOM')), "
                + "UNIQUE(expense_id, group_id), "
                + "FOREIGN KEY(group_id) REFERENCES groups(group_id) ON DELETE CASCADE, "
                + "FOREIGN KEY(group_id, paid_by) REFERENCES members(group_id, member_name)"
                + ")");

        // Table 4: Expense Splits
        db.execSQL("CREATE TABLE expense_splits ("
                + "split_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "expense_id INTEGER NOT NULL, "
                + "group_id INTEGER NOT NULL, "
                + "member_name TEXT NOT NULL, "
                + "share_satang INTEGER NOT NULL CHECK(typeof(share_satang) = 'integer' AND share_satang > 0), "
                + "UNIQUE(expense_id, member_name), "
                + "FOREIGN KEY(expense_id, group_id) REFERENCES expenses(expense_id, group_id) ON DELETE CASCADE, "
                + "FOREIGN KEY(group_id, member_name) REFERENCES members(group_id, member_name)"
                + ")");

        // Table 5: Settlements
        db.execSQL("CREATE TABLE settlements ("
                + "settlement_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "group_id INTEGER NOT NULL, "
                + "from_member TEXT NOT NULL, "
                + "to_member TEXT NOT NULL, "
                + "amount_satang INTEGER NOT NULL CHECK(typeof(amount_satang) = 'integer' AND amount_satang > 0), "
                + "status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'PAID')), "
                + "CHECK(from_member <> to_member), "
                + "FOREIGN KEY(group_id) REFERENCES groups(group_id) ON DELETE CASCADE, "
                + "FOREIGN KEY(group_id, from_member) REFERENCES members(group_id, member_name), "
                + "FOREIGN KEY(group_id, to_member) REFERENCES members(group_id, member_name)"
                + ")");

        // Indexes
        db.execSQL("CREATE INDEX idx_expenses_group_payer ON expenses(group_id, paid_by)");

        db.execSQL("CREATE INDEX idx_splits_group_member ON expense_splits(group_id, member_name)");

        db.execSQL("CREATE INDEX idx_settlements_group_status ON settlements(group_id, status)");

        db.execSQL("CREATE INDEX idx_settlements_group_from ON settlements(group_id, from_member)");

        db.execSQL("CREATE INDEX idx_settlements_group_to ON settlements(group_id, to_member)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {

        // เพิ่ม Migration ในอนาคตก่อนเปลี่ยน Database Version
        throw new SQLiteException(
                "Missing migration: " + oldVersion + " -> " + newVersion
        );
    }
}