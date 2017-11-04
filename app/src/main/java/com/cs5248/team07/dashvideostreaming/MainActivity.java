package com.cs5248.team07.dashvideostreaming;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.content.Intent;
import android.widget.ListView;
import android.app.Activity;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import android.widget.ProgressBar;


public class MainActivity extends AppCompatActivity{

    private TextView mTextMessage;
    private String outputPath;
    private Intent videoIntent;
    private ProgressBar segmentProgressBar;
    private ProgressBar uploadProgressBar;
    private TextView segmentProgressView;
    private TextView uploadProgressView;
    ListView fileList;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    ListAllVideos();
                    return true;
                case R.id.navigation_camera:
                    openCamera();
                    return true;
                case R.id.navigation_notifications:
                    uploadSingleFile(getAppStoragePath(MainActivity.this));
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        ListAllVideos();

    }

    public void ListAllVideos(){
        ArrayList<String> filesinfolder = GetFiles(getAppStoragePath(MainActivity.this));
        if(filesinfolder.size() >= 1) {
            fileList = (ListView) findViewById(R.id.fileListView);
            fileList.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, filesinfolder));
        }
    }

    public void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }


    public ArrayList<String> GetFiles(String directorypath){
        System.out.println("The directory path is "+ directorypath);
        ArrayList<String> Myfiles = new ArrayList<String>();
        File f = new File(directorypath);
        File[] files = f.listFiles();
        if(files.length==0){
            return Myfiles;
        }
        else{
            for(int i=0;i<files.length;i++) {
                System.out.println("File name is " + files[i].getName());
                Myfiles.add(files[i].getName());
            }
        }
        return Myfiles;
    }

    private String getAppStoragePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return dir.getAbsolutePath();
    }


    private void uploadSingleFile(String directorypath){
        ArrayList<String> filesinfolder = GetFiles(directorypath);
        String mainFile = directorypath+"/"+filesinfolder.get(3);
        String segmentsPath = directorypath+"/streamlets/"+filesinfolder.get(3)+"/";
        File f = new File(mainFile);
        UploadFile obj = new UploadFile();
        obj.execute(segmentsPath,f.getName(),"SamsungGalaxyTab");
    }

    private void segmentVideo(String directorypath) {
        //Segment the video in splits of 3 seconds
        Log.i("DASH","Inside segmentVideo()");
        ArrayList<String> filesinfolder = GetFiles(directorypath);
        String filepath = directorypath+"/"+filesinfolder.get(3);
        File f = new File(filepath);
        outputPath = getSegmentFolder(f.getName());
        Log.i("DASH", "Path where segments have to be saved is " + outputPath);

        CreateStreamlets obj = new CreateStreamlets(segmentProgressBar, segmentProgressView);
        obj.execute(filepath, outputPath, "3.0");
    }

    private String getSegmentFolder(String innerFolderName) {
        String folderName = "streamlets/" + innerFolderName;
        File segmentFolder = new File(getAppStoragePath(MainActivity.this), folderName);
        segmentFolder.mkdirs();

        return segmentFolder.getPath() + "/";
    }
}


//Rangarajan
