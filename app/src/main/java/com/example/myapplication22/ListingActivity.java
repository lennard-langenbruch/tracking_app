package com.example.myapplication22;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class ListingActivity extends AppCompatActivity {

    private static final int PADDING = 20;
    private static final int TRANSPARENT_WHITE = 0x00FFFFFF;

    private DatabaseHelper sqliteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listing);
        UiUtils.applySystemBarPadding(findViewById(R.id.verticalLayout));

        sqliteDatabase = new DatabaseHelper(this);
        List<Track> tracks = sqliteDatabase.getAllTracks();

        TableLayout tableLayout = findViewById(R.id.tableLayout);

        for (Track t : tracks) {
            TableRow row = new TableRow(this);

            Button nameButton = createRowButton(t.getName(), 0);
            nameButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, InspectActivity.class);
                intent.putExtra("trackid", t.getId()); // passing track id
                startActivity(intent);
            });
            row.addView(nameButton);

            Button renameButton = createRowButton("RENAME", PADDING);
            renameButton.setOnClickListener(v -> showRenameDialog(t.getId(), t.getName()));
            row.addView(renameButton);

            Button deleteButton = createRowButton("DELETE", PADDING);
            deleteButton.setOnClickListener(v -> {
                sqliteDatabase.updateSingleTrackById(t.getId(), null, null, null, null, null, "1");
                tableLayout.removeView(row);
            });
            row.addView(deleteButton);

            tableLayout.addView(row);
        }

        findViewById(R.id.listingToMainButton).setOnClickListener(
                v -> startActivity(new Intent(this, MainActivity.class)));
    }

    private Button createRowButton(String text, int paddingLeft) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setHeight(100);
        button.setBackground(new ColorDrawable(TRANSPARENT_WHITE));
        button.setTextColor(Color.BLACK);
        button.setPadding(paddingLeft, PADDING, PADDING, PADDING);
        return button;
    }

    private void showRenameDialog(long trackId, String currentName) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentName);

        new AlertDialog.Builder(this)
                .setTitle("Rename Track")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        sqliteDatabase.updateSingleTrackById(trackId, newName, null, null, null, null, null);
                        // Refresh the activity to reflect changes
                        recreate();
                    } else {
                        Toast.makeText(this, "Please enter new name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }
}
