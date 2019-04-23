package com.github.nvd.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import org.springframework.util.StringUtils;

import com.github.nvd.components.VideoDownloader;
import com.github.nvd.components.VideoPresetEditor;
import com.github.nvd.domain.DownloadedVideoFile;
import com.github.nvd.domain.VideoEncoderPreset;
import com.github.nvd.okhttp3.DownloadFileProgressListener;
import com.github.nvd.repo.VideoEncoderPresetRepository;
import com.github.nvd.service.DownloadService;
import com.github.nvd.service.EncodeService;
import com.github.nvd.util.FileUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;

import lombok.extern.slf4j.Slf4j;

@Theme(Material.class)
@Route
@Push
@Slf4j
public class MainView extends VerticalLayout {

    private VideoEncoderPresetRepository videoEncoderPresetRepository;
    private Grid<VideoEncoderPreset> grid = new Grid<>(VideoEncoderPreset.class);
    private Grid<DownloadedVideoFile> downloadedVideoFilesGrid = new Grid<>(DownloadedVideoFile.class);
    private final VideoPresetEditor editor;
    private final VideoDownloader videoDownloader;
    private DownloadService downloadService;

    private TextField filter = new TextField("", "Type to filter");
    private Label diskSpace = new Label();
    private Button addNewBtn = new Button("New preset", VaadinIcon.PLUS.create());
    private Button downloadBtn = new Button("Download", VaadinIcon.DOWNLOAD.create());
    private Button encodeBtn = new Button("Encode", VaadinIcon.AIRPLANE.create());
    private HorizontalLayout toolBar = new HorizontalLayout(filter, addNewBtn);
    private Div message = createMessageDiv("tes");
    private HorizontalLayout dateBar = new HorizontalLayout(message);
    private TextField nvrIpAddress = new TextField("NVR IP Address", "192.168.30.4", "");
    private TextField channel = new TextField("NVR channel", "3", "");
    private TextField startDateTime = new TextField("start datetime", "2019-04-19 13:46:00", "");
    private TextField endDateTime = new TextField("end datetime", "2019-04-19 13:47:30", "");
    private HorizontalLayout downloadParamsBar = new HorizontalLayout(nvrIpAddress, channel, startDateTime, endDateTime);
    private HorizontalLayout systemInfoBar = new HorizontalLayout(diskSpace);
    private VerticalLayout downloadVideoLayout = new VerticalLayout(systemInfoBar, downloadParamsBar, dateBar, downloadBtn, encodeBtn, downloadedVideoFilesGrid);
    private DownloadFileProgressListener progressListener;
    private Timer autoUpdateInfoOnPage = new Timer();

    public MainView(DownloadService downloadService, EncodeService encodeService, VideoEncoderPresetRepository videoEncoderPresetRepository, VideoPresetEditor editor, VideoDownloader videoDownloader) {
        File dir = new File(FileUtil.nvrHomeDir());
        if (!dir.exists())
            dir.mkdirs();
        this.downloadService = downloadService;
        diskSpace.setTitle("Free Disk Space");
        this.videoEncoderPresetRepository = videoEncoderPresetRepository;
        this.editor = editor;
        this.videoDownloader = videoDownloader;
        add(downloadVideoLayout, videoDownloader, toolBar, grid, editor);
        filter.setWidth("300px");
        filter.setValueChangeMode(ValueChangeMode.EAGER);
        filter.addValueChangeListener(e -> listPresets(e.getValue()));
        grid.asSingleSelect().addValueChangeListener(e -> {
            editor.editPreset(e.getValue());
        });
        grid.setHeight("300px");
        grid.setColumns("id", "presetName", "presetArgs");
        grid.getColumnByKey("id").setWidth("50px").setFlexGrow(0);
        grid.getColumnByKey("presetName").setWidth("350px").setFlexGrow(0);

        downloadedVideoFilesGrid.setColumns("fileName", "downloadProgress", "fileSize");
        downloadedVideoFilesGrid.getColumnByKey("downloadProgress").setWidth("350px").setFlexGrow(0);
        downloadedVideoFilesGrid.getColumnByKey("fileSize").setWidth("150px").setFlexGrow(0);

        addNewBtn.addClickListener(e -> editor.editPreset(new VideoEncoderPreset()));

        downloadBtn.addClickListener(e -> {
            CompletableFuture.runAsync(() -> downloadService.download(nvrIpAddress
                    .getValue(),
                channel.getValue(),
                startDateTime.getValue(),
                endDateTime.getValue(),
                progressListener));
        });

        encodeBtn.addClickListener(e -> {
            CompletableFuture.runAsync(() -> encodeService
                    .encode(nvrIpAddress.getValue(), channel.getValue(), startDateTime.getValue(), endDateTime.getValue()));
        });

        editor.setChangeHandler(() -> {
            editor.setVisible(false);
            listPresets(filter.getValue());
        });

        listPresets();
        message.setVisible(true);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        add(new Span("Waiting for updates"));

        progressListener = new DownloadFileProgressListener() {
            boolean firstUpdate = true;

            @Override
            public void update(long bytesRead, long contentLength, boolean done) {
                attachEvent.getUI().access(() -> {
                    freeSpace();
                    if (done) {
                        message.setText("downloaded " + FileUtil.humanReadableByteCount(contentLength));
                    } else {
                        if (firstUpdate) {
                            firstUpdate = false;
                            if (contentLength == -1) {
                                log.info("content-length: unknown");
                            } else {
                                System.out.format("content-length: %d\n", contentLength);
                            }
                        }

                        if (contentLength != -1) {
                            message.setText(String.valueOf((100 * bytesRead) / contentLength) + "% done "
                                    + FileUtil.humanReadableByteCount(bytesRead) + " of "
                                    + FileUtil.humanReadableByteCount(contentLength));
                        }
                    }
                });
            }
        };

        autoUpdateInfoOnPage.schedule(new TimerTask() {
            @Override
            public void run() {
                attachEvent.getUI().access(() -> {
                    freeSpace();
                    filesList();
                    attachEvent.getUI().push();
                });
            }
        }, 0, 1000 * 30);

    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // Cleanup
        progressListener = null;
        autoUpdateInfoOnPage = null;
    }

