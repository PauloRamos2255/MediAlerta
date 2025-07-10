package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CambiarClaveActivity extends AppCompatActivity {

    private EditText txtNuevaClave, txtConfirmarClave;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_clave);

        txtNuevaClave = findViewById(R.id.txtNuevaClave);
        txtConfirmarClave = findViewById(R.id.txtConfirmarClave);
        mAuth = FirebaseAuth.getInstance();

        findViewById(R.id.btnCambiarClave).setOnClickListener(v -> {
            String nuevaClave = txtNuevaClave.getText().toString().trim();
            String confirmarClave = txtConfirmarClave.getText().toString().trim();

            if (nuevaClave.isEmpty() || nuevaClave.length() < 6) {
                txtNuevaClave.setError("Mínimo 6 caracteres");
                txtNuevaClave.requestFocus();
                return;
            }

            if (!nuevaClave.equals(confirmarClave)) {
                txtConfirmarClave.setError("Las contraseñas no coinciden");
                txtConfirmarClave.requestFocus();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.updatePassword(nuevaClave)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Intent intent = new Intent(CambiarClaveActivity.this, RecuperacionExitosaActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Error al actualizar clave", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });


    }
}