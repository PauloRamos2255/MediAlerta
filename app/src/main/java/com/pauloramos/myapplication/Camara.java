package com.pauloramos.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Camara extends AppCompatActivity {

    private ImageButton toggleFlash, flipCamera;
    private CardView capture;
    private PreviewView previewView;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private ImageCapture imageCapture;
    private OrientationEventListener orientationEventListener;
    private int rotationDegrees = 0;

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        startCamera(cameraFacing);
                    } else {
                        Toast.makeText(Camara.this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camara);

        previewView = findViewById(R.id.camara);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.flashon);
        flipCamera = findViewById(R.id.flipoff);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }

        flipCamera.setOnClickListener(v -> {
            cameraFacing = (cameraFacing == CameraSelector.LENS_FACING_BACK) ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            startCamera(cameraFacing);
        });
    }

    private void startCamera(int cameraFacing) {
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(aspectRatio)
                        .build();

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing)
                        .build();

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                capture.setOnClickListener(v -> takePicture());
                toggleFlash.setOnClickListener(v -> toggleFlash(camera));

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePicture() {
        if (imageCapture == null) return;

        File file = new File(getExternalFilesDir(null), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(outputOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(Camara.this, Foto.class);
                    intent.putExtra("imagePath", file.getPath());
                    intent.putExtra("imageRotation", getRotationFromImage(file));
                    startActivity(intent);
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    Toast.makeText(Camara.this, "Error al guardar la imagen: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    startCamera(cameraFacing);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN || imageCapture == null) return;

                int newRotation;
                int newDegrees;

                if (orientation >= 315 || orientation < 45) {
                    newRotation = Surface.ROTATION_0;
                    newDegrees = 0;
                } else if (orientation >= 45 && orientation < 135) {
                    newRotation = Surface.ROTATION_270;
                    newDegrees = 270;
                } else if (orientation >= 135 && orientation < 225) {
                    newRotation = Surface.ROTATION_180;
                    newDegrees = 180;
                } else {
                    newRotation = Surface.ROTATION_90;
                    newDegrees = 90;
                }

                imageCapture.setTargetRotation(newRotation);
                toggleFlash.animate().rotation(newDegrees).setDuration(200).start();
                flipCamera.animate().rotation(newDegrees).setDuration(200).start();
                capture.animate().rotation(newDegrees).setDuration(200).start();
            }
        };
        orientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (orientationEventListener != null) {
            orientationEventListener.disable();
        }
    }

    private void toggleFlash(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            boolean isTorchOn = camera.getCameraInfo().getTorchState().getValue() == 1;
            camera.getCameraControl().enableTorch(!isTorchOn);
            toggleFlash.setImageResource(isTorchOn ? R.drawable.baseline_flash_on_24 : R.drawable.baseline_flash_off_24);
        } else {
            Toast.makeText(this, "Flash no disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private int aspectRatio(int width, int height) {
        double ratio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(ratio - 4.0 / 3.0) <= Math.abs(ratio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    private int getRotationFromImage(File imageFile) {
        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
