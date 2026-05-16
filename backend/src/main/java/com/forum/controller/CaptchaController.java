package com.forum.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.forum.dto.Result;
import com.forum.service.CaptchaStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class CaptchaController {

    private final CaptchaStore captchaStore;

    public CaptchaController(CaptchaStore captchaStore) {
        this.captchaStore = captchaStore;
    }

    @GetMapping("/api/captcha")
    public Result<?> getCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 20);
        String code = captcha.getCode();
        String id = UUID.randomUUID().toString();
        captchaStore.put(id, code);
        return Result.success(Map.of(
            "captchaId", id,
            "imageBase64", captcha.getImageBase64Data()
        ));
    }

}
