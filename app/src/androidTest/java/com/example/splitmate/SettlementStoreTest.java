package com.example.splitmate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SettlementStoreTest {
    private static final int GROUP = 1;
    private static final String PENDING = Settlement.STATUS_PENDING;
    private static final String PAID = Settlement.STATUS_PAID;

    private Map<String, Long> net(SQLiteDatabase db) {
        List<Expense> expenses = Arrays.asList(
                new Expense(1, GROUP, "Food", 1000, "A", "Food", "27/11/2026"),
                new Expense(2, GROUP, "Travel", 500, "B", "Travel", "27/11/2026"));
        Map<Integer, List<ExpenseSplit>> splits = new HashMap<>();
        splits.put(1, Arrays.asList(new ExpenseSplit(1, 1, "A", 250),
                new ExpenseSplit(2, 1, "B", 250), new ExpenseSplit(3, 1, "C", 250),
                new ExpenseSplit(4, 1, "D", 250)));
        splits.put(2, Arrays.asList(new ExpenseSplit(5, 2, "B", 250),
                new ExpenseSplit(6, 2, "D", 250)));
        return BalanceCalculator.calculateNetSatang(Arrays.asList("A", "B", "C", "D"),
                expenses, splits, SettlementStore.getSettlements(db, GROUP, PAID));
    }

    private List<Settlement> refresh(SQLiteDatabase db) {
        return SettlementStore.refreshPending(db, GROUP, () -> net(db));
    }

    @Test
    public void paidHistoryRefreshRollbackAndPersistence() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "splitmate_settlements_test.db";
        context.deleteDatabase(name);
        try {
            int paidId;
            try (DatabaseHelper helper = new DatabaseHelper(context, name)) {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.execSQL("INSERT INTO groups(group_id, group_name) VALUES (1,'Test'), (2,'Other')");
                db.execSQL("INSERT INTO members(group_id, member_name) VALUES (1,'A'),(1,'B'),(1,'C'),(1,'D'),(2,'X'),(2,'Y')");
                db.execSQL("INSERT INTO settlements(group_id,from_member,to_member,amount_satang,status) VALUES (2,'X','Y',100,'PENDING')");
                List<Settlement> pending = refresh(db);
                assertEquals(2, pending.size());
                Settlement first = pending.get(0);
                assertEquals("D", first.getFromMember());
                assertEquals("A", first.getToMember());
                assertEquals(500.0, first.getAmount(), 0.001);
                paidId = first.getSettlementId();
                assertTrue(SettlementStore.updateStatus(db, paidId, PAID));
                assertFalse(SettlementStore.updateStatus(db, paidId, "INVALID"));
                pending = refresh(db);
                assertEquals(1, pending.size());
                assertEquals("C", pending.get(0).getFromMember());
                assertEquals(250.0, pending.get(0).getAmount(), 0.001);
                int pendingId = pending.get(0).getSettlementId();
                // First transfer is valid, second references a missing member: rollback both.
                Map<String, Long> invalid = new LinkedHashMap<>();
                invalid.put("A", 300L);
                invalid.put("C", -200L);
                invalid.put("X", -100L);
                try {
                    SettlementStore.refreshPending(db, GROUP, () -> invalid);
                    fail("Expected foreign key violation");
                } catch (SQLiteException expected) {
                    assertEquals(pendingId, SettlementStore.getSettlements(db, GROUP, PENDING).get(0).getSettlementId());
                }
                assertEquals(1, SettlementStore.getSettlements(db, GROUP, PENDING).size());
                assertEquals(paidId, SettlementStore.getSettlements(db, GROUP, PAID).get(0).getSettlementId());
                assertEquals(1, SettlementStore.getSettlements(db, 2, PENDING).size());
            }
            try (DatabaseHelper helper = new DatabaseHelper(context, name)) {
                SQLiteDatabase db = helper.getWritableDatabase();
                assertEquals(paidId, SettlementStore.getSettlements(db, GROUP, PAID).get(0).getSettlementId());
                List<Settlement> pending = refresh(db);
                assertEquals(1, pending.size());
                assertTrue(SettlementStore.updateStatus(db, pending.get(0).getSettlementId(), PAID));
                assertTrue(refresh(db).isEmpty());
                assertTrue(BalanceCalculator.isAllSettled(net(db)));
                assertTrue(SettlementStore.deleteSettlement(db, paidId));
                pending = refresh(db);
                assertEquals(1, pending.size());
                assertEquals("D", pending.get(0).getFromMember());
                assertEquals(500.0, pending.get(0).getAmount(), 0.001);
                db.execSQL("DELETE FROM [groups] WHERE group_id = 1");
                assertTrue(SettlementStore.getSettlements(db, GROUP, PAID).isEmpty());
                assertTrue(SettlementStore.getSettlements(db, GROUP, PENDING).isEmpty());
                assertEquals(1, SettlementStore.getSettlements(db, 2, PENDING).size());
            }
        } finally {
            context.deleteDatabase(name);
        }
    }
}
