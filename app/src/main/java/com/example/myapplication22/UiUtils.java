package com.example.myapplication22;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class UiUtils {

    private UiUtils() {}

    /** Pads the root view by the system bar insets (needed for edge-to-edge layouts). */
    static void applySystemBarPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /** Warns if no Mapbox public token ("pk....") was entered in res/values/mapbox_access_token.xml. */
    static void warnIfMapboxTokenMissing(Context context) {
        if (!context.getString(R.string.mapbox_access_token).startsWith("pk.")) {
            Toast.makeText(context, "Mapbox access token missing - see res/values/mapbox_access_token.xml", Toast.LENGTH_LONG).show();
        }
    }
}
