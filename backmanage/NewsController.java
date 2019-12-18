package com.ccb.ark.backmanage;

import com.ccb.ark.dto.TNewsDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TNewsMapper;
import com.ccb.ark.model.DictParmCfg;
import com.ccb.ark.model.TNews;
import com.ccb.ark.po.NewsColumn;
import com.ccb.ark.service.impl.BackManageService;
import com.ccb.ark.service.impl.HRService;
import com.ccb.ark.vo.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
@RequestMapping("/api/news")
public class NewsController {
    private static final String TNEWS ="tNews";

    @Autowired
    private BackManageService backManageService;

    @Autowired
    private HRService service;

    @Resource
    private TNewsMapper tNewsMapper;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    /**
     * 新闻新增
     */
    @PostMapping(path = "/add" )
    @ResponseBody
    public ResponseData addNews(@RequestBody TNewsDTO tNewsDto) {
        TNews tNews = new TNews();
        BeanUtils.copyProperties(tNewsDto, tNews);
        return backManageService.addNews(tNews);
    }

    /**
     * 获取新闻 byid
     */
    @GetMapping (path = "/getNews")
    @ResponseBody
    public ResponseData getNews(String id) {
        return ResponseData.successInstance(tNewsMapper.selectByPrimaryKey(id));
    }
    /**
     * 新闻 修改
     */
    @PostMapping(path = "/update")
    @ResponseBody
    public ResponseData updateNews(@RequestBody TNewsDTO tNewsDto) {
        TNews tNews = new TNews();
        BeanUtils.copyProperties(tNewsDto, tNews);
        Map<String,Object> map=new HashMap<>();
        map.put(TNEWS,tNews);
        return backManageService.update(TNEWS,map);
    }

    /**
     * 新闻 删除
     */
    @PostMapping(path = "/delete")
    @ResponseBody
    public ResponseData deleteNews(String id) {
        return backManageService.delete(TNEWS,id);
    }

    /**
     * 新闻条件查询
     */
    @PostMapping(path = "/search")
    @ResponseBody
    public ResponseData getNewsList(@RequestBody TNewsDTO tNewsDto) {
        TNews tNews = new TNews();
        BeanUtils.copyProperties(tNewsDto, tNews);
        return backManageService.getNewsList(tNews);
    }

    /**
     * 获取 一级目录下拉框
     */
    @GetMapping(path = "/getFirstLevelList")
    @ResponseBody
    public ResponseData getFirstLevelList() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("newsLevel"));
    }

    /**
     * 获取 二级目录下拉框
     */
    @GetMapping(path = "/getTwoLevelList")
    @ResponseBody
    public ResponseData getTwoLevelList(String parmTpnm) {
        return ResponseData.successInstance(dictParmCfgMapper.getFinanceModel("newsSecondary",parmTpnm));
    }



    /** API H5
     * 新闻列表
     */
    @GetMapping(path = "/getNewsList")
    @ResponseBody
    public ResponseData getAPIList(TNewsDTO tNewsDto, @RequestParam String pageNo, @RequestParam String pageSize) {
        TNews tNews = new TNews();
        BeanUtils.copyProperties(tNewsDto, tNews);
        return backManageService.getAPIList(tNews,pageNo,pageSize);
    }
    /** API H5
     * 新闻详情
     */
    @GetMapping(path = "/news.html/{id}")
    public String showFinanceIncome(Model model, @PathVariable String id) {
        TNews tNews=tNewsMapper.selectByPrimaryKey(id);
        model.addAttribute("news", tNews);
        model.addAttribute("user", service.getUser());
        return "news/news";
    }

    /** API H5
     * 新闻栏目
     */
    @GetMapping(path = "/newsColumn")
    @ResponseBody
    public ResponseData getNewsColumn() {
        List<NewsColumn> result=new ArrayList<>();
        result.add(new NewsColumn("推荐","",new ArrayList<>()));
        List<DictParmCfg> firstLevelList= dictParmCfgMapper.findByParmTpcd("newsLevel");
        firstLevelList.stream().forEach(f->{
            List<NewsColumn> childList=new ArrayList<>();
            List<DictParmCfg> twoLevelList= dictParmCfgMapper.getParmCdByTpnm("newsSecondary",f.getParmVal());
            if(twoLevelList!=null&&!twoLevelList.isEmpty()){
                twoLevelList.stream().forEach(t-> childList.add(new NewsColumn(t.getParmVal(),t.getParmCd(),new ArrayList<>())));
            }
            result.add(new NewsColumn(f.getParmVal(),f.getParmCd(),childList));
        });

       return ResponseData.successInstance(result);
    }
}
