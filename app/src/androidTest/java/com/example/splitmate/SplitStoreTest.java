package com.example.splitmate;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SplitStoreTest {

    @Test
    public void saveReplaceRollbackAndPersistence() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String testName = "splitmate_splits_test.db";
        context.deleteDatabase(testName);
        try {
            try (DatabaseHelper helper = new DatabaseHelper(context, testName)) {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.execSQL("INSERT INTO groups(group_id, group_name) VALUES (1, 'Test'), (2, 'Other')");
                db.execSQL("INSERT INTO members(group_id, member_name) VALUES (1, 'A'), (1, 'B'), (1, 'C'), (2, 'X')");
                db.execSQL("INSERT INTO expenses(expense_id, group_id, title, amount_satang, paid_by, category, expense_date) VALUES (1, 1, 'Food', 10000, 'A', 'Food', '27/11/2026')");

                Map<String, Double> equal = SplitCalculator.equalSplit(100, Arrays.asList("A", "B", "C"));
                assertTrue(SplitStore.saveSplits(db, 1, equal, SplitCalculator.METHOD_EQUAL));
                assertEquals(3L, count(db));
                assertEquals(3334L, share(db, "A"));
                assertEquals(3333L, share(db, "B"));
                assertEquals(3333L, share(db, "C"));

                Map<String, Double> custom = new LinkedHashMap<>();
                custom.put("A", 70.0);
                custom.put("B", 30.0);
                assertTrue(SplitStore.saveSplits(db, 1, custom, SplitCalculator.METHOD_CUSTOM));
                assertCustom(db);

                // The first insert succeeds, but the second violates the member FK.
                // A rollback must restore the original CUSTOM split.
                Map<String, Double> invalid = new LinkedHashMap<>();
                invalid.put("A", 50.0);
                invalid.put("X", 50.0);
                assertFalse(SplitStore.saveSplits(db, 1, invalid, SplitCalculator.METHOD_EQUAL));
                assertCustom(db);

                invalid.clear();
                invalid.put("A", 99.0);
                assertFalse(SplitStore.saveSplits(db, 1, invalid, SplitCalculator.METHOD_CUSTOM));
                assertCustom(db);
            }
            try (DatabaseHelper helper = new DatabaseHelper(context, testName)) {
                SQLiteDatabase db = helper.getWritableDatabase();
                assertCustom(db);
                db.execSQL("DELETE FROM expenses WHERE expense_id = 1");
                assertEquals(0L, count(db));
            }
        } finally {
            context.deleteDatabase(testName);
        }
    }

    private static long count(SQLiteDatabase db) {
        return DatabaseUtils.longForQuery(db,
                "SELECT COUNT(*) FROM expense_splits WHERE expense_id = 1", null);
    }

    private static long share(SQLiteDatabase db, String member) {
        return DatabaseUtils.longForQuery(db,
                "SELECT share_satang FROM expense_splits WHERE expense_id = 1 AND member_name = ?",
                new String[]{member});
    }

    private static void assertCustom(SQLiteDatabase db) {
        assertEquals(2L, count(db));
        assertEquals(7000L, share(db, "A"));
        assertEquals(3000L, share(db, "B"));
        assertEquals("CUSTOM", DatabaseUtils.stringForQuery(db,
                "SELECT split_method FROM expenses WHERE expense_id = 1", null));
    }
}
