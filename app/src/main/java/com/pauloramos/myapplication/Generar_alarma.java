package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.app.TimePickerDialog;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Generar_alarma extends AppCompatActivity {

    private EditText medicationNameEditText, doseEditText, timeEditText;
    private ImageView doseUpArrow, doseDownArrow, backArrow;
    private Spinner repeatSpinner;
    private Button saveButton;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generar_alarma);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        medicationNameEditText = findViewById(R.id.medicationNameEditText);
        doseEditText = findViewById(R.id.doseEditText);
        timeEditText = findViewById(R.id.timeEditText);
        doseUpArrow = findViewById(R.id.doseUpArrow);
        doseDownArrow = findViewById(R.id.doseDownArrow);
        repeatSpinner = findViewById(R.id.repeatSpinner);
        saveButton = findViewById(R.id.saveButton);
        backArrow = findViewById(R.id.backArrow);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.repeat_options, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(adapter);

        // Valor inicial de la dosis
        if (doseEditText.getText().toString().isEmpty()) {
            doseEditText.setText("0");
        }

        doseUpArrow.setOnClickListener(v -> {
            int currentDose = safeParseInt(doseEditText.getText().toString(), 0);
            doseEditText.setText(String.valueOf(currentDose + 1));
        });

        doseDownArrow.setOnClickListener(v -> {
            int currentDose = safeParseInt(doseEditText.getText().toString(), 0);
            if (currentDose > 0) {
                doseEditText.setText(String.valueOf(currentDose - 1));
            }
        });

        timeEditText.setOnClickListener(v -> showTimePicker());
        backArrow.setOnClickListener(v -> onBackPressed());

        saveButton.setOnClickListener(v -> guardarMedicamento());
    }

    private void guardarMedicamento() {
        String nombre = medicationNameEditText.getText().toString().trim();
        String dosis = doseEditText.getText().toString().trim();
        String hora = timeEditText.getText().toString().trim();
        String repeticion = repeatSpinner.getSelectedItem().toString();

        if (nombre.isEmpty() || dosis.isEmpty() || hora.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        Medication medicamento = new Medication(nombre, dosis, hora, repeticion);
        guardarEnFirebase(medicamento);
    }

    private void guardarEnFirebase(Medication medication) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String idMedicamento = mDatabase.child("usuarios").child(uid).child("medicamentos").push().getKey();

        if (idMedicamento == null) {
            Toast.makeText(this, "No se pudo generar ID del medicamento.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("usuarios")
                .child(uid)
                .child("medicamentos")
                .child(idMedicamento)
                .setValue(medication)
                .addOnSuccessListener(aVoid -> {
                    long tiempo = obtenerHoraDelMedicamentoEnMillis(medication.getTime());

                    if (tiempo > System.currentTimeMillis()) {
                        programarAlarma(tiempo, medication.getName(), idMedicamento);
                    } else {
                        Toast.makeText(this, "La hora ya pasó. No se programó alarma.", Toast.LENGTH_SHORT).show();
                    }

                    Toast.makeText(this, "Medicamento guardado", Toast.LENGTH_SHORT).show();
                    clearForm();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
    }

    private void programarAlarma(long tiempo, String nombreMedicamento, String id) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmaReceiver.class);
        intent.putExtra("nombre_medicamento", nombreMedicamento);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager == null) {
            Toast.makeText(this, "No se pudo acceder al AlarmManager", Toast.LENGTH_SHORT).show();
            return;
        }

        // SOLO en Android 12+ verificar el permiso especial
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Debes permitir alarmas exactas en configuración", Toast.LENGTH_LONG).show();
                Intent configIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(configIntent);
                return;
            }
        }

        try {
            Log.d("ALARM", "Programando alarma para: " + tiempo + " (" + new Date(tiempo) + ")");
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tiempo, pendingIntent);
        } catch (SecurityException e) {
            Log.e("ALARM", "Permiso denegado para alarmas exactas", e);
            Toast.makeText(this, "No se pudo programar la alarma: permiso denegado", Toast.LENGTH_LONG).show();
        }
    }



    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, selectedMinute) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, selectedMinute);
                    timeEditText.setText(formattedTime);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private long obtenerHoraDelMedicamentoEnMillis(String hora) {
        SimpleDateFormat formato = new SimpleDateFormat("HH:mm", Locale.getDefault());

        try {
            Date fecha = formato.parse(hora);
            if (fecha != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(fecha);

                Calendar ahora = Calendar.getInstance();
                calendar.set(Calendar.YEAR, ahora.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, ahora.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, ahora.get(Calendar.DAY_OF_MONTH));

                if (calendar.getTimeInMillis() < ahora.getTimeInMillis()) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }

                return calendar.getTimeInMillis();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return System.currentTimeMillis();
    }

    private void clearForm() {
        medicationNameEditText.setText("");
        doseEditText.setText("0");
        timeEditText.setText("");
        repeatSpinner.setSelection(0);
    }

    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
