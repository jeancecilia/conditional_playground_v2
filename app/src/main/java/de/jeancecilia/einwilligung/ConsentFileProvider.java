package de.jeancecilia.einwilligung;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ConsentFileProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "application/pdf";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        File file = resolveFile(uri);
        String[] columns = projection != null ? projection :
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String column : columns) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) {
                row.add(file != null ? file.getName() : "Einwilligung.pdf");
            } else if (OpenableColumns.SIZE.equals(column)) {
                row.add(file != null && file.exists() ? file.length() : 0L);
            } else {
                row.add(null);
            }
        }
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = resolveFile(uri);
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("Datei nicht gefunden");
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private File resolveFile(Uri uri) {
        if (getContext() == null) return null;
        String encoded = uri.getLastPathSegment();
        if (encoded == null) return null;
        String name = Uri.decode(encoded);
        if (name.contains("/") || name.contains("\\") || name.contains("..")) return null;

        File base = new File(getContext().getFilesDir(), "consents");
        File candidate = new File(base, name);
        try {
            String basePath = base.getCanonicalPath();
            String filePath = candidate.getCanonicalPath();
            if (!filePath.startsWith(basePath + File.separator)) return null;
            return candidate;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Nur Lesen");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
