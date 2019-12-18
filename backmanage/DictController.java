package com.ccb.ark.backmanage;

import com.ccb.ark.dto.DictParmCfgDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.model.DictParmCfg;
import com.ccb.ark.service.impl.BackManageService;
import com.ccb.ark.vo.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/dict")
public class DictController {
    @Autowired
    private BackManageService backManageService;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;
    /**
     * 字典数据 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addDict(@RequestBody DictParmCfgDTO dictDto) {
        DictParmCfg dict = new DictParmCfg();
        BeanUtils.copyProperties(dictDto, dict);
        return backManageService.addDict(dict);
    }

    /**
     * 字典数据 修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateDict(@RequestBody DictParmCfgDTO dictDto) {
        DictParmCfg dict = new DictParmCfg();
        BeanUtils.copyProperties(dictDto, dict);
        Map<String,Object> map=new HashMap<>();
        map.put("dict",dict);
        return backManageService.update("dict",map);
    }

    /**
     * 字典数据 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteDict(String parmTpcd,String parmCd) {
        return backManageService.deleteDict(parmTpcd,parmCd);
    }

    /**
     * 字典数据 条件查询
     */
    @GetMapping(path = "/search")
    public ResponseData getDictList(DictParmCfgDTO dictDto) {
        DictParmCfg dict = new DictParmCfg();
        BeanUtils.copyProperties(dictDto, dict);
        return backManageService.getDictList(dict);
    }


    /**
     * 获取 字典类型代码 下拉框
     */
    @GetMapping(path = "/getParmTpcd")
    public ResponseData getParmTpcd() {
        return ResponseData.successInstance(dictParmCfgMapper.getParmTpcd());
    }

    /**
     * 获取 字典类型代码 下拉框
     */
    @GetMapping(path = "/getDictList")
    public ResponseData getDictList() {
        return ResponseData.successInstance(dictParmCfgMapper.selectAll());
    }



    /**
     * 获取 新闻栏目管理
     */
    @GetMapping(path = "/getParmTpnm")
    public ResponseData getParmTpnm() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("newsLevel"));
    }

    /**
     * 删除 新闻栏目
     */
    @PostMapping(path = "/deleteNews")
    public ResponseData deleteNews(String parmTpcd,String parmCd) {
        List<DictParmCfg> newsList= dictParmCfgMapper.getFinanceModel("newsSecondary",parmCd);
        if(newsList!=null&&!newsList.isEmpty()){
            return ResponseData.failInstance("请先删除二级栏目");
        }else {
            return backManageService.deleteDict(parmTpcd,parmCd);
        }
    }
    @PostMapping(path ="/get")
    public String get(){


        return "hello";
    }

}
