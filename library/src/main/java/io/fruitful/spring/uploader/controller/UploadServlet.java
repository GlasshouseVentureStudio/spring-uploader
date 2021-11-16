package io.fruitful.spring.uploader.controller;

import io.fruitful.spring.uploader.dto.MultipartUploadParser;
import io.fruitful.spring.uploader.dto.RequestParser;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.regex.Pattern;

public class UploadServlet extends HttpServlet {
	private static final long serialVersionUID = -1551073211800080798L;

	private static File UPLOAD_DIR;
	private static File TEMP_DIR;

	private static String CONTENT_LENGTH = "Content-Length";
	private static int SUCCESS_RESPONSE_CODE = 200;

	@Override
	public void init() throws ServletException {
//		UPLOAD_DIR = new File(FileUploadHelper.ROOT_UPLOAD_DIRECTORY);
//		TEMP_DIR = new File(FileUploadHelper.ROOT_TMP_DIRECTORY);
		UPLOAD_DIR = new File("");
		TEMP_DIR = new File("");
		UPLOAD_DIR.mkdirs();
		TEMP_DIR.mkdirs();
	}

	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String uuid = req.getPathInfo().replaceAll("/", "");

		handleDeleteFileRequest(uuid, resp);
	}

	private static void silenceDelete(File file) {
		try {
			file.delete();
		} catch (Exception e) { /** silence catch **/}
	}

	private void handleDeleteFileRequest(String uuid, HttpServletResponse resp) throws IOException {
		FileUtils.deleteDirectory(new File(UPLOAD_DIR, uuid));
		resp.setStatus(SUCCESS_RESPONSE_CODE);
	}

	@Override
	public void doOptions(HttpServletRequest req, HttpServletResponse resp) {
		resp.setStatus(SUCCESS_RESPONSE_CODE);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		RequestParser requestParser = null;

		boolean isIframe = req.getHeader("X-Requested-With") == null
				|| !req.getHeader("X-Requested-With").equals("XMLHttpRequest");

		try {
			resp.setContentType("text/plain");
			resp.setStatus(SUCCESS_RESPONSE_CODE);

			if (ServletFileUpload.isMultipartContent(req)) {
				MultipartUploadParser multipartUploadParser = new MultipartUploadParser(req, TEMP_DIR,
						getServletContext());
				requestParser = RequestParser.getInstance(req, multipartUploadParser);
				writeFileForMultipartRequest(requestParser);
				writeResponse(resp.getWriter(), requestParser.generateError() ? "Generated error" : null, isIframe,
						false, requestParser);
			} else {
				requestParser = RequestParser.getInstance(req, null);

				// handle POST delete file request
				if (requestParser.getMethod() != null && requestParser.getMethod().equalsIgnoreCase("DELETE")) {
					String uuid = requestParser.getUuid();
					handleDeleteFileRequest(uuid, resp);
				} else {
					writeFileForNonMultipartRequest(req, requestParser);
					writeResponse(resp.getWriter(), requestParser.generateError() ? "Generated error" : null, isIframe,
							false, requestParser);
				}
			}
		} catch (Exception e) {
			if (e instanceof MergePartsException) {
				writeResponse(resp.getWriter(), e.getMessage(), isIframe, true, requestParser);
			} else {
				writeResponse(resp.getWriter(), e.getMessage(), isIframe, false, requestParser);
			}
		}
	}

	private void writeFileForNonMultipartRequest(HttpServletRequest req, RequestParser requestParser) throws Exception {
//		log.info("File UUID {} PARTSIZE {} bytes; TOTAL {} bytes; index: {}, totalPart: {}",
//				requestParser.getUuid(),
//				requestParser.getFileSize() == null ? "N/A" : requestParser.getFileSize(),
//				requestParser.getTotalFileSize(),
//				requestParser.getPartIndex(),
//				requestParser.getTotalParts());


		File dir = new File(UPLOAD_DIR, requestParser.getUuid());
		dir.mkdirs();

		String contentLengthHeader = req.getHeader(CONTENT_LENGTH);
		long expectedFileSize = Long.parseLong(contentLengthHeader);

		if (requestParser.getPartIndex() >= 0) {
			if (requestParser.getPartIndex() >= requestParser.getTotalParts()) return;

			writeFile(req.getInputStream(),
					new File(dir, requestParser.getUuid() + "_" + String.format("%05d", requestParser.getPartIndex())),
					null);

			FilenameFilter acceptedFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase();
					if (lowercaseName.startsWith(requestParser.getUuid())) {
						return true;
					} else {
						return false;
					}
				}
			};

			synchronized (this) {
				if (requestParser.getTotalParts() == dir.list(acceptedFilter).length) {
					File[] parts = getPartitionFiles(dir, requestParser.getUuid());
					File outputFile = new File(dir, requestParser.getFilename());

					// Make sure we don't have any existing file before writing the output
					silenceDelete(outputFile);

					for (File part : parts) {
						mergeFiles(outputFile, part);
					}

					assertCombinedFileIsVaid(requestParser.getTotalFileSize(), outputFile, requestParser.getUuid());
					deletePartitionFiles(dir, requestParser.getUuid());
				}
			}
		} else {
			writeFile(req.getInputStream(), new File(dir, requestParser.getFilename()), expectedFileSize);
		}
	}

	private void writeFileForMultipartRequest(RequestParser requestParser) throws Exception {
//		log.info("File UUID {} PARTSIZE {} bytes; TOTAL {} bytes; index: {}, totalPart: {}",
//				requestParser.getUuid(),
//				requestParser.getFileSize() == null ? "N/A" : requestParser.getFileSize(),
//				requestParser.getTotalFileSize(),
//				requestParser.getPartIndex(),
//				requestParser.getTotalParts());

		File dir = new File(UPLOAD_DIR, requestParser.getUuid());
		dir.mkdirs();

		if (requestParser.getPartIndex() >= 0) {
			if (requestParser.getPartIndex() >= requestParser.getTotalParts()) return;

			writeFile(requestParser.getUploadItem().getInputStream(),
					new File(dir, requestParser.getUuid() + "_" + String.format("%05d", requestParser.getPartIndex())),
					requestParser.getFileSize());

			FilenameFilter acceptedFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase();
					if (lowercaseName.startsWith(requestParser.getUuid())) {
						return true;
					} else {
						return false;
					}
				}
			};

			synchronized (this) {
				if (requestParser.getTotalParts() == dir.list(acceptedFilter).length) {
					File[] parts = getPartitionFiles(dir, requestParser.getUuid());
					File outputFile = new File(dir, requestParser.getOriginalFilename());

					// Make sure we don't have any existing file before writing the output
					silenceDelete(outputFile);

					for (File part : parts) {
						mergeFiles(outputFile, part);
					}

					assertCombinedFileIsVaid(requestParser.getTotalFileSize(), outputFile, requestParser.getUuid());
					deletePartitionFiles(dir, requestParser.getUuid());
				}
			}
		} else {
			writeFile(requestParser.getUploadItem().getInputStream(),
					new File(dir, requestParser.getFilename()),
					requestParser.getFileSize());
		}
	}

	private void assertCombinedFileIsVaid(long totalFileSize, File outputFile, String uuid) throws MergePartsException {
		long outputFileSize = outputFile.length();

//		log.info("File UUID {} TOTAL {} bytes; file on disk is {} bytes", uuid, totalFileSize, outputFileSize);

		if (totalFileSize != outputFileSize) {
			deletePartitionFiles(UPLOAD_DIR, uuid);
			silenceDelete(outputFile);
			throw new MergePartsException("Incorrect combined file size!");
		}

	}

	private static class PartitionFilesFilter implements FilenameFilter {
		private String filename;

		PartitionFilesFilter(String filename) {
			this.filename = filename;
		}

		@Override
		public boolean accept(File file, String s) {
			return s.matches(Pattern.quote(filename) + "_\\d+");
		}
	}

	private static File[] getPartitionFiles(File directory, String filename) {
		File[] files = directory.listFiles(new PartitionFilesFilter(filename));
		Arrays.sort(files);
		return files;
	}

	private static void deletePartitionFiles(File directory, String filename) {
		File[] partFiles = getPartitionFiles(directory, filename);
		for (File partFile : partFiles) {
			partFile.delete();
		}
	}

	private File mergeFiles(File outputFile, File partFile) throws IOException {
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

	private void writeFile(InputStream in, File out, Long expectedFileSize) throws IOException {
		FileOutputStream fos = null;
		silenceDelete(out);
		try {
			fos = new FileOutputStream(out);

			IOUtils.copy(in, fos);

			if (expectedFileSize != null) {
				Long bytesWrittenToDisk = out.length();
				if (!expectedFileSize.equals(bytesWrittenToDisk)) {
//					log.warn("Expected file {} to be {} bytes; file on disk is {} bytes",
//							new Object[] { out.getAbsolutePath(), expectedFileSize, 1 });
					silenceDelete(out);
					throw new IOException(
							String.format("Unexpected file size mismatch. Actual bytes %s. Expected bytes %s.",
									bytesWrittenToDisk, expectedFileSize));
				}
			}
		} catch (Exception e) {
			silenceDelete(out);
			throw new IOException(e);
		} finally {
			try {
				if (fos != null) fos.flush();
			} catch (Exception e) {    /** silence catch **/}
			IOUtils.closeQuietly(fos);
		}
	}

	private void writeResponse(PrintWriter writer, String failureReason, boolean isIframe, boolean restartChunking,
							   RequestParser requestParser) {
		if (failureReason == null) {
			writer.print("{\"success\": true}");
		} else {
			if (restartChunking) {
				writer.print("{\"error\": \"" + failureReason + "\", \"reset\": true}");
			} else {
				writer.print("{\"error\": \"" + failureReason + "\"}");
			}
		}
	}

	private class MergePartsException extends Exception {
		MergePartsException(String message) {
			super(message);
		}
	}
}
