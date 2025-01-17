package com.ghondar.torrentstreamer;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Arguments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import java.util.Map;
import java.util.HashMap;
import com.frostwire.jlibtorrent.FileStorage;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

public class TorrentItem implements TorrentListener {
    private TorrentStream mTorrentStream = null;
    private final String magnetUrl;
    private final ICommand command;
    private Torrent _torrent = null;
    private final String _location;

    public TorrentItem(String magnetUrl, String location, Boolean removeAfterStop, ICommand command) {
        // Environment.DIRECTORY_DOWNLOADS
        // Environment.getExternalStoragePublicDirectory(Environment.getDownloadCacheDirectory());
        // Environment.getDownloadCacheDirectory().getPath();
        if (location == null)
            location = "" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        this._location = location;
        this.magnetUrl = magnetUrl;
        TorrentOptions torrentOptions = new TorrentOptions.Builder()
                .saveLocation(location)
                .maxConnections(200)
                .autoDownload(true)
                .removeFilesAfterStop(removeAfterStop)
                .build();

        this.mTorrentStream = TorrentStream.init(torrentOptions);
        this.mTorrentStream.addListener(this);
        this.command = command;
    }

    public void start() {
        this.mTorrentStream.startStream(this.magnetUrl);
    }

    public void stop() {
        if (this.mTorrentStream != null && this.mTorrentStream.isStreaming()) {
            this.mTorrentStream.stopStream();
        }
    }

    public void setSelectedFileIndex(Integer selectedFileIndex) {
        if (this._torrent != null)
            this._torrent.setSelectedFileIndex(selectedFileIndex);
    }

    private WritableArray getFileInfos() {
        FileStorage fileStorage = this._torrent.getTorrentHandle().torrentFile().files();
        WritableArray infos = Arguments.createArray();
        for (int i = 0; i < fileStorage.numFiles(); i++) {
            WritableMap info = Arguments.createMap();
            info.putString("path", this._location + "/" + fileStorage.filePath(i));
            info.putString("fileName", fileStorage.fileName(i));
            info.putDouble("size", fileStorage.fileSize(i));
            infos.pushMap(info);
        }
        return infos;
    }

    @Override
    public void onStreamPrepared(Torrent torrent) {
        WritableMap params = Arguments.createMap();
        params.putString("magnetUrl", "" + this.magnetUrl);
        params.putString("data", "OnStreamPrepared");
        this.command.sendEvent(this.magnetUrl, "progress", params);
    }

    @Override
    public void onStreamStarted(Torrent torrent) {
        this._torrent = torrent;
        WritableMap params = Arguments.createMap();
        params.putString("magnetUrl", "" + this.magnetUrl);
        params.putArray("files", this.getFileInfos());
        params.putString("data", "onStreamStarted");
        this.command.sendEvent(this.magnetUrl, "progress", params);
    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {
        WritableMap params = Arguments.createMap();
        params.putString("magnetUrl", "" + this.magnetUrl);
        params.putString("msg", e.getMessage());
        this.command.sendEvent(this.magnetUrl, "error", params);
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        WritableMap params = Arguments.createMap();
        params.putString("magnetUrl", "" + this.magnetUrl);
        params.putString("url", torrent.getVideoFile().toString());
        params.putString("filename", torrent.getTorrentHandle().name());
        this.command.sendEvent(this.magnetUrl, "ready", params);
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {
        WritableMap params = Arguments.createMap();
        params.putString("magnetUrl", "" + this.magnetUrl);
        params.putString("buffer", "" + status.bufferProgress);
        params.putString("downloadSpeed", "" + status.downloadSpeed);
        params.putString("progress", "" + status.progress);
        params.putString("seeds", "" + status.seeds);
        this.command.sendEvent(this.magnetUrl, "status", params);
    }

    @Override
    public void onStreamStopped() {
        WritableMap params = Arguments.createMap();
        params.putString("magnetUrl", "" + this.magnetUrl);
        params.putString("msg", "OnStreamStoped");
        this.command.sendEvent(this.magnetUrl, "stop", params);
    }
}