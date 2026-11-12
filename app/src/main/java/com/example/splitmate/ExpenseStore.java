
package com.example.splitmate;

import java.util.ArrayList;
import java.util.Iterator;

public class ExpenseStore {

    private static final ArrayList<Expense> expenses =
            new ArrayList<>();

    private static int nextId = 1;

    // Add a new expense
    public static void addExpense(
            int groupId,
            String title,
            double amount,
            String paidBy,
            String category,
            String expenseDate) {

        Expense expense = new Expense(
                nextId++,
                groupId,
                title,
                amount,
                paidBy,
                category,
                expenseDate
        );

        expenses.add(expense);
    }

    // Get expenses belonging to a specific group
    public static ArrayList<Expense> getExpenses(int groupId) {

        ArrayList<Expense> result = new ArrayList<>();

        for (Expense expense : expenses) {
            if (expense.getGroupId() == groupId) {
                result.add(expense);
            }
        }

        return result;
    }

    // Update an existing expense
    public static void updateExpense(Expense updatedExpense) {

        for (int i = 0; i < expenses.size(); i++) {

            if (expenses.get(i).getExpenseId()
                    == updatedExpense.getExpenseId()) {

                expenses.set(i, updatedExpense);
                return;
            }
        }
    }

    // Delete an expense by ID
    public static void deleteExpense(int expenseId) {

        Iterator<Expense> iterator = expenses.iterator();

        while (iterator.hasNext()) {

            Expense expense = iterator.next();

            if (expense.getExpenseId() == expenseId) {
                iterator.remove();
                return;
            }
        }
    }

    // Calculate total expenses for a group
    public static double getTotalExpense(int groupId) {

        double total = 0;

        for (Expense expense : expenses) {

            if (expense.getGroupId() == groupId) {
                total += expense.getAmount();
            }
        }

        return total;
    }
}
