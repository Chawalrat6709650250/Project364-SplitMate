package com.example.splitmate;

import android.content.Context;
import java.util.Locale;

public final class UiText {
    private UiText() {}
    public static String tr(Context context, String th, String en) {
        String language = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        return "en".equals(language) ? en : th;
    }
}
