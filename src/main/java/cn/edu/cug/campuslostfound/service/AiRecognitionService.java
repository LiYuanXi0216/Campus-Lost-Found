package cn.edu.cug.campuslostfound.service;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class AiRecognitionService {

    // 这是一个模拟的方法。真实场景下，这里会通过网络请求把图片发给百度/阿里云
    public String analyzeImageTags(String originalFilename) {
        // 简单模拟：随机返回几个标签
        String[] mockTags = {
                "黑色", "电子产品", "水杯", "书本", "卡片", "皮质", "塑料"
        };

        Random random = new Random();
        // 随机挑两个标签拼起来，中间加个空格
        String tag1 = mockTags[random.nextInt(mockTags.length)];
        String tag2 = mockTags[random.nextInt(mockTags.length)];

        return "【AI识别标签：" + tag1 + ", " + tag2 + "】";
    }
}
