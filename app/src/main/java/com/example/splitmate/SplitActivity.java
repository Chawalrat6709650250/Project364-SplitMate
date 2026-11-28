package com.example.splitmate;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SplitActivity extends AppCompatActivity {

    private int groupId;
    private int expenseId;
    private String groupName;
    private Expense expense;

    private RadioGroup rgSplitMethod;
    private RadioButton rbEqual;
    private RadioButton rbCustom;
    private LinearLayout memberContainer;
    private TextView tvSummary;
    private TextView tvNotice;
    private TextView tvEmpty;
    private Button btnSave;
    private Button btnSelectAll;

    private final List<MemberRow> rows = new ArrayList<>();

    private boolean isUpdatingProgrammatically = false;

    private static class MemberRow {
        final String name;
        final CheckBox checkBox;
        final EditText etAmount;

        MemberRow(String name, CheckBox checkBox, EditText etAmount) {
            this.name = name;
            this.checkBox = checkBox;
            this.etAmount = etAmount;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split);

        groupId = getIntent().getIntExtra("group_id", -1);
        expenseId = getIntent().getIntExtra("expense_id", -1);
        groupName = getIntent().getStringExtra("group_name");

        expense = findExpense(groupId, expenseId);

        if (groupId == -1 || expense == null) {
            Toast.makeText(this, "ไม่พบรายการค่าใช้จ่าย", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rgSplitMethod = findViewById(R.id.rg_split_method);
        rbEqual = findViewById(R.id.rb_equal);
        rbCustom = findViewById(R.id.rb_custom);
        memberContainer = findViewById(R.id.split_member_container);
        tvSummary = findViewById(R.id.tv_split_summary);
        tvNotice = findViewById(R.id.tv_split_notice);
        tvEmpty = findViewById(R.id.tv_split_empty);
        btnSave = findViewById(R.id.btn_save_split);
        btnSelectAll = findViewById(R.id.btn_select_all);

        TextView tvGroupName = findViewById(R.id.tv_split_group_name);
        TextView tvTitle = findViewById(R.id.tv_split_expense_title);
        TextView tvAmount = findViewById(R.id.tv_split_expense_amount);
        TextView tvPaidBy = findViewById(R.id.tv_split_paid_by);

        tvGroupName.setText("กลุ่ม: " + (groupName != null ? groupName : ""));
        tvTitle.setText(expense.getTitle());
        tvAmount.setText(formatBaht(expense.getAmount()));
        tvPaidBy.setText("ผู้จ่าย: " + expense.getPaidBy());

        findViewById(R.id.btn_back_split).setOnClickListener(v -> confirmLeave());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmLeave();
            }
        });

        buildMemberRows();
        loadExistingSplits();

        rgSplitMethod.setOnCheckedChangeListener((group, checkedId) -> onMethodChanged());
        btnSelectAll.setOnClickListener(v -> toggleSelectAll());
        btnSave.setOnClickListener(v -> saveSplit());

        onMethodChanged();
        if (savedInstanceState != null) {
            boolean custom = savedInstanceState.getBoolean("draft_custom_split", false);
            isUpdatingProgrammatically = true;
            if (custom) rbCustom.setChecked(true); else rbEqual.setChecked(true);
            for (int i = 0; i < rows.size(); i++) {
                MemberRow row = rows.get(i);
                row.checkBox.setChecked(savedInstanceState.getBoolean("draft_checked_" + i, true));
                row.etAmount.setText(savedInstanceState.getString("draft_amount_" + i, ""));
            }
            isUpdatingProgrammatically = false;
            onMethodChanged();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("draft_custom_split", rbCustom != null && rbCustom.isChecked());
        for (int i = 0; i < rows.size(); i++) {
            MemberRow row = rows.get(i);
            outState.putBoolean("draft_checked_" + i, row.checkBox.isChecked());
            outState.putString("draft_amount_" + i, row.etAmount.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    private Expense findExpense(int groupId, int expenseId) {
        for (Expense e : ExpenseStore.getExpenses(groupId)) {
            if (e.getExpenseId() == expenseId) {
                return e;
            }
        }
        return null;
    }

    private void buildMemberRows() {
        memberContainer.removeAllViews();
        rows.clear();

        ArrayList<String> members = GroupStore.getMembers(groupId);

        if (members.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            btnSelectAll.setEnabled(false);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        for (String name : members) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(16, 8, 16, 8);
            row.setBackgroundColor(getColor(R.color.surface_bg));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 8, 0, 0);
            row.setLayoutParams(rowParams);

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(name);
            checkBox.setTextSize(16);
            checkBox.setTextColor(getColor(R.color.text_primary));
            checkBox.setChecked(true); // ค่าเริ่มต้น: ทุกคนร่วมหาร

            EditText etAmount = new EditText(this);
            etAmount.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etAmount.setHint("0.00");
            etAmount.setSingleLine(true);
            etAmount.setGravity(Gravity.END);
            etAmount.setEms(5);

            TextView tvUnit = new TextView(this);
            tvUnit.setText(UiText.tr(this, " บาท", " THB"));
            tvUnit.setTextColor(getColor(R.color.text_secondary));

            row.addView(checkBox, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row.addView(etAmount);
            row.addView(tvUnit);

            MemberRow memberRow = new MemberRow(name, checkBox, etAmount);
            rows.add(memberRow);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    setAmountText(memberRow, "");
                }
                refresh();
            });

            etAmount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isUpdatingProgrammatically) {
                        return;
                    }
                    // พิมพ์เงินให้ใคร ถือว่าคนนั้นร่วมหารอัตโนมัติ
                    if (s.length() > 0 && !memberRow.checkBox.isChecked()) {
                        memberRow.checkBox.setChecked(true);
                    }
                    updateSummary();
                }
            });

            memberContainer.addView(row);
        }
    }

    private void loadExistingSplits() {
        ArrayList<ExpenseSplit> saved = SplitStore.getSplits(expenseId);

        if (saved.isEmpty()) {
            return;
        }

        Map<String, Double> savedShares = new HashMap<>();
        for (ExpenseSplit split : saved) {
            savedShares.put(split.getMemberName(), split.getShareAmount());
        }

        List<String> notices = new ArrayList<>();

        if (!SplitStore.isSplitValid(expense)) {
            notices.add(String.format(Locale.US,
                    "จำนวนเงินถูกแก้ไข ยอดที่หารไว้เดิมรวม %.2f บาท "
                            + "กรุณาหารใหม่",
                    SplitCalculator.sum(savedShares)));
        }

        List<String> removed = new ArrayList<>();
        for (String name : savedShares.keySet()) {
            if (!GroupStore.getMembers(groupId).contains(name)) {
                removed.add(name);
            }
        }
        if (!removed.isEmpty()) {
            notices.add("นำสมาชิกที่ไม่อยู่ในกลุ่มแล้วออก: "
                    + String.join(", ", removed));
        }

        String method = SplitStore.getSplitMethod(expenseId);
        isUpdatingProgrammatically = true;
        if (SplitCalculator.METHOD_CUSTOM.equals(method)) {
            rbCustom.setChecked(true);
        } else {
            rbEqual.setChecked(true);
        }

        for (MemberRow row : rows) {
            Double share = savedShares.get(row.name);
            row.checkBox.setChecked(share != null);
            row.etAmount.setText(share != null ? formatPlain(share) : "");
        }
        isUpdatingProgrammatically = false;

        if (!notices.isEmpty()) {
            tvNotice.setText(String.join("\n", notices));
            tvNotice.setVisibility(View.VISIBLE);
        }
    }

    private boolean isEqualMode() {
        return rgSplitMethod.getCheckedRadioButtonId() == R.id.rb_equal;
    }

    private void onMethodChanged() {
        boolean equal = isEqualMode();

        for (MemberRow row : rows) {
            // โหมดหารเท่ากัน: ช่องเงินแก้ไม่ได้ ระบบคำนวณให้
            row.etAmount.setEnabled(!equal);
            row.etAmount.setFocusable(!equal);
            row.etAmount.setFocusableInTouchMode(!equal);
        }

        refresh();
    }

    private void refresh() {
        if (isEqualMode()) {
            fillEqualAmounts();
        }
        updateSummary();
    }

    private void fillEqualAmounts() {
        List<String> selected = new ArrayList<>();
        for (MemberRow row : rows) {
            if (row.checkBox.isChecked()) {
                selected.add(row.name);
            }
        }

        Map<String, Double> shares =
                SplitCalculator.equalSplit(expense.getAmount(), selected);

        for (MemberRow row : rows) {
            Double share = shares.get(row.name);
            setAmountText(row, share != null ? formatPlain(share) : "");
        }
    }

    private void setAmountText(MemberRow row, String text) {
        isUpdatingProgrammatically = true;
        row.etAmount.setText(text);
        row.etAmount.setError(null);
        isUpdatingProgrammatically = false;
    }

    private void toggleSelectAll() {
        boolean allChecked = true;
        for (MemberRow row : rows) {
            if (!row.checkBox.isChecked()) {
                allChecked = false;
                break;
            }
        }

        for (MemberRow row : rows) {
            row.checkBox.setChecked(!allChecked);
        }
        refresh();
    }

    private void updateSummary() {
        Map<String, Double> shares = collectShares(false);
        double total = SplitCalculator.sum(shares);
        double remaining = SplitCalculator.remaining(expense.getAmount(), shares);

        String text = String.format(Locale.US,
                "รวม %.2f / %.2f บาท", total, expense.getAmount());

        int color;
        if (SplitCalculator.toSatang(remaining) == 0 && !shares.isEmpty()) {
            text += "  ✓";
            color = 0xFF15803D; // เขียว
        } else if (remaining > 0) {
            text += String.format(Locale.US, "\nShort by %.2f THB", remaining);
            color = 0xFFB45309; // ส้ม
        } else {
            text += String.format(Locale.US, "\nOver by %.2f THB", -remaining);
            color = 0xFFDC2626; // แดง
        }

        tvSummary.setText(text);
        tvSummary.setTextColor(color);
    }

    private Map<String, Double> collectShares(boolean showErrors) {
        Map<String, Double> shares = new LinkedHashMap<>();

        for (MemberRow row : rows) {
            if (!row.checkBox.isChecked()) {
                continue;
            }

            String text = row.etAmount.getText().toString().trim();
            Double value = null;

            if (!text.isEmpty()) {
                try {
                    value = Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    value = null;
                }
            }

            if (showErrors) {
                if (value == null) {
                    row.etAmount.setError("กรอกจำนวนเงิน");
                } else if (SplitCalculator.toSatang(value) <= 0) {
                    row.etAmount.setError("ต้องมากกว่า 0");
                }
            }

            shares.put(row.name, value);
        }

        return shares;
    }

    private void saveSplit() {
        Map<String, Double> shares = collectShares(true);

        SplitCalculator.ValidationResult result =
                SplitCalculator.validateSplit(expense.getAmount(), shares);

        if (!result.valid) {
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            return;
        }

        String method = isEqualMode()
                ? SplitCalculator.METHOD_EQUAL
                : SplitCalculator.METHOD_CUSTOM;

        if (!SplitStore.saveSplits(expenseId, shares, method)) {
            Toast.makeText(
                    this,
                    "ไม่สามารถบันทึกการหารเงินได้",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Toast.makeText(this, "บันทึกการหารแล้ว", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void confirmLeave() {
        if (SplitStore.isSplitValid(expense)) {
            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("ยังไม่ได้บันทึกการหาร")
                .setMessage("ค่าใช้จ่ายนี้ยังไม่ได้หาร "
                        + "จะยังไม่ถูกนำไปคิดยอดจนกว่าจะหาร")
                .setPositiveButton("ออกเลย", (d, w) -> finish())
                .setNegativeButton("อยู่ต่อ", null)
                .show();
    }


    private static String formatBaht(double amount) {
        return String.format(Locale.US, "%,.2f THB", amount);
    }

    private static String formatPlain(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}
