package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.widget.TimePicker;
import android.os.Bundle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class Generar_alarma extends AppCompatActivity {


    private EditText medicationNameEditText;
    private EditText doseEditText;
    private ImageView doseUpArrow;
    private ImageView doseDownArrow;
    private EditText timeEditText;
    private Spinner repeatSpinner;
    private Button saveButton;
    private ImageView backArrow;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generar_alarma);

        mDatabase = FirebaseDatabase .getInstance().getReference();


        medicationNameEditText = findViewById(R.id.medicationNameEditText);
        doseEditText = findViewById(R.id.doseEditText);
        doseUpArrow = findViewById(R.id.doseUpArrow);
        doseDownArrow = findViewById(R.id.doseDownArrow);
        timeEditText = findViewById(R.id.timeEditText);
        repeatSpinner = findViewById(R.id.repeatSpinner);
        saveButton = findViewById(R.id.saveButton);
        backArrow = findViewById(R.id.backArrow);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.repeat_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(adapter);

        if (doseEditText.getText().toString().isEmpty()) {
            doseEditText.setText("0");
        }

        doseUpArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int currentDose = Integer.parseInt(doseEditText.getText().toString());
                    doseEditText.setText(String.valueOf(currentDose + 1));
                } catch (NumberFormatException e) {
                    doseEditText.setText("1");
                }
            }
        });

        doseDownArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int currentDose = Integer.parseInt(doseEditText.getText().toString());
                    if (currentDose > 0) {
                        doseEditText.setText(String.valueOf(currentDose - 1));
                    }
                } catch (NumberFormatException e) {
                    doseEditText.setText("0");
                }
            }
        });

        timeEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String medicationName = medicationNameEditText.getText().toString().trim();
                String dose = doseEditText.getText().toString().trim();
                String time = timeEditText.getText().toString().trim();
                String repeatOption = repeatSpinner.getSelectedItem().toString();

                if (medicationName.isEmpty() || dose.isEmpty() || time.isEmpty()) {
                    Toast.makeText(Generar_alarma.this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
                } else {
                    Medication newMedication = new Medication(medicationName, dose, time, repeatOption);
                    saveMedicationToRealtimeDatabase(newMedication);
                    finish();
                }
            }
        });
    }


    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);


        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // Cuando el usuario selecciona una hora, se ejecuta este método
                        // Formatear la hora a HH:MM (por ejemplo, 08:05, 14:30)
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        timeEditText.setText(formattedTime); // Establecer la hora seleccionada en el EditText
                    }
                }, hour, minute, true); // true para formato de 24 horas, false para AM/PM

        timePickerDialog.show(); // Mostrar el diálogo
    }

    private void saveMedicationToRealtimeDatabase(Medication medication) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String uid = user.getUid(); // ID del usuario autenticado

            // Generar un ID único para el medicamento
            String medicationId = mDatabase.child("usuarios").child(uid).child("medicamentos").push().getKey();

            if (medicationId != null) {
                mDatabase.child("usuarios")
                        .child(uid)
                        .child("medicamentos")
                        .child(medicationId)
                        .setValue(medication)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(Generar_alarma.this, "Medicamento guardado con ID: " + medicationId, Toast.LENGTH_LONG).show();
                            clearForm();
                            long tiempoEnMilisegundos = obtenerHoraDelMedicamentoEnMillis(medication.getTime());

                            if (tiempoEnMilisegundos > System.currentTimeMillis()) {
                                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                                Intent intent = new Intent(this, Vista_alarma.class);
                                intent.putExtra("nombre_medicamento", medication.getName()); // puedes pasar datos al receiver

                                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                        this, medicationId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                );

                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tiempoEnMilisegundos, pendingIntent);

                                Toast.makeText(this, "Alarma programada", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "La hora ya pasó. No se programó alarma.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(Generar_alarma.this, "Error al guardar medicamento: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            System.err.println("Error saving medication: " + e.getMessage());
                        });
            } else {
                Toast.makeText(Generar_alarma.this, "No se pudo generar ID para el medicamento.", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(Generar_alarma.this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        medicationNameEditText.setText("");
        doseEditText.setText("0");
        timeEditText.setText("");
        repeatSpinner.setSelection(0);
    }

    private long obtenerHoraDelMedicamentoEnMillis(String hora) {
        try {
            SimpleDateFormat formato = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date fecha = formato.parse(hora);

            Calendar calendario = Calendar.getInstance();
            calendario.setTime(fecha);

            // Establece la hora actual con la hora del medicamento
            Calendar ahora = Calendar.getInstance();
            calendario.set(Calendar.YEAR, ahora.get(Calendar.YEAR));
            calendario.set(Calendar.MONTH, ahora.get(Calendar.MONTH));
            calendario.set(Calendar.DAY_OF_MONTH, ahora.get(Calendar.DAY_OF_MONTH));

            // Si la hora ya pasó, programa para mañana
            if (calendario.getTimeInMillis() < System.currentTimeMillis()) {
                calendario.add(Calendar.DAY_OF_MONTH, 1);
            }

            return calendario.getTimeInMillis();

        } catch (ParseException e) {
            e.printStackTrace();
            return System.currentTimeMillis();
        }
    }


}