package io.fruitful.spring.uploader.service;

import io.fruitful.spring.uploader.util.StringHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public abstract class MediaProcessor {

	public static final long CONVERSION_TIMEOUT = 10; // MAX 10 minutes

	protected String ffmpegPath;
	protected String ffprobePath;

	protected MediaProcessor(String ffmpegPath) {
		this.ffmpegPath = ffmpegPath;
		ffprobePath = this.ffmpegPath.replace("ffmpeg", "ffprobe");
	}

	public abstract File process(String input, String output);

	public String detectDuration(String media) {
		try {
			String result = new ShellCommandExecutor(ffprobePath, "-v", "error", "-show_entries", "format=duration",
			                                         "-of", "default=noprint_wrappers=1", media,
			                                         "-sexagesimal").execute(CONVERSION_TIMEOUT);
			log.debug(result);
			// result sample: duration=0:00:59.400000
			String duration = StringHelper.trimToEmpty(StringHelper.substringAfter(result, "="));
			log.debug(duration);
			return duration;

		} catch (Exception e) {
			log.error("Can not detect media duration..", e);
			return null;
		}
	}

}
