package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.TCustInfoDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TCustInfoMapper;
import com.ccb.ark.model.TCustInfo;
import com.ccb.ark.service.impl.BackManageService;
import com.ccb.ark.utils.ExcelUtil;
import com.ccb.ark.utils.LogUtil;
import com.ccb.ark.vo.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@Slf4j
@RequestMapping("api/cust")
public class CustController {
    private static final String TCUST_INFO ="tCustInfo";

    private static final String CUSTOMERKIND="CustomerKind";


    @Autowired
    private BackManageService backManageService;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    @Resource
    private TCustInfoMapper tCustInfoMapper;

    /**
     * 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addCust(@RequestBody TCustInfoDTO tCustInfoDto) {
        TCustInfo tCustInfo = new TCustInfo();
        BeanUtils.copyProperties(tCustInfoDto, tCustInfo);
        return backManageService.addCust(tCustInfo);
    }
    /**
     *  修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateCust(@RequestBody TCustInfoDTO tCustInfoDto) {
        TCustInfo tCustInfo = new TCustInfo();
        BeanUtils.copyProperties(tCustInfoDto, tCustInfo);
        Map<String,Object> map=new HashMap<>();
        map.put(TCUST_INFO,tCustInfo);
        return backManageService.update(TCUST_INFO,map);
    }
    /**
     * 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteCust(String custNo) {
        return backManageService.delete(TCUST_INFO,custNo);
    }

    /**
     *  条件查询
     */
    @PostMapping(path = "/search")
    public ResponseData getCustList(@RequestBody TCustInfoDTO tCustInfoDto) {
        TCustInfo tCustInfo = new TCustInfo();
        BeanUtils.copyProperties(tCustInfoDto, tCustInfo);
        return backManageService.getCustList(tCustInfo);
    }

    /**
     * excel导入
     */
     @RequestMapping(value="/import",method= RequestMethod.POST)
     @LogOperation( logSystem = "客户信息", logType = "excel导入")
     public ResponseData custImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
         if (!file.isEmpty()) {
             String filename=file.getOriginalFilename();
             String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
             if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                 return ResponseData.failInstance("文件格式不正确,请上传Excel文件!");
             }
             try {
                 List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                 if(!"客户号".equals(String.valueOf(list.get(0).get(0)))&& !"客户名称".equals(String.valueOf(list.get(0).get(1)))&&
                    !"是否签约".equals(String.valueOf(list.get(0).get(2)))&& !"战略客户标识".equals(String.valueOf(list.get(0).get(3)))){
                     return ResponseData.failInstance("请上传客户信息表!");
                 }
                 List<TCustInfo> tCustlist=new ArrayList<>() ;
                 int updataNum=0;
                 int addNum=0;
                 for(int i=1;i<list.size();i++){
                     TCustInfo tCustInfo=new TCustInfo();
                     List<Object> l=list.get(i);

                     String custNo=String.valueOf(l.get(0)).trim().replace(" ","");
                     if(custNo==null||custNo.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:客户号不能为空");
                     }
                     tCustInfo.setCustNo(custNo);
                     String custName=String.valueOf(l.get(1)).trim().replace(" ","");
                     if(custName==null||custName.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:客户名称不能为空");
                     }
                     tCustInfo.setCustName(custName);
                     String signInd=String.valueOf(l.get(2)).trim().replace(" ","");
                     if(signInd==null||signInd.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘是否签约’ 不能为空");
                     }
                     if("是".equals(signInd)){
                         tCustInfo.setSignInd("0");
                     }else if("否".equals(signInd)){
                         tCustInfo.setSignInd("1");
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:'是否签约'请输入‘ 是’ 或‘ 否’");
                     }
                     String custInd=String.valueOf(l.get(3)).trim().replace(" ","");
                     if(custInd==null||custInd.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘战略客户标识’ 不能为空");
                     }
                     if("是".equals(custInd)){
                         tCustInfo.setCustInd("0");
                     }else if("否".equals(custInd)){
                         tCustInfo.setCustInd("1");
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:'战略客户标识'请输入‘ 是’ 或‘ 否’");
                     }
                     String custCataVal=String.valueOf(l.get(4)).trim().replace(" ","");
                     if(custCataVal==null||custCataVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘客户一级分类’ 不能为空");
                     }
                     if(custName.contains("建设银行")){
                         if(!custCataVal.equals("母行")){
                             custCataVal="母行";
                         }
                     }else {
                         if(custCataVal.equals("母行")){
                             return ResponseData.failInstance("第"+(i+1)+"行:‘客户名称为‘建设银行’时， 客户一级分类为 ‘母行’");
                         }
                     }
                     String custCata= backManageService.getParmCd(CUSTOMERKIND,custCataVal);
                     if(custCata!=null){
                         tCustInfo.setCustCata(custCata);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:客户一级分类：'"+custCataVal+"' 不存在");
                     }

                     String custTypVal=String.valueOf(l.get(5)).trim().replace(" ","");
                     if(custTypVal!=null&&custTypVal.length()!=0){
                         String custTyp= backManageService.getParmCd(CUSTOMERKIND,custTypVal);
                         if(custTyp!=null){
                             tCustInfo.setCustTyp(custTyp);
                         }else {
                             return ResponseData.failInstance("第"+(i+1)+"行:客户二级分类：'"+custTypVal+"' 不存在");
                         }
                     }else {
                         tCustInfo.setCustTyp(custTypVal);
                     }
                     String branchCode=String.valueOf(l.get(6)).trim().replace(" ","");
                     if(branchCode==null||branchCode.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘销售大区’ 不能为空");
                     }
                     tCustInfo.setBranchCode(branchCode);

                     TCustInfo tCustInfo2=tCustInfoMapper.selectByPrimaryKey(custNo);
                     if(tCustInfo2!=null){
                         tCustInfoMapper.updateByPrimaryKey(tCustInfo);
                         updataNum+=1;
                     }else {
                         tCustlist.add(tCustInfo);
                     }
                 }
                 if(!tCustlist.isEmpty()){
                     List<TCustInfo> unique=new ArrayList<>();
                     tCustlist.stream().filter(LogUtil.distinctByKey(s-> s.getCustNo())).forEach(s->unique.add(s));
                     tCustInfoMapper.excelImport(unique);
                     addNum=unique.size();
                 }
                 LogUtil.setLogOperation(CustController.class,"custImport",updataNum,addNum);
             } catch (Exception e) {
                 return ResponseData.failInstance("上传失败");
             }
         }
            return ResponseData.successInstance("上传成功");
        }

    /**
     * 获取客户一级分类 下拉框
     */
    @GetMapping(path = "/getCustCataModel")
    public ResponseData getCustCataModel() {
        return ResponseData.successInstance(dictParmCfgMapper.getFinanceModel(CUSTOMERKIND,null));
    }

    /**
     * 获取客户二级分类 下拉框
     */
    @GetMapping(path = "/getCustTypModel")
    public ResponseData getCustTypModel() {
        return ResponseData.successInstance(dictParmCfgMapper.getFinanceModel(CUSTOMERKIND,"1000"));
    }


}
