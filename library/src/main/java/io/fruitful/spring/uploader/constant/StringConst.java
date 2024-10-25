package io.fruitful.spring.uploader.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringConst {
	public static final String EMPTY = "";
	public static final String SPACE = " ";
	public static final String HYPHEN = "-";
	public static final String HYPHEN_SPACE = " - ";
	public static final String COMMA = ",";
	public static final String COMMA_SPACE = ", ";
	public static final String SEMICOLON = ";";
	public static final String DOT = ".";
	public static final String UNIX_LINE_BREAK = "\n";
	public static final String UNDERSCORE = "_";
	public static final String FORWARD_SLASH = "/";
	public static final String BAR_SPACE = " | ";
	public static final String TWO_DOT = ":";
	public static final String S = "s";
	public static final String ESCAPED_QUOTE = "\"";
	public static final String DOLLAR = "$";
	public static final String NOT_AVAILABLE = "N/A";
	public static final String NON_BREAKING_SPACE = "\u00a0";
	public static final String LEFT_CURLY_BRACKET = "{";
	public static final String RIGHT_CURLY_BRACKET = "}";
	public static final String EQUAL_SIGN = "=";
	public static final String LEFT_BRACKET = "(";
	public static final String RIGHT_BRACKET = ")";
	public static final String AT = "@";

	public static <T> String retrieveWithDefaultEmpty(T value, Function<T, String> formatter) {
		return value != null ? formatter.apply(value) : EMPTY;
	}
}
