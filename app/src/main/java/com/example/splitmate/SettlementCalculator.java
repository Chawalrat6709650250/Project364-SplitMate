package com.example.splitmate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Settlement Algorithm แบบ Greedy:
// จับคู่ "คนที่ติดหนี้มากที่สุด" กับ "คนที่ต้องได้รับเงินมากที่สุด" ทีละคู่
// ทุกรอบจะมีอย่างน้อย 1 คนที่ยอดเป็นศูนย์ จึงได้ไม่เกิน (จำนวนคน - 1) รายการ
public final class SettlementCalculator {

    private SettlementCalculator() {
    }

    public static class Transfer {
        public final String from;
        public final String to;
        public final long amountSatang;

        Transfer(String from, String to, long amountSatang) {
            this.from = from;
            this.to = to;
            this.amountSatang = amountSatang;
        }

        public double getAmount() {
            return SplitCalculator.toBaht(amountSatang);
        }
    }

    public static List<Transfer> suggest(Map<String, Long> netSatang) {
        List<Transfer> result = new ArrayList<>();
        if (netSatang == null) {
            return result;
        }

        List<String> names = new ArrayList<>();
        List<Long> balances = new ArrayList<>();
        for (Map.Entry<String, Long> entry : netSatang.entrySet()) {
            if (entry.getValue() != null && entry.getValue() != 0) {
                names.add(entry.getKey());
                balances.add(entry.getValue());
            }
        }

        while (true) {
            int debtor = -1;
            int creditor = -1;
            for (int i = 0; i < balances.size(); i++) {
                long value = balances.get(i);
                if (value < 0 && (debtor == -1 || value < balances.get(debtor))) {
                    debtor = i;
                }
                if (value > 0 && (creditor == -1 || value > balances.get(creditor))) {
                    creditor = i;
                }
            }

            if (debtor == -1 || creditor == -1) {
                break;
            }

            long amount = Math.min(-balances.get(debtor), balances.get(creditor));
            result.add(new Transfer(names.get(debtor), names.get(creditor), amount));
            balances.set(debtor, balances.get(debtor) + amount);
            balances.set(creditor, balances.get(creditor) - amount);
        }

        return result;
    }
}
