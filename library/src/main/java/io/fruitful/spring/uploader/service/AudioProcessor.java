package io.fruitful.spring.uploader.service;

import io.fruitful.spring.uploader.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class AudioProcessor extends MediaProcessor {

	public AudioProcessor(String ffmpegPath) {
		super(ffmpegPath);
	}

	@Override
	public File process(String input, String destination) {

		try {
			File outputFile = new File(destination);
			FileUtils.silenceDelete(outputFile);

			new ShellCommandExecutor(ffmpegPath, "-y", "-loglevel", "panic", "-i", input, "-codec:a", "libmp3lame",
			                         "-b:a", "128k", destination).execute(CONVERSION_TIMEOUT);

			if (outputFile.exists() && outputFile.length() > 0) {
				return outputFile;
			}

		} catch (Exception e) {
			log.error("Convert failed..", e);
		}
		return null;
	}

}
