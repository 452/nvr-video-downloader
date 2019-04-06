# Dahua nvr video downloader

in investigating/development

## Shell example for download video file form Dahua NVR
```sh
wget --user admin --password $NVR_PASSWORD -O video.dav 'http://192.168.30.4/cgi-bin/loadfile.cgi?action=startLoad&channel=2&startTime=2018-11-05%2006:00:00&endTime=2018-11-05%2010:00:07'
```

## Shell examples Dahua dav video convertation to mp4
```sh
ffmpeg -v quiet -stats -err_detect ignore_err -f hevc -y -i /cloud/sample-2-wget.dav -ss 20 -c:v h264 video_fixed.mp4

time ffmpeg -v quiet -f h264 -i 6.dav -c:v copy -c:a copy output-file.mp4

time ffmpeg -v quiet -stats -f h264 -i video.dav -c:v hevc -filter:v "setpts=0.25*PTS" -r 16 ipc-2.mp4

time ffmpeg -hwaccel cuvid -v quiet -f h264 -i video.dav -c:v hevc_nvenc -filter:v "setpts=0.25*PTS" -r 16 -stats ipc-2.mp4

ffmpeg -v quiet -stats -err_detect ignore_err -f hevc -y -i /cloud/sample-2-wget.dav -r 16  -c:v h264 video_fixed.mp4

time ffmpeg -v quiet -stats -hwaccel cuvid -f hevc -i video.dav -c:v copy -c:a copy output-file.mp4

ffprobe -v quiet -print_format json -show_entries stream=width -f h264 -i video.dav | grep -q '"width": 0' && echo hevc || echo h264
```