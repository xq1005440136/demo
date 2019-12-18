package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.RecruitDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.RecruitMapper;
import com.ccb.ark.model.Recruit;
import com.ccb.ark.service.impl.BackManageService;
import com.ccb.ark.utils.ExcelUtil;
import com.ccb.ark.utils.LogUtil;
import com.ccb.ark.utils.StringUtils;
import com.ccb.ark.vo.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.regex.RegexUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/recruit")
public class RecruitController {
    private static final String RECRUIT="recruit";

    @Autowired
    private BackManageService backManageService;

    @Resource
    private RecruitMapper recruitMapper;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    /**a
     * 招聘 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addRecruit(@RequestBody RecruitDTO recruitDto) {
        Recruit recruit = new Recruit();
        BeanUtils.copyProperties(recruitDto, recruit);
        return backManageService.addRecruit(recruit);
    }

    /**
     * 招聘 修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateRecruit(@RequestBody RecruitDTO recruitDto) {
        Recruit recruit = new Recruit();
        BeanUtils.copyProperties(recruitDto, recruit);
        Map<String,Object> map=new HashMap<>();
        map.put(RECRUIT,recruit);
        return backManageService.update(RECRUIT,map);
    }

    /**
     * 招聘 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteRecruit(String id) {
        return backManageService.delete(RECRUIT,id);
    }

    /**
     * 招聘 条件查询
     */
    @PostMapping(path = "/search")
    public ResponseData getRecruitList(@RequestBody RecruitDTO recruitDto) {
        Recruit recruit = new Recruit();
        BeanUtils.copyProperties(recruitDto, recruit);
        return backManageService.getRecruitList(recruit);
    }

    /**
     * 招聘  excel导入
     */
    @RequestMapping(value="/import",method= RequestMethod.POST)
    @LogOperation( logSystem = "招聘信息", logType = "excel导入")
    public ResponseData recruitImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
        if (!file.isEmpty()) {
            String filename=file.getOriginalFilename();
            String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
            if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                return ResponseData.successInstance("文件格式不正确,请上传Excel文件!");
            }
            try {
                List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                if(!"机构".equals(String.valueOf(list.get(0).get(0)))&& !"计划招录人数".equals(String.valueOf(list.get(0).get(1)))&&
                        !"实际招录人数".equals(String.valueOf(list.get(0).get(2)))&& !"招聘年份".equals(String.valueOf(list.get(0).get(3)))){
                    return ResponseData.failInstance("请上传招聘信息表!");
                }
                List<Recruit> recruitlist=new ArrayList<>() ;
                int updataNum=0;
                int addNum=0;
                for(int i=1;i<list.size();i++){
                    Recruit recruit=new Recruit();
                    List<Object> l=list.get(i);
                    recruit.setId(StringUtils.getUUID());
                    String areaVal= String.valueOf(l.get(0)).trim().replace(" ","");
                    if(areaVal==null||areaVal.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:'机构' 不能为空");
                    }
                    String area=backManageService.getParmCd("posModel",areaVal);
                    if(area!=null){
                        recruit.setArea(area);
                    }else {
                        return ResponseData.failInstance("第"+(i+1)+"行:机构：'"+areaVal+"' 不存在");
                    }
                    String expectedNum=l.get(1).toString().trim().replace(" ","");
                    if(expectedNum==null||expectedNum.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:'计划招录人数' 不能为空");
                    }
                    if(expectedNum!=null&&expectedNum.length()>0&&!expectedNum.matches(RegexUtil.asRegex("\\d+(\\d+)?"))){
                        return ResponseData.failInstance("第"+(i+1)+"行:计划招录人数：'"+expectedNum+"' 请输入数字");
                    }
                    String actualNum=l.get(2).toString().trim().replace(" ","");
                    if(actualNum==null||actualNum.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:'实际招录人数' 不能为空");
                    }
                    if(actualNum!=null&&actualNum.length()>0&&!actualNum.matches(RegexUtil.asRegex("\\d+(\\d+)?"))){
                        return ResponseData.failInstance("第"+(i+1)+"行:实际招录人数：'"+actualNum+"' 请输入数字");
                    }

                    String publishDate=String.valueOf(l.get(3)).trim().replace(" ","");
                    if(publishDate==null||publishDate.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:'招聘年份' 不能为空");
                    }
                    if(publishDate!=null&&publishDate.length()>0&&publishDate.length()!=4){
                        return ResponseData.failInstance("Excel第"+(i+1)+"行:招聘年份：'"+publishDate+"' 格式不正确！请使用yyyy格式");
                    }

                    String jobCategory=String.valueOf(l.get(4)).trim().replace(" ","");

                    String recruitment=String.valueOf(l.get(5)).trim().replace(" ","");

                    String statisticalTime=String.valueOf(l.get(6)).trim().replace(" ","");
                    if(statisticalTime==null||statisticalTime.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:'统计日期' 不能为空");
                    }
                    if(statisticalTime!=null&&statisticalTime.length()>0&&statisticalTime.length()!=7){
                        return ResponseData.failInstance("第"+(i+1)+"行:统计日期：'"+statisticalTime+"' 格式不正确！请使用yyyy-MM格式");
                    }
                    recruit.setExpectedNum(Integer.valueOf(expectedNum));
                    recruit.setActualNum(Integer.valueOf(actualNum));
                    recruit.setPublishDate(publishDate);
                    recruit.setJobCategory(jobCategory);
                    recruit.setRecruitment(recruitment);
                    recruit.setStatisticalTime(statisticalTime);

                    Recruit r= recruitMapper.find(recruit);
                    if(r!=null) {
                        recruit.setId(r.getId());
                        recruitMapper.updateByPrimaryKey(recruit);
                        updataNum+=1;
                    }else {
                        recruitlist.add(recruit);
                    }
                }
                if(!recruitlist.isEmpty()){
                    List<Recruit> unique=new ArrayList<>();
                    recruitlist.stream().filter(LogUtil.distinctByKey(s->s.getArea()+s.getPublishDate()+s.getJobCategory()+s.getRecruitment())).forEach(s->unique.add(s));
                    recruitMapper.excelImport(unique);
                    addNum=unique.size();
                }
                LogUtil.setLogOperation(RecruitController.class,"recruitImport",updataNum,addNum);
            } catch (Exception e) {
                return ResponseData.successInstance("上传失败");
            }
        }
        return ResponseData.successInstance("上传成功");
    }

    /**
     * 获取 工作类别 下拉框
     */
    @GetMapping(path = "/getJobCategory")
    public ResponseData getJobCategory() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("PositionCata"));
    }


    /**
     * 获取 招聘方式 下拉框
     */
    @GetMapping(path = "/getRecruitMent")
    public ResponseData getRecruitMent() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("HrCategory"));
    }

}
