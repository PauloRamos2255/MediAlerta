package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class Vista_alarma extends AppCompatActivity {

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista_alarma);

        SharedPreferences prefs = getSharedPreferences("configuraciones", MODE_PRIVATE);
        String tonoSeleccionado = prefs.getString("tono_seleccionado", "tono1");

        try {
            if (tonoSeleccionado.equals("tono1")) {
                mediaPlayer = MediaPlayer.create(this, R.raw.tono1);
                iniciarAlarma();
            } else if (tonoSeleccionado.equals("tono2")) {
                mediaPlayer = MediaPlayer.create(this, R.raw.tono2);
                iniciarAlarma();
            } else if (tonoSeleccionado.equals("tono3")) {
                mediaPlayer = MediaPlayer.create(this, R.raw.tono3);
                iniciarAlarma();
            } else {
                Uri uri = Uri.parse(tonoSeleccionado);
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, uri);
                mediaPlayer.setOnPreparedListener(mp -> iniciarAlarma());
                mediaPlayer.prepareAsync(); // mejor que prepare() para archivos personalizados
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al reproducir el tono", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        findViewById(R.id.btnOmitir).setOnClickListener(v -> detenerYSalir());
    }

    private void iniciarAlarma() {
        try {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo iniciar el tono", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void detenerYSalir() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detenerYSalir();
    }
}
