package io.fruitful.spring.uploader.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;
import java.util.function.UnaryOperator;

@Getter
@Setter
@Builder
public class ChunkDoneConfig {

	private String temporaryFolder;
	private String uploadFolder;
	private String heifConvertPath;
	private String ffmpegPath;
	private String ffmpegThumbExt;
	private String ffmpegThumbStartTime;
	private Function<MediaInfo, String> mediaProcessHandler;
	private UnaryOperator<String> mediaExistedHandler;
}
