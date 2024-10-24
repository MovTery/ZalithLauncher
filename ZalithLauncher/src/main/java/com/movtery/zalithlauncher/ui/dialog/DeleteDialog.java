package com.movtery.zalithlauncher.ui.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import com.movtery.zalithlauncher.utils.file.FileDeletionHandler;

import net.kdt.pojavlaunch.R;

import java.io.File;
import java.util.List;

public class DeleteDialog extends TipDialog.Builder {
    private final Context context;
    private final Runnable runnable;

    public DeleteDialog(@NonNull Context context, Runnable runnable, List<File> files) {
        super(context);
        this.context = context;
        this.runnable = runnable;
        init(files);
    }

    private void init(List<File> files) {
        this.setCancelable(false);

        boolean singleFile = files.size() == 1;
        File file = files.get(0);
        boolean isFolder = file.isDirectory();
        setTitle(singleFile ? (isFolder ?
                R.string.file_delete_dir :
                R.string.file_tips) : R.string.file_delete_multiple_items_title);
        setMessage(singleFile ? (isFolder ?
                R.string.file_delete_dir_message :
                R.string.file_delete) : R.string.file_delete_multiple_items_message);
        setConfirm(R.string.generic_delete);

        setConfirmClickListener(() -> new FileDeletionHandler(context, files, runnable).start());
    }

    public void show() {
        buildDialog();
    }
}
