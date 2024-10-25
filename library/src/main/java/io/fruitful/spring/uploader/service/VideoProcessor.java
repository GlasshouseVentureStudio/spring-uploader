package io.fruitful.spring.uploader.service;

import io.fruitful.spring.uploader.constant.MediaConst;
import io.fruitful.spring.uploader.util.FileUtils;
import io.fruitful.spring.uploader.util.ImageUtils;
import io.fruitful.spring.uploader.util.StringHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class VideoProcessor extends MediaProcessor {

	public VideoProcessor(String ffmpegPath) {
		super(ffmpegPath);
	}

	/**
	 * Convert to MP4 format
	 */
	@Override
	public File process(String input, String destination) {

		try {
			File outputFile = new File(destination);
			FileUtils.delete(outputFile);
			new ShellCommandExecutor(ffmpegPath, "-y",
			                         "-loglevel", "panic",
			                         "-i", input,
			                         "-preset", "superfast",
			                         "-movflags", "+faststart",
			                         "-tune", "fastdecode",
			                         "-crf", "25",
			                         "-bufsize", "2M",
			                         "-c:a", "aac",
			                         destination).execute(CONVERSION_TIMEOUT);

			if (outputFile.exists() && outputFile.length() > 0) {
				return outputFile;
			}

		} catch (Exception e) {
			log.error("Convert failed..", e);
		}
		return null;
	}

	/**
	 * Extract video thumbnail
	 *
	 * @param input
	 * @param destination
	 * @param startTime
	 * @param thumbExt
	 * @return
	 */
	public File process(String input, String destination, String startTime, String thumbExt) {

		try {
			File outputFile = new File(destination);
			FileUtils.delete(outputFile);

			new ShellCommandExecutor(ffmpegPath, "-y", "-loglevel", "panic", "-i", input, "-ss", startTime,
			                         "-vframes",
			                         "1", destination).execute(CONVERSION_TIMEOUT);

			if (outputFile.exists() && outputFile.length() > 0) {

				FileInputStream fileInputStream = new FileInputStream(outputFile);
				InputStream stream = ImageUtils.resizeImage(fileInputStream, thumbExt,
				                                            MediaConst.THUMBNAIL_WIDTH, 0);
				org.apache.commons.io.FileUtils.copyInputStreamToFile(stream, outputFile);

				return outputFile;
			}

		} catch (Exception e) {
			log.error("Convert failed..", e);
		}
		return null;
	}

	public File process(File uploadDir, String localThumbnail, String playIconPath, String ext, Integer width,
	                    Integer height) {
		try {

			// generate temporary file name
			String randomName = String.format("%s.%s", StringHelper.generateUniqueString(), ext);

			File randomFile = new File(uploadDir, randomName);
			FileUtils.delete(randomFile);

			// ProcessBuilder thumbProcessBuilder = new
			// ProcessBuilder(ffmpegPath, "-y", "-loglevel", "panic", "-i",
			// filePath, "-i", playIconPath, "-filter_complex", "overlay=90:70",
			// "-preset", "superfast",
			// randomPath);

			String overlay = "overlay=90:70";
			if (width != null && height != null) {
				// size of logo is 60 x 60
				overlay = "overlay=" + (double) (width - 60) / 2 + ":" + (double) (height - 60) / 2;
			}

			new ShellCommandExecutor(ffmpegPath, "-y", "-loglevel", "panic", "-i", localThumbnail, "-i", playIconPath,
			                         "-filter_complex", overlay, "-preset", "superfast",
			                         randomFile.toPath().toString()).execute(
					CONVERSION_TIMEOUT);

			if (randomFile.exists() && randomFile.length() > 0) {

				File output = new File(localThumbnail);
				FileInputStream fileInputStream = new FileInputStream(randomFile);
				org.apache.commons.io.FileUtils.copyInputStreamToFile(fileInputStream, output);

				return output;
			}

		} catch (Exception e) {
			log.error("Can not add play icon to local thumbnail..", e);
		}
		return null;
	}

	public String detectRotation(String filePath) {
		// sample: "TAG:rotate=90"
		String regex = "rotate=(.*)";

		try {
			// // "v" is log level
			// ProcessBuilder pb = new ProcessBuilder(ffprobePath, "-v",
			// "error", "-show_entries", "stream_tags=rotate",
			// "-of", "default=noprint_wrappers=1", filePath);

			String outPutString = new ShellCommandExecutor(ffprobePath, "-v", "error", "-show_entries",
			                                               "stream_tags=rotate", "-of", "default=noprint_wrappers=1",
			                                               filePath).execute(CONVERSION_TIMEOUT);

			if (StringHelper.hasText(outPutString)) {
				// Find info in output string
				return StringHelper.extract(outPutString, regex);
			}
		} catch (Exception e) {
			log.error("Detect rotation error", e);
		}
		return null;
	}


	public String detectDuration(String filePath) {
		String[] cmd = {ffmpegPath, "-i", filePath};

		try {
			Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				if (line.contains("Duration:")) {
					String durationLine = line.trim();
					return durationLine.substring(durationLine.indexOf(":") + 2, durationLine.indexOf(","));
				}
			}

			process.waitFor();
			reader.close();

		} catch (IOException | InterruptedException e) {
			log.error(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
		return null;
	}

}
