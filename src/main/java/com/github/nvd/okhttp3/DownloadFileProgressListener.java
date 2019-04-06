package com.github.nvd.okhttp3;

public interface DownloadFileProgressListener {
    
    void update(long bytesRead, long contentLength, boolean done);
}