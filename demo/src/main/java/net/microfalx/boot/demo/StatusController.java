package net.microfalx.boot.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/status")
public class StatusController {

    @GetMapping("")
    @ResponseBody
    public String get() {
        return "Ready!";
    }
}
