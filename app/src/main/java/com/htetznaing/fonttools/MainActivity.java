package com.htetznaing.fonttools;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_CODE = 10;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressDialog = new ProgressDialog(this);
        reqPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0]!=PackageManager.PERMISSION_GRANTED){
            reqPermission();
        }
    }

    private boolean reqPermission() {
        int storage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (storage!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void unpack(View view) {
        Toast.makeText(this, "Select your emoji/color font ttf file!", Toast.LENGTH_SHORT).show();
        openFilePicker("ttf");
    }

    public void repack(View view) {
        Toast.makeText(this, "Select your ttx file!", Toast.LENGTH_SHORT).show();
        openFilePicker("ttx");
    }

    private void openFilePicker(String ext){
        Intent i = new Intent(this, FilePicker.class);
        i.putExtra(FilePicker.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePicker.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePicker.EXTRA_MODE, FilePicker.MODE_FILE);
        i.putExtra("extensions","."+ext);
        i.putExtra(FilePicker.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
        startActivityForResult(i, FILE_CODE);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            // Use the provided utility method to parse the result
            List<Uri> files = Utils.getSelectedFilesFromResult(intent);
            File file = Utils.getFileForUri(files.get(0));
            if (file.getName().endsWith(".ttf")){
                //Unpack
                worker(file.getPath(),true);
            }else {
                //Repack
                worker(file.getPath(),false);
            }
        }
    }

    private void worker(final String path, final boolean unpack){
        new AsyncTask<Void,Void,Boolean>(){
            private String message = "ERROR";
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(getApplicationContext()));
                }
                progressDialog.setMessage(unpack ? "Extracting.." : "Repacking..");
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                Python py = Python.getInstance();
                PyObject ttx = py.getModule("hello");
                if (unpack){
                    try {
                        File ttxFile = new File(path.replace(".ttf",".ttx"));
                        message = ttxFile.getPath()+"\n"+ttxFile.getParentFile().getPath()+"/bitmaps";
                        if (ttxFile.exists()){
                            ttxFile.delete();
                        }
                        File bm = new File(ttxFile.getParentFile().getPath()+"/bitmaps");
                        if (bm.exists()){
                            bm.delete();
                        }
                        ttx.callAttrThrows("extract",path);
                        return ttxFile.exists();
                    } catch (Throwable throwable) {
                        message = throwable.getMessage();
                        throwable.printStackTrace();
                        return false;
                    }
                }else {
                    try {
                        File ttxFile = new File(path.replace(".ttx","_REPACK.ttf"));
                        message = "Output => "+ttxFile.getPath();
                        if (ttxFile.exists()){
                            ttxFile.delete();
                        }
                        ttx.callAttrThrows("build",path);
                        File outFile = new File(path.replace(".ttx","#1.ttf"));
                        File bm = new File(ttxFile.getParentFile().getPath()+"/bitmaps");
                        if (bm.exists()){
                            bm.delete();
                        }
                        return outFile.exists() && outFile.renameTo(ttxFile);
                    } catch (Throwable throwable) {
                        message = throwable.getMessage();
                        throwable.printStackTrace();
                        return false;
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                progressDialog.dismiss();
                String title = "Failed!";
                if (aBoolean){
                    title = "Success!";
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("OK",null);
                builder.show();
            }
        }.execute();
    }
}
