package io.fruitful.spring.uploader.util;

import io.fruitful.spring.uploader.constant.StringConst;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class StringHelper {

	public static final int INDEX_NOT_FOUND = -1;

	public static String generateUniqueString() {
		return UUID.randomUUID().toString().replace(StringConst.HYPHEN, StringConst.EMPTY);
	}

	public static String extract(String source, String regex) {
		String result = null;
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(source);
		if (m.find()) {
			result = m.group(1);
			result = result.replace("\u00a0", " ").trim();
		}
		return result;
	}

	public static boolean contains(String str, String regex) {
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(str);
		return m.find();
	}

	public static String quote(String string) {
		return "\"" + string + "\"";
	}

	public static String trimToEmpty(final String str) {
		return str == null ? StringConst.EMPTY : str.trim();
	}

	public static String substringAfter(final String str, final String separator) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		if (separator == null) {
			return StringConst.EMPTY;
		}
		final int pos = str.indexOf(separator);
		if (pos == INDEX_NOT_FOUND) {
			return StringConst.EMPTY;
		}
		return str.substring(pos + separator.length());
	}

	/**
	 * \p{M} or \p{Mark}: a character intended to be combined with another character (e.g. accents, umlauts, enclosing
	 * boxes, etc.).
	 * <a href="https://www.regular-expressions.info/unicode.html">...</a>
	 */
	public static String stripAccents(String input) {
		if (input == null || input.isEmpty()) {
			return null;
		}

		return Normalizer.normalize(input, Normalizer.Form.NFKD).replaceAll("\\p{M}", StringConst.EMPTY);
	}

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	// create function auto gen string and number with length
	public static String generateRandomString(int length) {
		if (length <= 0) {
			throw new IllegalArgumentException("Length must be greater than zero");
		}

		StringBuilder randomString = new StringBuilder(length);
		SecureRandom random = new SecureRandom();

		for (int i = 0; i < length; i++) {
			int randomIndex = random.nextInt(CHARACTERS.length());
			char randomChar = CHARACTERS.charAt(randomIndex);
			randomString.append(randomChar);
		}
		return randomString.toString();
	}

	public static Integer countTargetCharacter(String text, String target) {
		return text.split(target, -1).length - 1;
	}

	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty() || !containsText(str);
	}

	public static boolean hasText(String str) {
		return str != null && !str.isEmpty() && containsText(str);
	}

	private static boolean containsText(CharSequence str) {
		int strLen = str.length();

		for (int i = 0; i < strLen; ++i) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasLength(String str) {
		return str != null && !str.isEmpty();
	}
}
