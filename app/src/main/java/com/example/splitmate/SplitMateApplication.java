package com.example.splitmate;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SplitMateApplication extends Application {

    private static DatabaseHelper databaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper.getInstance(this);

        // Create or open SQLite database
        databaseHelper.getWritableDatabase();

        Log.d("SplitMateDB", "Database initialized successfully");
    }

    // Return DatabaseHelper instance
    public static DatabaseHelper getDatabaseHelper() {
        if (databaseHelper == null) {
            throw new IllegalStateException(
                    "DatabaseHelper has not been initialized"
            );
        }

        return databaseHelper;
    }

    // Return SQLite database
    public static SQLiteDatabase getDatabase() {
        return getDatabaseHelper().getWritableDatabase();
    }
}