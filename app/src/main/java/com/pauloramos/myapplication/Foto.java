package com.pauloramos.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class Foto extends AppCompatActivity {

    private Bitmap rotatedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foto);

        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("imagePath");
        int imageRotation = intent.getIntExtra("imageRotation", 0);

        ImageView imageView = findViewById(R.id.imageView);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

        // Rotamos la imagen si es necesario
        Matrix matrix = new Matrix();
        matrix.postRotate(imageRotation);
        rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        imageView.setImageBitmap(rotatedBitmap);

        // Botón Confirmar
        CardView cdConfirmar = findViewById(R.id.cdconfirmar);
        cdConfirmar.setOnClickListener(v -> subirImagenAFirebase(rotatedBitmap));
    }

    private void subirImagenAFirebase(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();

        String userId = "usuario123"; // Puedes reemplazar esto por FirebaseAuth.getInstance().getCurrentUser().getUid()
        StorageReference imagenRef = FirebaseStorage.getInstance()
                .getReference()
                .child("fotos_perfil/" + userId + ".jpg");

        UploadTask uploadTask = imagenRef.putBytes(data);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imagenRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String urlImagen = uri.toString();
                guardarFotoEnRealtimeDatabase(userId, urlImagen);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void guardarFotoEnRealtimeDatabase(String userId, String url) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        // ✅ Ahora guarda en: usuarios/usuario123/fotoPerfil
        dbRef.child("usuarios").child(userId).child("fotoPerfil").setValue(url)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Imagen guardada correctamente", Toast.LENGTH_SHORT).show();
                   Intent intent = new Intent(Foto.this , Menu_Principal.class);
                   startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show();
                });
    }

}
