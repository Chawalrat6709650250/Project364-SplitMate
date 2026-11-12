package com.example.splitmate;

import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnCreateGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCreateGroup = findViewById(R.id.btnCreateGroup);

        btnCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    CreateGroupActivity.class
            );
            startActivity(intent);
        });

    }
}
