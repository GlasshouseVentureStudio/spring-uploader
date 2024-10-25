package io.fruitful.spring.uploader.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NumberHelper {

	public static Integer parseInt(String text) {
		try {
			return Integer.valueOf(text);
		} catch (Exception e) {
			return 0;
		}
	}

	public static Double parseDouble(String text) {
		if (StringHelper.isEmpty(text)) {
			return 0D;
		}
		try {
			return Double.valueOf(text);
		} catch (Exception e) {
			return 0D;
		}
	}

	public static Double parseDouble(Object obj) {
		if (obj == null) {
			return 0D;
		}
		try {
			return (Double) obj;
		} catch (Exception ignored) {
			return 0D;
		}
	}
}
