package com.wbrawner.simplemarkdown.utility;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String getDocsPath(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(
                Constants.KEY_DOCS_PATH,
                Environment.getExternalStorageDirectory() + "/" +
                        Environment.DIRECTORY_DOCUMENTS + "/"
        );
    }

    public static String getDefaultFileName(Context context) {
        File docsDir = new File(Utils.getDocsPath(context));
        Pattern defaultFilePattern = Pattern.compile("Untitled(-([0-9]+))*.md");
        File[] files = docsDir.listFiles();
        String defaultFileName = "Untitled.md";
        if (files != null && files.length > 0) {
            int count = 0;
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }

                Matcher fileMatcher = defaultFilePattern.matcher(file.getName());
                if (!fileMatcher.find()) {
                    continue;
                }

                if (file.getName().equals("Untitled.md")) {
                    if (count == 0) {
                        count = 1;
                    }
                    continue;
                }

                String defaultFileCount = fileMatcher.group(2);
                int fileCount = Integer.parseInt(defaultFileCount);
                if (fileCount >= count) {
                    count = fileCount + 1;
                }
            }

            if (count > 0) {
                defaultFileName = String.format(
                        Locale.ENGLISH,
                        "Untitled-%d.md",
                        count
                );
            }
        }

        return defaultFileName;
    }

    public static boolean isAutosaveEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(
                Constants.KEY_AUTOSAVE,
                true
        );
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean canAccessFiles(Context context) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }


    @SuppressWarnings("SameParameterValue")
    public static Handler createSafeHandler(String name) {
        HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.start();
        handlerThread.setUncaughtExceptionHandler((t, e) -> {
            // TODO: Report this?
//            Crashlytics.logException(e);
            t.interrupt();
        });
        return new Handler(handlerThread.getLooper());
    }
}
