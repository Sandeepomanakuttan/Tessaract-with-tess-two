package com.example.sandeep.tessartlib;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private TessBaseAPI tessBaseAPI;
    private static Uri photoURI;
    private String mCurrentPhotoPath;
    File file;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.img);
        textView = this.findViewById(R.id.textView);
        Button button = this.findViewById(R.id.button);
        checkPermission();
        button.setOnClickListener(v -> {
            checkPermission();
            TakePictureIntent();
            textView.setText("Please wait....");
        });
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }
    }


    @SuppressLint("QueryPermissionsNeeded")
    private void TakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {

            try {
                file = createImageFile(getApplicationContext());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (file != null) {
                photoURI = FileProvider.getUriForFile(getApplicationContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        file);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        }

    }

    private File
    createImageFile(Context context) throws IOException  {

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String mFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File mFile = File.createTempFile(mFileName, ".jpg", storageDir);
        Log.d("", String.valueOf(mFile));
        return mFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK && result!=null) {
                Uri resultUri = result.getUri();
                photoURI=resultUri;
                mCurrentPhotoPath = resultUri.getPath();
                prepareTessData();
                startOCR(photoURI);
                imageView.setImageURI(resultUri);

                Log.e("uploadImage", "=" + mCurrentPhotoPath);

            }
            if(resultCode==-1 && data==null)
            {

                CropImage.activity(photoURI)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setCropShape(CropImageView.CropShape.RECTANGLE) //You can skip this for free form aspect ratio)
                        .start(this);

            }
        }
        }

    private void cropImage(Uri img_uri) {
        CropImage.activity(img_uri).setGuidelines(CropImageView.Guidelines.ON).setMultiTouchEnabled(true).start(this);
    }

    private void prepareTessData(){
        try{
            File dir = getExternalFilesDir(mCurrentPhotoPath);
            if(!dir.exists()){
                if (!dir.mkdir()) {
                    Toast.makeText(getApplicationContext(), "The folder " + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
                }
            }
            String fileList[] = getAssets().list("");
            for(String fileName : fileList){
               // String pathToDataFile = dir + "/" + fileName;
                if(!(new File(mCurrentPhotoPath)).exists()){
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(mCurrentPhotoPath);
                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void startOCR(Uri photoURI){
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = 6;
            Bitmap bitmap = BitmapFactory.decodeFile(photoURI.getPath(), options);

                    String result = this.getText(bitmap);
                    textView.setText(result);


        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
        }
        String dataPath = getExternalFilesDir("/").getPath() + "/";
        tessBaseAPI.init(dataPath, "eng");
        tessBaseAPI.setImage(bitmap);
        String retStr = "No result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
            Toast.makeText(this, "bind error", Toast.LENGTH_SHORT).show();
        }
        tessBaseAPI.end();
        return retStr;
    }


}