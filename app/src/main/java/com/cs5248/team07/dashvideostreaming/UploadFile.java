package com.cs5248.team07.dashvideostreaming;

import android.os.AsyncTask;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

public class UploadFile extends AsyncTask<String, Void, String> {


    public ArrayList<String> GetFiles(String directorypath){
        System.out.println("The segment path is "+ directorypath);
        ArrayList<String> Myfiles = new ArrayList<String>();
        File f = new File(directorypath);
        File[] files = f.listFiles();
        if(files.length==0){
            return Myfiles;
        }
        else{
            for(int i=0;i<files.length;i++) {
                System.out.println("File name is " + files[i].getName());
                Myfiles.add(files[i].getAbsolutePath());
            }
        }
        return Myfiles;
    }

    @Override
    protected String doInBackground(String... params) {

        ArrayList<String> segmentList = GetFiles(params[0]);
        Iterator iter = segmentList.iterator();
        int i = 0;
        while(iter.hasNext()) {

            try {
                String sourceFileUri = iter.next().toString();
                System.out.println("Trying to upload the file " + sourceFileUri);
                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File sourceFile = new File(sourceFileUri);
                String videoTitle = params[1];
                String deviceId = params[2];
                System.out.println("Device id is " + deviceId);
                String totalStreamlets = Integer.toString(segmentList.size());
                String streamletNo = Integer.toString(i);
                i++;


                if (sourceFile.isFile()) {

                    try {
                        String upLoadServerUri = "http://monterosa.d2.comp.nus.edu.sg/~team07/upload.php";


                        // open a URL connection to the Servlet
                        FileInputStream fileInputStream = new FileInputStream(
                                sourceFile);
                        URL url = new URL(upLoadServerUri);

                        // Open a HTTP connection to the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE",
                                "multipart/form-data");
                        conn.setRequestProperty("Content-Type",
                                "multipart/form-data;boundary=" + boundary);
                        conn.setRequestProperty("fileToUpload", sourceFileUri);
                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\";filename=\""
                                + sourceFileUri + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                        bytesAvailable = fileInputStream.available();

                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {
                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                        }

                        // send multipart form data necesssary after file
                        // data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                        //send other params
                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"videoTitle\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(videoTitle);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"deviceId\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(deviceId);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"streamletNo\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(streamletNo);
                        dos.writeBytes(lineEnd); //to add multiple parameters write Content-Disposition: form-data; name=\"your parameter name\"" + crlf again and keep repeating till here :)
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"totalStreamlets\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(totalStreamlets);//your parameter value
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        // Responses from the server (code and message)
                        int serverResponseCode = conn.getResponseCode();
                        String serverResponseMessage = conn
                                .getResponseMessage();

                        if (serverResponseCode == 200) {
                            System.out.println("Sent all files to server");
                            //Toast.makeText(getActivity(), "File Upload Complete.",
                            //Toast.LENGTH_SHORT).show();
                        }
                        else{
                            //Toast.makeText(MainActivity.this, "File Upload Complete.",
                             //       Toast.LENGTH_LONG).show();
                        }

                        // close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();

                    } catch (Exception e) {

                        // dialog.dismiss();
                        e.printStackTrace();

                    }

                }


            } catch (Exception ex) {
                // dialog.dismiss();

                ex.printStackTrace();
            }
        }
        return "Executed";
    }

    @Override
    protected void onPostExecute(String result) {

    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
}