package cn.edu.cug.campuslostfound.service.impl;

import cn.edu.cug.campuslostfound.entity.Item;
import cn.edu.cug.campuslostfound.mapper.ItemMapper;
import cn.edu.cug.campuslostfound.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物品信息业务逻辑实现类
 */
@Service // 告诉 Spring Boot 这是一个 Service 层的组件
public class ItemServiceImpl implements ItemService {

    @Autowired // 自动注入我们之前写好的 Mapper 搬运工
    private ItemMapper itemMapper;

    @Override
    public boolean publishItem(Item item) {
        // 【领航员思考点】：在真正存入数据库前，是否需要做校验？
        // 比如：防呆设计。如果前端传来的标题为空，直接拒绝发布。
        if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
            System.out.println("发布失败：物品标题不能为空！");
            return false;
        }

        // 校验通过，调用 mapper 存入数据库
        int result = itemMapper.insert(item);
        return result > 0; // 如果影响的行数大于0，说明插入成功
    }

    @Override
    public List<Item> searchItems(Integer type, String keyword) {
        // 【领航员思考点】：如果用户没输入关键字直接点了搜索怎么办？
        // 处理边界情况：如果关键字为 null，我们把它变成空字符串，这样就能搜出所有该类型的信息
        if (keyword == null) {
            keyword = "";
        } else {
            // 去除用户不小心多打的前后空格
            keyword = keyword.trim();
        }

        // 调用 mapper 去数据库模糊查询
        return itemMapper.searchByTypeAndKeyword(type, keyword);
    }
}