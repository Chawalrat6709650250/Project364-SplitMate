
package com.example.splitmate;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

    private boolean isEditMode;
    private int expenseId;

    private ArrayList<String> members;

    private final String[] categories = {
            "อาหาร",
            "การเดินทาง",
            "ที่พัก",
            "ช้อปปิ้ง",
            "บันเทิง",
            "อื่น ๆ"
    };

    private EditText etExpenseTitle;
    private EditText etExpenseAmount;

    private Spinner spinnerCategory;
    private Spinner spinnerPaidBy;

    private TextView tvGroupName;
    private TextView tvExpenseDate;

    private View btnBack;
    private Button btnSelectDate;
    private Button btnSaveExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Receive group information
        groupId = getIntent().getIntExtra("group_id", -1);
        groupName = getIntent().getStringExtra("group_name");

        // Receive Edit Mode information
        isEditMode = getIntent().getBooleanExtra("is_edit", false);
        expenseId = getIntent().getIntExtra("expense_id", -1);

        if (groupId == -1) {
            Toast.makeText(this,
                    "ไม่พบข้อมูลกลุ่ม",
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
        tvGroupName.setText("กลุ่ม: " + groupName);

        // Setup category and member lists
        setupCategorySpinner();
        setupMemberSpinner();

        // Set today's date
        setTodayDate();

        // Load existing expense in Edit Mode
        if (isEditMode) {
            loadExpenseForEdit();
            btnSaveExpense.setText("บันทึกการแก้ไข");
        }

        // Button listeners
        btnBack.setOnClickListener(v -> finish());

        btnSelectDate.setOnClickListener(v -> showDatePicker());

        btnSaveExpense.setOnClickListener(v -> saveExpense());
    }

    // Setup category options
    private void setupCategorySpinner() {

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

        members = GroupStore.getMembers(groupId);

        ArrayList<String> memberOptions = new ArrayList<>();

        memberOptions.add("เลือกผู้จ่าย");
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

    // Load existing expense data for editing
    private void loadExpenseForEdit() {

        Expense existingExpense = null;

        ArrayList<Expense> expenseList =
                ExpenseStore.getExpenses(groupId);

        for (Expense expense : expenseList) {

            if (expense.getExpenseId() == expenseId) {
                existingExpense = expense;
                break;
            }
        }

        if (existingExpense == null) {

            Toast.makeText(this,
                    "ไม่พบรายการค่าใช้จ่าย",
                    Toast.LENGTH_SHORT).show();

            finish();
            return;
        }

        // Fill existing expense information
        etExpenseTitle.setText(existingExpense.getTitle());

        etExpenseAmount.setText(
                String.valueOf(existingExpense.getAmount())
        );

        selectedDate = existingExpense.getExpenseDate();
        tvExpenseDate.setText(selectedDate);

        // Select existing category
        for (int i = 0; i < categories.length; i++) {

            if (categories[i].equals(
                    existingExpense.getCategory())) {

                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Select existing payer
        int memberPosition = members.indexOf(
                existingExpense.getPaidBy()
        );

        if (memberPosition >= 0) {
            spinnerPaidBy.setSelection(memberPosition + 1);
        }
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
            etExpenseTitle.setError("กรุณากรอกชื่อรายการ");
            etExpenseTitle.requestFocus();
            return;
        }

        // Validate amount
        if (amountText.isEmpty()) {
            etExpenseAmount.setError("กรุณากรอกจำนวนเงิน");
            etExpenseAmount.requestFocus();
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            etExpenseAmount.setError("จำนวนเงินไม่ถูกต้อง");
            etExpenseAmount.requestFocus();
            return;
        }

        if (amount <= 0
                || Double.isInfinite(amount)
                || Double.isNaN(amount)) {

            etExpenseAmount.setError(
                    "จำนวนเงินต้องมากกว่า 0"
            );

            etExpenseAmount.requestFocus();
            return;
        }

        // Validate paid by
        if (spinnerPaidBy.getSelectedItemPosition() == 0) {

            Toast.makeText(this,
                    "กรุณาเลือกผู้จ่าย",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        String category =
                spinnerCategory.getSelectedItem().toString();

        String paidBy =
                spinnerPaidBy.getSelectedItem().toString();

        if (isEditMode) {

            // Update existing expense
            Expense updatedExpense = new Expense(
                    expenseId,
                    groupId,
                    title,
                    amount,
                    paidBy,
                    category,
                    selectedDate
            );

            ExpenseStore.updateExpense(updatedExpense);

            Toast.makeText(this,
                    "แก้ไขค่าใช้จ่ายแล้ว",
                    Toast.LENGTH_SHORT).show();

            if (!SplitStore.isSplitValid(updatedExpense)) {
                openSplitScreen(expenseId);
            }

        } else {

            // Add new expense
            int newExpenseId = ExpenseStore.addExpense(
                    groupId,
                    title,
                    amount,
                    paidBy,
                    category,
                    selectedDate
            );

            Toast.makeText(this,
                    "บันทึกค่าใช้จ่ายแล้ว",
                    Toast.LENGTH_SHORT).show();

            openSplitScreen(newExpenseId);
        }

        // Return to expense list
        finish();
    }

    private void openSplitScreen(int id) {
        Intent intent = new Intent(this, SplitActivity.class);
        intent.putExtra("group_id", groupId);
        intent.putExtra("group_name", groupName);
        intent.putExtra("expense_id", id);
        startActivity(intent);
    }
}
