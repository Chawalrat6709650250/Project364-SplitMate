package com.example.splitmate;

public class Settlement {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";

    private int settlementId;
    private int groupId;
    private String fromMember;
    private String toMember;
    private double amount;
    private String status;

    public Settlement(int settlementId, int groupId,
                      String fromMember, String toMember,
                      double amount, String status) {
        this.settlementId = settlementId;
        this.groupId = groupId;
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.amount = amount;
        this.status = status;
    }

    public int getSettlementId() {
        return settlementId;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getFromMember() {
        return fromMember;
    }

    public String getToMember() {
        return toMember;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public boolean isPaid() {
        return STATUS_PAID.equals(status);
    }

    public void setSettlementId(int settlementId) {
        this.settlementId = settlementId;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
