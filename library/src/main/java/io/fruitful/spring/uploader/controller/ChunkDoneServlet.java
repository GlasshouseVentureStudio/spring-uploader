package io.fruitful.spring.uploader.controller;

import io.fruitful.spring.uploader.dto.*;
import io.fruitful.spring.uploader.enumeration.FileSupportEnum;
import io.fruitful.spring.uploader.exception.MergePartsException;
import io.fruitful.spring.uploader.service.MediaHelperService;
import io.fruitful.spring.uploader.util.FileUtils;
import io.fruitful.spring.uploader.util.StringHelper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;

import java.io.*;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Slf4j
public class ChunkDoneServlet extends HttpServlet {

	@Serial
	private static final long serialVersionUID = -1551073211800080799L;
	private static final int SUCCESS_RESPONSE_CODE = 200;

	private final File uploadDir;
	private final File tempDir;
	private final ChunkDoneConfig config;

	public ChunkDoneServlet(ChunkDoneConfig chunkDoneConfig) {
		this.config = chunkDoneConfig;
		uploadDir = new File(Optional.ofNullable(config.getUploadFolder()).orElse(""));
		tempDir = new File(Optional.ofNullable(config.getTemporaryFolder()).orElse(""));
		FileUtils.mkDir(uploadDir);
		FileUtils.mkDir(tempDir);
	}

	@Override
	public void doOptions(HttpServletRequest req, HttpServletResponse resp) {
		resp.setStatus(SUCCESS_RESPONSE_CODE);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/json");
		resp.setStatus(SUCCESS_RESPONSE_CODE);
		try {
			RequestParser requestParser;
			String name;
			if (JakartaServletFileUpload.isMultipartContent(req)) {
				ServletContext servletContext = getServletContext();
				MultipartUploadParser multipartUploadParser = new MultipartUploadParser(req, tempDir, servletContext,
				                                                                        false);
				requestParser = RequestParser.getInstance(req, multipartUploadParser);
				name = requestParser.getOriginalFilename();
			} else {
				requestParser = RequestParser.getInstance(req, null);
				name = requestParser.getFilename();
			}

			File dir = new File(uploadDir, requestParser.getUuid());
			FileUtils.mkDir(dir);
			String mediaId = chunkDone(requestParser, dir, name);

			if (mediaId == null) {
				writeResponse(resp.getWriter(), requestParser.generateError() ? "Generated error" : null);
			} else {
				writeChunkDoneResponse(resp.getWriter(), mediaId);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			try {
				writeResponse(resp.getWriter(), e.getMessage());
			} catch (Exception responseError) {
				log.error("Error writing response", responseError);
			}
		}
	}

	private String chunkDone(RequestParser request, File dir, String outputFileName)
			throws IOException, MergePartsException {
		UnaryOperator<String> mediaExistedHandler = config.getMediaExistedHandler();
		String requestUuid = request.getUuid();
		if (mediaExistedHandler != null) {
			String mediaId = mediaExistedHandler.apply(requestUuid);
			if (StringHelper.hasText(mediaId)) {
				return mediaId;
			}
		}

		FilenameFilter acceptedFilter = (dir1, name) -> {
			String lowercaseName = name.toLowerCase();
			return lowercaseName.startsWith(requestUuid);
		};
		synchronized (this) {
			String[] dirs = dir.list(acceptedFilter);
			if (dirs == null || request.getTotalParts() != dirs.length) {
				return null;
			}
			File[] parts = FileUtils.getPartitionFiles(dir, requestUuid);
			File outputFile = new File(dir, outputFileName);
			// Make sure we don't have any existing file before writing the output
			FileUtils.silenceDelete(outputFile);
			for (File part : parts) {
				FileUtils.mergeFiles(outputFile, part);
			}

			FileUtils.assertCombinedFileIsValid(uploadDir, request.getTotalFileSize(), outputFile, requestUuid);
			FileUtils.deletePartitionFiles(dir, requestUuid);

			FileInputStream fileInputStream = new FileInputStream(outputFile);
			MultipartFile multipartFile = new DiskMultipartFile(request.getFilename(), request.getFilename(),
			                                                    null, fileInputStream);
			boolean original = Optional.ofNullable(request.getOriginal()).orElse(false);
			MediaInfo mediaInfo = buildMediaInfo(multipartFile, requestUuid, original);
			FileUtils.deleteDirectory(dir);

			Function<MediaInfo, String> mediaProcessHandler = config.getMediaProcessHandler();
			return mediaProcessHandler != null ? mediaProcessHandler.apply(mediaInfo) : null;
		}
	}

	private MediaInfo buildMediaInfo(MultipartFile file, String guid, boolean original) {
		if (file.getSize() == 0) {
			log.error("Upload file is null or empty");
			return null;
		}

		String contentType = file.getContentType();
		String fileType = FileSupportEnum.getFileType(contentType);
		String originalFilename = FileUtils.getName(file.getOriginalFilename());
		String originalExt = FileUtils.getExtension(file);
		MediaInfo media = new MediaInfo();
		try {
			File uploadedFile = FileUtils.saveFileOnServer(uploadDir, file.getInputStream(), originalExt, null);

			if (StringHelper.isEmpty(contentType) || contentType.equals("application/octet-stream")) {
				contentType = FileUtils.guessContentType(uploadedFile);
				fileType = FileSupportEnum.getFileType(contentType);
			}

			media.setGuid(guid);
			media.setContentType(contentType);
			media.setOriginalFilename(originalFilename);
			media.setUrl(uploadedFile.getName());
			media.setFilename(uploadedFile.getName());

			MediaHelperService.saveMediaInfo(uploadDir, media, uploadedFile, fileType, originalExt, original, config);

		} catch (IOException e) {
			log.error(e.getMessage());
		}
		return media;
	}

	private void writeResponse(PrintWriter writer, String failureReason) {
		if (failureReason == null) {
			writer.print("{\"responseData\": {\"success\": true}}");
		} else {
			writer.print("{\"error\": \"" + failureReason + "\"}");
		}
	}

	private void writeChunkDoneResponse(PrintWriter writer, String mediaId) {
		writer.print("{\"responseData\": {\"success\": true, \"mediaId\": \"" + mediaId + "\"}}");
	}
}
