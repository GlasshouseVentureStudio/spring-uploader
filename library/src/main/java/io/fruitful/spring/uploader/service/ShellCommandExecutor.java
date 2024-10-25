package io.fruitful.spring.uploader.service;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ShellCommandExecutor {

	private final ProcessBuilder builder;

	public ShellCommandExecutor(String app, String... arguments) {
		List<String> commands = new ArrayList<>();
		commands.add(app);
		if (arguments != null && arguments.length > 0) {
			commands.addAll(Arrays.asList(arguments));
		}
		builder = new ProcessBuilder(commands);
	}

	public String execute() {
		return this.execute(MediaProcessor.CONVERSION_TIMEOUT, TimeUnit.MINUTES);
	}

	public String execute(long timeout) {
		return this.execute(timeout, TimeUnit.MINUTES);
	}

	public String execute(long timeout, TimeUnit timeunit) {
		StringBuilder output = new StringBuilder();
		Process process = null;

		try {
			log.info("Prepare to execute command: {}", String.join(" ", builder.command()));
			process = builder.start();

			boolean status = process.waitFor(timeout, timeunit);
			if (status) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line);
				}
			}
		} catch (IOException | InterruptedException e) {
			log.error(e.getMessage(), e);
			Thread.currentThread().interrupt();
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return output.toString();
	}
}