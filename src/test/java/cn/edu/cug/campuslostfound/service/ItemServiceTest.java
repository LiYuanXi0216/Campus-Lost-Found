package cn.edu.cug.campuslostfound.service;

import cn.edu.cug.campuslostfound.entity.Item;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * ItemService 业务逻辑的单元测试类
 */
@SpringBootTest // 极度关键！这个注解会启动 Spring 容器，帮你连上本地 MySQL 数据库
public class ItemServiceTest {

    // 自动注入我们刚才写的结对编程成果
    @Autowired
    private ItemService itemService;

    @Test
    public void testPublishItem() {
        // 1. 准备测试数据 (模拟前端用户填写的表单信息)
        Item item = new Item();
        item.setUserId(1L); // 假设这是 ID 为 1 的用户发布的
        item.setType(0);    // 0 代表丢失(寻物)
        item.setTitle("测试丢失的黑色水杯");
        item.setDescription("在图书馆二楼丢失，杯底有个皮卡丘贴纸。");
        item.setLocation("图书馆二楼");

        // 2. 执行我们要测试的业务逻辑
        boolean isSuccess = itemService.publishItem(item);

        // 3. 断言 (Assertion) - 这是单元测试的灵魂所在！
        // 我们预期 isSuccess 必须是 true。如果是 true，测试标绿通过；如果是 false，测试标红报错。
        Assertions.assertTrue(isSuccess, "预期发布物品成功，但返回了 false");

        System.out.println("====== 发布逻辑测试成功！物品已存入数据库，数据库分配的ID为：" + item.getId() + " ======");
    }

    @Test
    public void testSearchItems() {
        // 1. 执行搜索逻辑 (搜索类型为 0 的寻物启事，关键字包含 "水杯")
        List<Item> searchResults = itemService.searchItems(0, "水杯");

        // 2. 断言
        Assertions.assertNotNull(searchResults, "搜索返回的列表不应该为 null");
        // 因为我们上面的测试刚刚往数据库里塞了一个"水杯"，所以这里搜出来的数量必须大于 0
        Assertions.assertTrue(searchResults.size() > 0, "数据库里应该至少能搜出一条包含'水杯'的数据");

        System.out.println("====== 搜索逻辑测试成功！共搜到 " + searchResults.size() + " 条结果 ======");
        for (Item result : searchResults) {
            System.out.println("找到物品 -> 标题: " + result.getTitle() + " | 地点: " + result.getLocation());
        }
    }
}