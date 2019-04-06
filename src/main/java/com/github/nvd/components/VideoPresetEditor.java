package com.github.nvd.components;

import org.springframework.beans.factory.annotation.Autowired;

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
public class VideoPresetEditor extends VerticalLayout implements KeyNotifier {

    private final VideoEncoderPresetRepository repository;
    /**
     * The currently edited video preset
     */
    private VideoEncoderPreset videoPreset;

    TextField presetName = new TextField("Preset name");
    TextField presetArgs = new TextField("Preset args");
    Button save = new Button("Save", VaadinIcon.CHECK.create());
    Button cancel = new Button("Cancel");
    Button delete = new Button("Delete", VaadinIcon.TRASH.create());
    HorizontalLayout actions = new HorizontalLayout(save, cancel, delete);

    Binder<VideoEncoderPreset> binder = new Binder<>(VideoEncoderPreset.class);
    @Setter
    private ChangeHandler changeHandler;

    @Autowired
    public VideoPresetEditor(VideoEncoderPresetRepository repository) {
        this.repository = repository;
        add(presetName, presetArgs, actions);
        binder.bindInstanceFields(this);
        setSpacing(true);
        save.getElement().getThemeList().add("primary");
        delete.getElement().getThemeList().add("error");
        addKeyPressListener(Key.ENTER, e -> save());
        save.addClickListener(e -> save());
        delete.addClickListener(e -> delete());
        cancel.addClickListener(e -> save());
        setVisible(false);
    }

    public void editPreset(VideoEncoderPreset preset) {
        if (preset == null) {
            setVisible(false);
            return;
        }

        if (preset.getId() != null) {
            videoPreset = repository.findById(preset.getId()).orElse(preset);
        } else {
            videoPreset = preset;
        }
        binder.setBean(videoPreset);
        setVisible(true);
        presetName.focus();
    }

    private void delete() {
        repository.delete(videoPreset);
        changeHandler.onChange();
    }

    private void save() {
        repository.save(videoPreset);
        log.info("Saved: {}", videoPreset.getId());
        changeHandler.onChange();
    }

    public interface ChangeHandler {
        void onChange();
    }
}