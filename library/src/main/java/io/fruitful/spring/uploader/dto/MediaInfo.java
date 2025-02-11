package io.fruitful.spring.uploader.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class MediaInfo {

	private String originalFilename;
	private String filename;
	private String contentType;
	private String url;
	private Boolean processing;
	private String guid;
	private Integer width;
	private Integer height;
	private MediaThumbnailInfo thumbnail;
	
	private Boolean external;
	private Long duration;
	private Date uploadedDate;
	private String cdnOrigin;
	private String cdnXLarge;
	private String cdnXXLarge;
	private String cdnLarge;
	private String cdnMedium;
	private String cdnSmall;
	private Integer type;
	private String durationText;
	private Date cdnTime;
	private String externalLink;
}
