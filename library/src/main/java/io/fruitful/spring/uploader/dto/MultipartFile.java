package io.fruitful.spring.uploader.dto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface MultipartFile extends InputStreamSource {
	String getName();

	String getOriginalFilename();

	String getContentType();

	boolean isEmpty();

	long getSize();

	byte[] getBytes() throws IOException;

	InputStream getInputStream() throws IOException;

	void transferTo(File var1) throws IOException, IllegalStateException;
}
