package io.fruitful.spring.uploader.processor;

import io.fruitful.spring.uploader.annotation.EnableUploader;
import io.fruitful.spring.uploader.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnableUploaderAnnotationProcessor {
	private final Logger LOGGER = LoggerFactory.getLogger(EnableUploaderAnnotationProcessor.class);
	private UploadService uploadService;

	public EnableUploaderAnnotationProcessor(UploadService uploadService) {
		this.uploadService = uploadService;
	}

	public void process(EnableUploader enableUploaderAnnotation) {
		LOGGER.info("Uploader feature enabled: {}", enableUploaderAnnotation);

		uploadService.initialize(enableUploaderAnnotation);
	}
}
