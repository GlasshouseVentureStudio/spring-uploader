package io.fruitful.spring.uploader.service;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class PartitionFilesFilter implements FilenameFilter {
	private final String filename;

	public PartitionFilesFilter(String filename) {
		this.filename = filename;
	}

	@Override
	public boolean accept(File file, String s) {
		return s.matches(Pattern.quote(filename) + "_\\d+");
	}
}
