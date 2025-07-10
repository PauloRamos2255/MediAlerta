package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;


public class Registro_Usuario extends AppCompatActivity {


    private EditText txtNombres, txtApellidos, txtTelefono, txtCorreo, txtClave;
    private Button btnRegistrar;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_usuario);

        txtNombres = findViewById(R.id.txtnombres);
        txtApellidos = findViewById(R.id.txtapellidos);
        txtTelefono = findViewById(R.id.txtTelefono);
        txtCorreo = findViewById(R.id.txtcorreo);
        txtClave = findViewById(R.id.txtclave);
        btnRegistrar = findViewById(R.id.btn_registrar);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("usuarios");

        btnRegistrar.setOnClickListener(v -> registrarUsuario());

    }


    private void registrarUsuario() {
        String nombres = txtNombres.getText().toString().trim();
        String apellidos = txtApellidos.getText().toString().trim();
        String telefono = txtTelefono.getText().toString().trim();
        String correo = txtCorreo.getText().toString().trim();
        String clave = txtClave.getText().toString().trim();

        if (nombres.isEmpty() || apellidos.isEmpty() || telefono.isEmpty() || correo.isEmpty() || clave.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        Map<String, Object> datos = new HashMap<>();
                        datos.put("nombres", nombres);
                        datos.put("apellidos", apellidos);
                        datos.put("telefono", telefono);
                        datos.put("correo", correo);
                        // No se pone "fotoPerfil" aún

                        databaseRef.child(uid).setValue(datos)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}