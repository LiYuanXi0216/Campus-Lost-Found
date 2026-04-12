package cn.edu.cug.campuslostfound.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@Service
public class AiRecognitionService {
    private static final String API_KEY = "af19bd8e-2679-4cef-99d2-074009290b6b";
    private static final String API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    private static final String MODEL_ID = "ep-20260408200220-ggjsc";

    public String analyzeImageTags(String localImagePath) {
        try {
            // 1. 读取本地图片并转 Base64
            File file = new File(localImagePath);
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(fileContent);

            // 2. 配置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(API_KEY); // Spring会自动帮你加上 "Bearer " 和空格

            // 3. 使用 Map 构建 JSON 体 (彻底告别手拼字符串的转义地狱！)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL_ID);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");

            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", "请识别图片中的物品，返回3-5个中文标签，用逗号分隔，不要多余内容");

            Map<String, Object> imageUrlMap = new HashMap<>();
            imageUrlMap.put("url", base64Image);
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            imageContent.put("image_url", imageUrlMap);

            message.put("content", Arrays.asList(textContent, imageContent));
            requestBody.put("messages", Collections.singletonList(message));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 4. 发送网络请求 (使用 RestTemplate)
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

            // 5. 使用 Jackson 优雅解析 JSON 返回值
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            // 精准提取 choices[0].message.content
            String tags = root.path("choices").get(0).path("message").path("content").asText();

            return "【AI识别标签：" + tags.trim() + "】";

        } catch (Exception e) {
            System.err.println("大模型调用失败：" + e.getMessage());
            return "【AI识别暂时不可用】";
        }
    }
}