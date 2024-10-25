package io.fruitful.spring.uploader.service;

import io.fruitful.spring.uploader.dto.UploadConfig;
import io.fruitful.spring.uploader.util.FileUtils;
import io.fruitful.spring.uploader.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Slf4j
public class UploadService {

	private final UploadConfig uploadConfig;
	private final String rootUploadDirectory;

	public UploadService(UploadConfig config) {
		this.uploadConfig = config;
		this.rootUploadDirectory = new File(
				Optional.ofNullable(config.getUploadFolder()).orElse("")).getAbsolutePath() + File.separator;
	}

	public VideoProcessor getVideoProcessor() {
		return new VideoProcessor(uploadConfig.getFfmpegPath());
	}

	public AudioProcessor getAudioProcessor() {
		return new AudioProcessor(uploadConfig.getFfmpegPath());
	}

	public String getFullFilePath(String fileName) {
		return String.format("%s%s", rootUploadDirectory, fileName);
	}

	public void removeFileOnServer(String fileName) {
		try {
			FileUtils.delete(new File(getFullFilePath(fileName)));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public String resizeGifImage(String imageFullPath, String size) throws IOException {
		return ImageUtils.resizeGifImage(imageFullPath, size, rootUploadDirectory, uploadConfig.getFfmpegPath());
	}

	public String convertHeicImage(String imageFullPath) {
		return ImageUtils.convertHeicImage(imageFullPath, rootUploadDirectory, uploadConfig.getHeifConvertPath(),
		                                   this::removeFileOnServer);
	}

	public String extractImageDifferentSizes(String imageFullPath, String size) {
		return ImageUtils.extractImageDifferentSizes(rootUploadDirectory, imageFullPath, size);
	}
}
