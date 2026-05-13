package io.github.nezumi0627.lemon.utils;

import android.content.Context;
import android.content.SharedPreferences;
import io.github.nezumi0627.lemon.constants.LemonConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ThemeManager {
    
    private static final String DEFAULT_THEME_ID = "3e261192-3a69-4849-b35d-35aeddd5a368";
    private static final String LINE_THEME_PREF_NAME = "ThemeManager";
    private static final String LINE_THEME_PREF_KEY = "ThemePackageName";
    private static final String THEME_ROOT_DIR = "theme";
    private static final String THEME_LOAD_DIR = "load";
    private static final String THEME_IMAGE_DIR = "images";
    private static final String THEME_TIMELINE_DIR = "timeline";
    private static final String THEME_GROUPBOARD_DIR = "groupboard";
    private static final String THEME_JSON_FILE = "theme.json";
    private static final String TEMP_ZIP_FILE = "lemon_theme.zip";
    private static final String BACKUP_DIR_NAME = "load.lemon_backup";
    private static final String BACKUPS_DIR_NAME = "backups";
    private static final int BUFFER_SIZE = 8192;
    
    public interface ThemeCallback {
        void onSuccess();
        void onError(String error);
        void onProgress(String message);
    }
    
    private static final AtomicBoolean isApplyingTheme = new AtomicBoolean(false);
    
    public static void applyTheme(Context context, String themeId, ThemeCallback callback) {
        if (!isApplyingTheme.compareAndSet(false, true)) {
            notifyError(callback, "テーマ適用が進行中です。完了までお待ちください。");
            return;
        }
        
        new Thread(() -> {
            try {
                notifyProgress(callback, "バックアップを作成中...");
                createBackup(context);
                
                notifyProgress(callback, "テーマをダウンロード中...");
                downloadAndReplaceTheme(context, themeId);
                
                notifyProgress(callback, "テーマを適用中...");
                applyThemeId(context, themeId);
                
                notifyProgress(callback, "適用完了。LINEを手動で再起動すると反映されます。");
                
                notifySuccess(callback);
                
            } catch (Exception e) {
                notifyError(callback, "テーマの適用に失敗しました: " + e.getMessage());
            } finally {
                isApplyingTheme.set(false);
            }
        }, "LemonThemeApply").start();
    }
    
    public static void restoreBackup(Context context, ThemeCallback callback) {
        new Thread(() -> {
            try {
                notifyProgress(callback, "バックアップを復元中...");
                restoreThemeBackup(context);
                
                notifyProgress(callback, "復元完了。LINEを手動で再起動すると反映されます。");
                
                notifySuccess(callback);
                
            } catch (Exception e) {
                notifyError(callback, "復元に失敗しました: " + e.getMessage());
            }
        }, "LemonThemeRestore").start();
    }
    
    private static void createBackup(Context context) throws IOException {
        File loadDir = getThemeLoadDir(context);
        File backupDir = getBackupDir(context);

        if (!loadDir.exists()) {
            Logger.i("Theme backup skipped because load directory does not exist");
            return;
        }

        deleteRecursively(backupDir);
        copyDirectory(loadDir, backupDir);
        createVersionedBackup(context, "auto");

        Logger.i("Theme backup created: " + backupDir.getAbsolutePath());
    }
    
    private static void restoreThemeBackup(Context context) throws IOException {
        File loadDir = getThemeLoadDir(context);
        File backupDir = getBackupDir(context);

        if (!backupDir.exists()) {
            throw new IOException("バックアップファイルが見つかりません");
        }

        deleteRecursively(loadDir);
        copyDirectory(backupDir, loadDir);
        resetThemeOverrideState(context);
        Logger.i("Theme backup restored: " + loadDir.getAbsolutePath());
    }
    
    private static void downloadAndReplaceTheme(Context context, String themeId) throws Exception {
        if (!isValidThemeId(themeId)) {
            throw new Exception("無効なテーマID形式です。8-4-4-4-4-12形式のUUIDを入力してください（例: ec4a14ea-a080-4cda-9214-38686259f592）");
        }

        File themeRootDir = getThemeRootDir(context);
        ensureDirectory(themeRootDir);
        File tempZipFile = new File(themeRootDir, TEMP_ZIP_FILE);

        ThemeDownloader.DownloadCallback callback = new ThemeDownloader.DownloadCallback() {
            @Override
            public void onProgress(int progress) {}

            @Override
            public void onSuccess(File file) {
                Logger.i("Theme downloaded successfully: " + file.getAbsolutePath());
            }

            @Override
            public void onError(Exception e) {
                Logger.e("Theme download failed: " + e.getMessage());
            }
        };

        try {
            ThemeDownloader.downloadThemeBlocking(themeId, tempZipFile, callback);
            callback.onSuccess(tempZipFile);

            File loadDir = getThemeLoadDir(context);
            deleteRecursively(loadDir);
            ensureDirectory(loadDir);

            extractThemeZip(tempZipFile, loadDir);
            validateExtractedTheme(loadDir);
        } finally {
            if (tempZipFile.exists() && !tempZipFile.delete()) {
                Logger.d("Temporary theme zip could not be deleted");
            }
        }
    }
    
    private static void extractThemeZip(File zipFile, File loadDir) throws Exception {
        try (ZipFile zip = new ZipFile(zipFile)) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                File targetFile = resolveLineThemeEntry(loadDir, entry.getName());
                if (targetFile == null) {
                    Logger.d("Skipping unsupported theme zip entry: " + entry.getName());
                    continue;
                }

                ensureDirectory(targetFile.getParentFile());
                try (InputStream input = zip.getInputStream(entry);
                     FileOutputStream output = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int length;
                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                }

                targetFile.setReadable(true, false);
                targetFile.setWritable(true, false);
            }

            Logger.i("Theme zip extracted successfully to: " + loadDir.getAbsolutePath());
        } catch (Exception e) {
            Logger.e("Failed to extract theme zip: " + e.getMessage());
            throw e;
        }
    }

    private static File resolveLineThemeEntry(File loadDir, String entryName) throws IOException {
        String normalizedName = entryName.replace('\\', '/');
        if (normalizedName.startsWith("/") || normalizedName.contains("../")) {
            throw new IOException("危険なZIPエントリを検出しました: " + entryName);
        }

        String lowerName = normalizedName.toLowerCase(Locale.US);
        if (lowerName.endsWith(".json")) {
            return new File(loadDir, THEME_JSON_FILE);
        }

        String topLevel = lowerName;
        int slashIndex = lowerName.indexOf('/');
        if (slashIndex >= 0) {
            topLevel = lowerName.substring(0, slashIndex);
        }

        String fileName = new File(normalizedName).getName();
        if (fileName.length() == 0 || ".".equals(fileName) || "..".equals(fileName)) {
            throw new IOException("不正なZIPエントリ名です: " + entryName);
        }

        if (THEME_IMAGE_DIR.equals(topLevel)) {
            return new File(new File(loadDir, THEME_IMAGE_DIR), fileName);
        }
        if (THEME_TIMELINE_DIR.equals(topLevel)) {
            return new File(new File(loadDir, THEME_TIMELINE_DIR), fileName);
        }
        if (THEME_GROUPBOARD_DIR.equals(topLevel)) {
            return new File(new File(loadDir, THEME_GROUPBOARD_DIR), fileName);
        }

        return null;
    }

    private static void validateExtractedTheme(File loadDir) throws Exception {
        File themeJsonFile = new File(loadDir, THEME_JSON_FILE);
        if (!themeJsonFile.exists() || themeJsonFile.length() == 0) {
            throw new Exception("theme.json の展開に失敗しました");
        }

        Logger.i("LINE compatible theme.json is ready: " + themeJsonFile.getAbsolutePath());
    }
    
    private static boolean isValidThemeId(String themeId) {
        if (themeId == null) return false;
        // Basic UUID format validation (8-4-4-4-12)
        return themeId.matches("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$");
    }
    
    private static void applyThemeId(Context context, String themeId) {
        if (!isValidThemeId(themeId)) {
            Logger.e("Invalid theme ID format: " + themeId);
            return;
        }

        setLineThemeId(context, DEFAULT_THEME_ID);
        clearOfficialThemeCheckFlags(context);
        Logger.i("LINE ThemeManager preference kept at default to avoid official theme verification loop");

        LemonSettings.setString(context, LemonConstants.KEY_THEME_FORCE_ID, themeId);
        LemonSettings.setBoolean(context, LemonConstants.KEY_THEME_FORCE_ENABLED, true);

        Logger.i("Local theme override enabled for source theme ID: " + themeId);
    }
    
    private static void notifyProgress(ThemeCallback callback, String message) {
        if (callback != null) {
            callback.onProgress(message);
        }
    }

    private static void notifySuccess(ThemeCallback callback) {
        if (callback != null) {
            callback.onSuccess();
        }
    }

    private static void notifyError(ThemeCallback callback, String error) {
        if (callback != null) {
            callback.onError(error);
        }
    }
    
    private static void copyFile(File source, File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null) {
            ensureDirectory(parent);
        }
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(destination).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    private static void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            ensureDirectory(destination);
            File[] children = source.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                copyDirectory(child, new File(destination, child.getName()));
            }
            return;
        }

        copyFile(source, destination);
    }

    private static void deleteRecursively(File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }

        if (!file.delete() && file.exists()) {
            throw new IOException("削除できません: " + file.getAbsolutePath());
        }
    }

    private static void ensureDirectory(File directory) throws IOException {
        if (directory == null) {
            throw new IOException("ディレクトリがnullです");
        }
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IOException("ディレクトリではありません: " + directory.getAbsolutePath());
            }
            return;
        }
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("ディレクトリを作成できません: " + directory.getAbsolutePath());
        }
    }

    private static File getThemeRootDir(Context context) {
        return new File(context.getApplicationInfo().dataDir, THEME_ROOT_DIR);
    }

    private static File getThemeLoadDir(Context context) {
        return new File(getThemeRootDir(context), THEME_LOAD_DIR);
    }

    private static File getBackupDir(Context context) {
        return new File(getThemeRootDir(context), BACKUP_DIR_NAME);
    }

    private static void setLineThemeId(Context context, String themeId) {
        SharedPreferences lineThemePreferences = context.getSharedPreferences(LINE_THEME_PREF_NAME, Context.MODE_PRIVATE);
        lineThemePreferences.edit().putString(LINE_THEME_PREF_KEY, themeId).apply();
    }

    private static void clearOfficialThemeCheckFlags(Context context) {
        try {
            SharedPreferences preferences = context.getSharedPreferences("jp.naver.line.android.settings", Context.MODE_PRIVATE);
            preferences.edit()
                    .remove("THEME_NEED_UPGRADE_CHECK")
                    .remove("THEME_NEED_UPGRADE_TARGET_ID")
                    .remove("THEME_NEED_UPGRADE_TARGET_VERSION")
                    .remove("SHOULD_CHECK_APPLIED_THEME_UPDATE")
                    .apply();
        } catch (Throwable t) {
            Logger.d("Failed to clear official theme check flags: " + t.getMessage());
        }
    }
    
    public static boolean backupExists(Context context) {
        return getBackupDir(context).exists();
    }

    public static String createVersionedBackup(Context context, String suffix) throws IOException {
        File loadDir = getThemeLoadDir(context);
        if (!loadDir.exists()) {
            throw new IOException("バックアップ対象が見つかりません");
        }
        File backupsDir = getBackupsDir(context);
        ensureDirectory(backupsDir);
        String safeSuffix = sanitizeBackupSuffix(suffix);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String name = "load_" + timestamp + "_" + safeSuffix;
        File target = new File(backupsDir, name);
        copyDirectory(loadDir, target);
        return name;
    }

    public static List<String> listVersionedBackups(Context context) {
        File backupsDir = getBackupsDir(context);
        File[] dirs = backupsDir.listFiles(File::isDirectory);
        if (dirs == null || dirs.length == 0) return new ArrayList<>();
        Arrays.sort(dirs, Comparator.comparingLong(File::lastModified).reversed());
        List<String> names = new ArrayList<>();
        for (File dir : dirs) names.add(dir.getName());
        return names;
    }

    public static void restoreVersionedBackup(Context context, String backupName, ThemeCallback callback) {
        new Thread(() -> {
            try {
                File source = new File(getBackupsDir(context), backupName);
                if (!source.exists() || !source.isDirectory()) {
                    throw new IOException("指定バックアップが見つかりません: " + backupName);
                }
                File loadDir = getThemeLoadDir(context);
                deleteRecursively(loadDir);
                copyDirectory(source, loadDir);
                resetThemeOverrideState(context);
                notifySuccess(callback);
            } catch (Exception e) {
                notifyError(callback, "復元に失敗しました: " + e.getMessage());
            }
        }, "LemonThemeRestoreVersioned").start();
    }

    public static void deleteVersionedBackup(Context context, String backupName) throws IOException {
        File source = new File(getBackupsDir(context), backupName);
        if (!source.exists()) return;
        deleteRecursively(source);
    }

    private static File getBackupsDir(Context context) {
        return new File(getThemeRootDir(context), BACKUPS_DIR_NAME);
    }

    private static String sanitizeBackupSuffix(String suffix) {
        if (suffix == null || suffix.trim().isEmpty()) return "manual";
        return suffix.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static void resetThemeOverrideState(Context context) {
        setLineThemeId(context, DEFAULT_THEME_ID);
        LemonSettings.setString(context, LemonConstants.KEY_THEME_FORCE_ID, DEFAULT_THEME_ID);
        LemonSettings.setBoolean(context, LemonConstants.KEY_THEME_FORCE_ENABLED, false);
    }
}
