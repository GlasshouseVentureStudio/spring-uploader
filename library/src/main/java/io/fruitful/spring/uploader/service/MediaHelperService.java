package io.fruitful.spring.uploader.service;

import io.fruitful.spring.uploader.constant.MediaConst;
import io.fruitful.spring.uploader.dto.MediaInfo;
import io.fruitful.spring.uploader.dto.MediaThumbnailInfo;
import io.fruitful.spring.uploader.dto.UploadConfig;
import io.fruitful.spring.uploader.enumeration.FileSupportEnum;
import io.fruitful.spring.uploader.util.FileUtils;
import io.fruitful.spring.uploader.util.ImageUtils;
import io.fruitful.spring.uploader.util.StringHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MediaHelperService {

	public static void saveMediaInfo(File uploadDir, MediaInfo media, File file, String fileType, String ext,
	                                 boolean origin, UploadConfig uploadConfig) {
		try {
			if (!file.exists()) {
				log.warn("File not exists {}", file.getName());
				return;
			}
			String mediaContentType = media.getContentType();
			if (StringHelper.isEmpty(ext) && StringHelper.hasText(media.getContentType())) {
				ext = FileUtils.CONTENT_MAP.get(mediaContentType);
			}

			if (isStaticImage(ext, mediaContentType)) {
				saveStaticImage(uploadDir, media, file, origin, ext);

			} else if (isGifImage(ext, mediaContentType)) {
				saveGifImage(uploadDir, media, file, ext);

			} else if (FileSupportEnum.VIDEO.name().equals(fileType)) {
				saveVideo(uploadDir, media, file, ext, uploadConfig);

			} else if (FileSupportEnum.AUDIO.name().equals(fileType)) {
				saveAudio(media, ext);

			} else if (isOtherImage(mediaContentType)) {
				String newFilePath = convertToPng(file, mediaContentType, uploadConfig.getFfmpegPath());
				if (StringHelper.hasText(newFilePath)) {
					ext = MediaConst.EXT_PNG;
					media.setFilename(FilenameUtils.getName(newFilePath));
					saveStaticImage(uploadDir, media, new File(newFilePath), origin, ext);
					Files.deleteIfExists(file.toPath());
				}
			}
		} catch (Exception e) {
			log.error("Save media info error", e);
		}
	}

	private static boolean isOtherImage(String mediaContentType) {
		return mediaContentType != null && FileSupportEnum.IMAGE.getTypes().contains(mediaContentType);
	}

	private static boolean isGifImage(String ext, String mediaContentType) {
		return MediaConst.EXT_GIF.equalsIgnoreCase(ext)
				|| (mediaContentType != null && FileSupportEnum.GIF_IMAGE.getTypes().contains(mediaContentType));
	}

	private static boolean isStaticImage(String ext, String mediaContentType) {
		return MediaConst.EXT_STATIC_IMAGE.contains(ext)
				|| (mediaContentType != null
				&& FileSupportEnum.READABLE_IMAGE.getTypes().contains(mediaContentType));
	}

	public static void saveStaticImage(File uploadDir, MediaInfo media, File imageFile, boolean origin, String ext)
			throws Exception {
		BufferedImage buffImage;
		// if not upload original image, must process rotate and enable
		// progressive ...
		if (!origin) {
			// read and check image rotated
			buffImage = ImageUtils.rotateImage(imageFile);
		} else {
			buffImage = ImageIO.read(imageFile);
		}

		// save width height
		if (buffImage != null) {
			media.setWidth(buffImage.getWidth());
			media.setHeight(buffImage.getHeight());
		}
		// extract thumbnail for static images
		FileInputStream fileInputStream = new FileInputStream(imageFile);
		File thumbnail = extractStaticImageThumbnail(uploadDir, fileInputStream, ext, null, null);

		if (thumbnail != null) {
			saveThumbnailMedia(media, thumbnail);
		} else {
			log.error("<<< Unable to extract thumbnail: {} >>>", imageFile.getName());
		}
	}

	public static void saveThumbnailMedia(MediaInfo media, File thumbnail) throws IOException {
		if (MediaConst.JPEG_MINE_TYPE.equals(media.getContentType())) {
			return;
		}

		if (thumbnail != null) {
			MediaThumbnailInfo thumbnailMedia = new MediaThumbnailInfo();
			thumbnailMedia.setContentType(MediaConst.JPEG_MINE_TYPE);
			thumbnailMedia.setOriginalFilename(thumbnail.getName());
			thumbnailMedia.setFilename(thumbnail.getName());
			thumbnailMedia.setGuid(String.format("%s%s", media.getGuid(), "thumbnail"));

			BufferedImage buffImage = ImageIO.read(thumbnail);
			// save width height
			if (buffImage != null) {
				thumbnailMedia.setWidth(buffImage.getWidth());
				thumbnailMedia.setHeight(buffImage.getHeight());
			}
			media.setThumbnail(thumbnailMedia);
		}
	}

	public static File extractStaticImageThumbnail(File uploadDir, InputStream inputStream, String ext,
	                                               Integer width, Integer height) {
		if (inputStream == null || StringHelper.isEmpty(ext)) {
			return null;
		}
		try {
			// only resize (presume image is rotated)
			width = width != null ? width : ImageUtils.THUMBNAIL_WIDTH;
			height = height != null ? height : 0;

			File thumbnail = null;
			// only extract from static images
			if (MediaConst.EXT_STATIC_IMAGE.contains(ext)) {
				InputStream resizedStream = ImageUtils.resizeImage(inputStream, ext, width, height);
				// save thumbnail image (default width = 500px)
				thumbnail = FileUtils.saveFileOnServer(uploadDir, resizedStream, MediaConst.EXT_JPG, null);
				inputStream.close();
			} else if (MediaConst.EXT_GIF.equalsIgnoreCase(ext)) {
				BufferedImage image = ImageIO.read(inputStream);
				if (image != null) {
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					ImageIO.write(image, MediaConst.EXT_GIF, output);
					thumbnail = FileUtils.saveFileOnServer(uploadDir, new ByteArrayInputStream(output.toByteArray()),
					                                       MediaConst.EXT_GIF, null);
				}
				inputStream.close();
			}
			return thumbnail;
		} catch (IOException e) {
			return null;
		}
	}

	public static void saveGifImage(File uploadDir, MediaInfo media, File imageFile, String ext) throws IOException {
		BufferedImage buffImage = ImageIO.read(imageFile);
		// save width height
		if (buffImage != null) {
			media.setWidth(buffImage.getWidth());
			media.setHeight(buffImage.getHeight());
		}
		// extract thumbnail for static images
		FileInputStream fileInputStream = new FileInputStream(imageFile);
		File thumbnail = extractStaticImageThumbnail(uploadDir, fileInputStream, ext, null, null);

		if (thumbnail != null) {
			saveThumbnailMedia(media, thumbnail);
		} else {
			log.error("<<< Unable to extract thumbnail: {} >>>", imageFile.getName());
		}
	}

	public static void saveVideo(File uploadDir, MediaInfo media, File videoFile, String ext,
	                             UploadConfig uploadConfig) throws IOException {
		// from now on we always convert the media to optimise the streaming speed
		media.setProcessing(true);
		// all video will be converted to mp4 format
		media.setUrl(media.getFilename().replace(ext, MediaConst.EXT_MP4));
		// can extract thumbnail from both mp4 and mov video
		File thumbnail = extractVideoThumbnail(uploadDir, videoFile.getPath(), uploadConfig);
		if (thumbnail != null) {
			saveThumbnailMedia(media, thumbnail);
		}
	}

	public static File extractVideoThumbnail(File uploadDir, String filePath, UploadConfig uploadConfig) {
		// using FFMPEG to get first frame
		String ffmpegPath = uploadConfig.getFfmpegPath();
		String ffmpegThumbExt = uploadConfig.getFfmpegThumbExt();
		String ffmpegStartTime = uploadConfig.getFfmpegThumbStartTime();

		String baseName = FilenameUtils.getBaseName(ffmpegPath);
		String thumbName = String.format("%s_extract%s%s", baseName, FilenameUtils.EXTENSION_SEPARATOR,
		                                 ffmpegThumbExt);
		String thumbPath = new File(uploadDir, thumbName).getPath();
		try {
			return new VideoProcessor(ffmpegPath).process(filePath, thumbPath, ffmpegStartTime, ffmpegThumbExt);
		} catch (Exception e) {
			log.error("Can not extract video thumbnail from filePath: {}", filePath, e);
		}
		return null;
	}

	public static void saveAudio(MediaInfo media, String ext) {
		// all audio will be converted to mp3 format
		media.setUrl(media.getFilename().replace(ext, MediaConst.EXT_MP3));
		media.setContentType("audio/mp3");
		// will not convert mp3 file
		if (ext.equalsIgnoreCase(MediaConst.EXT_MP3)) {
			media.setProcessing(null);
		} else {
			media.setProcessing(true);
		}
	}

	public static String convertToPng(File imageFile, String contentType, String ffmpegPath) {
		log.info("convert image with content type {} to png", contentType);
		String fileName = FilenameUtils.getName(imageFile.getPath());
		String newFilePath = imageFile.getParent() + File.separator + fileName + "." + MediaConst.EXT_PNG;

		String[] args = new String[]{ffmpegPath, "-i", imageFile.getPath(), newFilePath};

		try {
			int convertTimeout = 1; // 1 minute
			Process p = Runtime.getRuntime().exec(args);
			boolean status = p.waitFor(convertTimeout, TimeUnit.MINUTES);
			if (status && new File(newFilePath).exists()) {
				return newFilePath;
			}
		} catch (InterruptedException e) {
			log.error("convert to png error: {}", e.getMessage(), e);
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			log.error("convert to png error: {}", e.getMessage(), e);
		}
		return null;
	}
}
