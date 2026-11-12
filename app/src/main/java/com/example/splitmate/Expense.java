
package com.example.splitmate;

public class Expense {

    private int expenseId;
    private int groupId;
    private String title;
    private double amount;
    private String paidBy;
    private String category;
    private String expenseDate;

    public Expense(int expenseId, int groupId,
                   String title, double amount,
                   String paidBy, String category,
                   String expenseDate) {

        this.expenseId = expenseId;
        this.groupId = groupId;
        this.title = title;
        this.amount = amount;
        this.paidBy = paidBy;
        this.category = category;
        this.expenseDate = expenseDate;
    }

    public int getExpenseId() {
        return expenseId;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getTitle() {
        return title;
    }

    public double getAmount() {
        return amount;
    }

    public String getPaidBy() {
        return paidBy;
    }

    public String getCategory() {
        return category;
    }

    public String getExpenseDate() {
        return expenseDate;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setPaidBy(String paidBy) {
        this.paidBy = paidBy;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setExpenseDate(String expenseDate) {
        this.expenseDate = expenseDate;
    }
}
