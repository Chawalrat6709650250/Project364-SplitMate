package com.example.splitmate;

public class ExpenseSplit {

    private int splitId;
    private int expenseId;
    private String memberName;
    private double shareAmount;

    public ExpenseSplit(int splitId, int expenseId,
                        String memberName, double shareAmount) {
        this.splitId = splitId;
        this.expenseId = expenseId;
        this.memberName = memberName;
        this.shareAmount = shareAmount;
    }

    public int getSplitId() {
        return splitId;
    }

    public int getExpenseId() {
        return expenseId;
    }

    public String getMemberName() {
        return memberName;
    }

    public double getShareAmount() {
        return shareAmount;
    }

    public void setSplitId(int splitId) {
        this.splitId = splitId;
    }

    public void setShareAmount(double shareAmount) {
        this.shareAmount = shareAmount;
    }
}
