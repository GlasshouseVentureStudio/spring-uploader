package io.fruitful.spring.uploader.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

@Getter
@Setter
public class UploadConfig {

	private long maxFileSize; // no limitation
	private String temporaryFolder;
	private String uploadFolder;
	private Function<MediaInfo, String> mediaHandler;
}
