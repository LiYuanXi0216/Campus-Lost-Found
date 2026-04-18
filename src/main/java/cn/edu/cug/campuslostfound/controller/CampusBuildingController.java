package cn.edu.cug.campuslostfound.controller;

import cn.edu.cug.campuslostfound.entity.CampusBuilding;
import cn.edu.cug.campuslostfound.mapper.CampusBuildingMapper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/buildings")
@CrossOrigin
public class CampusBuildingController {

    private final CampusBuildingMapper mapper;

    public CampusBuildingController(CampusBuildingMapper mapper) {
        this.mapper = mapper;
    }

    // 免登录直接获取字典
    @GetMapping
    public List<CampusBuilding> getAllBuildings() {
        return mapper.selectList(null);
    }
}