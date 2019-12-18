package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.TProjInfoDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TProdInfoMapper;
import com.ccb.ark.mapper.TProjInfoMapper;
import com.ccb.ark.model.TProdInfo;
import com.ccb.ark.model.TProjInfo;
import com.ccb.ark.service.impl.BackManageService;
import com.ccb.ark.utils.BigDecimalUtil;
import com.ccb.ark.utils.ExcelUtil;
import com.ccb.ark.utils.LogUtil;
import com.ccb.ark.vo.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@Slf4j
@RequestMapping("api/proj")
public class ProjController {
    private static final String TPROJ_INFO ="tProjInfo";

    @Autowired
    private BackManageService backManageService;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    @Resource
    private TProjInfoMapper tProjInfoMapper;

    @Resource
    private TProdInfoMapper tProdInfoMapper;

    /**
     * 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addProj(@RequestBody TProjInfoDTO tProjInfoDto) {
        TProjInfo tProjInfo = new TProjInfo();
        BeanUtils.copyProperties(tProjInfoDto, tProjInfo);
        return backManageService.addProj(tProjInfo);
    }
    /**
     *  修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateProj(@RequestBody TProjInfoDTO tProjInfoDto) {
        TProjInfo tProjInfo = new TProjInfo();
        BeanUtils.copyProperties(tProjInfoDto, tProjInfo);
        Map<String,Object> map=new HashMap<>();
        map.put(TPROJ_INFO,tProjInfo);
        return backManageService.update(TPROJ_INFO,map);
    }
    /**
     * 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteProj(String prjCode) {
        return backManageService.delete(TPROJ_INFO,prjCode);
    }

    /**
     *  条件查询
     */
    @GetMapping(path = "/search")
    public ResponseData getProjList(TProjInfoDTO tProjInfoDto) {
        TProjInfo tProjInfo = new TProjInfo();
        BeanUtils.copyProperties(tProjInfoDto, tProjInfo);
        return backManageService.getProjList(tProjInfo);
    }

    /**
     * excel导入
     */
     @RequestMapping(value="/import",method= RequestMethod.POST)
     @LogOperation( logSystem = "项目信息", logType = "excel导入")
     public ResponseData projImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
         if (!file.isEmpty()) {
             String filename=file.getOriginalFilename();
             String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
             if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                 return ResponseData.failInstance("文件格式不正确,请上传Excel文件!");
             }
             try {
                 List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                 if(!"项目编号".equals(String.valueOf(list.get(0).get(0)))&& !"项目名称".equals(String.valueOf(list.get(0).get(1)))&&
                    !"任务书下达日期".equals(String.valueOf(list.get(0).get(2)))&& !"所属单位".equals(String.valueOf(list.get(0).get(3)))){
                     return ResponseData.failInstance("请上传项目信息表!");
                 }
                 List<TProjInfo> tProjlist=new ArrayList<>() ;
                 int updataNum=0;
                 int addNum=0;
                 for(int i=1;i<list.size();i++) {
                     TProjInfo tProjInfo = new TProjInfo();
                     List<Object> l = list.get(i);

                     String prjCode=String.valueOf(l.get(0)).trim().replace(" ","");
                     if(prjCode==null||prjCode.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:项目编号不能为空");
                     }
                     String projName=String.valueOf(l.get(1)).trim().replace(" ","");
                     if(projName==null||projName.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:项目名称不能为空");
                     }
                     String asinDt=String.valueOf(l.get(2)).trim().replace(" ","");
                     if(asinDt==null||asinDt.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:任务书下达日期不能为空");
                     }
                     if(asinDt!=null&&asinDt.length()>0&&asinDt.length()!=10){
                         return ResponseData.failInstance("第"+(i+1)+"行:任务书下达日期：'"+asinDt+"'格式不正确！请使用yyyy-MM-dd格式");
                     }

                     String statusVal=String.valueOf(l.get(3)).trim().replace(" ","");
                     if(statusVal==null||statusVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:项目状态不能为空");
                     }
                     String status= backManageService.getParmCd("projStatusModel",statusVal);
                     if(status!=null){
                         tProjInfo.setStatus(status);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:项目状态：'"+statusVal+"' 不 存在");
                     }
                     String progress=String.valueOf(l.get(4)).trim().replace(" ","");
                     tProjInfo.setProgress(BigDecimalUtil.getBigDecimal(progress).toString());

                     String techVal=String.valueOf(l.get(5)).trim().replace(" ","");
                     if(techVal!=null&&techVal.length()>0){
                         String tech= backManageService.getParmCd("abilityModel",techVal);
                         if(tech!=null){
                             tProjInfo.setTechAbility(tech);
                         }else {
                             return ResponseData.failInstance("第"+(i+1)+"行:新技术领域：'"+techVal+"' 不存 在");
                         }
                     }else {
                         tProjInfo.setTechAbility(techVal);
                     }
                     String bugtAmt=String.valueOf(l.get(6)).trim().replace(" ","");

                     tProjInfo.setPrjCode(prjCode);
                     tProjInfo.setProjName(projName);
                     tProjInfo.setAsinDt(asinDt);
                     tProjInfo.setBugtAmt(BigDecimalUtil.getBigDecimal(bugtAmt));

                     String avalProd=String.valueOf(l.get(7)).trim().replace(" ","");
                     if(avalProd!=null&&avalProd.length()!=0){
                         String[] prodArr = avalProd.split("、");
                         for(String prodCode:prodArr){
                             TProdInfo t=tProdInfoMapper.selectByPrimaryKey(prodCode);
                             if(t==null){
                                 return ResponseData.failInstance("第"+(i+1)+"行:对应可售产品：'"+prodCode+"' 不存在");
                             }
                         }

                     }
                     tProjInfo.setAvalProd(avalProd);
                     tProjInfo.setType(String.valueOf(l.get(8)).trim().replace(" ",""));

                     TProjInfo tProjInfo2=tProjInfoMapper.selectByPrimaryKey(prjCode);
                     if(tProjInfo2!=null){
                         tProjInfoMapper.updateByPrimaryKey(tProjInfo);
                         updataNum+=1;
                    }else {
                         tProjlist.add(tProjInfo);
                     }
                 }
                 if (!tProjlist.isEmpty()) {
                     List<TProjInfo> unique=new ArrayList<>();
                     tProjlist.stream().filter(LogUtil.distinctByKey(s->s.getPrjCode())).forEach(s->unique.add(s));
                     tProjInfoMapper.excelImport(unique);
                     addNum=unique.size();
                 }
                 LogUtil.setLogOperation(ProjController.class,"projImport",updataNum,addNum);
             } catch (Exception e) {
                 return ResponseData.failInstance("上传失败");
             }
         }
            return ResponseData.successInstance("上传成功");
        }

    /**
     * 获取项目状态 下拉框
     */
    @GetMapping(path = "/getStatusModel")
    public ResponseData getStatusModel() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("projStatusModel"));
    }

    /**
     * 获取 对应可售产品 下拉框
     */
    @GetMapping(path = "/getAvalProdModel")
    public ResponseData getAvalProdModel() {
        return ResponseData.successInstance(tProdInfoMapper.selectAll());
    }

}
