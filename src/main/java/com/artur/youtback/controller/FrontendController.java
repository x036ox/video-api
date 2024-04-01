package com.artur.youtback.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller("/controller")
public class FrontendController {
    @GetMapping(path = "/{path:^(?!.*\\..*$).*}")
    public String index(){
        return "index";
    }

}
