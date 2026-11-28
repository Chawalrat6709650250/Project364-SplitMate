
package com.example.splitmate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layoutGroupList;
    private TextView tvEmptyGroups;
    private Button btnCreateGroup;
    private long lastBackAt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutGroupList = findViewById(R.id.layoutGroupList);
        tvEmptyGroups = findViewById(R.id.tvEmptyGroups);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        Button btnLanguage = findViewById(R.id.btnLanguage);
        btnLanguage.setOnClickListener(v -> {
            String language = getResources().getConfiguration().getLocales().get(0).getLanguage();
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(
                    "en".equals(language) ? "th" : "en"));
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (System.currentTimeMillis() - lastBackAt < 2000) {
                    finish();
                } else {
                    lastBackAt = System.currentTimeMillis();
                    Toast.makeText(MainActivity.this,
                            UiText.tr(MainActivity.this, "กด Back อีกครั้งเพื่อออกจากแอป",
                                    "Press Back again to exit"), Toast.LENGTH_SHORT).show();
                }
            }
        });

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

            groupItem.setText("👥  " + group.name + "\n     " +
                    UiText.tr(this, "สมาชิก ", "Members: ") + memberCount +
                    UiText.tr(this, " คน", "") + "   ›");

            groupItem.setTextSize(18);
            groupItem.setTextColor(getColor(R.color.text_primary));
            groupItem.setBackgroundResource(R.drawable.sm_card);

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
