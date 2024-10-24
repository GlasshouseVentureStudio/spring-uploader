package io.fruitful.spring.uploader.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MediaConst {
	public static final List<String> EXT_STATIC_IMAGE = new ArrayList<>();
	public static final List<String> EXT_AUDIO = new ArrayList<>();
	public static final List<String> EXT_VIDEO = new ArrayList<>();

	public static final String TAG_UUID = "guid";

	// Content type
	public static final String JPEG_MINE_TYPE = "image/jpeg";
	public static final String MP4_MINE_TYPE = "video/mp4";
	public static final String MP3_MINE_TYPE = "audio/mpeg";
	public static final String HEIC_MINE_TYPE = "heic";
	public static final String EXT_JPG = "jpg";
	public static final String EXT_JPEG = "jpeg";
	public static final String EXT_PJPEG = "pjpeg"; // progressive image
	public static final String EXT_PNG = "png";
	public static final String EXT_GIF = "gif";
	public static final String EXT_IMG = "img";
	public static final String EXT_JFIF = "jfif";
	public static final String EXT_DMS = "dms";
	public static final String EXT_MP4 = "mp4";
	public static final String EXT_MOV = "mov";
	public static final String EXT_M4V = "m4v";

	public static final String EXT_HEVC = "hevc";
	public static final String EXT_MP3 = "mp3";
	public static final String EXT_WAV = "wav";
	public static final String EXT_M4A = "m4a";
	public static final String EXT_WEBP = "webp";
	public static final String EXT_CSV = "csv";
	public static final String EXT_WMA = "wma";
	public static final String EXT_PDF = "pdf";


	// Default cache is 1 week = 604800 seconds
	public static final int IMAGE_CACHE_PERIOD = 604800;
	public static final String HEADER_IMAGE_CACHE = String.format("max-age=%d, public", IMAGE_CACHE_PERIOD);

	public static final String IMAGE_SIZE_LARGE = "l";
	public static final String IMAGE_SIZE_MEDIUM = "m";
	public static final String IMAGE_SIZE_SMALL = "s";


	// will resize image width to this value if too large
	public static final int RESIZE_IMAGE_WIDTH = 2048;
	public static final int THUMBNAIL_WIDTH = 500;
	public static final String IMAGE_SIZE_XLARGE = "xl";
	public static final String IMAGE_SIZE_XXLARGE = "xxl";
	public static final double IMAGE_SIZE_LARGE_WIDTH = 1200;
	public static final double IMAGE_SIZE_MEDIUM_WIDTH = 640;
	public static final double IMAGE_SIZE_SMALL_WIDTH = 250;
	public static final double IMAGE_SIZE_XLARGE_WIDTH = 1440;
	public static final double IMAGE_SIZE_XXLARGE_WIDTH = 1920;
	public static final int MAX_STATIC_IMAGE_FILE_SIZE = 3000000;

	static {
		EXT_STATIC_IMAGE.add(MediaConst.EXT_JPG);
		EXT_STATIC_IMAGE.add(MediaConst.EXT_JPEG);
		EXT_STATIC_IMAGE.add(MediaConst.EXT_PJPEG);
		EXT_STATIC_IMAGE.add(MediaConst.EXT_PNG);
		EXT_STATIC_IMAGE.add(MediaConst.EXT_JFIF);
		EXT_STATIC_IMAGE.add(MediaConst.EXT_DMS);
		EXT_STATIC_IMAGE.add(MediaConst.EXT_IMG);
		EXT_AUDIO.add(MediaConst.EXT_MP3);
		EXT_AUDIO.add(MediaConst.EXT_WAV);
		EXT_AUDIO.add(MediaConst.EXT_M4A);
		EXT_AUDIO.add(MediaConst.EXT_WMA);
		EXT_VIDEO.add(MediaConst.EXT_MP4);
		EXT_VIDEO.add(MediaConst.EXT_MOV);
		EXT_VIDEO.add(MediaConst.EXT_M4V);
		EXT_VIDEO.add(MediaConst.EXT_HEVC);
	}
}
