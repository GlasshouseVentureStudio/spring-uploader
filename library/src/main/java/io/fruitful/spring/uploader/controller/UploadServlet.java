package io.fruitful.spring.uploader.controller;

import io.fruitful.spring.uploader.dto.*;
import io.fruitful.spring.uploader.enumeration.FileSupportEnum;
import io.fruitful.spring.uploader.exception.MergePartsException;
import io.fruitful.spring.uploader.service.MediaHelperService;
import io.fruitful.spring.uploader.util.FileUtils;
import io.fruitful.spring.uploader.util.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class UploadServlet extends HttpServlet {

	private static final long serialVersionUID = -1551073211800080798L;
	private static final int SUCCESS_RESPONSE_CODE = 200;

	private File uploadDir;
	private File tempDir;
	private final UploadConfig config;

	public UploadServlet(UploadConfig uploadConfig) {
		this.config = uploadConfig;
	}

	@Override
	public void init() {
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
		resp.setContentType("text/plain");
		resp.setStatus(SUCCESS_RESPONSE_CODE);
		try {
			if (ServletFileUpload.isMultipartContent(req)) {
				MultipartUploadParser multipartUploadParser = new MultipartUploadParser(
						req, tempDir, getServletContext());
				RequestParser requestParser = RequestParser.getInstance(req, multipartUploadParser);
				writeFileForMultipartRequest(requestParser, resp);

			} else {
				RequestParser requestParser = RequestParser.getInstance(req, null);
				// handle POST delete file request
				if (requestParser.getMethod() != null && requestParser.getMethod().equalsIgnoreCase("DELETE")) {
					String uuid = requestParser.getUuid();
					FileUtils.deleteDirectory(new File(uploadDir, uuid));
				} else {
					writeFileForNonMultipartRequest(requestParser, req, resp);
				}
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

	private void writeFileForNonMultipartRequest(RequestParser requestParser,
	                                             HttpServletRequest req,
	                                             HttpServletResponse resp) throws Exception {
		log.info("File UUID {} PART SIZE {} bytes; TOTAL {} bytes; index: {}, totalPart: {}",
		         requestParser.getUuid(),
		         requestParser.getFileSize() == null ? "N/A" : requestParser.getFileSize(),
		         requestParser.getTotalFileSize(),
		         requestParser.getPartIndex(),
		         requestParser.getTotalParts());


		File dir = new File(uploadDir, requestParser.getUuid());
		FileUtils.mkDir(dir);

		String contentLengthHeader = req.getHeader("Content-Length");
		long expectedFileSize = Long.parseLong(contentLengthHeader);

		String mediaId = null;
		if (requestParser.getPartIndex() >= 0) {
			if (requestParser.getPartIndex() >= requestParser.getTotalParts()) {
				return;
			}

			String outputFile = requestParser.getUuid() + "_" + String.format("%05d", requestParser.getPartIndex());
			FileUtils.writeFile(req.getInputStream(), new File(dir, outputFile), null);

			mediaId = chunkDone(requestParser, dir, requestParser.getFilename());
		} else {
			FileUtils.writeFile(req.getInputStream(), new File(dir, requestParser.getFilename()), expectedFileSize);
		}

		if (mediaId == null) {
			writeResponse(resp.getWriter(), requestParser.generateError() ? "Generated error" : null);
		} else {
			writeChunkDoneResponse(resp.getWriter(), mediaId);
		}
	}

	private void writeFileForMultipartRequest(RequestParser requestParser,
	                                          HttpServletResponse resp) throws Exception {
		log.info("File UUID {} PART SIZE {} bytes; TOTAL {} bytes; index: {}, totalPart: {}",
		         requestParser.getUuid(),
		         requestParser.getFileSize() == null ? "N/A" : requestParser.getFileSize(),
		         requestParser.getTotalFileSize(),
		         requestParser.getPartIndex(),
		         requestParser.getTotalParts());

		File dir = new File(uploadDir, requestParser.getUuid());
		FileUtils.mkDir(dir);

		String mediaId = null;
		if (requestParser.getPartIndex() >= 0) {
			if (requestParser.getPartIndex() >= requestParser.getTotalParts()) {
				return;
			}

			String outputFile = requestParser.getUuid() + "_" + String.format("%05d", requestParser.getPartIndex());
			FileUtils.writeFile(requestParser.getUploadItem().getInputStream(),
			                    new File(dir, outputFile),
			                    requestParser.getFileSize());

			mediaId = chunkDone(requestParser, dir, requestParser.getOriginalFilename());
		} else {
			FileUtils.writeFile(requestParser.getUploadItem().getInputStream(),
			                    new File(dir, requestParser.getFilename()),
			                    requestParser.getFileSize());
		}

		if (mediaId == null) {
			writeResponse(resp.getWriter(), requestParser.generateError() ? "Generated error" : null);
		} else {
			writeChunkDoneResponse(resp.getWriter(), mediaId);
		}
	}

	private String chunkDone(RequestParser request, File dir, String outputFileName)
			throws IOException, MergePartsException {
		FilenameFilter acceptedFilter = (dir1, name) -> {
			String lowercaseName = name.toLowerCase();
			return lowercaseName.startsWith(request.getUuid());
		};
		synchronized (this) {
			String[] dirs = dir.list(acceptedFilter);
			if (dirs == null || request.getTotalParts() != dirs.length) {
				return null;
			}
			File[] parts = FileUtils.getPartitionFiles(dir, request.getUuid());
			File outputFile = new File(dir, outputFileName);
			// Make sure we don't have any existing file before writing the output
			FileUtils.silenceDelete(outputFile);
			for (File part : parts) {
				FileUtils.mergeFiles(outputFile, part);
			}

			FileUtils.assertCombinedFileIsValid(uploadDir, request.getTotalFileSize(), outputFile,
			                                    request.getUuid());
			FileUtils.deletePartitionFiles(dir, request.getUuid());

			FileInputStream fileInputStream = new FileInputStream(outputFile);
			MultipartFile multipartFile = new DiskMultipartFile(request.getFilename(), request.getFilename(),
			                                                    null, fileInputStream);
			MediaInfo mediaInfo = buildMediaInfo(multipartFile, request.getUuid(), request.getOriginal());
			FileUtils.deleteDirectory(dir);

			Function<MediaInfo, String> mediaHandler = config.getMediaHandler();
			return mediaHandler != null ? mediaHandler.apply(mediaInfo) : null;
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
			writer.print("{\"success\": true}");
		} else {
			writer.print("{\"error\": \"" + failureReason + "\"}");
		}
	}

	private void writeChunkDoneResponse(PrintWriter writer, String mediaId) {
		writer.print("{\"success\": true, \"mediaId\": \"" + mediaId + "\"}");
	}
}
