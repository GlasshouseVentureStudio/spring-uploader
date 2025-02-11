package io.fruitful.spring.uploader.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateTimeHelper {

	private static final String YEAR = "year";
	public static final String MONTH = "month";
	public static final String DAY_OF_YEAR = "dayOfYear";
	public static final String WEEK = "week";

	public static final String HOUR = "hour";
	public static final String MINUTE = "minute";
	public static final String SECOND = "second";

	public static final String TZ_AU = "Australia/Sydney";
	public static final String TZ_UTC = "UTC";

	public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_TIME_FORMAT_IN_CSV = "dd/MM/yyyy h:m:s a";
	public static final String DATE_PATTERN = "yyyy-MM-dd";
	public static final String STAST_PERFORM_DATE_FORMAT = "yyyy-MM-dd'Z'";
	public static final String STAST_PERFORM_DATE_TIME_FORMAT = "yyyy-MM-dd'Z'hh:mm:ss'Z'";

	public static final String MONGO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final long ONE_DAY_MILLI = 24 * 60 * 60 * 1000L;
	public static final long ONE_HOUR_SECOND = (long) 60 * 60;
	public static final long ONE_MIN_SECOND = 60;
	public static final String ONLY_MONTH_YEAR_DATE_FORMAT = "yyyyMM";

	public static Date getCurrentDate() {
		return new Date();
	}

	public static long daysBetweenTwoDates(Date dateFrom, Date dateTo) {
		return ChronoUnit.DAYS.between(Instant.ofEpochMilli(dateFrom.getTime()),
		                               Instant.ofEpochMilli(dateTo.getTime()));
	}

	public static Date plus(Date date, long duration, TimeUnit unit) {
		if (date == null) {
			return null;
		}

		Instant instant = date.toInstant();
		return Date.from(instant.plusMillis(unit.toMillis(duration)));
	}

	public static Date addTime(Date targetDate, String typeOfTime, int value) {

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(targetDate);

		if (YEAR.equals(typeOfTime)) {
			calendar.add(Calendar.YEAR, value);
		} else if (MONTH.equals(typeOfTime)) {
			calendar.add(Calendar.MONTH, value);
		} else if (DAY_OF_YEAR.equals(typeOfTime)) {
			calendar.add(Calendar.DAY_OF_YEAR, value);
		} else if (WEEK.equals(typeOfTime)) {
			calendar.add(Calendar.WEEK_OF_YEAR, value);
		} else if (HOUR.equals(typeOfTime)) {
			calendar.add(Calendar.HOUR, value);
		} else if (MINUTE.equals(typeOfTime)) {
			calendar.add(Calendar.MINUTE, value);
		} else if (SECOND.equals(typeOfTime)) {
			calendar.add(Calendar.SECOND, value);
		}

		return calendar.getTime();
	}
}
