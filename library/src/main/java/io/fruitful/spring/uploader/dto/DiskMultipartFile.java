package io.fruitful.spring.uploader.dto;

import io.fruitful.spring.uploader.util.FileUtils;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Getter
public class DiskMultipartFile implements MultipartFile {
	private String name;
	private String originalFilename;
	private String contentType;
	private byte[] content;

	/**
	 * Create a new MockMultipartFile with the given content.
	 *
	 * @param name    the name of the file
	 * @param content the content of the file
	 */
	public DiskMultipartFile(String name, byte[] content) {
		this(name, "", null, content);
	}

	/**
	 * Create a new MockMultipartFile with the given content.
	 *
	 * @param name          the name of the file
	 * @param contentStream the content of the file as stream
	 * @throws IOException if reading from the stream failed
	 */
	public DiskMultipartFile(String name, InputStream contentStream) throws IOException {
		this(name, "", null, FileUtils.copyToByteArray(contentStream));
	}

	/**
	 * Create a new MockMultipartFile with the given content.
	 *
	 * @param name             the name of the file
	 * @param originalFilename the original filename (as on the client's machine)
	 * @param contentType      the content type (if known)
	 * @param content          the content of the file
	 */
	public DiskMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
		this.name = name;
		this.originalFilename = (originalFilename != null ? originalFilename : "");
		this.contentType = contentType;
		this.content = (content != null ? content : new byte[0]);
	}

	/**
	 * Create a new MockMultipartFile with the given content.
	 *
	 * @param name             the name of the file
	 * @param originalFilename the original filename (as on the client's machine)
	 * @param contentType      the content type (if known)
	 * @param contentStream    the content of the file as stream
	 * @throws IOException if reading from the stream failed
	 */
	public DiskMultipartFile(String name, String originalFilename, String contentType, InputStream contentStream)
			throws IOException {

		this(name, originalFilename, contentType, FileUtils.copyToByteArray(contentStream));
	}

	@Override
	public boolean isEmpty() {
		return (this.content.length == 0);
	}

	@Override
	public long getSize() {
		return this.content.length;
	}

	@Override
	public byte[] getBytes() throws IOException {
		return this.content;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.content);
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		FileUtils.copy(this.content, dest);
	}

}
