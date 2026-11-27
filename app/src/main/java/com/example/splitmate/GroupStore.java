package com.example.splitmate;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;

public class GroupStore {

    public static class Group {
        public final int id;
        public String name;
        public final ArrayList<String> members;

        public Group(int id, String name) {
            this.id = id;
            this.name = name;
            this.members = new ArrayList<>();
        }
    }

    private static SQLiteDatabase db() {
        return SplitMateApplication.getDatabase();
    }

    public static int addGroup(String name) {
        if (name == null || name.trim().isEmpty()) return -1;
        ContentValues values = new ContentValues();
        values.put("group_name", name.trim());
        try {
            return (int) db().insertOrThrow("groups", null, values);
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot create group", e);
            return -1;
        }
    }

    public static ArrayList<Group> getGroups() {
        ArrayList<Group> result = new ArrayList<>();
        try (Cursor cursor = db().query("groups",
                new String[]{"group_id", "group_name"},
                null, null, null, null, "group_id ASC")) {
            while (cursor.moveToNext()) {
                Group group = new Group(cursor.getInt(0), cursor.getString(1));
                group.members.addAll(getMembers(group.id));
                result.add(group);
            }
        }
        return result;
    }

    public static Group getGroup(int id) {
        try (Cursor cursor = db().query("groups",
                new String[]{"group_id", "group_name"},
                "group_id = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            if (!cursor.moveToFirst()) return null;
            Group group = new Group(cursor.getInt(0), cursor.getString(1));
            group.members.addAll(getMembers(id));
            return group;
        }
    }

    public static boolean updateGroup(int id, String newName) {
        if (newName == null || newName.trim().isEmpty()) return false;
        ContentValues values = new ContentValues();
        values.put("group_name", newName.trim());
        try {
            return db().update("groups", values, "group_id = ?",
                    new String[]{String.valueOf(id)}) > 0;
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot update group", e);
            return false;
        }
    }

    public static boolean deleteGroup(int groupId) {
        try {
            return db().delete("groups", "group_id = ?",
                    new String[]{String.valueOf(groupId)}) > 0;
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot delete group", e);
            return false;
        }
    }

    public static boolean addMember(int groupId, String memberName) {
        if (memberName == null || memberName.trim().isEmpty()) return false;
        ContentValues values = new ContentValues();
        values.put("group_id", groupId);
        values.put("member_name", memberName.trim());
        try {
            db().insertOrThrow("members", null, values);
            return true;
        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot add member", e);
            return false;
        }
    }

    public static ArrayList<String> getMembers(int groupId) {
        ArrayList<String> result = new ArrayList<>();
        try (Cursor cursor = db().query("members",
                new String[]{"member_name"}, "group_id = ?",
                new String[]{String.valueOf(groupId)},
                null, null, "member_id ASC")) {
            while (cursor.moveToNext()) {
                result.add(cursor.getString(0));
            }
        }
        return result;
    }

    public static boolean removeMember(int groupId, String memberName) {

        if (memberName == null || memberName.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase database = db();
        database.beginTransaction();

        try {
            // ตรวจสอบประวัติค่าใช้จ่าย
            if (SplitStore.countExpensesInvolving(
                    groupId, memberName) > 0) {
                return false;
            }

            // ตรวจสอบประวัติการชำระเงิน
            for (Settlement settlement :
                    SettlementStore.getSettlements(
                            groupId, Settlement.STATUS_PAID)) {

                if (memberName.equals(settlement.getFromMember())
                        || memberName.equals(settlement.getToMember())) {
                    return false;
                }
            }

            // ลบรายการ PENDING เก่าที่เกี่ยวข้อง
            database.delete(
                    "settlements",
                    "group_id = ? AND status = ? AND "
                            + "(from_member = ? OR to_member = ?)",
                    new String[]{
                            String.valueOf(groupId),
                            Settlement.STATUS_PENDING,
                            memberName,
                            memberName
                    }
            );

            int deleted = database.delete(
                    "members",
                    "group_id = ? AND member_name = ?",
                    new String[]{
                            String.valueOf(groupId),
                            memberName
                    }
            );

            if (deleted != 1) {
                return false;
            }

            database.setTransactionSuccessful();
            return true;

        } catch (SQLiteException e) {
            Log.e("SplitMateDB", "Cannot remove member", e);
            return false;

        } finally {
            database.endTransaction();
        }
    }
}
