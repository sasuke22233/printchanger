package com.example.fingerprintchanger;

import android.os.Bundle;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {
    private EditText fingerprintInput;
    private Button changeButton;
    private TextView currentFingerprintText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fingerprintInput = findViewById(R.id.fingerprintInput);
        changeButton = findViewById(R.id.changeButton);
        currentFingerprintText = findViewById(R.id.currentFingerprint);

        // Показываем текущий fingerprint
        currentFingerprintText.setText("Текущий fingerprint:\n" + Build.FINGERPRINT);

        // Проверяем root при запуске
        checkRootAccess();

        changeButton.setOnClickListener(v -> {
            String newFingerprint = fingerprintInput.getText().toString().trim();
            if (newFingerprint.isEmpty()) {
                Toast.makeText(this, "Введите новый fingerprint", Toast.LENGTH_SHORT).show();
                return;
            }

            // Подтверждение действия
            new AlertDialog.Builder(this)
                .setTitle("Подтверждение")
                .setMessage("Вы уверены, что хотите изменить fingerprint? Это действие требует перезагрузки устройства.")
                .setPositiveButton("Да", (dialog, which) -> changeFingerprint(newFingerprint))
                .setNegativeButton("Отмена", null)
                .show();
        });
    }

    private void checkRootAccess() {
        new Thread(() -> {
            boolean hasRoot = RootManager.isRootAvailable();
            runOnUiThread(() -> {
                if (!hasRoot) {
                    new AlertDialog.Builder(this)
                        .setTitle("Root не обнаружен")
                        .setMessage("Это приложение требует root доступ для работы")
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
                } else {
                    changeButton.setEnabled(true);
                }
            });
        }).start();
    }

    private void changeFingerprint(String newFingerprint) {
        changeButton.setEnabled(false);
        
        new Thread(() -> {
            boolean success = FingerprintChanger.changeFingerprint(newFingerprint);
            runOnUiThread(() -> {
                changeButton.setEnabled(true);
                if (success) {
                    new AlertDialog.Builder(this)
                        .setTitle("Успешно")
                        .setMessage("Fingerprint изменен. Необходима перезагрузка устройства.")
                        .setPositiveButton("Перезагрузить", (dialog, which) -> {
                            RootManager.executeCommand("reboot");
                        })
                        .setNegativeButton("Позже", null)
                        .show();
                } else {
                    Toast.makeText(this, "Ошибка при изменении fingerprint", 
                        Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}