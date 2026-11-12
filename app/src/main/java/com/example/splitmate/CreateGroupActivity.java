package com.example.splitmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText etGroupName;
    private Button btnSaveGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            finish();
        });


        etGroupName = findViewById(R.id.etGroupName);
        btnSaveGroup = findViewById(R.id.btnSaveGroup);

        btnSaveGroup.setOnClickListener(v -> {

            String groupName =
                    etGroupName.getText().toString().trim();

            if (groupName.isEmpty()) {
                etGroupName.setError("กรุณากรอกชื่อกลุ่ม");
                etGroupName.requestFocus();
                return;
            }

            int groupId = GroupStore.addGroup(groupName);
            Intent intent = new Intent(
                    CreateGroupActivity.this,
                    GroupDetailActivity.class
            );

            intent.putExtra("group_id", groupId);
            intent.putExtra("group_name", groupName);


            startActivity(intent);
        });
    }
}
