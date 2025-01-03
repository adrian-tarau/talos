package net.microfalx.boot.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/")
public class HomeController {

    @GetMapping("")
    @ResponseBody
    public String get() {
        return "Hello! I'm a simple demo application based on Spring Boot.";
    }
}
