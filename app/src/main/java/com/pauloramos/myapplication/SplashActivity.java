package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import android.Manifest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {


    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }


        new Handler().postDelayed(() -> {
            FirebaseUser usuarioActual = FirebaseAuth.getInstance().getCurrentUser();

            if (usuarioActual != null) {
                String uid = usuarioActual.getUid();

                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("usuarios")
                        .child(uid);

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Usuario usuario = snapshot.getValue(Usuario.class);

                            Intent intent = new Intent(SplashActivity.this, Menu_Principal.class);
                            intent.putExtra("usuario_nombre", usuario.getNombres());
                            intent.putExtra("usuario_telefono", usuario.getTelefono());
                            intent.putExtra("usuario_correo", usuario.getCorreo());
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(SplashActivity.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SplashActivity.this, Login.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(SplashActivity.this, "Error al leer datos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SplashActivity.this, Login.class));
                        finish();
                    }
                });

            } else {
                startActivity(new Intent(SplashActivity.this, Login.class));
                finish();
            }
        }, 2000);

    }
}