package io.fruitful.spring.uploader.dto;

import io.fruitful.spring.uploader.util.StringHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.FileItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class RequestParser {
	private static final String FILENAME_PARAM = "qqfile";
	private static final String PART_INDEX_PARAM = "qqpartindex";
	private static final String FILE_SIZE_PARAM = "qqtotalfilesize";
	private static final String TOTAL_PARTS_PARAM = "qqtotalparts";
	private static final String UUID_PARAM = "qquuid";
	private static final String PART_FILENAME_PARAM = "qqfilename";
	private static final String PART_FILESIZE_PARAM = "qqfilesize";
	private static final String ORIGINAL_PARAM = "original";
	private static final String METHOD_PARAM = "_method";

	private static final String GENERATE_ERROR_PARAM = "generateError";

	private String filename;
	private FileItem uploadItem;
	private boolean generateError;

	private int partIndex = -1;
	private long totalFileSize;
	private Long fileSize;
	private int totalParts;
	private String uuid;
	private String originalFilename;
	private String method;
	private Boolean original;

	private final Map<String, String> customParams = new HashMap<>();

	//2nd param is null unless a MPFR
	public static RequestParser getInstance(HttpServletRequest request,
	                                        MultipartUploadParser multipartUploadParser) throws IOException {
		RequestParser requestParser = new RequestParser();

		if (multipartUploadParser == null) {
			if (request.getMethod().equals("POST") && request.getContentType() == null) {
				parseXdrPostParams(request, requestParser);
			} else {
				requestParser.filename = request.getParameter(FILENAME_PARAM);
				parseQueryStringParams(requestParser, request);
			}
		} else {
			DiskFileItem fileItem = multipartUploadParser.getFirstFile();
			if (fileItem != null) {
				requestParser.uploadItem = fileItem;
				requestParser.filename = fileItem.getName();
			}

			//params could be in body or query string, depending on Fine Uploader request option properties
			parseRequestBodyParams(requestParser, multipartUploadParser);
			parseQueryStringParams(requestParser, request);
		}

		removeQqParams(requestParser.customParams);
		return requestParser;
	}

	public String getFilename() {
		return originalFilename != null ? originalFilename : filename;
	}

	public boolean generateError() {
		return generateError;
	}

	private static void parseRequestBodyParams(RequestParser requestParser,
	                                           MultipartUploadParser multipartUploadParser) {
		if (multipartUploadParser.getParams().get(GENERATE_ERROR_PARAM) != null) {
			requestParser.generateError = Boolean.parseBoolean(
					multipartUploadParser.getParams().get(GENERATE_ERROR_PARAM));
		}

		String partNumStr = multipartUploadParser.getParams().get(PART_INDEX_PARAM);
		if (partNumStr != null) {
			requestParser.partIndex = Integer.parseInt(partNumStr);
		}

		String fileSizeStr = multipartUploadParser.getParams().get(FILE_SIZE_PARAM);
		if (fileSizeStr != null) {
			requestParser.totalFileSize = Long.parseLong(fileSizeStr);
		}

		String totalPartStr = multipartUploadParser.getParams().get(TOTAL_PARTS_PARAM);
		if (totalPartStr != null) {
			requestParser.totalParts = Integer.parseInt(totalPartStr);
		}

		String partFileSizeStr = multipartUploadParser.getParams().get(PART_FILESIZE_PARAM);
		if (partFileSizeStr != null) {
			try {
				requestParser.fileSize = Long.parseLong(partFileSizeStr);
			} catch (NumberFormatException nfe) {
				requestParser.fileSize = null;
			}
		}

		requestParser.customParams.putAll(multipartUploadParser.getParams());

		if (requestParser.uuid == null) {
			requestParser.uuid = multipartUploadParser.getParams().get(UUID_PARAM);
		}

		if (requestParser.originalFilename == null) {
			requestParser.originalFilename = multipartUploadParser.getParams().get(PART_FILENAME_PARAM);
		}

		if (requestParser.original == null) {
			requestParser.original = formatBooleanString(multipartUploadParser.getParams().get(ORIGINAL_PARAM));
		}
	}

	private static void parseQueryStringParams(RequestParser requestParser, HttpServletRequest req) {
		String generateErrorStr = req.getParameter(GENERATE_ERROR_PARAM);
		if (generateErrorStr != null) {
			requestParser.generateError = Boolean.parseBoolean(generateErrorStr);
		}

		String partNumStr = req.getParameter(PART_INDEX_PARAM);
		if (partNumStr != null) {
			requestParser.partIndex = Integer.parseInt(partNumStr);
		}

		String fileSizeStr = req.getParameter(FILE_SIZE_PARAM);
		if (fileSizeStr != null) {
			requestParser.totalFileSize = Long.parseLong(fileSizeStr);
		}

		String totalPartStr = req.getParameter(TOTAL_PARTS_PARAM);
		if (totalPartStr != null) {
			requestParser.totalParts = Integer.parseInt(totalPartStr);
		}

		String partFileSizeStr = req.getParameter(PART_FILESIZE_PARAM);
		try {
			requestParser.fileSize = partFileSizeStr != null ? Long.parseLong(partFileSizeStr) : null;
		} catch (NumberFormatException nfe) {
			requestParser.fileSize = null;
		}

		Enumeration<String> paramNames = req.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = paramNames.nextElement();
			requestParser.customParams.put(paramName, req.getParameter(paramName));
		}

		if (requestParser.uuid == null) {
			requestParser.uuid = req.getParameter(UUID_PARAM);
		}

		if (requestParser.method == null) {
			requestParser.method = req.getParameter(METHOD_PARAM);
		}

		if (requestParser.originalFilename == null) {
			requestParser.originalFilename = req.getParameter(PART_FILENAME_PARAM);
		}

		if (requestParser.original == null) {
			requestParser.original = formatBooleanString(req.getParameter(ORIGINAL_PARAM));
		}
	}

	private static void removeQqParams(Map<String, String> customParams) {
		customParams.entrySet().removeIf(paramEntry -> paramEntry.getKey().startsWith("qq"));
	}

	private static void parseXdrPostParams(HttpServletRequest request, RequestParser requestParser) throws IOException {
		String queryString = getQueryStringFromRequestBody(request);
		String[] queryParams = queryString.split("&");

		for (String queryParam : queryParams) {
			String[] keyAndVal = queryParam.split("=");
			String key = URLDecoder.decode(keyAndVal[0], "UTF-8");
			String value = URLDecoder.decode(keyAndVal[1], "UTF-8");

			switch (key) {
				case UUID_PARAM:
					requestParser.uuid = value;
					break;
				case METHOD_PARAM:
					requestParser.method = value;
					break;
				case ORIGINAL_PARAM:
					requestParser.original = formatBooleanString(value);
					break;
				default:
					requestParser.customParams.put(key, value);
					break;
			}
		}
	}

	private static String getQueryStringFromRequestBody(HttpServletRequest request) throws IOException {
		StringBuilder content = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = request.getReader();
			char[] chars = new char[128];
			int bytesRead;
			while ((bytesRead = reader.read(chars)) != -1) {
				content.append(chars, 0, bytesRead);
			}
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return content.toString();
	}

	private static Boolean formatBooleanString(String boolValue) {
		try {
			return StringHelper.hasText(boolValue) ? Boolean.valueOf(boolValue) : null;
		} catch (Exception e) {
			return null;
		}
	}
}
