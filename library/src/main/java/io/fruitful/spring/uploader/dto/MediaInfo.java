package io.fruitful.spring.uploader.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class MediaInfo {

	private String id;
	private String originalFilename;
	private String filename;
	private String thumbnailId;
	private String contentType;
	private String url;
	private Boolean external;
	private Boolean processing;
	private Long duration;
	private Date uploadedDate;
	private String cdnOrigin;
	private String cdnXLarge;
	private String cdnXXLarge;
	private String cdnLarge;
	private String cdnMedium;
	private String cdnSmall;
	private String guid;
	private String userId;
	private Integer type;
	private String durationText;
	private Date cdnTime;
	private Integer width;
	private Integer height;
	private String externalLink;
	private MediaInfo thumbnail;
}
