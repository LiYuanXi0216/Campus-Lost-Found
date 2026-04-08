package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.dto.ItemPostCreateRequest;
import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.service.ItemPostService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController // 标记这是一个返回 JSON 数据的接口类
@RequestMapping("/api/posts") // 规定这些接口的统一前缀路径
@CrossOrigin // 简单粗暴地允许前端跨域请求，方便你们前后端本地联调
public class ItemPostController
{
    private final ItemPostService service;

    public ItemPostController(ItemPostService service) {
        this.service = service;
    }

    // API 1: 发布帖子 (使用 POST 请求)
    @PostMapping
    public ItemPost create(@RequestBody ItemPostCreateRequest request) {
        // @RequestBody 会自动把前端传来的 JSON 数据转换成 ItemPostCreateRequest 对象
        return service.createPost(request);
    }

    // API 2: 浏览所有帖子 (使用 GET 请求)
    @GetMapping
    public List<ItemPost> listAll() {
        return service.getAllPosts();
    }

    // 新增 API 3: 根据类型浏览帖子
    // 路径会长这样：http://localhost:8080/api/posts/type/LOST
    @GetMapping("/type/{type}")
    public List<ItemPost> listByType(@PathVariable String type) {
        // @PathVariable 会把路径里的 {type} 提取出来传给方法的参数
        return service.getPostsByType(type);
    }

    // 新增 API: 综合检索接口
    // 访问路径类似：http://localhost:8080/api/posts/search?type=LOST&keyword=黑色
    @GetMapping("/search")
    public List<ItemPost> search(
            // @RequestParam 表示从 URL 的问号后面取参数，required = false 表示前端可以不传这个参数
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {

        return service.searchPosts(type, keyword);
    }
}
