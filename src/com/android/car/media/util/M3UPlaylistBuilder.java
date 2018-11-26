package com.android.car.media.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class M3UPlaylistBuilder {

    private static final String TAG = M3UPlaylistBuilder.class.getSimpleName();
    private static final String PLAYLISTS_FOLDER_PATH = "/Music/Playlists/";
    private static final String M3U_FILE_HEADER = "#EXTM3U";
    private static final String M3U_TRACK_HEADER = "#EXTINF:";
    private static final String M3U_FILE_EXTENSION = ".m3u";

    private List<String> mContents;

    public M3UPlaylistBuilder() {
        mContents = new ArrayList<>();
        mContents.add(M3U_FILE_HEADER);
    }

    public void addTrack(int duration, String artist, String title, String path) {
        String trackInfo = M3U_TRACK_HEADER + duration + ", " + artist + " - " + title;
		Log.d(TAG, trackInfo);
		Log.d(TAG, path);
        mContents.add(trackInfo);
        mContents.add(path);
    }

    public boolean save() {
        String playlistsFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                PLAYLISTS_FOLDER_PATH;
        File playlistsFolder = new File(playlistsFolderPath);
        if (!playlistsFolder.exists()) {
            if (!playlistsFolder.mkdirs()) {
                Log.e(TAG, "Error while creating playlists directory.");
                return false;
            }
        }
        String playlistFilePath = playlistsFolderPath +
                getCurrentDate() + M3U_FILE_EXTENSION;
        try (OutputStreamWriter writer =
                     new OutputStreamWriter(new FileOutputStream(playlistFilePath),
                             StandardCharsets.UTF_8)) {
            for (String content : mContents) {
                writer.write(content);
                writer.write("\n");
            }
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error while saving playlist.", e);
            return false;
        }
        return true;
    }

    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.US);
        return dateFormat.format(new Date());
    }

}
