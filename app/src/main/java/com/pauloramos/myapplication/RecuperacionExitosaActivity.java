package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class RecuperacionExitosaActivity extends AppCompatActivity {

    private Button login;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperacion_exitosa);

        login.findViewById(R.id.btnVolverLogin);

        login.setOnClickListener(v -> {
            startActivity(new Intent(RecuperacionExitosaActivity.this , Login.class));
        });


    }
}