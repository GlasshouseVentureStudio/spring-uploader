package io.fruitful.spring.uploader.util;

import io.fruitful.spring.uploader.exception.MergePartsException;
import io.fruitful.spring.uploader.service.PartitionFilesFilter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtils {

	public static final int NOT_FOUND = -1;
	public static final char UNIX_SEPARATOR = '/';
	public static final char WINDOWS_SEPARATOR = '\\';
	public static final char EXTENSION_SEPARATOR = '.';
	public static final Map<String, String> CONTENT_MAP = new HashMap<>();

	static {
		CONTENT_MAP.put("image/jpg", "jpg");
		CONTENT_MAP.put("image/jpeg", "jpg");
		CONTENT_MAP.put("image/png", "png");
		CONTENT_MAP.put("image/gif", "gif");
		CONTENT_MAP.put("audio/mp3", "mp3");
		CONTENT_MAP.put("audio/mpeg", "mp3");
		CONTENT_MAP.put("video/mp4", "mp4");
		CONTENT_MAP.put("video/mov", "mov");
		CONTENT_MAP.put("video/quicktime", "mov");
		CONTENT_MAP.put("audio/m4a", "m4a");
		CONTENT_MAP.put("audio/x-m4a", "m4a");
		CONTENT_MAP.put("video/m4v", "m4v");
		CONTENT_MAP.put("video/x-m4v", "m4v");
		CONTENT_MAP.put("audio/wav", "wav");
		CONTENT_MAP.put("audio/x-wav", "wav");
		CONTENT_MAP.put("image/heic", "heic");
	}

	public static void silenceDelete(File file) {
		try {
			boolean deleteResult = file.delete();
			if (!deleteResult) {
				log.error("Error deleting file {}", file.getName());
			}
		} catch (Exception e) {
			log.error("Error deleting file", e);
		}
	}

	public static File[] getPartitionFiles(File directory, String filename) {
		File[] files = directory.listFiles(new PartitionFilesFilter(filename));
		if (files != null) {
			Arrays.sort(files);
		}
		return files;
	}

	public static void deletePartitionFiles(File directory, String filename) {
		File[] partFiles = getPartitionFiles(directory, filename);
		for (File partFile : partFiles) {
			FileUtils.silenceDelete(partFile);
		}
	}

	public static void mkDir(File dir) {
		if (dir != null && !dir.exists()) {
			boolean mkdirResult = dir.mkdirs();
			if (!mkdirResult) {
				log.error("Cannot create directory {}", dir.getAbsolutePath());
			}
		}
	}

	public static File mergeFiles(File outputFile, File partFile) throws IOException {
		FileOutputStream fos = new FileOutputStream(outputFile, true);

		try {
			FileInputStream fis = new FileInputStream(partFile);

			try {
				IOUtils.copy(fis, fos);
			} finally {
				IOUtils.closeQuietly(fis);
			}
		} finally {
			IOUtils.closeQuietly(fos);
		}

		return outputFile;
	}

	public static String getName(final String fileName) {
		if (fileName == null) {
			return null;
		}
		requireNonNullChars(fileName);
		final int index = indexOfLastSeparator(fileName);
		return fileName.substring(index + 1);
	}

	private static void requireNonNullChars(final String path) {
		if (path.indexOf(0) >= 0) {
			throw new IllegalArgumentException("Null byte present in file/path name. There are no "
					                                   + "known legitimate use cases for such data, but several " +
					                                   "injection attacks may use it");
		}
	}

	public static int indexOfLastSeparator(final String fileName) {
		if (fileName == null) {
			return NOT_FOUND;
		}
		final int lastUnixPos = fileName.lastIndexOf(UNIX_SEPARATOR);
		final int lastWindowsPos = fileName.lastIndexOf(WINDOWS_SEPARATOR);
		return Math.max(lastUnixPos, lastWindowsPos);
	}

	public static String getExtension(MultipartFile file) {
		// get from content type first
		String ext = CONTENT_MAP.get(file.getContentType());

		// get from file name
		if (ext == null || ext.isEmpty()) {
			ext = FilenameUtils.getExtension(file.getOriginalFilename());
		}
		return ext == null ? "" : ext.toLowerCase();
	}

	public static void copy(final InputStream in, final OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int count;

		while ((count = in.read(buffer)) != -1) {
			out.write(buffer, 0, count);
		}

		// Flush out stream, to write any remaining buffered data
		out.flush();
	}

	public static File saveFileOnServer(File uploadDir, InputStream file, String extension, String fileName)
			throws IOException {
		// Generate random file name if not provide
		if (!StringUtils.hasText(fileName)) {
			fileName = UUID.randomUUID().toString().replace("-", "");
			// generate random name with no extension
			if (StringUtils.hasText(extension)) {
				fileName = String.format("%s%s%s", fileName, FileUtils.EXTENSION_SEPARATOR, extension);
			}
		}

		File fileOnServer = new File(uploadDir, fileName);

		try (FileOutputStream fos = new FileOutputStream(fileOnServer)) {
			FileUtils.copy(file, fos);
			// To be certain that the file is actually written to disk
			fos.flush();
			fos.getFD().sync();
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		} finally {
			IOUtils.closeQuietly(file);
		}
		return fileOnServer;
	}

	public static String guessContentType(File uploadedFile) {
		String contentType = null;
		if (uploadedFile != null && uploadedFile.exists()) {
			try {
				Tika tika = new Tika();

				// detect by filename first
				contentType = tika.detect(uploadedFile.getName());

				if (!StringUtils.hasText(contentType) || contentType.equalsIgnoreCase(MimeTypes.OCTET_STREAM)) {
					FileInputStream fileInputStream = new FileInputStream(uploadedFile);
					contentType = tika.detect(fileInputStream);
				}

			} catch (Exception e) {
				log.error("Unable to detect mime type");
			}
		}
		return contentType;
	}

	public static void writeFile(InputStream in, File out, Long expectedFileSize) throws IOException {
		FileOutputStream fos = null;
		FileUtils.silenceDelete(out);
		try {
			fos = new FileOutputStream(out);

			IOUtils.copy(in, fos);

			if (expectedFileSize != null) {
				Long bytesWrittenToDisk = out.length();
				if (!expectedFileSize.equals(bytesWrittenToDisk)) {
					log.warn("Expected file {} to be {} bytes; file on disk is {} bytes",
					         out.getAbsolutePath(), expectedFileSize, 1);
					FileUtils.silenceDelete(out);
					throw new IOException(
							String.format("Unexpected file size mismatch. Actual bytes %s. Expected bytes %s.",
							              bytesWrittenToDisk, expectedFileSize));
				}
			}
		} catch (Exception e) {
			FileUtils.silenceDelete(out);
			throw new IOException(e);
		} finally {
			try {
				if (fos != null) {
					fos.flush();
				}
			} catch (Exception e) {
				// ignore
			}
			IOUtils.closeQuietly(fos);
		}
	}

	public static void deleteDirectory(final File directory) throws IOException {
		Objects.requireNonNull(directory, "directory");
		if (!directory.exists()) {
			return;
		}
		if (!isSymlink(directory)) {
			cleanDirectory(directory);
		}
		delete(directory);
	}

	public static boolean isSymlink(final File file) {
		return file != null && Files.isSymbolicLink(file.toPath());
	}

	public static File delete(final File file) throws IOException {
		Objects.requireNonNull(file, "file");
		Files.delete(file.toPath());
		return file;
	}

	public static void cleanDirectory(final File directory) throws IOException {
		File[] files = directory.listFiles();

		final List<Exception> causeList = new ArrayList<>();
		assert files != null;
		for (File file : files) {
			try {
				boolean deleteResult = file.delete();
				if (!deleteResult) {
					throw new IOException("Cannot delete file: " + file);
				}
			} catch (final Exception ioe) {
				log.error(ioe.getMessage(), ioe);
				causeList.add(ioe);
			}
		}

		if (!causeList.isEmpty()) {
			throw new IOException(directory.toString());
		}
	}

	public static void assertCombinedFileIsValid(File uploadDir, long totalFileSize, File outputFile, String uuid)
			throws MergePartsException {
		long outputFileSize = outputFile.length();
		log.info("File UUID {} TOTAL {} bytes; file on disk is {} bytes", uuid, totalFileSize, outputFileSize);
		if (totalFileSize != outputFileSize) {
			FileUtils.deletePartitionFiles(uploadDir, uuid);
			FileUtils.silenceDelete(outputFile);
			throw new MergePartsException("Incorrect combined file size!");
		}
	}
}
