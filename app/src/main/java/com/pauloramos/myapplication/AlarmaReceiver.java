package com.pauloramos.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.widget.Toast;

public class AlarmaReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String nombreMedicamento = intent.getStringExtra("nombre_medicamento");

        Intent ventanaAlarma = new Intent(context, Vista_alarma.class);
        ventanaAlarma.putExtra("nombre_medicamento", nombreMedicamento);
        ventanaAlarma.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Esto es obligatorio

        context.startActivity(ventanaAlarma);
    }
}

