package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.service.AiRecognitionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@CrossOrigin
public class FileUploadController {

    private final AiRecognitionService aiService;

    public FileUploadController(AiRecognitionService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        if (file.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择要上传的图片");
            return result;
        }

        try {
            // 1. 生成唯一的文件名，防止多人上传同名图片被覆盖
            String originalName = file.getOriginalFilename();
            String suffix = originalName.substring(originalName.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + suffix; // 例如: 123e4567-e89b...jpg

            // 2. 将文件保存到硬盘的 uploads 文件夹
            String savePath = System.getProperty("user.dir") + "/uploads/" + newFileName;
            file.transferTo(new File(savePath));

            // 3. 生成可以通过网络访问的 URL
            // 假设你的后端运行在 localhost:8080
            String imageUrl = "http://localhost:8080/uploads/" + newFileName;

            // 4. 调用 AI 服务提取图片标签
            String aiTags = "";
            try {
                // 确保你传入的是绝对路径 savePath
                aiTags = aiService.analyzeImageTags(savePath);
            } catch (Exception e) {
                // 即使 AI 识别报错或超时，也不要中断整个流程
                System.err.println("AI识别失败，跳过该步骤：" + e.getMessage());
            }

            // 5. 将结果打包返回给前端
            result.put("success", true);
            result.put("imageUrl", imageUrl);
            result.put("aiTags", aiTags);

            return result;

        } catch (IOException e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "文件上传失败：" + e.getMessage());
            return result;
        }
    }
}
