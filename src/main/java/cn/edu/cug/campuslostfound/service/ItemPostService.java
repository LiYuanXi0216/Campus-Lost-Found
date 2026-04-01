package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.ItemPost;
import cn.edu.cug.campuslostfound.mapper.ItemPostMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ItemPostService {

    private final ItemPostMapper mapper;

    public ItemPostService(ItemPostMapper mapper) {
        this.mapper = mapper;
    }

    // 业务功能 1：发布帖子
    public ItemPost createPost(ItemPost post) {
        post.setCreateTime(LocalDateTime.now());
        mapper.insert(post); // MyBatis-Plus 自带的插入方法
        return post;
    }

    // 业务功能 2：浏览所有帖子
    public List<ItemPost> getAllPosts() {
        return mapper.selectList(null); // 传 null 表示查询所有，没有条件
    }
}
