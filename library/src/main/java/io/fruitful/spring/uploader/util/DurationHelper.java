package io.fruitful.spring.uploader.util;

import io.fruitful.spring.uploader.constant.StringConst;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Duration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DurationHelper {

	public static final String DURATION_FORMAT_LT_HOUR = "mm:ss";
	public static final String DURATION_FORMAT_GT_HOUR = "HH:mm:ss";

	public static long convertFromText(String durationText) {
		long duration = 0;
		String[] splitTimes = durationText.split(StringConst.TWO_DOT);
		if (splitTimes.length > 0) {
			int hour = NumberHelper.parseInt(splitTimes[0]);
			duration += hour * DateTimeHelper.ONE_HOUR_SECOND;
		}

		if (splitTimes.length > 1) {
			int min = NumberHelper.parseInt(splitTimes[1]);
			duration += min * DateTimeHelper.ONE_MIN_SECOND;
		}

		if (splitTimes.length > 2) {

			duration += NumberHelper.parseDouble(splitTimes[2]).longValue();
		}

		return duration;
	}


	public static long parseDuration(String durationText) {
		Duration duration = Duration.parse(durationText);
		return duration.getSeconds();
	}

	public static String formatDuration(long durationInSeconds) {
		String format = DURATION_FORMAT_GT_HOUR;
		if (durationInSeconds < DateTimeHelper.ONE_HOUR_SECOND) {
			format = DURATION_FORMAT_LT_HOUR;
		}
		Duration duration = Duration.ofSeconds(durationInSeconds);
		return DurationFormatUtils.formatDuration(duration.toMillis(), format);
	}
}
