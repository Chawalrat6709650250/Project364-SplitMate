package com.example.splitmate;

import android.widget.EditText;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** Shared confirmation for leaving unfinished forms. */
public final class NavigationGuard {
    private NavigationGuard() {}

    public static void attach(AppCompatActivity activity, EditText... fields) {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                confirmExit(activity, fields);
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, callback);
    }

    public static void confirmExit(AppCompatActivity activity, EditText... fields) {
        boolean hasData = false;
        for (EditText field : fields) {
            if (field != null && field.getText().toString().trim().length() > 0) {
                hasData = true;
                break;
            }
        }
        if (!hasData) {
            activity.finish();
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(UiText.tr(activity, "ยังไม่ได้บันทึก", "Unsaved changes"))
                .setMessage(UiText.tr(activity,
                        "หากออกจากหน้านี้ ข้อมูลที่ยังไม่ได้บันทึกจะหายไป ต้องการออกหรือไม่?",
                        "Unsaved information will be lost. Leave this screen?"))
                .setPositiveButton(UiText.tr(activity, "ออกจากหน้านี้", "Leave"), (d, w) -> activity.finish())
                .setNegativeButton(UiText.tr(activity, "อยู่ต่อ", "Stay"), null)
                .show();
    }
}
