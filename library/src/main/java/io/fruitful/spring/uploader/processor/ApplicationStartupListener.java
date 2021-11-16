package io.fruitful.spring.uploader.processor;

import io.fruitful.spring.uploader.annotation.EnableUploader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Optional;

public class ApplicationStartupListener implements
		ApplicationListener<ContextRefreshedEvent> {

	private ApplicationContext context;
	private EnableUploaderAnnotationProcessor processor;

	public ApplicationStartupListener(ApplicationContext context,
									  EnableUploaderAnnotationProcessor processor) {
		this.context = context;
		this.processor = processor;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
		Optional<EnableUploader> annotation =
				context.getBeansWithAnnotation(EnableUploader.class).keySet().stream()
						.map(key -> context.findAnnotationOnBean(key, EnableUploader.class))
						.findFirst();
		annotation.ifPresent(enableUploader -> processor.process(enableUploader));
	}
}
