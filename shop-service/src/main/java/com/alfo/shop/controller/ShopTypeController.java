package com.alfo.shop.controller;




import com.alfo.common.domain.dto.Result;

import com.alfo.shop.domain.po.ShopType;
import com.alfo.shop.service.impl.ShopTypeServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private ShopTypeServiceImpl shopTypeService;

    @GetMapping("list")
    public Result queryTypeList() {
        List<ShopType> typeList = shopTypeService.getTypeList();
        return Result.ok(typeList);
    }
}
