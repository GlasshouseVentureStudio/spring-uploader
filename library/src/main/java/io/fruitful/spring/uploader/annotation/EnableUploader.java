package io.fruitful.spring.uploader.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableUploader {
	long maxFileSize() default 2L*1024*1024*1024; // 2G file size maximum default set
	String temporaryUploadFolder() default "tmp_upload"; // relative folder from executing file directory
}
