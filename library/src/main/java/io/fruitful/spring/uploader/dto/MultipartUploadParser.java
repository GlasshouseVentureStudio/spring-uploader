package io.fruitful.spring.uploader.dto;

import io.fruitful.spring.uploader.util.StringHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
@Setter
public class MultipartUploadParser {
	final Logger log = LoggerFactory.getLogger(MultipartUploadParser.class);

	private Map<String, String> params = new HashMap<>();

	private List<DiskFileItem> files = new ArrayList<>();

	// fileItemsFactory is a field (even though it's scoped to the constructor) to
	// prevent the
	// org.apache.commons.fileupload.servlet.FileCleanerCleanup thread from
	// attempting to delete the
	// temp file before while it is still being used.
	//
	// FileCleanerCleanup uses a java.lang.ref.ReferenceQueue to delete the temp
	// file when the FileItemsFactory marker object is GCed
	private DiskFileItemFactory fileItemsFactory;

	public MultipartUploadParser(HttpServletRequest request, File repository)
			throws IOException {
		if (!repository.exists() && !repository.mkdirs()) {
			throw new IOException("Unable to mkdirs to " + repository.getPath());
		}

		fileItemsFactory = setupFileItemFactory(repository);

		JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory> upload =
				new JakartaServletFileUpload<>(fileItemsFactory);
		List<DiskFileItem> formFileItems = upload.parseRequest(request);

		parseFormFields(formFileItems);

		if (files.isEmpty()) {
			log.warn("No files were found when processing the requst. Debugging info follows.");

			writeDebugInfo(request);

			throw new FileUploadException("No files were found when processing the requst.");
		} else {
			if (log.isDebugEnabled()) {
				writeDebugInfo(request);
			}
		}
	}

	private DiskFileItemFactory setupFileItemFactory(File repository) {
		return DiskFileItemFactory.builder()
				.setBufferSize(DiskFileItemFactory.DEFAULT_THRESHOLD)
				.setPath(repository.toPath())
				.setFileCleaningTracker(new FileCleaningTracker())
				.get();
	}

	private void writeDebugInfo(HttpServletRequest request) {
		log.debug("-- POST HEADERS --");
		for (Object header : Collections.list(request.getHeaderNames())) {
			log.debug("{}: {}", header, request.getHeader(header.toString()));
		}

		log.debug("-- POST PARAMS --");
		for (Map.Entry<String, String> entry : params.entrySet()) {
			log.debug("{}: {}", entry.getKey(), entry.getValue());
		}
	}

	private void parseFormFields(List<DiskFileItem> items) throws IOException {
		for (DiskFileItem item : items) {
			if (item.isFormField()) {
				String key = item.getFieldName();
				String value = item.getString(StandardCharsets.UTF_8);
				if (StringHelper.hasLength(key)) {
					params.put(key, value);
				}
			} else {
				files.add(item);
			}
		}
	}

	public List<DiskFileItem> getFiles() {
		if (files.isEmpty()) {
			throw new RuntimeException("No FileItems exist.");
		}

		return files;
	}

	public DiskFileItem getFirstFile() {
		if (files.isEmpty()) {
			throw new RuntimeException("No FileItems exist.");
		}

		return files.iterator().next();
	}
}
