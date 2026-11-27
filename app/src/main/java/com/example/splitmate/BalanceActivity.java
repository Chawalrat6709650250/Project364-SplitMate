package com.example.splitmate;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BalanceActivity extends AppCompatActivity {

    private static final int COLOR_TEXT = Color.parseColor("#292940");
    private static final int COLOR_SUB = Color.parseColor("#77768C");
    private static final int COLOR_GREEN = Color.parseColor("#2E8B57");
    private static final int COLOR_RED = Color.parseColor("#D95C69");
    private static final int COLOR_PURPLE = Color.parseColor("#6558D3");

    private int groupId;

    private TextView tvGrandTotal;
    private TextView tvSummaryInfo;
    private TextView tvSettledProgress;
    private ProgressBar progressSettled;
    private TextView tvUnsplitWarning;
    private TextView tvAllSettled;
    private TextView tvPaidHeader;
    private LinearLayout layoutBalanceList;
    private LinearLayout layoutPendingList;
    private LinearLayout layoutPaidList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);

        groupId = getIntent().getIntExtra("group_id", -1);
        String groupName = getIntent().getStringExtra("group_name");

        if (groupId == -1) {
            Toast.makeText(this, "ไม่พบรหัสกลุ่ม", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvGroupName = findViewById(R.id.tvBalanceGroupName);
        tvGrandTotal = findViewById(R.id.tvGrandTotal);
        tvSummaryInfo = findViewById(R.id.tvSummaryInfo);
        tvSettledProgress = findViewById(R.id.tvSettledProgress);
        progressSettled = findViewById(R.id.progressSettled);
        tvUnsplitWarning = findViewById(R.id.tvUnsplitWarning);
        tvAllSettled = findViewById(R.id.tvAllSettled);
        tvPaidHeader = findViewById(R.id.tvPaidHeader);
        layoutBalanceList = findViewById(R.id.layoutBalanceList);
        layoutPendingList = findViewById(R.id.layoutPendingList);
        layoutPaidList = findViewById(R.id.layoutPaidList);

        findViewById(R.id.cardSummary).setBackground(rounded(COLOR_PURPLE));

        ImageView btnBack = findViewById(R.id.btnBackBalance);
        btnBack.setOnClickListener(v -> finish());

        if (groupName != null) {
            tvGroupName.setText("กลุ่ม " + groupName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        int unsplit = SplitStore.getUnsplitExpenseIds(groupId).size();
        if (unsplit > 0) {
            tvUnsplitWarning.setText("⚠ มีค่าใช้จ่าย " + unsplit
                    + " รายการที่ยังไม่ได้หารเงิน จึงยังไม่ถูกนำมาคิดยอด");
            tvUnsplitWarning.setVisibility(View.VISIBLE);
        } else {
            tvUnsplitWarning.setVisibility(View.GONE);
        }

        Map<String, BalanceCalculator.MemberSummary> summaries =
                BalanceCalculator.summarize(groupId);
        Map<String, Long> net = BalanceCalculator.calculateNetSatang(groupId);
        List<Settlement> pending = SettlementStore.refreshPending(groupId);
        ArrayList<Settlement> paid =
                SettlementStore.getSettlements(groupId, Settlement.STATUS_PAID);

        showSummary(summaries, pending, paid);
        showMembers(summaries, net);
        showPending(pending);
        showPaid(paid);
    }

    private void showSummary(Map<String, BalanceCalculator.MemberSummary> summaries,
                             List<Settlement> pending, List<Settlement> paid) {
        long total = 0;
        for (BalanceCalculator.MemberSummary s : summaries.values()) {
            total += s.paid;
        }
        int expenseCount = ExpenseStore.getExpenses(groupId).size();

        tvGrandTotal.setText(formatBaht(total));
        tvSummaryInfo.setText(expenseCount + " รายการ · สมาชิก " + summaries.size() + " คน");

        long paidSum = sumSatang(paid);
        long allSum = paidSum + sumSatang(pending);

        if (allSum == 0) {
            progressSettled.setProgress(total > 0 ? 100 : 0);
            tvSettledProgress.setText(total > 0 ? "ไม่มีใครต้องโอนเงิน" : "ยังไม่มีค่าใช้จ่าย");
        } else {
            progressSettled.setProgress((int) (paidSum * 100 / allSum));
            tvSettledProgress.setText("โอนคืนแล้ว " + formatBaht(paidSum)
                    + " จาก " + formatBaht(allSum));
        }
    }

    private void showMembers(Map<String, BalanceCalculator.MemberSummary> summaries,
                             Map<String, Long> net) {
        layoutBalanceList.removeAllViews();

        for (BalanceCalculator.MemberSummary s : summaries.values()) {
            Long value = net.get(s.name);
            long remaining = value == null ? 0 : value;

            LinearLayout card = createCard();
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(text(s.name, 17, COLOR_TEXT, true));
            left.addView(text("ออกเงินไป " + formatBaht(s.paid), 14, COLOR_SUB, false));
            left.addView(text("ส่วนของตัวเอง " + formatBaht(s.share), 14, COLOR_SUB, false));
            card.addView(left, weight());

            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.END);

            if (remaining > 0) {
                right.addView(text("รอรับคืน", 13, COLOR_GREEN, false));
                right.addView(text(formatBaht(remaining), 17, COLOR_GREEN, true));
            } else if (remaining < 0) {
                right.addView(text("ต้องจ่าย", 13, COLOR_RED, false));
                right.addView(text(formatBaht(-remaining), 17, COLOR_RED, true));
            } else {
                right.addView(text("✓ เคลียร์แล้ว", 15, COLOR_SUB, true));
            }
            card.addView(right);

            layoutBalanceList.addView(card);
        }
    }

    private void showPending(List<Settlement> pending) {
        layoutPendingList.removeAllViews();
        tvAllSettled.setVisibility(pending.isEmpty() ? View.VISIBLE : View.GONE);

        for (Settlement settlement : pending) {
            LinearLayout card = createCard();

            // ชื่อผู้โอน → ผู้รับ
            LinearLayout names = new LinearLayout(this);
            names.setOrientation(LinearLayout.HORIZONTAL);
            names.setGravity(Gravity.CENTER_VERTICAL);
            names.addView(text(settlement.getFromMember(), 17, COLOR_RED, true));
            names.addView(text("  ต้องโอนให้  ", 15, COLOR_SUB, false));
            names.addView(text(settlement.getToMember(), 17, COLOR_GREEN, true));
            card.addView(names);

            LinearLayout bottom = new LinearLayout(this);
            bottom.setOrientation(LinearLayout.HORIZONTAL);
            bottom.setGravity(Gravity.CENTER_VERTICAL);
            bottom.setPadding(0, 12, 0, 0);

            TextView amount = text(
                    formatBaht(SplitCalculator.toSatang(settlement.getAmount())),
                    24, COLOR_TEXT, true);
            bottom.addView(amount, weight());

            Button btnPaid = new Button(this);
            btnPaid.setText("โอนแล้ว ✓");
            btnPaid.setAllCaps(false);
            btnPaid.setTextColor(Color.WHITE);
            btnPaid.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(COLOR_GREEN));
            btnPaid.setOnClickListener(v -> confirmPaid(settlement));
            bottom.addView(btnPaid);

            card.addView(bottom);
            layoutPendingList.addView(card);
        }
    }

    private void showPaid(ArrayList<Settlement> paid) {
        layoutPaidList.removeAllViews();
        int visibility = paid.isEmpty() ? View.GONE : View.VISIBLE;
        tvPaidHeader.setVisibility(visibility);
        layoutPaidList.setVisibility(visibility);

        for (Settlement settlement : paid) {
            LinearLayout card = createCard();
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);

            card.addView(text("✓ " + describe(settlement), 15, COLOR_SUB, false), weight());

            TextView btnUndo = text("ยกเลิก", 14, COLOR_RED, false);
            btnUndo.setPadding(24, 16, 8, 16);
            btnUndo.setOnClickListener(v -> confirmUndo(settlement));
            card.addView(btnUndo);

            layoutPaidList.addView(card);
        }
    }

    private void confirmPaid(Settlement settlement) {
        new AlertDialog.Builder(this)
                .setTitle("ยืนยันการโอนเงิน")
                .setMessage(describe(settlement) + "\nโอนเรียบร้อยแล้วใช่ไหม?")
                .setPositiveButton("ใช่ โอนแล้ว", (dialog, which) -> {
                    SettlementStore.updateStatus(
                            settlement.getSettlementId(), Settlement.STATUS_PAID);
                    refresh();
                })
                .setNegativeButton("ยังก่อน", null)
                .show();
    }

    private void confirmUndo(Settlement settlement) {
        new AlertDialog.Builder(this)
                .setTitle("ยกเลิกการโอน")
                .setMessage("ยกเลิกรายการ " + describe(settlement)
                        + " ?\nยอดจะถูกคำนวณใหม่")
                .setPositiveButton("ยืนยัน", (dialog, which) -> {
                    SettlementStore.deleteSettlement(settlement.getSettlementId());
                    refresh();
                })
                .setNegativeButton("ปิด", null)
                .show();
    }

    private String describe(Settlement settlement) {
        return settlement.getFromMember() + " โอนให้ " + settlement.getToMember() + " "
                + formatBaht(SplitCalculator.toSatang(settlement.getAmount()));
    }

    private long sumSatang(List<Settlement> list) {
        long sum = 0;
        for (Settlement settlement : list) {
            sum += SplitCalculator.toSatang(settlement.getAmount());
        }
        return sum;
    }

    private String formatBaht(long satang) {
        return String.format(Locale.US, "%,.2f บาท", SplitCalculator.toBaht(satang));
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(40, 32, 40, 32);
        card.setBackground(rounded(Color.WHITE));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 20;
        card.setLayoutParams(params);
        return card;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(36);
        return drawable;
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sizeSp);
        tv.setTextColor(color);
        if (bold) {
            tv.setTypeface(null, Typeface.BOLD);
        }
        return tv;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    }
}
