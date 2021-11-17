package io.fruitful.spring.uploader;

public class UploadConfig {
	private long  maxFileSize; // no limitation
	private String temporaryUploadFolder;

	public long getMaxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(long maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public String getTemporaryUploadFolder() {
		return temporaryUploadFolder;
	}

	public void setTemporaryUploadFolder(String temporaryUploadFolder) {
		this.temporaryUploadFolder = temporaryUploadFolder;
	}
}
