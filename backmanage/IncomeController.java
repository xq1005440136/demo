package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.TBizIncomeDTO;
import com.ccb.ark.mapper.TBizIncomeMapper;
import com.ccb.ark.model.TBizIncome;
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
import java.util.*;

@RestController
@Slf4j
@RequestMapping("/api/income")
public class IncomeController {
    private static final String TBIZ_INCOME ="tBizIncome";

    private static final String NUM =RegexUtil.asRegex("\\d+(\\.\\d+)?");

    private static final String MEG="请输入数据";

    @Autowired
    private BackManageService backManageService;

    @Resource
    private TBizIncomeMapper tBizIncomeMapper;


    /**a
     * 营业收入 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addIncome(@RequestBody TBizIncomeDTO tBizIncomeDto) {
        TBizIncome tBizIncome = new TBizIncome();
        BeanUtils.copyProperties(tBizIncomeDto, tBizIncome);
        return backManageService.addIncome(tBizIncome);
    }

    /**
     * 营业收入 修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateIncome(@RequestBody TBizIncomeDTO tBizIncomeDto) {
        TBizIncome tBizIncome = new TBizIncome();
        BeanUtils.copyProperties(tBizIncomeDto, tBizIncome);
        Map<String,Object> map=new HashMap<>();
        map.put(TBIZ_INCOME,tBizIncome);
        return backManageService.update(TBIZ_INCOME,map);
    }

    /**
     * 营业收入 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteIncome(String id) {
        return backManageService.delete(TBIZ_INCOME,id);
    }

    /**
     * 营业收入 条件查询
     */
    @PostMapping(path = "/search")
    public ResponseData getIncomeList(@RequestBody TBizIncomeDTO tBizIncomeDto) {
        TBizIncome tBizIncome = new TBizIncome();
        BeanUtils.copyProperties(tBizIncomeDto, tBizIncome);
        return backManageService.getIncomeList(tBizIncome);
    }

