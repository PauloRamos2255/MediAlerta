package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Source;

public class Login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText correoEditText, passwordEditText;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        correoEditText = findViewById(R.id.txtusuario);
        passwordEditText = findViewById(R.id.txtclave);
        loginButton = findViewById(R.id.btningresar);
        TextView txtOlvidarClave = findViewById(R.id.txtOlvidarClave);
        TextView txtIrRegistrar = findViewById(R.id.txtIrRegistrar);

        txtOlvidarClave.setOnClickListener(v -> {
            startActivity(new Intent(this, RecuperarClaveActivity.class));
        });

        txtIrRegistrar.setOnClickListener(v -> {
            startActivity(new Intent(this, Registro_Usuario.class));
        });

        loginButton.setOnClickListener(view -> {
            String email = correoEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            } else {
                iniciarSesion(email, password);
            }
        });
    }

    private void iniciarSesion(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            Toast.makeText(this, "UID: " + uid, Toast.LENGTH_SHORT).show();

                            // Leer los datos del usuario desde Realtime Database
                            DatabaseReference ref = FirebaseDatabase.getInstance()
                                    .getReference("usuarios")
                                    .child(uid);

                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        Usuario usuario = snapshot.getValue(Usuario.class);

                                        Log.d("Usuario", "Nombres: " + usuario.getNombres());
                                        Log.d("Usuario", "Teléfono: " + usuario.getTelefono());
                                        Log.d("Usuario", "Correo: " + usuario.getCorreo());

                                        Intent intent = new Intent(Login.this, Menu_Principal.class);
                                        intent.putExtra("usuario_nombre", usuario.getNombres());
                                        intent.putExtra("usuario_telefono", usuario.getTelefono());
                                        intent.putExtra("usuario_correo", usuario.getCorreo());
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(Login.this, "No se encontró el usuario", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    Toast.makeText(Login.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this, "Error al iniciar sesión: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("LoginError", "Fallo de autenticación", e);
                    }
                });
    }


}
