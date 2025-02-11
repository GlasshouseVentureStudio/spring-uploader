package io.fruitful.spring.uploader.enumeration;

import io.fruitful.spring.uploader.util.StringHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum FileSupportEnum {

	IMAGE("image/jpg, image/jpeg, image/png, image/gif, image/bmp, image/svg+xml, image/tiff, image/vnd.ms-photo, image/webp, image/x-ms-bmp"),
	READABLE_IMAGE("image/jpg, image/jpeg, image/png, image/bmp, image/wbmp"),
	GIF_IMAGE("image/gif"),
	HEIC_IMAGE("image/heic"),
	AUDIO("audio/mp3, audio/mpeg, audio/m4a, audio/mp4, audio/x-m4a, audio/wav, audio/x-wav, audio/x-ms-wma, video/x-ms-asf"),
	VIDEO("video/mp4, video/mov, video/quicktime, video/m4v, video/x-m4v, video/x-ms-wmv"),
	FILE("");

	private final String types;

	public static String getFileType(String contentType) {
		if (contentType != null) {
			for (FileSupportEnum item : FileSupportEnum.values()) {
				if (item.getTypes().toLowerCase().contains(contentType.toLowerCase())) {
					return item.name();
				}
			}
		}
		return null;
	}

	public static FileSupportEnum getFileSupport(String contentType) {
		return StringHelper.hasText(contentType) ?
		       Arrays.stream(FileSupportEnum.values())
				       .filter(item -> item.getTypes().toLowerCase().contains(contentType.toLowerCase()))
				       .findFirst()
				       .orElse(null) :
		       null;
	}
}
