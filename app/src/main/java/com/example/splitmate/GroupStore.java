
package com.example.splitmate;

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

    private static final ArrayList<Group> groups =
            new ArrayList<>();

    private static int nextId = 1;

    // สร้างกลุ่ม
    public static int addGroup(String name) {
        int id = nextId++;
        groups.add(new Group(id, name));
        return id;
    }

    // อ่านกลุ่มทั้งหมด
    public static ArrayList<Group> getGroups() {
        return new ArrayList<>(groups);
    }

    // ค้นหากลุ่มตาม ID
    public static Group getGroup(int id) {
        for (Group group : groups) {
            if (group.id == id) {
                return group;
            }
        }
        return null;
    }

    // แก้ไขชื่อกลุ่ม
    public static boolean updateGroup(int id, String newName) {
        Group group = getGroup(id);

        if (group == null || newName == null
                || newName.trim().isEmpty()) {
            return false;
        }

        group.name = newName.trim();
        return true;
    }

    // ลบกลุ่ม
    public static boolean deleteGroup(int groupId) {
        return groups.removeIf(group -> group.id == groupId);
    }

    // เพิ่มสมาชิกในกลุ่ม
    public static boolean addMember(int groupId, String memberName) {
        Group group = getGroup(groupId);

        if (group == null || memberName == null
                || memberName.trim().isEmpty()) {
            return false;
        }

        group.members.add(memberName.trim());
        return true;
    }

    // อ่านสมาชิกในกลุ่ม
    public static ArrayList<String> getMembers(int groupId) {
        Group group = getGroup(groupId);

        if (group == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(group.members);
    }

    // ลบสมาชิกในกลุ่ม
    public static boolean removeMember(
            int groupId, String memberName) {

        Group group = getGroup(groupId);

        if (group == null) {
            return false;
        }

        return group.members.remove(memberName);
    }
}
