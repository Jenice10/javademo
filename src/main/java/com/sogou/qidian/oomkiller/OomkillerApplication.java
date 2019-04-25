package com.sogou.qidian.oomkiller;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class OomkillerApplication {

  public static void main(String[] args) {
    SpringApplication.run(OomkillerApplication.class, args);
  }

  @RestController
  static class TestController {

    List<Byte[]> cache = new LinkedList<>();

    @RequestMapping(value = "hi")
    public String hi() {
      for (int i = 0; i < 100000000; i++) {
        cache.add(new Byte[100000000]);
      }
//      for (int i = 0; i < 10; i++) {
//        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
//        cache.add(buffer);
//      }
      try {
        TimeUnit.SECONDS.sleep(30);
      } catch (InterruptedException e) {
      }
      return "hi, 😀";
    }


    @RequestMapping(value = "hello")
    public String hello() {
     return "hello!";
    }
  }
}