    private void filesList() {
        Collection<DownloadedVideoFile> items = new LinkedList<DownloadedVideoFile>();
        try {
            Files.newDirectoryStream(Paths.get(FileUtil.nvrHomeDir()), path -> path.toString().endsWith(".dav") || path.toString().endsWith(".mp4")).forEach(file -> {
                DownloadedVideoFile item = new DownloadedVideoFile();
                item.setFileName(file.toFile().getAbsolutePath());
                item.setFileSize(FileUtil.humanReadableByteCount(file.toFile().length()));
                items.add(item);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadedVideoFilesGrid.setItems(items);
    }

    private void freeSpace() {
        diskSpace.setText(FileUtil.humanReadableByteCount(new File("/").getFreeSpace()));
    }

    private void listPresets() {
        listPresets(null);
    }

    private void listPresets(String string) {
        if (StringUtils.isEmpty(string)) {
            grid.setItems(videoEncoderPresetRepository.findAll());
        } else {
            grid.setItems(videoEncoderPresetRepository.findByPresetNameOrPresetArgsStartsWithIgnoreCase(string, string));
        }
    }

    private void UpdateMessage(Div message, DatePicker datePicker) {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate != null) {
            message.setText("Day: " + selectedDate.getDayOfMonth() + "\nMonth: " + selectedDate.getMonthValue() + "\nYear: "
                    + selectedDate.getYear() + "\nLocale: " + datePicker.getLocale());
        } else {
            message.setText("No date is selected");
        }
    }

    private Div createMessageDiv(String id) {
        Div message = new Div();
        message.setId(id);
        message.getStyle().set("whiteSpace", "pre");
        return message;
    }

}
//
// startDatePicker.addValueChangeListener(event -> {
// LocalDate selectedDate = event.getValue();
// LocalDate endDate = endDatePicker.getValue();
// if (selectedDate != null) {
// //endDatePicker.setMin(selectedDate.plusDays(1));
// if (endDate == null) {
// endDatePicker.setOpened(true);
// message.setText("Select the ending date");
// } else {
// message.setText(
// "Selected period:\nFrom " + selectedDate.toString()
// + " to " + endDate.toString());
// }
// } else {
// //endDatePicker.setMin(null);
// message.setText("Select the starting date");
// }
// });
//
// endDatePicker.addValueChangeListener(event -> {
// LocalDate selectedDate = event.getValue();
// LocalDate startDate = startDatePicker.getValue();
// if (selectedDate != null) {
// startDatePicker.setMax(selectedDate.minusDays(1));
// if (startDate != null) {
// message.setText(
// "Selected period:\nFrom " + startDate.toString()
// + " to " + selectedDate.toString());
// } else {
// message.setText("Select the starting date");
// }
// } else {
// //startDatePicker.setMax(null);
// if (startDate != null) {
// message.setText("Select the ending date");
// } else {
// message.setText("No date is selected");
// }
// }
// });
