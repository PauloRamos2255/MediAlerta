package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class RecuperarClaveActivity extends AppCompatActivity {


    private EditText txtCorreo ;
    private TextView txtvolver;
    private Button btnEnviar , btnOtraForma;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_clave);

        txtCorreo = findViewById(R.id.txtCorreoRecuperar);
        txtvolver = findViewById(R.id.tvVolverLogin);
        btnEnviar = findViewById(R.id.btnEnviarCorreo);
        btnOtraForma = findViewById(R.id.btnRecuperarPorTelefono);
        mAuth = FirebaseAuth.getInstance();


        txtvolver.setOnClickListener(v ->{
            finish();
        });
        btnOtraForma.setOnClickListener(view -> {
            startActivity(new Intent(RecuperarClaveActivity.this , RecuperarPorTelefonoActivity.class));
                }
        );

        btnEnviar.setOnClickListener(view -> {
            String correo = txtCorreo.getText().toString().trim();

            if (correo.isEmpty()) {
                txtCorreo.setError("El correo es obligatorio");
                txtCorreo.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                txtCorreo.setError("Correo inválido");
                txtCorreo.requestFocus();
                return;
            }

            enviarCorreoRecuperacion(view, correo);
        });
    }

    private void enviarCorreoRecuperacion(View view, String correo) {
        mAuth.sendPasswordResetEmail(correo)
                .addOnSuccessListener(aVoid -> {
                    Snackbar.make(view, "Revisa tu correo para restablecer la contraseña", Snackbar.LENGTH_LONG).show();
                    Intent intent = new Intent(RecuperarClaveActivity.this , Login.class);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Snackbar.make(view, "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }
}