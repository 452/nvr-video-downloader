package com.github.nvd.components;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.nvd.domain.DownloadedVideoFile;
import com.github.nvd.domain.VideoEncoderPreset;
import com.github.nvd.repo.VideoEncoderPresetRepository;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@SpringComponent
@UIScope
@Slf4j
public class VideoDownloader extends VerticalLayout implements KeyNotifier {

    /**
     * The currently edited video file
     */
    private DownloadedVideoFile videoFile;

    TextField fileName = new TextField("Preset name");
    TextField downloadProgress = new TextField("Preset args");
    Button cancel = new Button("Cancel");
    Button delete = new Button("Delete", VaadinIcon.TRASH.create());
    HorizontalLayout actions = new HorizontalLayout(cancel, delete);

    Binder<DownloadedVideoFile> binder = new Binder<>(DownloadedVideoFile.class);
    @Setter
    private ChangeHandler changeHandler;

    @Autowired
    public VideoDownloader() {
        add(fileName, downloadProgress, actions);
        binder.bindInstanceFields(this);
        setSpacing(true);
        delete.getElement().getThemeList().add("error");
        addKeyPressListener(Key.ENTER, e -> delete());
        delete.addClickListener(e -> delete());
        cancel.addClickListener(e -> delete());
        setVisible(false);
    }

    public void editPreset(DownloadedVideoFile videoFile) {
        if (videoFile == null) {
            setVisible(false);
            return;
        }
        this.videoFile = videoFile;
        binder.setBean(videoFile);
        setVisible(true);
        fileName.focus();
    }

    private void delete() {
        changeHandler.onChange();
    }

    public interface ChangeHandler {
        void onChange();
    }
}