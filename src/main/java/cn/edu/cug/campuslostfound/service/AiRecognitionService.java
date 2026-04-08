package cn.edu.cug.campuslostfound.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

@Service
public class AiRecognitionService {

    // ✅ 完全匹配你截图里的信息，直接用！
    private static final String API_KEY = "af19bd8e-2679-4cef-99d2-074009290b6b";
    // ✅ 正确的接口地址：必须用你的接入点ID
    private static final String API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    // ✅ 正确的模型ID：就是你的接入点ID！
    private static final String MODEL_ID = "ep-20260408200220-ggjsc";

    public String analyzeImageTags(String localImagePath) {
        try {
            // 1. 读取图片 → 转Base64（带完整前缀，服务器才能识别）
            File file = new File(localImagePath);
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(fileContent);

            // 2. 创建HTTP连接，设置超时（避免断连）
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            // ✅ 正确的认证头：Bearer + 你的API Key（Bearer后必须有空格！）
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(20000);  // 20秒连接超时
            conn.setReadTimeout(30000);     // 30秒读取超时
            conn.setDoOutput(true);

            // 3. 构造完全符合豆包视觉模型要求的请求体
            String requestBody = """
            {
                "model": "%s",
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "请识别图片中的物品，返回3-5个中文标签，用逗号分隔，不要多余内容"
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": "%s"
                                }
                            }
                        ]
                    }
                ]
            }
            """.formatted(MODEL_ID, base64Image);

            // 4. 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes("UTF-8"));
                os.flush();
            }

            // 5. 处理响应（区分成功/错误流，避免空指针）
            int status = conn.getResponseCode();
            System.out.println("HTTP状态码：" + status);

            BufferedReader reader;
            if (status >= 200 && status < 300) {
                // 成功：读取正常响应
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                // 失败：读取错误流，打印详细报错
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                StringBuilder errorMsg = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorMsg.append(line);
                }
                System.out.println("API错误详情：" + errorMsg);
                return "【AI识别失败：" + errorMsg + "】";
            }

            // 6. 读取完整响应
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            // 7. 解析标签
            return parseTags(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return "【AI识别失败：" + e.getMessage() + "】";
        }
    }

    /**
     * 解析豆包API返回的JSON，提取标签
     * 适配豆包视觉模型的标准返回格式：{"choices":[{"message":{"content":"标签1,标签2"}}]}
     */
    private String parseTags(String json) {
        try {
            // 精准定位content字段
            int contentIdx = json.indexOf("\"content\":\"");
            if (contentIdx == -1) {
                System.out.println("JSON解析失败，未找到content字段，完整响应：" + json);
                return "解析失败";
            }
            int start = contentIdx + 11; // 跳过"content":"
            int end = json.indexOf("\"", start);
            if (end == -1) {
                return "解析失败";
            }
            String result = json.substring(start, end);
            return "【AI识别标签：" + result + "】";
        } catch (Exception e) {
            e.printStackTrace();
            return "【解析失败：" + e.getMessage() + "】";
        }
    }
}