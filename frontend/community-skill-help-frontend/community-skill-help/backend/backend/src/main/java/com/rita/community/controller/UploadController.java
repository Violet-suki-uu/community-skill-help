package com.rita.community.controller;

import com.rita.community.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    // 上传图片（返回可访问的 url）
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return Result.fail("文件为空");
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }

        String filename = UUID.randomUUID() + ext;

        // 保存到项目根目录的 uploads/
        File dir = new File("uploads");
        if (!dir.exists()) dir.mkdirs();

        File dest = new File(dir, filename);
        file.transferTo(dest);

        // 对外访问路径：/uploads/xxx
        return Result.ok("http://localhost:8080/uploads/" + filename);
    }
}
