package io.fruitful.spring.uploader;

import io.fruitful.spring.uploader.controller.UploadServlet;
import io.fruitful.spring.uploader.processor.ApplicationStartupListener;
import io.fruitful.spring.uploader.processor.EnableUploaderAnnotationProcessor;
import io.fruitful.spring.uploader.service.UploadService;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("io.fruitful.spring.uploader")
public class UploaderAutoConfiguration {
	@Bean
	public ServletRegistrationBean servletBean() {
		ServletRegistrationBean bean = new ServletRegistrationBean(
				new UploadServlet(), "/upload");
		bean.setLoadOnStartup(1);
		return bean;
	}

	@Bean
	UploadService uploadService() {
		return new UploadService();
	}

	@Bean
	ApplicationStartupListener listener(ApplicationContext context) {
		return new ApplicationStartupListener(context, uploadAnnotationProcessor());
	}

	@Bean
	EnableUploaderAnnotationProcessor uploadAnnotationProcessor() {
		return new EnableUploaderAnnotationProcessor(uploadService());
	}
}
