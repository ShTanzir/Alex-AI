package com.alexai.assistant.ui.dialogs;

import android.content.Context;
import android.content.DialogInterface;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Shows what Alex understood and exactly what will happen, with explicit Confirm / Cancel. */
public final class ConfirmationManager {
    private ConfirmationManager() {}

    public static void show(Context context, String summary, final Runnable onConfirm, final Runnable onCancel) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Confirm action")
                .setMessage(summary)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { onConfirm.run(); }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { onCancel.run(); }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override public void onCancel(DialogInterface d) { onCancel.run(); }
                })
                .show();
    }
}
