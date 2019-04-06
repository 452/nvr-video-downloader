package com.github.nvd.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Entity
@Data
public class VideoEncoderPreset {

	@Id
	@GeneratedValue
	private Long id;
	private String presetName;
	private String presetArgs;

}