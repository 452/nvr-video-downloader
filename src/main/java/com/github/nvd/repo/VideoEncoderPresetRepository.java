package com.github.nvd.repo;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.github.nvd.domain.VideoEncoderPreset;

@Repository
public interface VideoEncoderPresetRepository extends JpaRepository<VideoEncoderPreset, Long> {

    public Collection<VideoEncoderPreset> findByPresetNameOrPresetArgsStartsWithIgnoreCase(String string, String string2);

}