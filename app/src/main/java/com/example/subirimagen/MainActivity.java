package com.example.subirimagen;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imagenSeleccionada.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] byteArray = baos.toByteArray();
        String base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

        Map<String, String> cuerpo = new HashMap<String, String>();
        cuerpo.put("imagen_base64", base64);
        JSONObject jsonObject = new JSONObject(cuerpo);

        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST,
                "https://apcpruebas.es/toni/subir_imagenes",
                jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // array disponible

                        try {
                            int resultado = response.getInt("estado");
                            if(resultado == 0){
                                Toast.makeText(MainActivity.this, "Imagen subida", Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(MainActivity.this, "Error al subir la imagen", Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.i("error subida imagen", error.toString());
                Toast.makeText(MainActivity.this, "Error al subir la imagen", Toast.LENGTH_LONG).show();
            }

        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Auto", "21232F297A57A5A743894A0E4A801FC3");
                return headers;
            }
        };


        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(peticion);
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
