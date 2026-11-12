package com.example.splitmate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class GroupDetailActivity extends AppCompatActivity {

    private TextView tvGroupName;
    private TextView tvMemberCount;
    private TextView tvEmptyMembers;
    private LinearLayout layoutMemberList;
    private int groupId = -1;
    private final ArrayList<String> members = new ArrayList<>();

    private final ActivityResultLauncher<Intent> addMemberLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            String name = result.getData()
                                    .getStringExtra("member_name");

                            if (name != null && !name.trim().isEmpty()) {

                                if (GroupStore.addMember(groupId, name.trim())) {
                                    members.clear();
                                    members.addAll(GroupStore.getMembers(groupId));
                                    updateMemberList();
                                }
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        tvGroupName = findViewById(R.id.tvGroupName);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvEmptyMembers = findViewById(R.id.tvEmptyMembers);
        layoutMemberList = findViewById(R.id.layoutMemberList);

        ImageView btnBackDetail = findViewById(R.id.btnBackDetail);
        Button btnAddMember = findViewById(R.id.btnAddMember);
        Button btnEditGroup = findViewById(R.id.btnEditGroup);
        Button btnDeleteGroup = findViewById(R.id.btnDeleteGroup);

        groupId = getIntent().getIntExtra("group_id", -1);



        btnEditGroup.setOnClickListener(v -> {

            EditText input = new EditText(GroupDetailActivity.this);
            input.setSingleLine(true);
            input.setText(tvGroupName.getText().toString());
            input.selectAll();

            AlertDialog dialog = new AlertDialog.Builder(
                    GroupDetailActivity.this
            )
                    .setTitle("แก้ไขชื่อกลุ่ม")
                    .setView(input)
                    .setPositiveButton("บันทึก", null)
                    .setNegativeButton("ยกเลิก", null)
                    .create();

            dialog.setOnShowListener(d -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(view -> {

                            String newName =
                                    input.getText().toString().trim();

                            if (newName.isEmpty()) {
                                input.setError("กรุณากรอกชื่อกลุ่ม");
                                return;
                            }


                            if (GroupStore.updateGroup(groupId, newName)) {
                                tvGroupName.setText(newName);
                                dialog.dismiss();
                            } else {
                                input.setError("ไม่สามารถแก้ไขชื่อกลุ่มได้");
                            }

                        });
            });

            dialog.show();
        });



        btnDeleteGroup.setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("ลบกลุ่ม")
                    .setMessage("ต้องการลบกลุ่มนี้หรือไม่?")
                    .setPositiveButton("ลบ", (dialog, which) -> {

                        if (groupId == -1) {
                            Toast.makeText(
                                    this,
                                    "ไม่พบรหัสกลุ่ม",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        boolean deleted = GroupStore.deleteGroup(groupId);

                        if (deleted) {
                            Intent intent = new Intent(
                                    GroupDetailActivity.this,
                                    MainActivity.class
                            );

                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                            );

                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(
                                    this,
                                    "ไม่พบกลุ่มที่ต้องการลบ",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    })
                    .setNegativeButton("ยกเลิก", null)
                    .show();
        });


        String groupName =
                getIntent().getStringExtra("group_name");

        if (groupName != null && !groupName.isEmpty()) {
            tvGroupName.setText(groupName);
        }


        members.clear();
        members.addAll(GroupStore.getMembers(groupId));


        btnBackDetail.setOnClickListener(v -> finish());

        btnAddMember.setOnClickListener(v -> {
            Intent intent = new Intent(
                    GroupDetailActivity.this,
                    AddMemberActivity.class
            );
            addMemberLauncher.launch(intent);
        });

        updateMemberList();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList("members", members);
        super.onSaveInstanceState(outState);
    }

    private void updateMemberList() {

        layoutMemberList.removeAllViews();

        tvMemberCount.setText(members.size() + " คน");

        if (members.isEmpty()) {
            tvEmptyMembers.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyMembers.setVisibility(View.GONE);

        for (String name : new ArrayList<>(members)) {

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(24, 20, 24, 20);
            row.setBackgroundColor(Color.WHITE);

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextSize(16);
            tvName.setTextColor(Color.parseColor("#292940"));

            LinearLayout.LayoutParams nameParams =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    );

            row.addView(tvName, nameParams);

            Button btnRemove = new Button(this);
            btnRemove.setText("ลบ");

            row.addView(btnRemove);

            btnRemove.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("ลบสมาชิก")
                        .setMessage("ต้องการลบ " + name + " หรือไม่?")
                        .setPositiveButton("ลบ", (dialog, which) -> {

                            if (GroupStore.removeMember(groupId, name)) {
                                members.clear();
                                members.addAll(GroupStore.getMembers(groupId));
                                updateMemberList();
                            }

                        })
                        .setNegativeButton("ยกเลิก", null)
                        .show();
            });

            layoutMemberList.addView(row);
        }
    }
}