    /**
     * 营业收入  excel导入
     */
    @RequestMapping(value="/import",method= RequestMethod.POST)
    @LogOperation( logSystem = "营业收入", logType = "excel导入")
    public ResponseData incomeImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
        if (!file.isEmpty()) {
            String filename=file.getOriginalFilename();
            String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
            if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                return ResponseData.successInstance("文件格式不正确,请上传Excel文件!");
            }
            try {
                List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                if(!"机构".equals(String.valueOf(list.get(0).get(0))) && !"每笔交易收费".equals(String.valueOf(list.get(0).get(1)))&&
                   !"客户量成分".equals(String.valueOf(list.get(0).get(2)))&& !"服务收入".equals(String.valueOf(list.get(0).get(3)))){
                    return ResponseData.failInstance("请上传 营业收入表!");
                }
                List<TBizIncome> tBizIncomelist=new ArrayList<>() ;
                int updataNum=0;
                int addNum=0;
                for(int i=1;i<list.size();i++){
                    TBizIncome tBizIncome=new TBizIncome();
                    List<Object> l=list.get(i);
                    tBizIncome.setId(StringUtils.getUUID());
                    String orgCodeVal=String.valueOf(l.get(0)).trim().replace(" ","");
                    if(orgCodeVal==null||orgCodeVal.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:机构 不能为空");
                    }
                    String orgCode=backManageService.getParmCd("posModel",orgCodeVal);
                    if(orgCode!=null){
                        tBizIncome.setOrgCode(orgCode);
                    }else {
                        return ResponseData.failInstance("第"+(i+1)+"行:机构：'"+orgCodeVal+"' 不存在");
                    }
                    String txFee=l.get(1).toString().trim().replace(" ","");
                    if(txFee!=null&&txFee.length()>0&&!txFee.matches(NUM)){
                        return ResponseData.failInstance("第"+(i+1)+"行:每笔交易收费：'"+txFee+MEG);
                    }
                    String custDiv=String.valueOf(l.get(2)).trim().replace(" ","");
                    if(custDiv!=null&&custDiv.length()>0&&!custDiv.matches(NUM)){
                        return ResponseData.failInstance("第"+(i+1)+"行:客户量成分：'"+custDiv+MEG);
                    }
                    String srvFee=String.valueOf(l.get(3)).trim().replace(" ","");
                    if(srvFee!=null&&srvFee.length()>0&&!srvFee.matches(NUM)){
                        return ResponseData.failInstance("第"+(i+1)+"行:服务收入：'"+srvFee+MEG);
                    }
                    String annualFee=String.valueOf(l.get(4)).trim().replace(" ","");
                    if(annualFee!=null&&annualFee.length()>0&&!annualFee.matches(NUM)){
                        return ResponseData.failInstance("Excel第"+(i+1)+"行:年度收费：'"+annualFee+MEG);
                    }
                    String fixFee=String.valueOf(l.get(5)).trim().replace(" ","");
                    if(fixFee!=null&&fixFee.length()>0&&!fixFee.matches(NUM)){
                        return ResponseData.failInstance("第"+(i+1)+"行:固定收费：'"+fixFee+MEG);
                    }
                    String incomeAmt=String.valueOf(l.get(6)).trim().replace(" ","");
                    if(incomeAmt!=null&&incomeAmt.length()>0&&!incomeAmt.matches(NUM)){
                        return ResponseData.failInstance("第"+(i+1)+"行:收入金额：'"+incomeAmt+MEG);
                    }
                    String acctDt=String.valueOf(l.get(7)).trim().replace(" ","");
                    if(acctDt!=null&&acctDt.length()>0&&acctDt.length()!=10){
                        return ResponseData.failInstance("第"+(i+1)+"行:入账时间：'"+acctDt+"' 格式不正确！请使用yyyy-MM-dd格式");
                    }
                    tBizIncome.setTxFee(BigDecimalUtil.getBigDecimal(txFee));
                    tBizIncome.setCustDiv(BigDecimalUtil.getBigDecimal(custDiv));
                    tBizIncome.setSrvFee(BigDecimalUtil.getBigDecimal(srvFee));
                    tBizIncome.setAnnualFee(BigDecimalUtil.getBigDecimal(annualFee));
                    tBizIncome.setFixFee(BigDecimalUtil.getBigDecimal(fixFee));
                    tBizIncome.setIncomeAmt(BigDecimalUtil.getBigDecimal(incomeAmt));
                    tBizIncome.setAcctDt(acctDt);
                    String agmtNo=String.valueOf(l.get(8)).trim().replace(" ","");
                    if(agmtNo.length()==0){
                        tBizIncome.setAgmtNo(null);
                    }else {
                        tBizIncome.setAgmtNo(agmtNo);
                    }
                    tBizIncome.setItemCode(String.valueOf(l.get(9)));

                    String caliberType=String.valueOf(l.get(10)).trim().replace(" ","");
                    if(caliberType==null||caliberType.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:'口径类别 '不能为空");
                    }
                    if(caliberType.equals("确认金额")){
                        caliberType="1";
                    }else if(caliberType.equals("实收金额")) {
                        caliberType="2";
                    }else {
                        return ResponseData.failInstance("第"+(i+1)+"行:'口径类别： ' 请输入 ‘确认金额’或‘实收金额’");
                    }
                    tBizIncome.setCaliberType(caliberType);
                    TBizIncome f= tBizIncomeMapper.findbyOrgAndData(tBizIncome);
                    if(f!=null) {
                        tBizIncome.setId(f.getId());
                        tBizIncomeMapper.updateByPrimaryKey(tBizIncome);
                        updataNum+=1;
                    }else {
                        tBizIncomelist.add(tBizIncome);
                    }
                }
                if(!tBizIncomelist.isEmpty()){
                    //确认过 每次上传都是新的数据 不去重 不区分
                    tBizIncomeMapper.excelImport(tBizIncomelist);
                    addNum=tBizIncomelist.size();
                }
                LogUtil.setLogOperation(IncomeController.class,"incomeImport",updataNum,addNum);
            } catch (Exception e) {
                return ResponseData.successInstance("上传失败");
            }
        }
        return ResponseData.successInstance("上传成功");
    }

}
