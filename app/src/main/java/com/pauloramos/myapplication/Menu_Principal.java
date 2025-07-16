package com.pauloramos.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;



public class Menu_Principal extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int REQUEST_IMAGE_CAMERA = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private ImageView menu;
    private ImageView historial;
    private FloatingActionButton agregar;
    private RecyclerView medicationsRecyclerView;
    private MedicationAdapter medicationAdapter;
    private List<Medication> medicationList;
    private DatabaseReference mDatabase;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Uri imageUri;
    private TextView nombre , correo;

    private ImageView imageViewUser;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private String userId = "usuario123";

    private static final int REQUEST_CODE_NOTIFICACIONES = 1001;
    private static final String CHANNEL_ID = "canal_bienvenida";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        databaseReference = FirebaseDatabase.getInstance().getReference("usuarios");
        storageReference = FirebaseStorage.getInstance().getReference("fotos_perfil");

        menu = findViewById(R.id.menu_ajus);
        historial = findViewById(R.id.historial_alert);
        agregar = findViewById(R.id.addMedicationFab);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        nombre = headerView.findViewById(R.id.textViewUserName);
        correo = headerView.findViewById(R.id.textViewSubtitle);
        imageViewUser = headerView.findViewById(R.id.imageViewUser);
        ImageView imageViewCamera = headerView.findViewById(R.id.imageViewCamera);

        navigationView.setNavigationItemSelectedListener(this);

        mDatabase = FirebaseDatabase.getInstance().getReference("medicamentos");
        medicationsRecyclerView = findViewById(R.id.medicationsRecyclerView);
        medicationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        medicationList = new ArrayList<>();
        medicationAdapter = new MedicationAdapter(medicationList);
        medicationsRecyclerView.setAdapter(medicationAdapter);
        loadMedicationsFromFirebase();


        String nombres = getIntent().getStringExtra("usuario_nombre");
        String telefonos= getIntent().getStringExtra("usuario_telefono");
        String coreos = getIntent().getStringExtra("usuario_correo");

        nombre.setText(nombres);
        correo.setText(coreos);



        menu.setOnClickListener(v -> drawerLayout.openDrawer(navigationView));

        imageViewCamera.setOnClickListener(v -> {
            verificarPermisos();
        });

        historial.setOnClickListener(v -> startActivity(new Intent(this, Historial.class)));

        agregar.setOnClickListener(v -> startActivity(new Intent(this, Generar_alarma.class)));

        cargarFotoPerfil();

        SharedPreferences prefs = getSharedPreferences("configuraciones", MODE_PRIVATE);
        boolean notificacionesActivadas = prefs.getBoolean("notificaciones_activadas", true);
        String idioma = prefs.getString("idioma_seleccionado", "Español");
        String tono = prefs.getString("tono_seleccionado", "Tono 1");

        if (notificacionesActivadas) {
            mostrarNotificacionBienvenida(idioma);
        }
    }

    private void verificarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            showPhotoOptionsDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPhotoOptionsDialog();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cargarFotoPerfil() {
        databaseReference.child(userId).child("fotoPerfil").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !url.isEmpty()) {
                    Glide.with(Menu_Principal.this)
                            .load(url)
                            .placeholder(R.drawable.defaultplaceholder)
                            .into(imageViewUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error al obtener la imagen", error.toException());
            }
        });
    }

    private void showPhotoOptionsDialog() {
        String[] options = {"Seleccionar de galería", "Tomar foto"};
        new AlertDialog.Builder(this)
                .setTitle("Cambiar foto de perfil")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        abrirGaleria();
                    } else {
                        abrirCamara();
                    }
                })
                .show();
    }

    private void abrirCamara() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            Intent camara = new Intent(Menu_Principal.this , Camara.class);
            startActivity(camara);
        } else {
            Toast.makeText(this, "No se encontró aplicación de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirGaleria() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_IMAGE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_GALLERY && data != null) {
                imageUri = data.getData();
                subirFotoFirebase();
            } else if (requestCode == REQUEST_IMAGE_CAMERA && data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageUri = getImageUriFromBitmap(photo);
                subirFotoFirebase();
            }
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "FotoPerfil", null);
        return Uri.parse(path);
    }

    private void subirFotoFirebase() {
        if (imageUri != null) {
            StorageReference fileRef = storageReference.child(userId + ".jpg");

            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        databaseReference.child(userId).child("fotoPerfil").setValue(downloadUrl);
                        Glide.with(this).load(downloadUrl).placeholder(R.drawable.defaultplaceholder).into(imageViewUser);
                        Toast.makeText(this, "Foto actualizada correctamente", Toast.LENGTH_SHORT).show();
                    }))
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al subir la foto: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void loadMedicationsFromFirebase() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = usuario.getUid();
        DatabaseReference medicamentosRef = FirebaseDatabase.getInstance()
                .getReference("usuarios")
                .child(uid)
                .child("medicamentos");

        medicamentosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                medicationList.clear();

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Medication medication = postSnapshot.getValue(Medication.class);
                    if (medication != null) {
                        medicationList.add(medication);
                    }
                }

                medicationAdapter.updateMedications(medicationList); // Asegúrate de tener este método en tu Adapter
                Log.d("Menu_Principal", "Cargados: " + medicationList.size() + " medicamentos");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Menu_Principal", "Error al cargar: " + error.getMessage());
                Toast.makeText(Menu_Principal.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Toast.makeText(this, "Mis Medicamentos", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_history) {
            startActivity(new Intent(this, Historial.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, Ajuste.class));
        } else if (id == R.id.nav_compra) {
            startActivity(new Intent(this,compra.class));
        }

        drawerLayout.closeDrawer(navigationView);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }


    private void mostrarNotificacionBienvenida(String idioma) {
        crearCanalDeNotificacion();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de notificaciones no concedido", Toast.LENGTH_SHORT).show();
            return;
        }

        String mensaje = idioma.equals("Inglés") ? "Welcome back!" :
                idioma.equals("Portugués") ? "Bem-vindo de volta!" :
                        "¡Bienvenido de nuevo!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "canal_bienvenida")
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("¡Bienvenido!")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        try {
            notificationManager.notify(1, builder.build());
        } catch (SecurityException e) {
            Toast.makeText(this, "No se pudo mostrar la notificación: falta permiso", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void crearCanalDeNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "canal_bienvenida",
                    "Notificaciones de bienvenida",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Este canal muestra una notificación de bienvenida al usuario");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

}