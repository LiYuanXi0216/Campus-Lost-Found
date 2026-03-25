package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.Item;
import cn.edu.cug.campuslostfound.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物品信息 API 接口类 (给前端调用的“门把手”)
 */
@RestController // 告诉 Spring Boot 这是提供 RESTful 接口的类，所有返回值都会自动转成 JSON
@RequestMapping("/api/items") // 统一的路由前缀，这类的所有接口路径都以 /api/items 开头
public class ItemController {

    @Autowired // 注入我们测试过的 Service
    private ItemService itemService;

    /**
     * 1. 发布物品信息接口 (前端用 POST 请求调用)
     * 请求路径：POST /api/items/publish
     * @RequestBody：告诉 Spring Boot，前端传来的 JSON 数据会自动转换成 Java 的 Item 对象
     */
    @PostMapping("/publish")
    public Map<String, Object> publishItem(@RequestBody Item item) {
        // 创建一个用于返回统一 JSON 格式的 Map
        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = itemService.publishItem(item);
            if (success) {
                response.put("code", 200);
                response.put("message", "发布成功");
                response.put("data", item); // 可以把发布成功的对象再传回给前端
            } else {
                response.put("code", 400);
                response.put("message", "发布失败：核心逻辑校验未通过");
            }
        } catch (Exception e) {
            // 捕获异常，防止后端直接报错给前端看，显得不专业
            response.put("code", 500);
            response.put("message", "服务器内部错误：" + e.getMessage());
        }

        return response;
    }

    /**
     * 2. 搜索物品信息接口 (前端用 GET 请求调用)
     * 请求路径示例：GET /api/items/search?type=0&keyword=水杯
     */
    @GetMapping("/search")
    public Map<String, Object> searchItems(
            @RequestParam("type") Integer type, // 必须传的参数：类型 (0或1)
            @RequestParam(value = "keyword", required = false) String keyword // 可选参数：搜索关键字
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Item> resultList = itemService.searchItems(type, keyword);
            response.put("code", 200);
            response.put("message", "搜索成功");
            response.put("data", resultList);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "搜索失败：" + e.getMessage());
        }

        return response;
    }
}