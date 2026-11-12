
package com.example.splitmate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layoutGroupList;
    private TextView tvEmptyGroups;
    private Button btnCreateGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutGroupList = findViewById(R.id.layoutGroupList);
        tvEmptyGroups = findViewById(R.id.tvEmptyGroups);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);

        btnCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    CreateGroupActivity.class
            );
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGroupList();
    }

    private void updateGroupList() {

        layoutGroupList.removeAllViews();

        if (GroupStore.getGroups().isEmpty()) {
            tvEmptyGroups.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyGroups.setVisibility(View.GONE);

        for (GroupStore.Group group : GroupStore.getGroups()) {

            TextView groupItem = new TextView(this);


            int memberCount = GroupStore.getMembers(group.id).size();

            groupItem.setText(
                    "👥  " + group.name +
                            "\n     สมาชิก " + memberCount + " คน   ›"
            );

            groupItem.setTextSize(18);
            groupItem.setTextColor(Color.parseColor("#292940"));
            groupItem.setBackgroundColor(Color.WHITE);

            int padding = (int) (16 * getResources()
                    .getDisplayMetrics().density);

            groupItem.setPadding(
                    padding, padding, padding, padding
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.bottomMargin = padding / 2;

            layoutGroupList.addView(groupItem, params);

            groupItem.setOnClickListener(v -> {

                Intent intent = new Intent(
                        MainActivity.this,
                        GroupDetailActivity.class
                );

                intent.putExtra("group_id", group.id);
                intent.putExtra("group_name", group.name);

                startActivity(intent);
            });
        }
    }
}
