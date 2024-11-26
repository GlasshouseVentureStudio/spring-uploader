package io.fruitful.spring.uploader.controller;

import io.fruitful.spring.uploader.dto.MultipartUploadParser;
import io.fruitful.spring.uploader.dto.RequestParser;
import io.fruitful.spring.uploader.dto.UploadConfig;
import io.fruitful.spring.uploader.util.FileUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.Optional;

@Slf4j
public class UploadServlet extends HttpServlet {

	@Serial
	private static final long serialVersionUID = -1551073211800080798L;
	private static final int SUCCESS_RESPONSE_CODE = 200;

	private final File uploadDir;
	private final File tempDir;
	private final UploadConfig config;

	public UploadServlet(UploadConfig uploadConfig) {
		this.config = uploadConfig;
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
			if (JakartaServletFileUpload.isMultipartContent(req)) {
				ServletContext servletContext = getServletContext();
				MultipartUploadParser multipartUploadParser = new MultipartUploadParser(req, tempDir, servletContext,
				                                                                        true);
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

		if (requestParser.getPartIndex() >= 0) {
			if (requestParser.getPartIndex() >= requestParser.getTotalParts()) {
				return;
			}
			String outputFile = requestParser.getUuid() + "_" + String.format("%05d", requestParser.getPartIndex());
			FileUtils.writeFile(req.getInputStream(), new File(dir, outputFile), null);
		} else {
			FileUtils.writeFile(req.getInputStream(), new File(dir, requestParser.getFilename()), expectedFileSize);
		}
		writeResponse(resp.getWriter(), requestParser.generateError() ? "Generated error" : null);
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

		if (requestParser.getPartIndex() >= 0) {
			if (requestParser.getPartIndex() >= requestParser.getTotalParts()) {
				return;
			}
			String outputFile = requestParser.getUuid() + "_" + String.format("%05d", requestParser.getPartIndex());
			FileUtils.writeFile(requestParser.getUploadItem().getInputStream(),
			                    new File(dir, outputFile),
			                    requestParser.getFileSize());
		} else {
			FileUtils.writeFile(requestParser.getUploadItem().getInputStream(),
			                    new File(dir, requestParser.getFilename()),
			                    requestParser.getFileSize());
		}
		writeResponse(resp.getWriter(), requestParser.generateError() ? "Generated error" : null);
	}

	private void writeResponse(PrintWriter writer, String failureReason) {
		if (failureReason == null) {
			writer.print("{\"responseData\": {\"success\": true}}");
		} else {
			writer.print("{\"error\": \"" + failureReason + "\"}");
		}
	}
}
