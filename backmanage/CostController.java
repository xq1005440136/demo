package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.TBizCostDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TBizCostMapper;
import com.ccb.ark.model.TBizCost;
import com.ccb.ark.service.impl.BackManageService;
import com.ccb.ark.utils.BigDecimalUtil;
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
@RequestMapping("/api/cost")
public class CostController {
    private static final String TBIZ_COST ="tBizCost";

    @Autowired
    private BackManageService backManageService;

    @Resource
    private TBizCostMapper tBizCostMapper;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    /**
     * 营业成本 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addStaff(@RequestBody TBizCostDTO tBizCostDto) {
        TBizCost tBizCost = new TBizCost();
        BeanUtils.copyProperties(tBizCostDto, tBizCost);
        return backManageService.addCost(tBizCost);
    }

    /**
     * 营业成本 修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateCost( @RequestBody TBizCostDTO tBizCostDto) {
        TBizCost tBizCost = new TBizCost();
        BeanUtils.copyProperties(tBizCostDto, tBizCost);
        Map<String,Object> map=new HashMap<>();
        map.put(TBIZ_COST,tBizCost);
        return backManageService.update(TBIZ_COST,map);
    }

    /**
     * 营业成本 删除，删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteCost(String id) {
        return backManageService.delete(TBIZ_COST,id);
    }

    /**
     * 营业成本 条件查询
     */
    @PostMapping(path = "/search")
    public ResponseData getCostList(@RequestBody TBizCostDTO tBizCostDto) {
        TBizCost tBizCost = new TBizCost();
        BeanUtils.copyProperties(tBizCostDto, tBizCost);
        return backManageService.getCostList(tBizCost);
    }

    /**
     * 营业成本  excel导入
     */
    @RequestMapping(value="/import",method= RequestMethod.POST)
    @LogOperation( logSystem = "营业支出", logType = "excel导入")
    public ResponseData costImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
        if (!file.isEmpty()) {
            String filename=file.getOriginalFilename();
            String suffix = filename.substring(filename.lastIndexOf('.'));
            if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                return ResponseData.successInstance("文件格式不正确,请上传Excel文件!");
            }
            try {
                List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                if(!"机构".equals(String.valueOf(list.get(0).get(0)))&& !"所属科目".equals(String.valueOf(list.get(0).get(1)))&&
                   !"成本金额".equals(String.valueOf(list.get(0).get(2)))&& !"入账时间".equals(String.valueOf(list.get(0).get(3))))
                {
                    return ResponseData.failInstance("请上传营业成本表!");
                }
                List<TBizCost> tBizCostlist=new ArrayList<>() ;
                int updataNum=0;
                int addNum=0;
                for(int i=1;i<list.size();i++){
                    TBizCost tBizCost=new TBizCost();
                    List<Object> l=list.get(i);
                    tBizCost.setId(StringUtils.getUUID());
                    String orgCodeVal=String.valueOf(l.get(0)).trim().replace(" ","");
                    if(orgCodeVal==null||orgCodeVal.length()==0) {
                        return ResponseData.failInstance("第 "+(i+1)+"行:机构不能为空");
                    }
                    String orgCode=backManageService.getParmCd("posModel",orgCodeVal);
                    if(orgCode==null) {
                        return ResponseData.failInstance("第"+(i+1)+"行:机构：'"+orgCodeVal+"' 不 存在");
                    }else {
                        tBizCost.setOrgCode(orgCode);
                    }
                    tBizCost.setItemCode(String.valueOf(l.get(1)));

                    String costAmt=String.valueOf(l.get(2)).trim().replace(" ","");
                    if(costAmt!=null&&costAmt.length()>0&&!costAmt.matches(RegexUtil.asRegex("[+-]?\\d+(\\.\\d+)?"))) {
                        return ResponseData.failInstance("第"+(i+1)+"行:成本金额：'"+costAmt+"' 请输入数字");
                    }
                    tBizCost.setCostAmt(BigDecimalUtil.getBigDecimal(costAmt));

                    String acctDt=String.valueOf(l.get(3)).trim().replace(" ","");
                    if(acctDt!=null&&acctDt.length()>0&&acctDt.length()!=10) {
                        return ResponseData.failInstance("第"+(i+1)+"行:入账时间 ：'"+acctDt+"' 格式不正确！请使用yyyy-MM-dd格式");
                    }
                    tBizCost.setAcctDt(acctDt);

                    String spendTypeOneVal=String.valueOf(l.get(4)).trim().replace(" ","");
                    if(spendTypeOneVal==null||spendTypeOneVal.length()==0) {
                        return ResponseData.failInstance("第 "+(i+1)+"行:'支出类型（一级）' 不能为空");
                    }
                    String spendTypeOne=backManageService.getParmCd("financeIndex",spendTypeOneVal);
                    if(spendTypeOne==null) {
                        return ResponseData.failInstance("第"+(i+1)+"行:支出类型（一级）：'"+spendTypeOneVal+"' 不存在");
                    }else {
                        tBizCost.setSpendTypeOne(spendTypeOne);
                    }
                    String spendTypeTwo=String.valueOf(l.get(5)).trim().replace(" ","");
                    tBizCost.setSpendTypeTwo(spendTypeTwo);

                    String spendTypeThreeVal=String.valueOf(l.get(6)).trim().replace(" ","");
                    if("财务费用".equals(spendTypeOneVal)||"其他".equals(spendTypeOneVal)) {
                        tBizCost.setSpendTypeThree("");
                    }else {
                        if(spendTypeThreeVal!=null&&spendTypeThreeVal.length()!=0) {
                            String spendTypeThree=backManageService.getParmCd("financeType",spendTypeThreeVal);
                            if(spendTypeThree==null) {
                                return ResponseData.failInstance("第"+(i+1)+"行:支出类型（三级）：'"+spendTypeThreeVal+"' 不 存在");
                            }else {
                                tBizCost.setSpendTypeThree(spendTypeThree);
                            }
                        }else {
                            tBizCost.setSpendTypeThree(spendTypeThreeVal);
                        }
                    }

                    String caliberType=String.valueOf(l.get(7)).trim().replace(" ","");
                    if(caliberType==null||caliberType.length()==0)
                    {
                        return ResponseData.failInstance("第"+(i+1)+"行:'口径类别  '不能为空");
                    }
                    if(caliberType.equals("确认金额"))
                    {
                        caliberType="1";
                    }else if(caliberType.equals("实付金额")) {
                        caliberType="2";
                    }else {
                        return ResponseData.failInstance("第"+(i+1)+"行:'口径类别： ' 请输入 ‘确认金额’或‘实付金额’");
                    }
                    tBizCost.setCaliberType(caliberType);

                    tBizCostlist.add(tBizCost);
                }
                if(!tBizCostlist.isEmpty()) {
                    tBizCostMapper.excelImport(tBizCostlist);
                    addNum=tBizCostlist.size();
                }
                LogUtil.setLogOperation(CostController.class,"costImport",updataNum,addNum);
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
        return ResponseData.successInstance(dictParmCfgMapper.getFinanceModel("financeIndex","2"));
    }


    /**
     * 财务类型 下拉框
     */
    @GetMapping(path = "/getFinanceType")
    public ResponseData getFinanceType() {
        return ResponseData.successInstance(dictParmCfgMapper.getFinanceModel("financeType","2"));
    }

}
