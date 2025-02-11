package io.fruitful.spring.uploader.util;


import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import io.fruitful.spring.uploader.constant.MediaConst;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageUtils {

	private static final BufferedImageOp[] NO_FILTER = new BufferedImageOp[]{null};

	public static final int THUMBNAIL_WIDTH = MediaConst.THUMBNAIL_WIDTH;
	public static final double IMAGE_SIZE_LARGE_WIDTH = 1200;
	public static final double IMAGE_SIZE_MEDIUM_WIDTH = 640;
	public static final double IMAGE_SIZE_SMALL_WIDTH = 250;
	public static final double IMAGE_SIZE_XLARGE_WIDTH = 1440;
	public static final double IMAGE_SIZE_XXLARGE_WIDTH = 1920;
	public static final int MAX_STATIC_IMAGE_FILE_SIZE = 3000000;

	public static BufferedImage rotateImage(File imageFile) {
		long start = System.currentTimeMillis();
		BufferedImage buffOriginalImage = null;
		if (imageFile.exists()) {
			try {
				buffOriginalImage = ImageIO.read(imageFile);

				// must resize if image too large
				if (imageFile.length() >= MAX_STATIC_IMAGE_FILE_SIZE && buffOriginalImage.getWidth() > 0) {
					float ratio = ((float) buffOriginalImage.getHeight() / (float) buffOriginalImage.getWidth());
					int targetWidth = MediaConst.RESIZE_IMAGE_WIDTH;
					int targetHeight = Math.round(targetWidth * ratio);

					buffOriginalImage = Scalr.resize(buffOriginalImage, Scalr.Method.QUALITY, targetWidth,
					                                 targetHeight, NO_FILTER);
				}


				// Check image EXIF to read image size, orientation
				Map<Object, Object> info = readImageEXIF(imageFile);

				int orientation = 0;
				if (info.get("orientation") != null) {
					orientation = (int) info.get("orientation");
				}

				// Rotate image if needed
				if (orientation > 1) {
					switch (orientation) {
						case 2: // 2) transform="-flip horizontal";
							buffOriginalImage = Scalr.rotate(buffOriginalImage, Scalr.Rotation.FLIP_HORZ, NO_FILTER);
							break;
						case 3: // 3) transform="-rotate 180";
							buffOriginalImage = Scalr.rotate(buffOriginalImage, Scalr.Rotation.CW_180, NO_FILTER);
							break;
						case 4: // 4) transform="-flip vertical";
							buffOriginalImage = Scalr.rotate(buffOriginalImage, Scalr.Rotation.FLIP_VERT, NO_FILTER);
							break;
						case 5: // 5) transform="-transpose";
							// Current library not support this
							break;
						case 6: // 6) transform="-rotate 90";
							buffOriginalImage = Scalr.rotate(buffOriginalImage, Scalr.Rotation.CW_90, NO_FILTER);
							break;
						case 7: // 7) transform="-transverse";
							// Current library not support this
							break;
						case 8: // 8) transform="-rotate 270";
							buffOriginalImage = Scalr.rotate(buffOriginalImage, Scalr.Rotation.CW_270, NO_FILTER);
							break;
						default:
							break;
					}
				}

			} catch (Exception e) {
				log.error("Unable to check Image Rotated");
			}
		}
		log.warn("Rotate image in {} ms", System.currentTimeMillis() - start);
		return buffOriginalImage;
	}

	public static InputStream resizeImage(InputStream input, String ext, int width, int height) throws IOException {

		BufferedImage originalImage = ImageIO.read(input);
		BufferedImage scaledImage = Scalr.resize(originalImage, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_WIDTH, width,
		                                         height, Scalr.OP_ANTIALIAS);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(scaledImage, ext, output);
		return new ByteArrayInputStream(output.toByteArray());
	}

	/**
	 * Will resize original image to different sizes: small, medium, large.
	 */
	public static String extractImageDifferentSizes(String uploadDir, String imageFullPath, String size) {

		try {
			// Resize to large, medium, small images and upload to cdn
			double width = 0;
			double height = 0;
			File imageFile = new File(imageFullPath);
			BufferedImage buffImage = ImageIO.read(imageFile);
			String baseName = FilenameUtils.getBaseName(imageFullPath);
			String extension = FilenameUtils.getExtension(imageFullPath);
			Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
			ExifIFD0Directory exifIFD0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
			if (exifIFD0 != null) {
				int orientation = exifIFD0.getInt(ExifDirectoryBase.TAG_ORIENTATION);
				Scalr.Rotation rotation = getRotation(orientation);
				if (rotation != null) {
					buffImage = Scalr.rotate(buffImage, rotation, Scalr.OP_ANTIALIAS);
				}
			}
			// generate small size
			switch (size) {
				case MediaConst.IMAGE_SIZE_SMALL:
					if (buffImage.getWidth() >= IMAGE_SIZE_SMALL_WIDTH) {
						String smallPath = String.format("%s%s_s.%s", uploadDir, baseName,
						                                 MediaConst.EXT_JPG);

						width = IMAGE_SIZE_SMALL_WIDTH;
						height = width * buffImage.getHeight() / buffImage.getWidth();

						// Resize the buffered image
						BufferedImage resizedBufferedImage = Scalr.resize(buffImage, Scalr.Method.QUALITY, (int) width,
						                                                  (int) height, NO_FILTER);

						File smallFile = new File(smallPath);
						if (extension.equals(MediaConst.EXT_PNG)) {
							InputStream inputStream = convertPNGBufferedImageToInputStream(resizedBufferedImage);
							FileOutputStream outputStream = new FileOutputStream(smallPath);
							FileUtils.copy(inputStream, outputStream);
						} else {
							ImageIO.write(resizedBufferedImage, MediaConst.EXT_JPG, smallFile);
						}
						return smallPath;
					}
					break;
				case MediaConst.IMAGE_SIZE_MEDIUM:
					if (buffImage.getWidth() >= IMAGE_SIZE_MEDIUM_WIDTH) {
						String mediumPath = String.format("%s%s_m.%s", uploadDir, baseName,
						                                  MediaConst.EXT_JPG);

						width = IMAGE_SIZE_MEDIUM_WIDTH;
						height = width * buffImage.getHeight() / buffImage.getWidth();

						// Resize the buffered image
						BufferedImage resizedBufferedImage = Scalr.resize(buffImage, Scalr.Method.QUALITY, (int) width,
						                                                  (int) height, NO_FILTER);

						File mediumFile = new File(mediumPath);
						if (extension.equals(MediaConst.EXT_PNG)) {
							InputStream inputStream = convertPNGBufferedImageToInputStream(resizedBufferedImage);
							FileOutputStream outputStream = new FileOutputStream(mediumPath);
							FileUtils.copy(inputStream, outputStream);
						} else {
							ImageIO.write(resizedBufferedImage, MediaConst.EXT_JPG, mediumFile);
						}
						return mediumPath;
					}
					break;
				case MediaConst.IMAGE_SIZE_LARGE:
					if (buffImage.getWidth() >= IMAGE_SIZE_LARGE_WIDTH) {
						String largePath = String.format("%s%s_l.%s", uploadDir, baseName,
						                                 MediaConst.EXT_JPG);
						width = IMAGE_SIZE_LARGE_WIDTH;
						height = width * buffImage.getHeight() / buffImage.getWidth();

						// Resize the buffered image
						BufferedImage resizedBufferedImage = Scalr.resize(buffImage, Scalr.Method.QUALITY, (int) width,
						                                                  (int) height, NO_FILTER);

						File largeFile = new File(largePath);
						if (extension.equals(MediaConst.EXT_PNG)) {
							InputStream inputStream = convertPNGBufferedImageToInputStream(resizedBufferedImage);
							FileOutputStream outputStream = new FileOutputStream(largePath);
							FileUtils.copy(inputStream, outputStream);
						} else {
							ImageIO.write(resizedBufferedImage, MediaConst.EXT_JPG, largeFile);
						}
						return largePath;
					}
					break;

				case MediaConst.IMAGE_SIZE_XLARGE:
					if (buffImage.getWidth() >= IMAGE_SIZE_XLARGE_WIDTH) {
						String xlargePath = String.format("%s%s_xl.%s", uploadDir,
						                                  baseName,
						                                  MediaConst.EXT_JPG);
						width = IMAGE_SIZE_XLARGE_WIDTH;
						height = width * buffImage.getHeight() / buffImage.getWidth();

						// Resize the buffered image
						BufferedImage resizedBufferedImage = Scalr.resize(buffImage, Scalr.Method.QUALITY, (int) width,
						                                                  (int) height, NO_FILTER);

						File xlargeFile = new File(xlargePath);
						if (extension.equals(MediaConst.EXT_PNG)) {
							InputStream inputStream = convertPNGBufferedImageToInputStream(resizedBufferedImage);
							FileOutputStream outputStream = new FileOutputStream(xlargePath);
							FileUtils.copy(inputStream, outputStream);
						} else {
							ImageIO.write(resizedBufferedImage, MediaConst.EXT_JPG, xlargeFile);
						}
						return xlargePath;
					}
					break;

				case MediaConst.IMAGE_SIZE_XXLARGE:
					if (buffImage.getWidth() >= IMAGE_SIZE_XXLARGE_WIDTH) {
						String xxlargePath = String.format("%s%s_xxl.%s", uploadDir,
						                                   baseName,
						                                   MediaConst.EXT_JPG);
						width = IMAGE_SIZE_XXLARGE_WIDTH;
						height = width * buffImage.getHeight() / buffImage.getWidth();

						// Resize the buffered image
						BufferedImage resizedBufferedImage = Scalr.resize(buffImage, Scalr.Method.QUALITY, (int) width,
						                                                  (int) height, NO_FILTER);

						File xxlargeFile = new File(xxlargePath);
						if (extension.equals(MediaConst.EXT_PNG)) {
							InputStream inputStream = convertPNGBufferedImageToInputStream(resizedBufferedImage);
							FileOutputStream outputStream = new FileOutputStream(xxlargePath);
							FileUtils.copy(inputStream, outputStream);
						} else {
							ImageIO.write(resizedBufferedImage, MediaConst.EXT_JPG, xxlargeFile);
						}
						return xxlargePath;
					}
					break;

				default:
					break;
			}

			buffImage.getGraphics().dispose();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * Read image info from EXIF in header
	 */
	private static Map<Object, Object> readImageEXIF(File imageFile) {

		Map<Object, Object> map = new HashMap<>();

		Metadata metadata;
		try {
			// read all meta data of image
			metadata = ImageMetadataReader.readMetadata(imageFile);
		} catch (Exception e) {
			return Collections.emptyMap();
		}

		if (metadata != null) {
			int orientation = 0;
			try {
				// use this directory to read EXIF
				Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
				orientation = exifDirectory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
			} catch (Exception e) {
				orientation = 1;
			}

			int width;
			int height;
			try {
				// use this directory to read image size
				JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
				width = jpegDirectory.getImageWidth();
				height = jpegDirectory.getImageHeight();
			} catch (Exception e) {
				width = 0;
				height = 0;
			}

			map.put("orientation", orientation);
			map.put("width", width);
			map.put("height", height);
		}

		return map;
	}

	public static Scalr.Rotation getRotation(int orientation) {
		return switch (orientation) {
			case 6 -> Scalr.Rotation.CW_90;
			case 3 -> Scalr.Rotation.CW_180;
			case 8 -> Scalr.Rotation.CW_270;
			default -> null;
		};
	}

	public static InputStream convertPNGBufferedImageToInputStream(BufferedImage image) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, MediaConst.EXT_PNG, outputStream);
		byte[] imageBytes = outputStream.toByteArray();
		return new ByteArrayInputStream(imageBytes);
	}

	public static String resizeGifImage(String imageFullPath, String size, String rootUploadDirectory,
	                                    String ffmpegPath) throws IOException {
		log.info("Start resize GIF image {}", size);
		double width = 0;
		double height = 0;
		String outputPath = null;
		File imageFile = new File(imageFullPath);
		String baseName = FilenameUtils.getBaseName(imageFullPath);
		BufferedImage buffImage = ImageIO.read(imageFile);
		if (buffImage == null) {
			return null;
		}

		switch (size) {
			case MediaConst.IMAGE_SIZE_SMALL:
				width = ImageUtils.IMAGE_SIZE_SMALL_WIDTH;
				height = width * buffImage.getHeight() / buffImage.getWidth();
				outputPath = String.format("%s%s_s.%s", rootUploadDirectory, baseName, MediaConst.EXT_GIF);
				break;
			case MediaConst.IMAGE_SIZE_MEDIUM:
				width = ImageUtils.IMAGE_SIZE_MEDIUM_WIDTH;
				height = width * buffImage.getHeight() / buffImage.getWidth();
				outputPath = String.format("%s%s_m.%s", rootUploadDirectory, baseName, MediaConst.EXT_GIF);
				break;
			case MediaConst.IMAGE_SIZE_LARGE:
				width = ImageUtils.IMAGE_SIZE_LARGE_WIDTH;
				height = width * buffImage.getHeight() / buffImage.getWidth();
				outputPath = String.format("%s%s_l.%s", rootUploadDirectory, baseName, MediaConst.EXT_GIF);
				break;
			default:
				break;
		}

		if (outputPath != null) {
			String scale = String.format("scale=%f:%f", width, height);
			String[] args = new String[]{ffmpegPath, "-i", imageFullPath, "-vf", scale, outputPath};
			try {
				int convertTimeout = 1; // 1 minute
				Process p = Runtime.getRuntime().exec(args);
				boolean status = p.waitFor(convertTimeout, TimeUnit.MINUTES);
				if (status && new File(outputPath).exists()) {
					log.info("Resize GIF image successfully");
					return outputPath;
				}
			} catch (InterruptedException e) {
				log.error("convert to gif error: {}", e.getMessage());
				Thread.currentThread().interrupt();
			}
		}

		return outputPath;
	}

	public static String convertHeicImage(String imageFullPath, String rootUploadDirectory, String heifConvertPath,
	                                      Consumer<String> removeFileOnServer) {
		log.info("Start convert HEIC image {} to JPG", imageFullPath);
		String baseName = FilenameUtils.getBaseName(imageFullPath);
		String outputPath = String.format("%s%s.%s", rootUploadDirectory, baseName, MediaConst.EXT_JPG);

		String[] args = new String[]{heifConvertPath, imageFullPath, outputPath};
		try {
			int convertTimeout = 1; // 1 minute
			Process p = Runtime.getRuntime().exec(args);
			boolean status = p.waitFor(convertTimeout, TimeUnit.MINUTES);
			if (status && new File(outputPath).exists()) {
				log.info("Convert HEIC image successfully");
				// remove file heic
				removeFileOnServer.accept(imageFullPath);
				return outputPath;
			}
		} catch (InterruptedException e) {
			log.error("convert to HEIC error: {}", e.getMessage());
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			log.error("convert to HEIC error: {}", e.getMessage(), e);
		}
		return null;
	}
}
