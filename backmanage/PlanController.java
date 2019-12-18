package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.TBizPlanDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TBizPlanMapper;
import com.ccb.ark.model.TBizPlan;
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
@RequestMapping("/api/plan")
public class PlanController {

    private static final String TBIZ_PLAN="tBizPlan";

    @Autowired
    private BackManageService backManageService;

    @Resource
    private TBizPlanMapper tBizPlanMapper;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    /**a
     * 计划 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addPlan(@RequestBody TBizPlanDTO tBizPlanDto) {
        TBizPlan tBizPlan = new TBizPlan();
        BeanUtils.copyProperties(tBizPlanDto, tBizPlan);
        return backManageService.addPlan(tBizPlan);
    }

    /**
     * 计划 修改
     */
    @PostMapping(path = "/update")
    public ResponseData updatePlan(@RequestBody TBizPlanDTO tBizPlanDto) {
        TBizPlan tBizPlan = new TBizPlan();
        BeanUtils.copyProperties(tBizPlanDto, tBizPlan);
        Map<String,Object> map=new HashMap<>();
        map.put(TBIZ_PLAN,tBizPlan);
        return backManageService.update(TBIZ_PLAN,map);
    }

    /**
     * 计划 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deletePlan(String id) {
        return backManageService.delete(TBIZ_PLAN,id);
    }

    /**
     * 计划 条件查询
     */
    @PostMapping(path = "/search")
    public ResponseData getPlanList(@RequestBody TBizPlanDTO tBizPlanDto) {
        TBizPlan tBizPlan = new TBizPlan();
        BeanUtils.copyProperties(tBizPlanDto, tBizPlan);
        return backManageService.getPlanList(tBizPlan);
    }

    /**
     * 计划  excel导入
     */
    @RequestMapping(value="/import",method= RequestMethod.POST)
    @LogOperation( logSystem = "计划", logType = "excel导入")
    public ResponseData planImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
        if (!file.isEmpty()) {
            String filename=file.getOriginalFilename();
            String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
            if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                return ResponseData.successInstance("文件格式不正确,请上传Excel文件!");
            }
            try {
                List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                if(!"机构".equals(String.valueOf(list.get(0).get(0)))&& !"指标类型".equals(String.valueOf(list.get(0).get(1)))&&
                   !"一级分类".equals(String.valueOf(list.get(0).get(2)))&& !"二级分类".equals(String.valueOf(list.get(0).get(3)))){
                    return ResponseData.failInstance("请上传计划表!");
                }
                List<TBizPlan> tBizPlanlist=new ArrayList<>() ;
                int updataNum=0;
                int addNum=0;
                for(int i=1;i<list.size();i++){
                    TBizPlan tBizPlan=new TBizPlan();
                    List<Object> l=list.get(i);
                    tBizPlan.setId(StringUtils.getUUID());
                    String orgCodeVal=String.valueOf(l.get(0)).trim().replace(" ","");
                    if(orgCodeVal==null||orgCodeVal.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:机构不能为空");
                    }
                    String orgCode=backManageService.getParmCd("posModel",orgCodeVal);
                    if(orgCode!=null){
                        tBizPlan.setOrgCode(orgCode);
                    }else {
                        return ResponseData.failInstance("第"+(i+1)+"行:机构：'"+orgCodeVal+"' 不 存在");
                    }
                    String indexTypeVal=l.get(1).toString().trim().replace(" ","");
                    if(indexTypeVal==null||indexTypeVal.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:'指标类型' 不能为空");
                    }
                    String indexType=backManageService.getParmCd("financeIndex",indexTypeVal);
                    if(indexType!=null){
                        tBizPlan.setIndexType(indexType);
                    }else {
                        return ResponseData.failInstance("第"+(i+1)+"行:指标类型：'"+indexTypeVal+"' 不存 在");
                    }
                    String level1=String.valueOf(l.get(2)).trim().replace(" ","");
                    if("营业收入".equals(indexTypeVal)&&!("服务母行收入".equals(level1)||"服务母行战略协同收入".equals(level1)||"其他".equals(level1))){
                        return ResponseData.failInstance("第"+(i+1)+"行:指标类为‘营业收入’时，一级分类 为‘服务母行收入’或‘服务母行战略协同收入’或‘其他’");
                    }
                    tBizPlan.setLevel1(level1);

                    String level2Val=String.valueOf(l.get(3)).trim().replace(" ","");
                    if("财务费用".equals(indexTypeVal)||"净利润".equals(indexTypeVal)||"其他".equals(indexTypeVal)){
                        tBizPlan.setLevel2("");
                    }else {
                        if(level2Val!=null&&level2Val.length()!=0){
                            String level2=backManageService.getParmCd("financeType",level2Val);
                            if(level2!=null){
                                tBizPlan.setLevel2(level2);
                            }else {
                                return ResponseData.failInstance("第"+(i+1)+"行:二级分类：'"+level2Val+"' 不存在");
                            }
                        }else {
                            tBizPlan.setLevel2(level2Val);
                        }
                    }


                    String dataDt=String.valueOf(l.get(4)).trim().replace(" ","");
                    if(dataDt==null||dataDt.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:'年份' 不能为空");
                    }
                    if(dataDt!=null&&dataDt.length()>0&&!dataDt.matches(RegexUtil.asRegex("\\d+(\\.\\d+)?"))){
                        return ResponseData.failInstance("第"+(i+1)+"行: 请输正确年份");
                    }
                    String indVal=String.valueOf(l.get(5)).trim().replace(" ","");
                    if(indVal!=null&&indVal.length()>0&&!indVal.matches(RegexUtil.asRegex("\\d+(\\.\\d+)?"))){
                        return ResponseData.failInstance("第"+(i+1)+"行:指标值：'"+indVal+"' 请输入数字");
                    }

                    tBizPlan.setDataDt(dataDt);
                    tBizPlan.setIndVal(indVal);


                    TBizPlan t= tBizPlanMapper.find(tBizPlan);
                    if(t!=null) {
                        tBizPlan.setId(t.getId());
                        tBizPlanMapper.updateByPrimaryKey(tBizPlan);
                        updataNum+=1;
                    }else {
                        tBizPlanlist.add(tBizPlan);
                    }
                }
                if(!tBizPlanlist.isEmpty()){
                    List<TBizPlan> unique=new ArrayList<>();
                    tBizPlanlist.stream().filter(LogUtil.distinctByKey(s->s.getOrgCode()+s.getIndexType()+s.getLevel1()+s.getLevel2()+s.getDataDt())).forEach(s->unique.add(s));
                    tBizPlanMapper.excelImport(unique);
                    addNum=unique.size();
                }
                LogUtil.setLogOperation(PlanController.class,"planImport",updataNum,addNum);
            } catch (Exception e) {
                return ResponseData.successInstance("上传失败");
            }
        }
        return ResponseData.successInstance("上传成功");
    }

    /**
     * 财务指标 下拉框
     */
    @GetMapping(path = "/getFinanceIndex")
    public ResponseData getFinanceIndex() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("financeIndex"));
    }


    /**
     * 财务类型 下拉框
     */
    @GetMapping(path = "/getFinanceType")
    public ResponseData getFinanceType() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("financeType"));
    }

}
