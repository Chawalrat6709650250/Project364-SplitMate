package com.example.splitmate;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {

    @Test
    public void schemaConstraintsAndPersistence() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String testName = "splitmate_schema_test.db";
        context.deleteDatabase(testName);
        try {
            try (DatabaseHelper helper = new DatabaseHelper(context, testName)) {
                SQLiteDatabase db = helper.getWritableDatabase();
                assertEquals(1L, DatabaseUtils.longForQuery(db, "PRAGMA foreign_keys", null));
                db.execSQL("INSERT INTO groups(group_id, group_name) VALUES (1, 'Test'), (2, 'Other')");
                db.execSQL("INSERT INTO members(group_id, member_name) VALUES (1, 'A'), (1, 'B'), (2, 'X')");
                db.execSQL("INSERT INTO expenses(expense_id, group_id, title, amount_satang, paid_by, category, expense_date) VALUES (1, 1, 'Food', 100000, 'A', 'Food', '27/11/2026'), (2, 1, 'Travel', 50000, 'B', 'Travel', '27/11/2026')");
                db.execSQL("INSERT INTO expense_splits(expense_id, group_id, member_name, share_satang) VALUES (1, 1, 'A', 50000), (1, 1, 'B', 50000), (2, 1, 'B', 50000)");
                db.execSQL("INSERT INTO settlements(group_id, from_member, to_member, amount_satang, status) VALUES (1, 'B', 'A', 50000, 'PAID')");
                reject(db, "INSERT INTO members(group_id, member_name) VALUES (1, 'A')");
                reject(db, "INSERT INTO expense_splits(expense_id, group_id, member_name, share_satang) VALUES (1, 2, 'X', 1)");
                reject(db, "UPDATE expenses SET paid_by = 'X' WHERE expense_id = 1");
                reject(db, "UPDATE settlements SET status = 'INVALID'");
                reject(db, "DELETE FROM members WHERE group_id = 1 AND member_name = 'A'");
            }
            try (DatabaseHelper helper = new DatabaseHelper(context, testName)) {
                SQLiteDatabase db = helper.getWritableDatabase();
                assertEquals(2L, DatabaseUtils.queryNumEntries(db, "expenses"));
                assertEquals(3L, DatabaseUtils.queryNumEntries(db, "expense_splits"));
                assertEquals(1L, DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM settlements WHERE status = 'PAID'", null));
                db.execSQL("DELETE FROM expenses WHERE expense_id = 2");
                assertEquals(2L, DatabaseUtils.queryNumEntries(db, "expense_splits"));
                db.execSQL("DELETE FROM groups WHERE group_id = 1");
                assertEquals(1L, DatabaseUtils.queryNumEntries(db, "groups"));
                assertEquals(1L, DatabaseUtils.queryNumEntries(db, "members"));
                assertEquals(0L, DatabaseUtils.queryNumEntries(db, "expenses"));
                assertEquals(0L, DatabaseUtils.queryNumEntries(db, "expense_splits"));
                assertEquals(0L, DatabaseUtils.queryNumEntries(db, "settlements"));
            }
        } finally {
            context.deleteDatabase(testName);
        }
    }

    private static void reject(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
            fail("Expected SQLiteConstraintException: " + sql);
        } catch (SQLiteConstraintException expected) {
            // Constraint correctly rejected the statement.
        }
    }
}
