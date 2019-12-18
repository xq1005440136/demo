package com.ccb.ark.backmanage;

import com.ccb.ark.dto.TEcoIntnDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TEcoIntnMapper;
import com.ccb.ark.model.TEcoIntn;
import com.ccb.ark.service.impl.BackManageService;
import com.ccb.ark.utils.BigDecimalUtil;
import com.ccb.ark.utils.ExcelUtil;
import com.ccb.ark.vo.ResponseData;
import lombok.extern.slf4j.Slf4j;

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
@RequestMapping("api/intn")
public class EcoIntnController {

    @Autowired
    private BackManageService backManageService;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    @Resource
    private TEcoIntnMapper tEcoIntnMapper;

    /**
     * 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addIntn(@RequestBody TEcoIntnDTO tEcoIntnDto) {
        TEcoIntn tEcoIntn = new TEcoIntn();
        BeanUtils.copyProperties(tEcoIntnDto, tEcoIntn);
        return backManageService.addIntn(tEcoIntn);
    }
    /**
     *  修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateIntn(@RequestBody TEcoIntnDTO tEcoIntnDto) {
        TEcoIntn tEcoIntn = new TEcoIntn();
        BeanUtils.copyProperties(tEcoIntnDto, tEcoIntn);
        Map<String,Object> map=new HashMap<>();
        map.put("tEcoIntn",tEcoIntn);
        return backManageService.update("tEcoIntn",map);
    }
    /**
     * 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteIntn(String dataDt,String prjCode) {
        return backManageService.deleteIntn(dataDt,prjCode);
    }

    /**
     *  条件查询
     */
    @GetMapping(path = "/search")
    public ResponseData getIntnList(TEcoIntnDTO tEcoIntnDto) {
        TEcoIntn tEcoIntn = new TEcoIntn();
        BeanUtils.copyProperties(tEcoIntnDto, tEcoIntn);
        return backManageService.getIntnList(tEcoIntn);
    }

    /**
     * excel导入
     */
     @RequestMapping(value="/import",method= RequestMethod.POST)
     public ResponseData intnImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
         if (!file.isEmpty()) {
             String filename=file.getOriginalFilename();
             String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
             if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                 return ResponseData.failInstance("文件格式不正确,请上传Excel文件!");
             }
             try {
                 List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                 if(!"数据日期".equals(String.valueOf(list.get(0).get(0)))&& !"项目编号".equals(String.valueOf(list.get(0).get(1)))&&
                    !"项目名称".equals(String.valueOf(list.get(0).get(2)))&& !"建信子公司全称".equals(String.valueOf(list.get(0).get(3)))){
                     return ResponseData.failInstance("请上传内部生态表!");
                 }
                 List<TEcoIntn> intnlist=new ArrayList<>() ;
                 for(int i=1;i<list.size();i++){
                     TEcoIntn tEcoIntn=new TEcoIntn();
                     List<Object> l=list.get(i);

                     String dataDt=String.valueOf(l.get(0)).trim().replace(" ","");
                     if(dataDt==null||dataDt.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘数据日期’不能为空");
                     }
                     String prjCode=String.valueOf(l.get(1)).trim().replace(" ","");
                     if(prjCode==null||prjCode.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘项目编号’不能为空");
                     }
                     String projName=String.valueOf(l.get(2)).trim().replace(" ","");
                     if(projName==null||projName.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘项目名称’ 不能为空");
                     }
                     String orgName=String.valueOf(l.get(3)).trim().replace(" ","");

                     String orgShtname=String.valueOf(l.get(4)).trim().replace(" ","");
                     if(orgShtname==null||orgShtname.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘建信子公司简称’ 不能为空");
                     }

                     String orgCode=backManageService.getParmCd("CCBFTSubCompany",orgShtname);
                     if(orgCode!=null){
                         tEcoIntn.setOrgCode(orgCode);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:建信子公司简称：‘"+orgShtname+ "’不存在");
                     }

                     String projNum=String.valueOf(l.get(5)).trim().replace(" ","");

                     String projAmt=String.valueOf(l.get(6)).trim().replace(" ","");
                     String majProjNum=String.valueOf(l.get(7)).trim().replace(" ","");

                     tEcoIntn.setDataDt(dataDt);
                     tEcoIntn.setPrjCode(prjCode);
                     tEcoIntn.setProjName(projName);

                     tEcoIntn.setOrgName(orgName);
                     tEcoIntn.setOrgShtname(orgShtname);
                     tEcoIntn.setProjNum(Integer.valueOf(projNum));
                     tEcoIntn.setProjAmt(BigDecimalUtil.getBigDecimal(projAmt));
                     tEcoIntn.setMajProjNum(Integer.valueOf(majProjNum));

                     TEcoIntn tEcoIntn2=tEcoIntnMapper.selectByPrimaryKey(dataDt,prjCode);
                     if(tEcoIntn2!=null){
                         tEcoIntnMapper.updateByPrimaryKey(tEcoIntn);
                     }else {
                         intnlist.add(tEcoIntn);
                     }
                 }
                 if(!intnlist.isEmpty()){
                     tEcoIntnMapper.excelImport(intnlist);
                 }

             } catch (Exception e) {
                 return ResponseData.failInstance("上传失败");
             }
         }
            return ResponseData.successInstance("上传成功");
        }

}
