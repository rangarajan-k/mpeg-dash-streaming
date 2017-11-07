package com.cs5248.team07.dashvideostreaming;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
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
import java.util.concurrent.ExecutionException;

import android.widget.ProgressBar;

import org.w3c.dom.Text;

import static android.provider.AlarmClock.EXTRA_MESSAGE;


public class MainActivity extends AppCompatActivity{

    private TextView mTextMessage;
    private TextView videoPopUpTitle;
    private TextView videoPopupPlay;
    private TextView videoPopupUpload;
    private TextView videoPopupDelete;
    private String outputPath;
    private Intent videoIntent;
    private ProgressBar segmentProgressBar;
    private ProgressBar uploadProgressBar;
    private TextView segmentProgressView;
    private TextView uploadProgressView;
    private PopupWindow videoPopupWindow;
    private FrameLayout mainLayout;
    private Context mContext;
    private Activity mActivity;
    private TextView mainTitleText;
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
                    //uploadSingleFile(getAppStoragePath(MainActivity.this));
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
        mainTitleText = (TextView) findViewById(R.id.mainTitleText);
        fileList = (ListView) findViewById(R.id.fileListView);
        ArrayList<String> filesinfolder = GetFiles(getAppStoragePath(MainActivity.this));
        if(filesinfolder.size() > 0) {

            fileList.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, filesinfolder));
            mainTitleText.setText("List of Videos");
            fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                                        long id) {
                    String itemName = parent.getAdapter().getItem(position).toString();
                    System.out.println(itemName + "is clicked");
                    showVideoPopup(itemName);
                }
            });
        }
        else{
            mainTitleText.setText("No video files");
            fileList.setAdapter(null);
        }
    }

    public void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    public void showVideoPopup(String videoName){

        mContext = getApplicationContext();
        mActivity = MainActivity.this;
        mainLayout = (FrameLayout) findViewById(R.id.content);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.popup_window,null);
        // Initialize a new instance of popup window
        videoPopupWindow = new PopupWindow(
                customView,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        videoPopupWindow.setElevation(5.0f);

        ImageButton closeButton = (ImageButton) customView.findViewById(R.id.ib_close);

        // Set a click listener for the popup window close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dismiss the popup window
                videoPopupWindow.dismiss();
            }
        });

        videoPopupWindow.showAtLocation(mainLayout, Gravity.CENTER,0,0);
        videoPopUpTitle = (TextView) customView.findViewById(R.id.videopopup_title);
        videoPopUpTitle.setText(videoName);
        //set listeners for play, upload and delete buttons
        videoPopupUpload = (TextView) customView.findViewById(R.id.videopopup_upload);
        videoPopupUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String videoTitle = videoPopUpTitle.getText().toString();
                Toast.makeText(MainActivity.this,"You Clicked : " + videoTitle,Toast.LENGTH_SHORT).show();
                //Call the function that splits into segments and uploads
                String dir = getAppStoragePath(MainActivity.this);
                segmentVideo(dir,videoTitle);
                //uploadSingleFile(dir,videoTitle);
                videoPopupWindow.dismiss();
            }
        });

        videoPopupDelete = (TextView) customView.findViewById(R.id.videopopup_delete);
        videoPopupDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String videoTitle = videoPopUpTitle.getText().toString();
                Toast.makeText(MainActivity.this,"You Clicked : " + videoTitle,Toast.LENGTH_SHORT).show();
                //Call the function that splits into segments and uploads
                String dir = getAppStoragePath(MainActivity.this);
                deleteVideo(dir,videoTitle);
                //uploadSingleFile(dir,videoTitle);
                videoPopupWindow.dismiss();
            }
        });

        videoPopupPlay = (TextView) customView.findViewById(R.id.videopopup_play);
        videoPopupPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String videoTitle = videoPopUpTitle.getText().toString();
                Toast.makeText(MainActivity.this,"You Clicked : " + videoTitle,Toast.LENGTH_SHORT).show();
                //Call the function that splits into segments and uploads
                String dir = getAppStoragePath(MainActivity.this);
                playVideo(dir,videoTitle);
                //uploadSingleFile(dir,videoTitle);
                videoPopupWindow.dismiss();
            }
        });

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
                String name = files[i].getName();
                if(name.contains(".mp4")) {
                    Myfiles.add(name);
                }
            }
        }
        return Myfiles;
    }

    private String getAppStoragePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return dir.getAbsolutePath();
    }

    private String getStreamletStoragePath(Context context){
        final File dir = context.getExternalFilesDir(null);
        return dir.getAbsolutePath()+"/streamlets";
    }


    private void uploadSingleFile(String directorypath,String fileName){
        //ArrayList<String> filesinfolder = GetFiles(directorypath);
        String mainFile = directorypath+"/"+fileName;
        String segmentsPath = directorypath+"/streamlets/"+fileName+"/";
        File f = new File(mainFile);
        UploadFile obj = new UploadFile();
        try {
            String result = obj.execute(segmentsPath,f.getName(),"SamsungGalaxyTab").get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void deleteVideo(String directorypath, String fileName){
        String filepath = directorypath+"/"+fileName;
        File f = new File(filepath);
        f.delete();
        ListAllVideos();
    }

    private void playVideo(String directorypath, String fileName){
        String filepath = directorypath+"/"+fileName;
        File f = new File(filepath);
        Context mContext = getApplicationContext();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //intent.setDataAndType(Uri.fromFile(f), "video/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri apkURI = FileProvider.getUriForFile(
                mContext,
                mContext.getApplicationContext()
                        .getPackageName() + ".provider", f);
        intent.setDataAndType(apkURI, "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(intent);

    }
    private void segmentVideo(String directorypath, String fileName) {
        //Segment the video in splits of 3 seconds
        Log.i("DASH","Inside segmentVideo()");
       //ArrayList<String> filesinfolder = GetFiles(directorypath);
        String filepath = directorypath+"/"+fileName;
        File f = new File(filepath);
        outputPath = getSegmentFolder(f.getName());
        Log.i("DASH", "Path where segments have to be saved is " + outputPath);

        CreateStreamlets obj = new CreateStreamlets(segmentProgressBar, segmentProgressView);
        try {
            Integer result = obj.execute(filepath, outputPath, "3.0").get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private String getSegmentFolder(String innerFolderName) {
        String folderName = "streamlets/" + innerFolderName;
        File segmentFolder = new File(getAppStoragePath(MainActivity.this), folderName);
        segmentFolder.mkdirs();

        return segmentFolder.getPath() + "/";
    }
}


//Rangarajan
