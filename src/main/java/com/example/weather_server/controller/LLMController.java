package com.example.weather_server.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.weather_server.service.TimetableConvertService;

@RestController
public class LLMController {

    private final TimetableConvertService convertService;

    public LLMController(TimetableConvertService convertService) {
        this.convertService = convertService;
    }

    /** 上传 xlsx，返回可直接导入的课表文本（需登录：拦截器统一保护） */
    @PostMapping("/timetable/convert")
    public Map<String, Object> convertTimetable(@RequestParam("file") MultipartFile file) {
        String text = convertService.convert(file);
        return Map.of("code", 0, "text", text);
    }
}
