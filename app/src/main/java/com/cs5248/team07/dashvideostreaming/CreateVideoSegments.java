package com.cs5248.team07.dashvideostreaming;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class CreateVideoSegments extends AsyncTask<String, Double, Integer> {

    private String videoPath;
    private String outputPath;
    private String filename;
    private boolean set;
    private double videoTime;
    private double percentage;
    private int segmentNumber;
    private  ProgressBar segmentProgress;
    private  TextView textview;


    public CreateVideoSegments(ProgressBar segmentProgress, TextView textView) {
        this.segmentProgress = segmentProgress;
        this.textview = textView;
    }
    @Override
    protected Integer doInBackground(String... params) {

        Log.i("DASH" , "Inside split background function ");

        if (params.length != 3)
            throw new IllegalArgumentException("Not enough params");

        String path = params[0];
        String destPath = params[1];
        double splitDuration = Double.parseDouble(params[2]);
        return Integer.valueOf(this.split(path, destPath, splitDuration));
    }
    @Override
    protected void onPreExecute() {
        segmentProgress.setMax(100);
    }

    public int split(String path, String destinationPath, double splitDuration) {
        double startTime = 0.00;
        segmentNumber = 1;

        videoTime = 0.0;
        set = false;
        videoPath = path;
        outputPath = destinationPath;
        filename = new File(videoPath).getName().replace(".mp4", "");
        /*
        Movie movie = null;
        try {
            movie = MovieCreator.build(videoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total movie time is = "+movie.getTimescale());
        List<Track> tracks = movie.getTracks();
        Track track = tracks.get(0);
        videoTime = correctSyncSamples(track, 50000, true);

        int fullvideotime = (int) videoTime;
        int totalsegments = fullvideotime/3;
        if(fullvideotime%3 != 0){
            totalsegments++;
        }
        System.out.println ("Total segments needed is " + totalsegments);
        Log.i("DASH" , "Total segments needed is  "+ totalsegments);
        */
        long start1 = System.currentTimeMillis();
        try {
            while (performSplit(startTime, startTime + splitDuration, segmentNumber)) {
                segmentNumber++;
                startTime += splitDuration;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long start2 = System.currentTimeMillis();
        Log.i("DASH", "Total time taken to create " + Integer.toString( segmentNumber - 1) +
                " segments: " + Long.toString( start2 - start1) + "ms" );

        return segmentNumber - 1;
    }


    private boolean performSplit(double startTime, double endTime, int segmentNumber) throws IOException, FileNotFoundException {
        Movie movie = MovieCreator.build(videoPath);
        System.out.println("Total movie time is = "+movie.getTimescale());
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());
        boolean timeCorrected = false;
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (timeCorrected) {
                    throw new RuntimeException("Some exception");
                }
                startTime = correctSyncSamples(track, startTime, true);
                endTime = correctSyncSamples(track, endTime, true);
                timeCorrected = true;
                if(!set) {
                    videoTime = correctSyncSamples(track, 50000, true);
                    set = true;
                   System.out.println("Video total time =" + videoTime);
                }
            }
        }

        percentage = (startTime * 100) / videoTime;
        publishProgress(percentage);

        if (startTime == endTime)
            return false;

        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            double lastTime = 0;
            long startSample1 = 0;
            long endSample1 = -1;
            for (int i = 0; i < track.getSampleDurations().length; i++) {
                long delta = track.getSampleDurations()[i];


                if (currentTime > lastTime && currentTime <= startTime) {
                    startSample1 = currentSample;
                }
                if (currentTime > lastTime && currentTime <= endTime) {
                    endSample1 = currentSample;
                }

                lastTime = currentTime;
                currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
            Log.i("TAG", "Start time is = " + startTime + ", End time is = " + endTime);
            movie.addTrack(new CroppedTrack(track, startSample1, endSample1));
        }
        long start1 = System.currentTimeMillis();
        Container out = new DefaultMp4Builder().build(movie);
        long start2 = System.currentTimeMillis();
        FileOutputStream fos = new FileOutputStream(outputPath + String.format("%s_%d.mp4", filename  , segmentNumber));
        FileChannel fc = fos.getChannel();
        out.writeContainer(fc);

        fc.close();
        fos.close();
        long start3 = System.currentTimeMillis();
        return true;
    }

    @Override
    protected void onProgressUpdate(Double... values) {
        segmentProgress.setProgress(values[0].intValue());
        //Updates the number of segments done below Progress Bar on UI
        textview.setText(segmentNumber - 1 + " segments created");
    }

    private double correctSyncSamples(Track track, double endLimit, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;

        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample >= endLimit) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

}
