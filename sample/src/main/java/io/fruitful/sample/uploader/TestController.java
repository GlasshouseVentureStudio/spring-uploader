package io.fruitful.sample.uploader;


import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "test")
public class TestController {

	@GetMapping
	public ResponseEntity<String> test() {
//		uploadService.upload();
		return ResponseEntity.ok("Hello world!!");
	}

}
