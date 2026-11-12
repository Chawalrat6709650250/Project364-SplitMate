
package com.example.splitmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class AddMemberActivity extends AppCompatActivity {

    private EditText etMemberName;
    private Button btnSaveMember;
    private ImageView btnBackAddMember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        etMemberName = findViewById(R.id.etMemberName);
        btnSaveMember = findViewById(R.id.btnSaveMember);
        btnBackAddMember = findViewById(R.id.btnBackAddMember);

        btnBackAddMember.setOnClickListener(v -> finish());

        btnSaveMember.setOnClickListener(v -> {

            String memberName =
                    etMemberName.getText().toString().trim();

            if (memberName.isEmpty()) {
                etMemberName.setError("กรุณากรอกชื่อสมาชิก");
                etMemberName.requestFocus();
                return;
            }

            Intent result = new Intent();
            result.putExtra("member_name", memberName);

            setResult(RESULT_OK, result);
            finish();
        });
    }
}
