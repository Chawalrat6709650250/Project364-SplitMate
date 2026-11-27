package com.example.splitmate;

import java.util.ArrayList;
import java.util.List;

// เก็บรายการคืนเงิน (ตาราง Settlements) ไว้ในหน่วยความจำ
// เมื่อ Person 5 ทำ DatabaseHelper เสร็จ ให้เปลี่ยนเมธอดในคลาสนี้ไปเรียก SQLite แทน
public class SettlementStore {

    private static final ArrayList<Settlement> settlements = new ArrayList<>();

    private static int nextId = 1;

    public static int addSettlement(int groupId, String from, String to,
                                    double amount, String status) {
        Settlement settlement = new Settlement(
                nextId++, groupId, from, to, amount, status);
        settlements.add(settlement);
        return settlement.getSettlementId();
    }

    public static ArrayList<Settlement> getSettlements(int groupId) {
        ArrayList<Settlement> result = new ArrayList<>();
        for (Settlement settlement : settlements) {
            if (settlement.getGroupId() == groupId) {
                result.add(settlement);
            }
        }
        return result;
    }

    public static ArrayList<Settlement> getSettlements(int groupId, String status) {
        ArrayList<Settlement> result = new ArrayList<>();
        for (Settlement settlement : getSettlements(groupId)) {
            if (settlement.getStatus().equals(status)) {
                result.add(settlement);
            }
        }
        return result;
    }

    public static Settlement getSettlement(int settlementId) {
        for (Settlement settlement : settlements) {
            if (settlement.getSettlementId() == settlementId) {
                return settlement;
            }
        }
        return null;
    }

    public static boolean updateStatus(int settlementId, String status) {
        Settlement settlement = getSettlement(settlementId);
        if (settlement == null) {
            return false;
        }
        settlement.setStatus(status);
        return true;
    }

    public static boolean deleteSettlement(int settlementId) {
        return settlements.removeIf(s -> s.getSettlementId() == settlementId);
    }

    public static void deleteSettlementsForGroup(int groupId) {
        settlements.removeIf(s -> s.getGroupId() == groupId);
    }

    // คำนวณรายการแนะนำใหม่ทุกครั้งที่ยอดเปลี่ยน (เช่น แก้ไขค่าใช้จ่าย)
    // ลบเฉพาะรายการ PENDING เดิมทิ้ง ส่วนรายการ PAID เก็บไว้เป็นประวัติและนำมาคิดยอดคงเหลือ
    public static ArrayList<Settlement> refreshPending(int groupId) {
        settlements.removeIf(s -> s.getGroupId() == groupId && !s.isPaid());

        List<SettlementCalculator.Transfer> transfers =
                SettlementCalculator.suggest(BalanceCalculator.calculateNetSatang(groupId));

        for (SettlementCalculator.Transfer transfer : transfers) {
            addSettlement(groupId, transfer.from, transfer.to,
                    SplitCalculator.toBaht(transfer.amountSatang),
                    Settlement.STATUS_PENDING);
        }

        return getSettlements(groupId, Settlement.STATUS_PENDING);
    }

    static void clearAll() {
        settlements.clear();
        nextId = 1;
    }
}
