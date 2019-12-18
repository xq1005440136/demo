package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.TBizOppsDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TBizOppsMapper;
import com.ccb.ark.mapper.TCustInfoMapper;
import com.ccb.ark.mapper.TProdInfoMapper;
import com.ccb.ark.model.TBizOpps;
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
@RequestMapping("api/opps")
public class OppsController {
    private static final String TBIZ_OPPS ="tBizOpps";

    private static final String BIZOPPS_STAGE ="BizOppsStage";



    @Autowired
    private BackManageService backManageService;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    @Resource
    private TBizOppsMapper tBizOppsMapper;

    @Resource
    private TCustInfoMapper tCustInfoMapper;

    @Resource
    private TProdInfoMapper tProdInfoMapper;
    /**
     * 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addOpps(@RequestBody TBizOppsDTO tBizOppsDto) {
        TBizOpps tBizOpps = new TBizOpps();
        BeanUtils.copyProperties(tBizOppsDto, tBizOpps);
        return backManageService.addOpps(tBizOpps);
    }
    /**
     *  修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateOpps(@RequestBody TBizOppsDTO tBizOppsDto) {
        TBizOpps tBizOpps = new TBizOpps();
        BeanUtils.copyProperties(tBizOppsDto, tBizOpps);
        Map<String,Object> map=new HashMap<>();
        map.put(TBIZ_OPPS,tBizOpps);
        return backManageService.update(TBIZ_OPPS,map);
    }
    /**
     * 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteOpps(String id) {
        return backManageService.delete(TBIZ_OPPS,id);
    }

    /**
     *  条件查询
     */
    @GetMapping(path = "/search")
    public ResponseData getOppsList(TBizOppsDTO tBizOppsDto) {
        TBizOpps tBizOpps = new TBizOpps();
        BeanUtils.copyProperties(tBizOppsDto, tBizOpps);
        return backManageService.getOppsList(tBizOpps);
    }

    /**
     * excel导入
     */
     @RequestMapping(value="/import",method= RequestMethod.POST)
     @LogOperation( logSystem = "商机信息", logType = "excel导入")
     public ResponseData oppsImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
         if (!file.isEmpty()) {
             String filename=file.getOriginalFilename();
             String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
             if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                 return ResponseData.failInstance("文件格式不正确,请上传Excel文件!");
             }
             try {
                 List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                 if(!"商机ID".equals(String.valueOf(list.get(0).get(0)))&& !"客户号".equals(String.valueOf(list.get(0).get(1)))&&
                    !"产品编号".equals(String.valueOf(list.get(0).get(2)))&& !"商机阶段".equals(String.valueOf(list.get(0).get(3)))){
                     return ResponseData.failInstance("请上传商机表!");
                 }
                 List<TBizOpps> oppslist=new ArrayList<>() ;
                 int updataNum=0;
                 int addNum=0;
                 for(int i=1;i<list.size();i++){
                     TBizOpps tBizOpps=new TBizOpps();
                     List<Object> l=list.get(i);

                     String id=String.valueOf(l.get(0)).trim().replace(" ","");
                     if(id==null||id.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:'商机ID'不能为空");
                     }
                     String custNo=String.valueOf(l.get(1)).trim().replace(" ","");
                     if(custNo==null||custNo.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:'客户号'不能为空");
                     }
                     if(tCustInfoMapper.selectByPrimaryKey(custNo)==null){
                         return ResponseData.failInstance("第"+(i+1)+"行:'客户号'不存在");
                     }

                     String prdCode=String.valueOf(l.get(2)).trim().replace(" ","");
                     if(prdCode==null||prdCode.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:'产品编号'不能为空");
                     }
                     if(tProdInfoMapper.selectByPrimaryKey(prdCode)==null){
                         return ResponseData.failInstance("第"+(i+1)+"行:'产品编号'不存在");
                     }

                     String stageVal=String.valueOf(l.get(3)).trim().replace(" ","");
                     if(stageVal==null||stageVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:'商机阶段'不能为空");
                     }
                     String stage= backManageService.getParmCd(BIZOPPS_STAGE,stageVal);
                     if(stage!=null){
                         tBizOpps.setStage(stage);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:商机阶段：'"+stageVal+"' 不存在");
                     }

                     String signInd=String.valueOf(l.get(5)).trim().replace(" ","");
                     if(signInd==null||signInd.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘合同是否签署’ 不能为空");
                     }
                     if("是".equals(signInd)){
                         tBizOpps.setSignInd("0");
                     }else if("否".equals(signInd)){
                         tBizOpps.setSignInd("1");
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:'合同是否签署'请输入‘ 是’ 或‘ 否’");
                     }
                     String agmtNo=String.valueOf(l.get(6)).trim().replace(" ","");
                     String signDt=String.valueOf(l.get(7)).trim().replace(" ","");
                     if(signDt==null||signDt.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:合同签署时间 不能为空");
                     }
                     if(signDt!=null&&signDt.length()>0&&signDt.length()!=10){
                         return ResponseData.failInstance("第"+(i+1)+"行:合同签署时间：'"+signDt+"'格式不正确！请使用yyyy-MM-dd格式");
                     }
                     String statusVal=String.valueOf(l.get(8)).trim().replace(" ","");
                     if(statusVal==null||statusVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘商机状态’ 不能为空");
                     }
                     String status= backManageService.getParmCd("BizOppsStatus",statusVal);
                     if(status!=null){
                         tBizOpps.setStatus(status);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:商机状态：'"+statusVal+"' 不存在");
                     }
                     String oppAmt=String.valueOf(l.get(9)).trim().replace(" ","");

                     tBizOpps.setId(id);
                     tBizOpps.setCustNo(custNo);
                     tBizOpps.setPrdCode(prdCode);
                     tBizOpps.setAgmtNo(agmtNo);
                     tBizOpps.setSignDt(signDt);
                     tBizOpps.setOppAmt(BigDecimalUtil.getBigDecimal(oppAmt));

                     TBizOpps tBizOpps2=tBizOppsMapper.selectByPrimaryKey(id);
                     if(tBizOpps2!=null){
                         tBizOppsMapper.updateByPrimaryKey(tBizOpps);
                         updataNum+=1;
                     }else {
                         oppslist.add(tBizOpps);
                     }
                 }
                 if(!oppslist.isEmpty()){
                     List<TBizOpps> unique=new ArrayList<>();
                     oppslist.stream().filter(LogUtil.distinctByKey(s->s.getId())).forEach(s->unique.add(s));
                     tBizOppsMapper.excelImport(unique);
                     addNum=unique.size();
                 }
                 LogUtil.setLogOperation(OppsController.class,"oppsImport",updataNum,addNum);
             } catch (Exception e) {
                 return ResponseData.failInstance("上传失败");
             }
         }
            return ResponseData.successInstance("上传成功");
        }

    /**
     * 获取客户号下拉框
     */
    @GetMapping(path = "/getCustNoModel")
    public ResponseData getCustNoModel() {
        return ResponseData.successInstance(tCustInfoMapper.selectAll());
    }

    /**
     * 获取商机阶段下拉框
     */
    @GetMapping(path = "/getStageModel")
    public ResponseData getStageModel() {
        return ResponseData.successInstance(dictParmCfgMapper.getFinanceModel(BIZOPPS_STAGE,null));
    }

    /**
     * 获取 商机阶段二级分类下拉框
     */
    @GetMapping(path = "/getPhaseModel")
    public ResponseData getPhaseModel() {
        return ResponseData.successInstance(dictParmCfgMapper.getFinanceModel(BIZOPPS_STAGE,"upCd"));
    }


}
