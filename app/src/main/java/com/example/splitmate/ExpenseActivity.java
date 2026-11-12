package com.example.splitmate;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class ExpenseActivity extends AppCompatActivity {

    private int groupId;
    private String groupName;

    private TextView tvGroupName;
    private TextView tvTotalExpense;
    private TextView tvEmptyExpense;

    private LinearLayout expenseListContainer;

    private Button btnBack;
    private Button btnAddExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_expense);

        // Receive group information
        groupId = getIntent().getIntExtra("group_id", -1);
        groupName = getIntent().getStringExtra("group_name");

        if (groupId == -1) {
            Toast.makeText(this,
                    "Invalid group information",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        tvGroupName = findViewById(R.id.tv_group_name);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvEmptyExpense = findViewById(R.id.tv_empty_expense);

        expenseListContainer =
                findViewById(R.id.expense_list_container);

        btnBack = findViewById(R.id.btn_back);
        btnAddExpense = findViewById(R.id.btn_add_expense);

        // Display group name
        if (groupName != null) {
            tvGroupName.setText(groupName);
        }

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Add expense button
        btnAddExpense.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ExpenseActivity.this,
                    AddExpenseActivity.class
            );

            intent.putExtra("group_id", groupId);
            intent.putExtra("group_name", groupName);

            startActivity(intent);
        });

        // Load expenses
        loadExpenses();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (expenseListContainer != null) {
            loadExpenses();
        }
    }

    // Load expenses belonging to the selected group
    private void loadExpenses() {

        expenseListContainer.removeAllViews();

        ArrayList<Expense> expenseList =
                ExpenseStore.getExpenses(groupId);

        double total =
                ExpenseStore.getTotalExpense(groupId);

        tvTotalExpense.setText(
                String.format(Locale.getDefault(),
                        "%.2f THB", total)
        );

        if (expenseList.isEmpty()) {
            tvEmptyExpense.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyExpense.setVisibility(View.GONE);

        for (Expense expense : expenseList) {
            addExpenseItem(expense);
        }
    }

    // Display one expense item
    private void addExpenseItem(Expense expense) {

        LinearLayout item = new LinearLayout(this);

        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(24, 20, 24, 20);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 12, 0, 0);

        item.setLayoutParams(params);
        item.setBackgroundResource(
                android.R.drawable.dialog_holo_light_frame
        );

        // Expense title
        TextView title = new TextView(this);

        title.setText(expense.getTitle());
        title.setTextSize(16);
        title.setTextColor(0xFF20243A);

        item.addView(title);

        // Paid by
        TextView paidBy = new TextView(this);

        paidBy.setText("Paid by: " + expense.getPaidBy());
        paidBy.setTextSize(14);
        paidBy.setTextColor(0xFF777777);

        item.addView(paidBy);

        // Category and date
        TextView details = new TextView(this);

        details.setText(
                expense.getCategory()
                        + " • "
                        + expense.getExpenseDate()
        );

        details.setTextSize(12);
        details.setTextColor(0xFF888888);

        item.addView(details);

        // Amount
        TextView amount = new TextView(this);

        amount.setText(
                String.format(Locale.getDefault(),
                        "%.2f THB", expense.getAmount())
        );

        amount.setTextSize(18);
        amount.setTextColor(0xFF5145CD);

        item.addView(amount);

        expenseListContainer.addView(item);
    }
}
