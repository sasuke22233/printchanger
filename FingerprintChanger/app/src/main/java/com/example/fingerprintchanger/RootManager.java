package com.example.fingerprintchanger;

import android.util.Log;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RootManager {
    private static final String TAG = "RootManager";
    
    public static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            
            int exitValue = process.waitFor();
            return exitValue == 0;
        } catch (Exception e) {
            Log.e(TAG, "Root check failed", e);
            return false;
        }
    }
    
    public static boolean executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.e(TAG, "Error: " + line);
                }
            }
            
            return exitCode == 0;
        } catch (Exception e) {
            Log.e(TAG, "Command execution failed: " + command, e);
            return false;
        }
    }
    
    public static String executeCommandWithOutput(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            Log.e(TAG, "Command execution failed: " + command, e);
            return "";
        }
    }
}