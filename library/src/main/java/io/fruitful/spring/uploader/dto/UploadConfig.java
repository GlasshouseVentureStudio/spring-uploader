package io.fruitful.spring.uploader.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UploadConfig {

	private String temporaryFolder;
	private String uploadFolder;
	private String heifConvertPath;
	private String ffmpegPath;
	private String ffmpegThumbExt;
	private String ffmpegThumbStartTime;
}
