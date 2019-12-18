package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.TBizProfitDTO;
import com.ccb.ark.mapper.TBizProfitMapper;
import com.ccb.ark.model.TBizProfit;
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
@RequestMapping("/api/profit")
public class ProfitController {
    private static final String TBIZ_PROFIT ="tBizProfit";

    private static final String NUM =RegexUtil.asRegex("\\d+(\\.\\d+)?");

    @Autowired
    private BackManageService backManageService;

    @Resource
    private TBizProfitMapper tBizProfitMapper;
    /**
     * 利润EBITDA 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addProfit(@RequestBody TBizProfitDTO tBizProfitDto) {
        TBizProfit tBizProfit = new TBizProfit();
        BeanUtils.copyProperties(tBizProfitDto, tBizProfit);
        return backManageService.addProfit(tBizProfit);
    }

   /**
     * 利润EBITDA 修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateProfit(@RequestBody TBizProfitDTO tBizProfitDto) {
        TBizProfit tBizProfit = new TBizProfit();
        BeanUtils.copyProperties(tBizProfitDto, tBizProfit);
        Map<String,Object> map=new HashMap<>();
        map.put(TBIZ_PROFIT,tBizProfit);
        return backManageService.update(TBIZ_PROFIT,map);
    }

    /**
     * 利润EBITDA 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteProfit(String id) {
        return backManageService.delete(TBIZ_PROFIT,id);
    }

    /**
     * 利润EBITDA 条件查询
     */
    @PostMapping(path = "/search")
    public ResponseData getProfitList(@RequestBody TBizProfitDTO tBizProfitDto) {
        TBizProfit tBizProfit = new TBizProfit();
        BeanUtils.copyProperties(tBizProfitDto, tBizProfit);
        return backManageService.getProfitList(tBizProfit);
    }


    /**
     * 利润EBITDA  excel导入
     */
    @RequestMapping(value="/import",method= RequestMethod.POST)
    @LogOperation( logSystem = "净利润", logType = "excel导入")
    public ResponseData profitImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
        if (!file.isEmpty()) {
            String filename=file.getOriginalFilename();
            String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
            if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                return ResponseData.successInstance("文件格式不正确,请上传Excel文件!");
            }
            try {
                List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                if(!"机构".equals(String.valueOf(list.get(0).get(0)))&& !"报告日期".equals(String.valueOf(list.get(0).get(1)))&&
                   !"营业利润".equals(String.valueOf(list.get(0).get(2)))&& !"利润总额".equals(String.valueOf(list.get(0).get(3)))){
                    return ResponseData.failInstance("请上传利润表!");
                }
                List<TBizProfit> tBizProfitlist=new ArrayList<>() ;
                int updataNum=0;
                int addNum=0;
                for(int i=1;i<list.size();i++){
                    TBizProfit tBizProfit=new TBizProfit();
                    List<Object> l=list.get(i);
                    tBizProfit.setId(StringUtils.getUUID());
                    String orgCodeVal=String.valueOf(l.get(0)).trim().replace(" ","");
                    if(orgCodeVal==null||orgCodeVal.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+ " 行:机构 不能为空");
                    }
                    String orgCode=backManageService.getParmCd("posModel",orgCodeVal);
                    if(orgCode==null){
                        return ResponseData.failInstance("第"+(i+1)+" 行:机构：'"+orgCodeVal+"' 不存在");
                    }else {
                        tBizProfit.setOrgCode(orgCode);
                    }
                    String dataDt=String.valueOf(l.get(1)).trim().replace(" ","");
                    if(dataDt==null||dataDt.length()==0){
                        return ResponseData.failInstance("第"+(i+1)+"行:'报告日期'不能为空");
                    }
                    if(dataDt!=null&&dataDt.length()>0&&dataDt.length()!=10){
                        return ResponseData.failInstance("第"+(i+1)+"行:报告日期：'"+dataDt+"' 格式不正确！请使用yyyy-MM-dd格式");
                    }
                    String profitBiz=String.valueOf(l.get(2)).trim().replace(" ","");
                    if(profitBiz!=null&&profitBiz.length()>0&&!profitBiz.matches(NUM)){
                        return ResponseData.failInstance("第"+(i+1)+"行:营业利润：'"+profitBiz+"' 请输入数字");
                    }
                    String profitAmt=String.valueOf(l.get(3)).trim().replace(" ","");
                    if(profitAmt!=null&&profitAmt.length()>0&&!profitAmt.matches(NUM)){
                        return ResponseData.failInstance("第"+(i+1)+"行:利润总额：'"+profitAmt+" '请输入数字");
                    }
                    String profitNet=String.valueOf(l.get(4)).trim().replace(" ","");
                    if(profitNet!=null&&profitNet.length()>0&&!profitNet.matches(NUM)){
                        return ResponseData.failInstance("第"+(i+1)+"行:净利润：'"+profitNet+" '请输入数字");
                    }


                    tBizProfit.setDataDt(dataDt);
                    tBizProfit.setProfitBiz(BigDecimalUtil.getBigDecimal(profitBiz));
                    tBizProfit.setProfitAmt(BigDecimalUtil.getBigDecimal(profitAmt));
                    tBizProfit.setProfitNet(BigDecimalUtil.getBigDecimal(profitNet));

                    TBizProfit t= tBizProfitMapper.findbyOrgAndData(tBizProfit);
                    if(t!=null) {
                        tBizProfit.setId(t.getId());
                        tBizProfitMapper.updateByPrimaryKey(tBizProfit);
                        updataNum+=1;
                    }else {
                        tBizProfitlist.add(tBizProfit);
                    }
                }
                if(!tBizProfitlist.isEmpty()){
                    List<TBizProfit> unique=new ArrayList<>();
                    tBizProfitlist.stream().filter(LogUtil.distinctByKey(s->s.getOrgCode()+s.getDataDt())).forEach(s->unique.add(s));
                    tBizProfitMapper.excelImport(unique);
                    addNum=unique.size();
                }
                LogUtil.setLogOperation(ProfitController.class,"profitImport",updataNum,addNum);
            } catch (Exception e) {
                return ResponseData.successInstance("上传失败");
            }
        }
        return ResponseData.successInstance("上传成功");
    }


}
