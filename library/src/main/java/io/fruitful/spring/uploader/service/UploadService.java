package io.fruitful.spring.uploader.service;

import io.fruitful.spring.uploader.UploadConfig;
import io.fruitful.spring.uploader.annotation.EnableUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadService {

	private final Logger LOGGER = LoggerFactory.getLogger(UploadService.class);

	private UploadConfig config;

	public UploadService(UploadConfig config) {
		this.config = config;
	}

	public void initialize(EnableUploader enableUploader) {
//		if(config == null) {
//			LOGGER.error("If you want to use Uploader library, please enable by adding @EnableUploader on you Application class!!!");
//		}

		config.setMaxFileSize(enableUploader.maxFileSize());
		config.setTemporaryUploadFolder(enableUploader.temporaryUploadFolder());

		// Now initialize

		// 1. Create temporary upload folder


	}

	public String test() {
		return "Hello world";
	}
}
