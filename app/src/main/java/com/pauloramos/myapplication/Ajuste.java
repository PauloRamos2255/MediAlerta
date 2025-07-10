package com.pauloramos.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ajuste extends AppCompatActivity {

    private SharedPreferences prefs;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final int REQUEST_CODE_TONO = 1001;



    private Handler handlerRepeticion = new Handler();
    private Runnable repetirPreview;
    private boolean dialogoAbierto = false;

    private MediaPlayer mediaPlayer;
    private int tonoSeleccionadoIndex = -1;
    private String tonoSeleccionadoTemp = "";
    private Uri tonoPersonalizadoUri = null;
    private boolean seleccionPersonalizada = false;
    private String nombreArchivoPersonalizado = "";
    private AlertDialog alertDialog;

    private static final int REQUEST_TONO_PERSONALIZADO = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajuste);



        prefs = getSharedPreferences("configuraciones", MODE_PRIVATE);

        LinearLayout opcionIdioma = findViewById(R.id.opcion_idioma);
        LinearLayout opcionTono = findViewById(R.id.opcion_tono);
        Switch switchNotificaciones = findViewById(R.id.switchNotificaciones);

        boolean notificacionesActivas = prefs.getBoolean("notificaciones_activadas", true);
        switchNotificaciones.setChecked(notificacionesActivas);

        switchNotificaciones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("notificaciones_activadas", isChecked);
            editor.apply();
        });

        switchNotificaciones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            REQUEST_NOTIFICATION_PERMISSION);
                } else {
                    guardarEstadoNotificacion(true);
                    Toast.makeText(this, "Notificaciones activadas", Toast.LENGTH_SHORT).show();
                }
            } else {
                guardarEstadoNotificacion(isChecked);
                if (!isChecked) {
                    Toast.makeText(this, "Notificaciones desactivadas", Toast.LENGTH_SHORT).show();
                }
            }
        });

        opcionIdioma.setOnClickListener(v -> mostrarDialogoIdioma());
        opcionTono.setOnClickListener(v -> mostrarDialogoTonoPersonalizado());
    }


    private void guardarEstadoNotificacion(boolean estado) {
        SharedPreferences.Editor editor = getSharedPreferences("configuraciones", MODE_PRIVATE).edit();
        editor.putBoolean("notificaciones_activadas", estado);
        editor.apply();
    }

    private void mostrarDialogoIdioma() {


        String[] idiomas = {"Español", "Inglés", "Portugués"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona un idioma")
                .setItems(idiomas, (dialog, which) -> {
                    String idiomaSeleccionado = idiomas[which];

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("idioma_seleccionado", idiomaSeleccionado);
                    editor.apply();

                    Toast.makeText(this, "Idioma seleccionado: " + idiomaSeleccionado, Toast.LENGTH_SHORT).show();
                });
        builder.create().show();
    }


    private void mostrarDialogoTonoPersonalizado() {
        dialogoAbierto = true;
        prefs = getSharedPreferences("configuraciones", MODE_PRIVATE);
        tonoSeleccionadoTemp = prefs.getString("tono_seleccionado", "");
        String nombreGuardado = prefs.getString("nombre_tono_personalizado", "");

        List<String> listaTonos = new ArrayList<>(Arrays.asList("Tono 1", "Tono 2", "Tono 3"));
        String opcionPersonalizada = nombreGuardado.isEmpty() ? "Seleccionar desde almacenamiento"
                : "Tono personalizado: " + nombreGuardado;
        listaTonos.add(opcionPersonalizada);
        String[] tonos = listaTonos.toArray(new String[0]);

        int posicionSeleccionada = -1;
        switch (tonoSeleccionadoTemp) {
            case "tono1": posicionSeleccionada = 0; break;
            case "tono2": posicionSeleccionada = 1; break;
            case "tono3": posicionSeleccionada = 2; break;
            default:
                if (!tonoSeleccionadoTemp.isEmpty()) posicionSeleccionada = 3;
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona un tono de alarma");

        builder.setSingleChoiceItems(tonos, posicionSeleccionada, (dialog, which) -> {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            switch (which) {
                case 0:
                    mediaPlayer = MediaPlayer.create(this, R.raw.tono1);
                    tonoSeleccionadoTemp = "tono1";
                    break;
                case 1:
                    mediaPlayer = MediaPlayer.create(this, R.raw.tono2);
                    tonoSeleccionadoTemp = "tono2";
                    break;
                case 2:
                    mediaPlayer = MediaPlayer.create(this, R.raw.tono3);
                    tonoSeleccionadoTemp = "tono3";
                    break;
                case 3:
                    abrirSelectorDeArchivo(); // NO cerrar el diálogo
                    return;
            }

            if (mediaPlayer != null) {
                mediaPlayer.start();
                new Handler().postDelayed(() -> {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                        Toast.makeText(this, "Reproducción detenida automáticamente", Toast.LENGTH_SHORT).show();
                    }
                }, 60000);

                repetirPreview = new Runnable() {
                    @Override
                    public void run() {
                        if (dialogoAbierto && mediaPlayer != null) {
                            try {
                                mediaPlayer.seekTo(0);
                                mediaPlayer.start();
                                handlerRepeticion.postDelayed(this, 20000);
                            } catch (Exception ignored) {}
                        }
                    }
                };
                handlerRepeticion.postDelayed(repetirPreview, 20000);
            }
        });

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            dialogoAbierto = false;
            handlerRepeticion.removeCallbacks(repetirPreview);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("tono_seleccionado", tonoSeleccionadoTemp);
            editor.apply();
            Toast.makeText(this, "Tono guardado: " + tonoSeleccionadoTemp, Toast.LENGTH_SHORT).show();
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialogoAbierto = false;
            handlerRepeticion.removeCallbacks(repetirPreview);
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            dialog.dismiss();
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    private void abrirSelectorDeArchivo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_TONO_PERSONALIZADO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TONO_PERSONALIZADO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                tonoSeleccionadoTemp = uri.toString();
                String nombreArchivo = obtenerNombreArchivo(uri);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("tono_seleccionado", tonoSeleccionadoTemp);
                editor.putString("nombre_tono_personalizado", nombreArchivo);
                editor.apply();

                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(this, uri);
                    mediaPlayer.prepare();
                    mediaPlayer.start();

                    new Handler().postDelayed(() -> {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            mediaPlayer.release();
                            mediaPlayer = null;
                        }
                    }, 60000);

                    Toast.makeText(this, "Tono personalizado seleccionado", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Error al reproducir el tono", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

                // Reabrir el diálogo si fue cerrado
                mostrarDialogoTonoPersonalizado();
            }
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = "";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nombreIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nombreIndex >= 0) {
                nombre = cursor.getString(nombreIndex);
            }
            cursor.close();
        }
        return nombre;
    }







}
