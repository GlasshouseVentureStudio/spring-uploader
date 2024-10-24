package io.fruitful.spring.uploader.service;

import io.fruitful.spring.uploader.constant.MediaConst;
import io.fruitful.spring.uploader.dto.MediaInfo;
import io.fruitful.spring.uploader.enumeration.FileSupportEnum;
import io.fruitful.spring.uploader.util.FileUtils;
import io.fruitful.spring.uploader.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

@Slf4j
public class MediaHelperService {

	public static void saveMediaInfo(File uploadDir, MediaInfo media, File file, String fileType, String ext,
	                                 boolean origin) {
		try {
			if (!file.exists()) {
				log.warn("File not exists {}", file.getName());
				return;
			}

			if (!StringUtils.hasText(ext) && StringUtils.hasText(media.getContentType())) {
				ext = FileUtils.CONTENT_MAP.get(media.getContentType());
			}

			if (MediaConst.EXT_STATIC_IMAGE.contains(ext)
					|| FileSupportEnum.READABLE_IMAGE.getTypes().contains(media.getContentType())) {
				saveStaticImage(uploadDir, media, file, origin, ext);

			} else if (MediaConst.EXT_GIF.equalsIgnoreCase(ext)
					|| FileSupportEnum.GIF_IMAGE.getTypes().contains(media.getContentType())) {
				saveGifImage(uploadDir, media, file, ext);

			} else if (FileSupportEnum.VIDEO.name().equals(fileType)) {
//				saveVideo(media, filePath, usingQueue, fileName, ext);

			} else if (FileSupportEnum.AUDIO.name().equals(fileType)) {
//				saveAudio(media, fileName, ext);

			} else if (FileSupportEnum.IMAGE.getTypes().contains(media.getContentType())) {
//				String newfilePath = convertToPng(filePath, media.getContentType());
//				if (StringUtils.hasText(newfilePath)) {
//					ext = MediaConst.EXT_PNG;
//					media.setFilename(FilenameUtils.getName(newfilePath));
//					saveStaticImage(uploadDir, media, newfilePath, origin, ext);
//					Files.deleteIfExists(file.toPath());
//				}
			}
		} catch (Exception e) {
			log.error("Save media info error", e);
		}
	}

	public static void saveStaticImage(File uploadDir, MediaInfo media, File imageFile, boolean origin, String ext) throws Exception {
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
			MediaInfo thumbnailMedia = new MediaInfo();
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
		if (inputStream == null || !StringUtils.hasText(ext)) {
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
}
