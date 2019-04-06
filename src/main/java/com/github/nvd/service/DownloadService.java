package com.github.nvd.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.github.nvd.okhttp3.DownloadFileProgressListener;
import com.github.nvd.okhttp3.ProgressResponseBody;
import com.github.nvd.util.FileUtil;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

@Service
@Slf4j
public class DownloadService {

    public void download(String nvrIpAddress, String channel, String startDateTime, String endDateTime, DownloadFileProgressListener progressListener) {

        String url = ("http://" + nvrIpAddress + "/cgi-bin/loadfile.cgi?action=startLoad&channel=" + channel + "&startTime="
                + startDateTime + "&endTime=" + endDateTime).replace(" ", "%20");
        log.debug("Url for download from NVR: ", url);

        try (Response response = httpRequest(url, progressListener)) {
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);
            log.info("Code: {} File size: {}", response.code(), FileUtil.humanReadableByteCount(response.body().contentLength()));
            saveToFile(response, FileUtil.makeFileName(channel, startDateTime, endDateTime));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void saveToFile(Response response, String fileName) throws IOException {
        File videoFile = new File("/tmp/test/" + fileName + ".dav");
        // try (BufferedSource bufferedSource = response.body().source()) {
        // BufferedSink bufferedSink = Okio.buffer(Okio.sink(videoFile));
        // bufferedSink.writeAll(bufferedSource);
        // bufferedSink.close();
        // }
        final int DOWNLOAD_CHUNK_SIZE = 2048;
        BufferedSink sink = Okio.buffer(Okio.sink(videoFile));

        long totalRead = 0;
        long read = 0;
        BufferedSource source = response.body().source();
        while ((read = source.read(sink.buffer(), DOWNLOAD_CHUNK_SIZE)) != -1) {
            totalRead += read;
            // int progress = (int) ((totalRead * 100) / response.body().contentLength());
            // publishProgress(progress);
        }
        sink.writeAll(source);
        sink.flush();
        sink.close();
    }

    private Response httpRequest(String url, DownloadFileProgressListener progressListener) throws IOException {
        final Map<String, com.burgstaller.okhttp.digest.CachingAuthenticator> authCache = new ConcurrentHashMap<>();
        DigestAuthenticator authenticator = new DigestAuthenticator(new Credentials("admin", System.getenv("NVR_PASSWORD")));
        OkHttpClient client = new OkHttpClient.Builder().authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                            .build();
                })
                .build();
        Request httpRequest = new Request.Builder().url(url)
                // .header("content-type", "application/json")
                .build();
        return client.newCall(httpRequest).execute();
    }

}