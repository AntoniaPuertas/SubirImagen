package com.example.subirimagen;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    Button btnGaleria;
    Button btnCamara;
    ImageView imgImagen;
    Button btnSubirImagen;
    Bitmap imagenSeleccionada;

    final static int RESULTADO_GALERIA = 2;
    final static int RESULTADO_CAMARA = 4;

    final static int MY_PERMISSIONS_REQUEST = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGaleria = findViewById(R.id.btnGaleria);
        btnCamara = findViewById(R.id.btnCamara);
        imgImagen = findViewById(R.id.imgImagen);
        btnSubirImagen = findViewById(R.id.btnSubirImagen);

        btnGaleria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, RESULTADO_GALERIA);
            }
        });

        btnCamara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solicitarPermiso();

                }
        });

        btnSubirImagen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imagenSeleccionada != null){
                    enviarServidor();
                }else{
                    Toast.makeText(MainActivity.this, "Tienes que seleccionar una imagen", Toast.LENGTH_LONG).show();
                }
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == RESULTADO_GALERIA){
            if(resultCode == Activity.RESULT_OK){

                ponerFoto(data.getDataString());
                Log.d("Ruta imagen", data.getDataString());
            }else{
                Toast.makeText(MainActivity.this,
                        "La foto no se ha cargado",
                        Toast.LENGTH_SHORT).show();
            }

        }else if(requestCode == RESULTADO_CAMARA){

            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imgImagen.setImageBitmap(imageBitmap);
            saveImage(imageBitmap);
        }
    }

    public void ponerFoto(String uri){
        if(uri != null && !uri.isEmpty() && !uri.equals("null")){
            Uri imageUri = Uri.parse(uri);
            imgImagen.setImageURI(imageUri);
            try {
                saveImage(MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveImage(Bitmap bitmap){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "Image_" + timeStamp + "jpg";
        File file = new File(directory, fileName);
        if(!file.exists()){
            Log.d("pathImage", file.toString());
            FileOutputStream fos = null;
            try{
                fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();
                imagenSeleccionada = bitmap;
                Log.d("pathImage", file.toString());
            }catch (java.io.IOException e){
                e.printStackTrace();
            }
        }
    }


    private void enviarServidor(){
        Toast.makeText(MainActivity.this, "enviar al servidor", Toast.LENGTH_LONG).show();
    }

    private void  solicitarPermiso(){
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Explicamos por qué necesitamos el permiso
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.CAMERA)) {

                // continuamos con el proceso

            } else {

                // El usuario no necesita explicación, puedes solicitar el permiso

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST);

            }
        }else{
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(intent.resolveActivity(getPackageManager()) != null){
                startActivityForResult(intent, RESULTADO_CAMARA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //tenemos permiso para utilizar la cámara
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if(intent.resolveActivity(getPackageManager()) != null){
                        startActivityForResult(intent, RESULTADO_CAMARA);
                    }

                } else {
                    //no tenemos permiso para utilizar la cámara
                    Toast.makeText(MainActivity.this, "La app no tiene permiso para utilizar la cámara", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
}
