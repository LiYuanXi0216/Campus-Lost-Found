package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.Item;
import java.util.List;

/**
 * 物品信息业务逻辑接口
 */
public interface ItemService {

    /**
     * 发布一条寻物/招领信息
     * @param item 包含前端传来的物品信息
     * @return 是否发布成功
     */
    boolean publishItem(Item item);

    /**
     * 根据类型和关键字搜索信息
     * @param type 信息类型 (0-丢失, 1-拾取)
     * @param keyword 搜索关键字 (可以为空)
     * @return 匹配的物品列表
     */
    List<Item> searchItems(Integer type, String keyword);
}