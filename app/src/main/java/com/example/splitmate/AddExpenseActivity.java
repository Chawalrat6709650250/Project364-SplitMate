package com.example.splitmate;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    private int groupId;
    private String groupName;
    private String selectedDate;

    private EditText etExpenseTitle;
    private EditText etExpenseAmount;

    private Spinner spinnerCategory;
    private Spinner spinnerPaidBy;

    private TextView tvGroupName;
    private TextView tvExpenseDate;

    private Button btnBack;
    private Button btnSelectDate;
    private Button btnSaveExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

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
        etExpenseTitle = findViewById(R.id.et_expense_title);
        etExpenseAmount = findViewById(R.id.et_expense_amount);

        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerPaidBy = findViewById(R.id.spinner_paid_by);

        tvGroupName = findViewById(R.id.tv_add_group_name);
        tvExpenseDate = findViewById(R.id.tv_expense_date);

        btnBack = findViewById(R.id.btn_back_add);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSaveExpense = findViewById(R.id.btn_save_expense);

        // Display group name
        tvGroupName.setText("Group: " + groupName);

        // Setup category and member lists
        setupCategorySpinner();
        setupMemberSpinner();

        // Set today's date
        setTodayDate();

        // Button listeners
        btnBack.setOnClickListener(v -> finish());

        btnSelectDate.setOnClickListener(v -> showDatePicker());

        btnSaveExpense.setOnClickListener(v -> saveExpense());
    }

    // Setup category options
    private void setupCategorySpinner() {

        String[] categories = {
                "Food",
                "Transportation",
                "Accommodation",
                "Shopping",
                "Entertainment",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerCategory.setAdapter(adapter);
    }

    // Load members from selected group
    private void setupMemberSpinner() {

        ArrayList<String> members =
                GroupStore.getMembers(groupId);

        ArrayList<String> memberOptions = new ArrayList<>();

        memberOptions.add("Select member");
        memberOptions.addAll(members);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                memberOptions
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerPaidBy.setAdapter(adapter);
    }

    // Set today's date
    private void setTodayDate() {

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                );

        selectedDate = dateFormat.format(calendar.getTime());

        tvExpenseDate.setText(selectedDate);
    }

    // Show date picker
    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            Calendar selected = Calendar.getInstance();

                            selected.set(
                                    year,
                                    month,
                                    dayOfMonth
                            );

                            SimpleDateFormat dateFormat =
                                    new SimpleDateFormat(
                                            "dd/MM/yyyy",
                                            Locale.getDefault()
                                    );

                            selectedDate =
                                    dateFormat.format(selected.getTime());

                            tvExpenseDate.setText(selectedDate);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        datePickerDialog.show();
    }

    // Validate and save expense
    private void saveExpense() {

        String title =
                etExpenseTitle.getText().toString().trim();

        String amountText =
                etExpenseAmount.getText().toString().trim();

        // Validate title
        if (title.isEmpty()) {
            etExpenseTitle.setError(
                    "Please enter expense name"
            );
            etExpenseTitle.requestFocus();
            return;
        }

        // Validate amount
        if (amountText.isEmpty()) {
            etExpenseAmount.setError(
                    "Please enter amount"
            );
            etExpenseAmount.requestFocus();
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            etExpenseAmount.setError("Invalid amount");
            etExpenseAmount.requestFocus();
            return;
        }

        if (amount <= 0
                || Double.isInfinite(amount)
                || Double.isNaN(amount)) {

            etExpenseAmount.setError(
                    "Amount must be greater than zero"
            );
            etExpenseAmount.requestFocus();
            return;
        }

        // Validate paid by
        if (spinnerPaidBy.getSelectedItemPosition() == 0) {
            Toast.makeText(this,
                    "Please select who paid",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String category =
                spinnerCategory.getSelectedItem().toString();

        String paidBy =
                spinnerPaidBy.getSelectedItem().toString();

        // Save expense
        ExpenseStore.addExpense(
                groupId,
                title,
                amount,
                paidBy,
                category,
                selectedDate
        );

        Toast.makeText(this,
                "Expense saved successfully",
                Toast.LENGTH_SHORT).show();

        // Return to expense list
        finish();
    }
}
