package com.github.nvd.domain;

import lombok.Data;

@Data
public class DownloadedVideoFile {

    private String fileName;
    private String downloadProgress;
    private String fileSize;
}