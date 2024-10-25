package io.fruitful.spring.uploader.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

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
	private Function<MediaInfo, String> mediaHandler;
}
