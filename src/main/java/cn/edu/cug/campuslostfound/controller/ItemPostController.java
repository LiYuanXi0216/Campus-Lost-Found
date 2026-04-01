package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.service.ItemPostService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController // 标记这是一个返回 JSON 数据的接口类
@RequestMapping("/api/posts") // 规定这些接口的统一前缀路径
@CrossOrigin // 简单粗暴地允许前端跨域请求，方便你们前后端本地联调
public class ItemPostController {

    private final ItemPostService service;

    public ItemPostController(ItemPostService service) {
        this.service = service;
    }

    // API 1: 发布帖子 (使用 POST 请求)
    @PostMapping
    public ItemPost create(@RequestBody ItemPost post) {
        // @RequestBody 会自动把前端传来的 JSON 数据转换成 ItemPost 对象
        return service.createPost(post);
    }

    // API 2: 浏览所有帖子 (使用 GET 请求)
    @GetMapping
    public List<ItemPost> listAll() {
        return service.getAllPosts();
    }
}
