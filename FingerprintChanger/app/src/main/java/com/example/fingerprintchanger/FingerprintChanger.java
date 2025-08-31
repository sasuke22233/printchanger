package com.example.fingerprintchanger;

import android.util.Log;

public class FingerprintChanger {
    private static final String TAG = "FingerprintChanger";
    private static final String BUILD_PROP = "/system/build.prop";
    private static final String VENDOR_BUILD_PROP = "/vendor/build.prop";
    private static final String TEMP_DIR = "/data/local/tmp";
    
    public static boolean changeFingerprint(String newFingerprint) {
        if (!RootManager.isRootAvailable()) {
            Log.e(TAG, "Root access not available");
            return false;
        }
        
        try {
            // Создаем резервные копии
            Log.d(TAG, "Creating backups...");
            if (!createBackups()) {
                Log.e(TAG, "Failed to create backups");
                return false;
            }
            
            // Монтируем разделы в режиме записи
            Log.d(TAG, "Mounting partitions...");
            if (!mountPartitionsRW()) {
                Log.e(TAG, "Failed to mount partitions");
                return false;
            }
            
            // Изменяем build.prop файлы
            Log.d(TAG, "Modifying build.prop files...");
            boolean systemModified = modifyBuildProp(BUILD_PROP, newFingerprint);
            boolean vendorModified = modifyBuildProp(VENDOR_BUILD_PROP, newFingerprint);
            
            // Применяем изменения через setprop
            Log.d(TAG, "Setting properties...");
            RootManager.executeCommand("setprop ro.build.fingerprint \"" + newFingerprint + "\"");
            RootManager.executeCommand("setprop ro.vendor.build.fingerprint \"" + newFingerprint + "\"");
            
            // Монтируем обратно в режим только чтения
            mountPartitionsRO();
            
            return systemModified || vendorModified;
        } catch (Exception e) {
            Log.e(TAG, "Error changing fingerprint", e);
            mountPartitionsRO(); // Пытаемся вернуть в исходное состояние
            return false;
        }
    }
    
    private static boolean createBackups() {
        boolean success = true;
        
        // Резервная копия system/build.prop
        if (RootManager.executeCommand("test -f " + BUILD_PROP)) {
            success &= RootManager.executeCommand("cp -f " + BUILD_PROP + " " + BUILD_PROP + ".bak");
        }
        
        // Резервная копия vendor/build.prop
        if (RootManager.executeCommand("test -f " + VENDOR_BUILD_PROP)) {
            success &= RootManager.executeCommand("cp -f " + VENDOR_BUILD_PROP + " " + VENDOR_BUILD_PROP + ".bak");
        }
        
        return success;
    }
    
    private static boolean mountPartitionsRW() {
        boolean success = true;
        
        // Пробуем разные способы монтирования
        success &= RootManager.executeCommand("mount -o rw,remount /system") ||
                   RootManager.executeCommand("mount -o rw,remount /system /system") ||
                   RootManager.executeCommand("mount -o rw,remount /");
        
        success &= RootManager.executeCommand("mount -o rw,remount /vendor") ||
                   RootManager.executeCommand("mount -o rw,remount /vendor /vendor");
        
        return success;
    }
    
    private static boolean mountPartitionsRO() {
        RootManager.executeCommand("mount -o ro,remount /system");
        RootManager.executeCommand("mount -o ro,remount /vendor");
        RootManager.executeCommand("mount -o ro,remount /");
        return true;
    }
    
    private static boolean modifyBuildProp(String propFile, String newFingerprint) {
        // Проверяем существование файла
        if (!RootManager.executeCommand("test -f " + propFile)) {
            Log.w(TAG, "File not found: " + propFile);
            return false;
        }
        
        String tempFile = TEMP_DIR + "/build.prop.tmp";
        
        // Копируем файл во временную директорию
        if (!RootManager.executeCommand("cp -f " + propFile + " " + tempFile)) {
            return false;
        }
        
        // Даем права на изменение
        RootManager.executeCommand("chmod 666 " + tempFile);
        
        // Экранируем специальные символы в fingerprint
        String escapedFingerprint = newFingerprint
            .replace("\\", "\\\\")
            .replace("/", "\\/")
            .replace("&", "\\&");
        
        // Изменяем или добавляем строку с fingerprint
        String sedCommand = String.format(
            "sed -i 's/^ro\\.build\\.fingerprint=.*/ro.build.fingerprint=%s/' %s",
            escapedFingerprint,
            tempFile
        );
        
        if (!RootManager.executeCommand(sedCommand)) {
            // Если sed не сработал, пробуем добавить строку
            String appendCommand = String.format(
                "echo 'ro.build.fingerprint=%s' >> %s",
                newFingerprint,
                tempFile
            );
            RootManager.executeCommand(appendCommand);
        }
        
        // Также изменяем ro.vendor.build.fingerprint если это vendor/build.prop
        if (propFile.contains("vendor")) {
            String vendorSedCommand = String.format(
                "sed -i 's/^ro\\.vendor\\.build\\.fingerprint=.*/ro.vendor.build.fingerprint=%s/' %s",
                escapedFingerprint,
                tempFile
            );
            RootManager.executeCommand(vendorSedCommand);
        }
        
        // Копируем измененный файл обратно
        boolean success = RootManager.executeCommand("cp -f " + tempFile + " " + propFile);
        
        // Восстанавливаем права
        RootManager.executeCommand("chmod 644 " + propFile);
        
        // Удаляем временный файл
        RootManager.executeCommand("rm -f " + tempFile);
        
        return success;
    }
    
    public static boolean restoreBackups() {
        boolean success = true;
        
        if (!mountPartitionsRW()) {
            return false;
        }
        
        // Восстанавливаем system/build.prop
        if (RootManager.executeCommand("test -f " + BUILD_PROP + ".bak")) {
            success &= RootManager.executeCommand("cp -f " + BUILD_PROP + ".bak " + BUILD_PROP);
        }
        
        // Восстанавливаем vendor/build.prop
        if (RootManager.executeCommand("test -f " + VENDOR_BUILD_PROP + ".bak")) {
            success &= RootManager.executeCommand("cp -f " + VENDOR_BUILD_PROP + ".bak " + VENDOR_BUILD_PROP);
        }
        
        mountPartitionsRO();
        return success;
    }
}