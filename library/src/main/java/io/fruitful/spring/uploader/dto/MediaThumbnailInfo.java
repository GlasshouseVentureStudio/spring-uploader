package io.fruitful.spring.uploader.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MediaThumbnailInfo {

	private String contentType;
	private String originalFilename;
	private String filename;
	private Integer width;
	private Integer height;
	private String guid;
}
