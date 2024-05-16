package com.example.demo;

import feign.Feign;
import feign.RequestLine;
import feign.Response;
import feign.Target;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(DemoApplication.class, args);

        var feign = Feign.builder().client(new Http2Client()).build();
        var testApi = feign.newInstance(new Target.HardCodedTarget<>(TestApi.class, "http://localhost:8080"));
        try (Response response = testApi.test()) {
            var reader = response.body().asReader(StandardCharsets.UTF_8);
            new BufferedReader(reader)
                    .lines()
                    .forEach(line -> {
                        // Real time processing ...
                        System.out.println("get line content: " + line);
                        // send remote data
                    });
        }
    }

    public interface TestApi {
        @RequestLine("GET /test")
        Response test();
    }

    @RestController
    public static class TestController {
        @GetMapping("/test")
        public ResponseBodyEmitter test() {
            var emitter = new ResponseBodyEmitter(Long.MAX_VALUE);

            new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    try {
                        Thread.sleep(3_000);
                    } catch (InterruptedException ignored) {
                    }

                    try {
                        emitter.send(UUID.randomUUID() + System.lineSeparator(), MediaType.TEXT_EVENT_STREAM);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

            return emitter;
        }
    }
}
