package com.github.nvd.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.github.nvd.util.FileUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EncodeService {

    private final static Path BIN = Paths.get("/usr/bin");

    public void encode(String nvrIpAddress, String channel, String startDateTime, String endDateTime) {
        log.info("Encode started");
        String videoFile = "/tmp/test/" + FileUtil.makeFileName(channel, startDateTime, endDateTime);
        Path video = Paths.get(videoFile + ".dav");
        Path outputVideo = Paths.get(videoFile + ".mp4");
        final AtomicLong duration = new AtomicLong();
        ProgressListener listener = new ProgressListener() {
            private long lastReportTs = System.currentTimeMillis();

            @Override
            public void onProgress(FFmpegProgress progress) {
                // long now = System.currentTimeMillis();
                // if (lastReportTs + 1000 < now) {
                // long percent = 100 * progress.getTimeMillis() / duration.incrementAndGet();
                // System.out.println("Progress: " + percent + "%");
                if (progress.getFps() != null)
                    log.info("FPS: {} [Duration time: {} minutes, {} secconds]",
                        progress.getFps(),
                        progress.getTime(TimeUnit.MINUTES),
                        progress.getTime(TimeUnit.MILLISECONDS));
                // }
            }
        };
        // Future<FFmpegResult> result =
        FFmpeg.atPath(BIN)
                .setLogLevel(LogLevel.DEBUG)
                .addInput(UrlInput.fromPath(video)
                        // .addArguments("-err_detect", "ignore_err")
                        // .setFormat("rawvideo")
                        .setFormat(codecType(video)))
                .addOutput(
                    UrlOutput.toPath(outputVideo).copyAllCodecs().setCodec(StreamType.VIDEO, "h264").copyCodec(StreamType.AUDIO))
                .setOverwriteOutput(true)
                .setProgressListener(listener)
                .execute();

        log.info("Encode done {}");// , FileUtil.humanReadableByteCount(result.getVideoSize()));
    }

    private String codecType(Path video) {
        String result = "h264";
        FFprobeResult ffProbeResult = FFprobe.atPath(BIN).setInput(video).setShowStreams(true).addArguments("-f", "hevc").execute();
        for (Stream stream : ffProbeResult.getStreams()) {
            if (stream.getWidth() > 0) {
                result = "hevc";
            }
            log.info("Resolution: {}x{} Compression: {} {}",
                stream.getWidth(),
                stream.getHeight(),
                stream.getCodecName(),
                stream.getCodecLongName());
        }
        return result;
    }

}