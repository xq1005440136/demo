package com.ccb.ark.backmanage;

import com.ccb.ark.dto.TEcoExtnDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TEcoExtnMapper;
import com.ccb.ark.model.TEcoExtn;
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
@RequestMapping("api/extn")
public class EcoExtnController {

    @Autowired
    private BackManageService backManageService;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    @Resource
    private TEcoExtnMapper tEcoExtnMapper;

    /**
     * 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addExtn(@RequestBody TEcoExtnDTO tEcoExtnDto) {
        TEcoExtn tEcoExtn = new TEcoExtn();
        BeanUtils.copyProperties(tEcoExtnDto, tEcoExtn);
        return backManageService.addExtn(tEcoExtn);
    }
    /**
     *  修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateExtn(@RequestBody TEcoExtnDTO tEcoExtnDto) {
        TEcoExtn tEcoExtn = new TEcoExtn();
        BeanUtils.copyProperties(tEcoExtnDto, tEcoExtn);
        Map<String,Object> map=new HashMap<>();
        map.put("tEcoExtn",tEcoExtn);
        return backManageService.update("tEcoExtn",map);
    }
    /**
     * 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteExtn(String dataDt, String prdCode) {
        return backManageService.deleteExtn(dataDt,prdCode);
    }

    /**
     *  条件查询
     */
    @GetMapping(path = "/search")
    public ResponseData getExtnList(TEcoExtnDTO tEcoExtnDto) {
        TEcoExtn tEcoExtn = new TEcoExtn();
        BeanUtils.copyProperties(tEcoExtnDto, tEcoExtn);
        return backManageService.getExtnList(tEcoExtn);
    }

    /**
     * excel导入
     */
     @RequestMapping(value="/import",method= RequestMethod.POST)
     public ResponseData extnImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
         if (!file.isEmpty()) {
             String filename=file.getOriginalFilename();
             String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
             if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                 return ResponseData.failInstance("文件格式不正确,请上传Excel文件!");
             }
             try {
                 List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                 if(!"数据日期".equals(String.valueOf(list.get(0).get(0)))&& !"产品编号".equals(String.valueOf(list.get(0).get(1)))&&
                    !"客户类型".equals(String.valueOf(list.get(0).get(2)))&& !"产品名称".equals(String.valueOf(list.get(0).get(3)))){
                     return ResponseData.failInstance("请上传外部生态表!");
                 }
                 List<TEcoExtn> extnlist=new ArrayList<>() ;
                 for(int i=1;i<list.size();i++){
                     TEcoExtn tEcoExtn=new TEcoExtn();
                     List<Object> l=list.get(i);

                     String dataDt=String.valueOf(l.get(0)).trim().replace(" ","");
                     if(dataDt==null||dataDt.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:'数据日期'不能为空");
                     }

                     String prdCode=String.valueOf(l.get(1)).trim().replace(" ","");
                     if(prdCode==null||prdCode.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:'产品编号'不能为空");
                     }

                     String custCataVal=String.valueOf(l.get(2)).trim().replace(" ","");
                     if(custCataVal==null||custCataVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘客户类型’ 不能为空");
                     }
                     String custCata= backManageService.getParmCd("CustomerKind",custCataVal);
                     if(custCata!=null){
                         tEcoExtn.setCustCata(custCata);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:客户类型：'"+custCataVal+"' 不存在");
                     }


                     String prodName=String.valueOf(l.get(3)).trim().replace(" ","");
                     if(prodName==null||prodName.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘产品名称’ 不能为空");
                     }

                     String signTimes=String.valueOf(l.get(4)).trim().replace(" ","");
                     if(signTimes==null||signTimes.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘签约次数’ 不能为空");
                     }

                     String signAmt=String.valueOf(l.get(5)).trim().replace(" ","");
                     String pltCustNum=String.valueOf(l.get(6)).trim().replace(" ","");
                     String pltTxAmt=String.valueOf(l.get(7)).trim().replace(" ","");

                     tEcoExtn.setDataDt(dataDt);
                     tEcoExtn.setPrdCode(prdCode);

                     tEcoExtn.setProdName(prodName);
                     tEcoExtn.setSignTimes(Integer.valueOf(signTimes));
                     tEcoExtn.setSignAmt(BigDecimalUtil.getBigDecimal(signAmt));
                     tEcoExtn.setPltCustNum(Integer.valueOf(pltCustNum));
                     tEcoExtn.setPltTxAmt(BigDecimalUtil.getBigDecimal(pltTxAmt));

                     TEcoExtn tEcoExtn2=tEcoExtnMapper.selectByPrimaryKey(dataDt,prdCode);
                     if(tEcoExtn2!=null){
                         tEcoExtnMapper.updateByPrimaryKey(tEcoExtn);
                     }else {
                         extnlist.add(tEcoExtn);
                     }
                 }
                 if(!extnlist.isEmpty()){
                     tEcoExtnMapper.excelImport(extnlist);
                 }

             } catch (Exception e) {
                 return ResponseData.failInstance("上传失败");
             }
         }
            return ResponseData.successInstance("上传成功");
        }


}
