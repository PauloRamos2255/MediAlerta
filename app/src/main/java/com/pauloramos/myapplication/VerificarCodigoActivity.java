package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class VerificarCodigoActivity extends AppCompatActivity {

    private EditText txtCodigo;
    private String verificationId;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verificar_codigo);

        txtCodigo = findViewById(R.id.txtCodigo);
        mAuth = FirebaseAuth.getInstance();

        verificationId = getIntent().getStringExtra("verificationId");

        findViewById(R.id.btnVerificarCodigo).setOnClickListener(v -> {
            String codigo = txtCodigo.getText().toString().trim();

            if (codigo.isEmpty() || codigo.length() < 6) {
                txtCodigo.setError("Código inválido");
                txtCodigo.requestFocus();
                return;
            }

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, codigo);

            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(VerificarCodigoActivity.this, CambiarClaveActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show();
                        }
                    });
        });



    }
}