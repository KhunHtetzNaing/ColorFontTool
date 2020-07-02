package com.htetznaing.fonttools;

import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;
import com.nononsenseapps.filepicker.FilePickerFragment;

import java.io.File;
import java.util.ArrayList;

public class FilePicker extends AbstractFilePickerActivity<File> {
    private static ArrayList<String> EXTENSIONS = new ArrayList<>();
    @Override
    protected AbstractFilePickerFragment<File> getFragment(
            @Nullable final String startPath, final int mode, final boolean allowMultiple,
            final boolean allowCreateDir, final boolean allowExistingFile,
            final boolean singleClick) {
        EXTENSIONS.clear();
        EXTENSIONS.add(getIntent().getStringExtra("extensions"));
        AbstractFilePickerFragment<File> fragment = new CustomFilePickerFragment();
        // startPath is allowed to be null. In that case, default folder should be SD-card and not "/"
        fragment.setArgs(startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath(),
                mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick);
        return fragment;
    }

    public static class CustomFilePickerFragment extends FilePickerFragment {
        /**
         *
         * @param file
         * @return The file extension. If file has no extension, it returns null.
         */
        private String getExtension(@NonNull File file) {
            String path = file.getPath();
            int i = path.lastIndexOf(".");
            if (i < 0) {
                return null;
            } else {
                return path.substring(i);
            }
        }

        @Override
        protected boolean isItemVisible(final File file) {
            boolean ret = super.isItemVisible(file);
            if (ret && !isDir(file) && (mode == MODE_FILE || mode == MODE_FILE_AND_DIR)) {
                String ext = getExtension(file);
                return ext != null && EXTENSIONS.contains(ext);
            }
            return ret;
        }
    }
}
