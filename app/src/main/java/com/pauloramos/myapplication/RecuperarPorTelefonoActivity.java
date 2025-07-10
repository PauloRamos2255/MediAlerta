package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RecuperarPorTelefonoActivity extends AppCompatActivity {

    private EditText txtNumeroTelefono;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_por_telefono);

        txtNumeroTelefono = findViewById(R.id.txtNumeroTelefono);
        mAuth = FirebaseAuth.getInstance();
        findViewById(R.id.btnEnviarCodigo).setOnClickListener(v -> {
            String numero = txtNumeroTelefono.getText().toString().trim();

            if (numero.isEmpty() || numero.length() < 9) {
                txtNumeroTelefono.setError("Número inválido");
                txtNumeroTelefono.requestFocus();
                return;
            }
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber("+51" + numero) // cambia código según tu país
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(PhoneAuthCredential credential) {
                            // Auto-verificado
                        }

                        @Override
                        public void onVerificationFailed(FirebaseException e) {
                            Toast.makeText(RecuperarPorTelefonoActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                            Intent intent = new Intent(RecuperarPorTelefonoActivity.this, VerificarCodigoActivity.class);
                            intent.putExtra("verificationId", verificationId);
                            intent.putExtra("numero", numero);
                            startActivity(intent);
                        }
                    })
                    .build();

            PhoneAuthProvider.verifyPhoneNumber(options);
        });
    }
}