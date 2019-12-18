package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.TProdInfoDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TProdInfoMapper;
import com.ccb.ark.model.TProdInfo;
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
@RequestMapping("api/prod")
public class ProdController {
    private static final String TPROD_INFO ="tProdInfo";

    @Autowired
    private BackManageService backManageService;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    @Resource
    private TProdInfoMapper tProdInfoMapper;

    /**
     * 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addProd(@RequestBody TProdInfoDTO tProdInfoDto) {
        TProdInfo tProdInfo = new TProdInfo();
        BeanUtils.copyProperties(tProdInfoDto, tProdInfo);
        return backManageService.addProd(tProdInfo);
    }
    /**
     *  修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateProd(@RequestBody TProdInfoDTO tProdInfoDto) {
        TProdInfo tProdInfo = new TProdInfo();
        BeanUtils.copyProperties(tProdInfoDto, tProdInfo);
        Map<String,Object> map=new HashMap<>();
        map.put(TPROD_INFO,tProdInfo);
        return backManageService.update(TPROD_INFO,map);
    }
    /**
     * 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteProd(String prdCode) {
        return backManageService.delete(TPROD_INFO,prdCode);
    }

    /**
     *  条件查询
     */
    @GetMapping(path = "/search")
    public ResponseData getProdList(TProdInfoDTO tProdInfoDto) {
        TProdInfo tProdInfo = new TProdInfo();
        BeanUtils.copyProperties(tProdInfoDto, tProdInfo);
        return backManageService.getProdList(tProdInfo);
    }

    /**
     * excel导入
     */
     @RequestMapping(value="/import",method= RequestMethod.POST)
     @LogOperation( logSystem = "产品信息", logType = "excel导入")
     public ResponseData prodImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
         if (!file.isEmpty()) {
             String filename=file.getOriginalFilename();
             String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
             if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                 return ResponseData.failInstance("文件格式不正确,请上传Excel文件!");
             }
             try {
                 List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                 if(!"产品编号".equals(String.valueOf(list.get(0).get(0)))&& !"产品线名称".equals(String.valueOf(list.get(0).get(1)))&&
                    !"基础产品名称".equals(String.valueOf(list.get(0).get(2)))&& !"可售产品名称".equals(String.valueOf(list.get(0).get(3)))){
                     return ResponseData.failInstance("请上传产品表!");
                 }
                 List<TProdInfo> tprodlist=new ArrayList<>() ;
                 int updataNum=0;
                 int addNum=0;
                 for(int i=1;i<list.size();i++){
                     TProdInfo tProdInfo=new TProdInfo();
                     List<Object> l=list.get(i);

                     String prdCode=String.valueOf(l.get(0)).trim().replace(" ","");
                     if(prdCode==null||prdCode.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:产品编号不能为空");
                     }

                     String prodLnNameVal=String.valueOf(l.get(1)).trim().replace(" ","");
                     if(prodLnNameVal==null||prodLnNameVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:产品线名称不能为空");
                     }
                     String prodLnName= backManageService.getParmCd("ProdServeType",prodLnNameVal);
                     if(prodLnName!=null){
                         tProdInfo.setProdLnName(prodLnName);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:产品线名称：'"+prodLnNameVal+"' 不 存在");
                     }
                     String prodBsName=String.valueOf(l.get(2)).trim().replace(" ","");
                     if(prodBsName==null||prodBsName.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:基础产品名称不能为空");
                     }
                     String prodSlName=String.valueOf(l.get(3)).trim().replace(" ","");
                     if(prodBsName==null||prodBsName.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:可售产品名称不能为空");
                     }
                     String orgCodeVal=String.valueOf(l.get(4)).trim().replace(" ","");
                     if(orgCodeVal==null||orgCodeVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:所属单位不能为空");
                     }
                     String orgCode= backManageService.getParmCd("posModel",orgCodeVal);
                     if(orgCode!=null){
                         tProdInfo.setOrgCode(orgCode);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:所属单位：'"+orgCodeVal+"' 不存 在");
                     }
                     String shelfInd=String.valueOf(l.get(5)).trim().replace(" ","");
                     if(shelfInd==null||shelfInd.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘是否上架’不能为空");
                     }
                     if("是".equals(shelfInd)){
                         tProdInfo.setShelfInd("0");
                     }else if("否".equals(shelfInd)){
                         tProdInfo.setShelfInd("1");
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:'是否上架 列'请输入‘ 是’ 或‘ 否’");
                     }
                     String shelfOperVal=String.valueOf(l.get(6)).trim().replace(" ","");
                     if("是".equals(shelfInd)){
                         if(shelfOperVal==null||shelfOperVal.length()==0){
                             return ResponseData.failInstance("第"+(i+1)+"行:“是否上架”字段填“是”时，上架方 不能为空");
                         }
                         String shelfOper= backManageService.getParmCd("ShelfKind",shelfOperVal);
                         if(shelfOper!=null){
                             tProdInfo.setShelfOper(shelfOper);
                         }else {
                             return ResponseData.failInstance("第"+(i+1)+"行:上架方：'"+shelfOperVal+"' 不 存在");
                         }

                     }else {
                         tProdInfo.setShelfOper("");
                     }

                     String goodsShelfVal=String.valueOf(l.get(7)).trim().replace(" ","");
                     if("是".equals(shelfInd)){
                         if(goodsShelfVal==null||goodsShelfVal.length()==0){
                             return ResponseData.failInstance("第"+(i+1)+"行:“是否上架”字段填“是”时，产品货架 不能为空");
                         }
                         String goodsShelf= backManageService.getParmCd("ProductShelf",goodsShelfVal);
                         if(goodsShelf!=null){
                             tProdInfo.setGoodsShelf(goodsShelf);
                         }else {
                             return ResponseData.failInstance("第"+(i+1)+"行:产品货架：'"+goodsShelfVal+"' 不存在");
                         }
                     }else {
                         tProdInfo.setGoodsShelf("");
                     }

                     String prodTpcdVal=String.valueOf(l.get(8)).trim().replace(" ","");
                     if("是".equals(shelfInd)){
                         if(prodTpcdVal==null||prodTpcdVal.length()==0){
                             return ResponseData.failInstance("第"+(i+1)+"行:“是否上架”字段填“是”时，产品大类 不能为空");
                         }
                         String prodTpcd= backManageService.getParmCd("ProductKind",prodTpcdVal);
                         if(prodTpcd!=null){
                             tProdInfo.setProdTpcd(prodTpcd);
                         }else {
                             return ResponseData.failInstance("第"+(i+1)+"行:产品大类：'"+prodTpcdVal+"' 不存 在");
                         }
                     }else {
                         tProdInfo.setProdTpcd("");
                     }

                     String priceStd=String.valueOf(l.get(9)).trim().replace(" ","");
                     String shelfDt=String.valueOf(l.get(10)).trim().replace(" ","");

                     if(shelfDt!=null&&shelfDt.length()>0&&shelfDt.length()!=10){
                         return ResponseData.failInstance("第"+(i+1)+"行:上架日期：'"+shelfDt+"'格式不正确！请使用yyyy-MM-dd格式");
                     }
                     tProdInfo.setPrdCode(prdCode);

                     tProdInfo.setProdBsName(prodBsName);
                     tProdInfo.setProdSlName(prodSlName);

                     tProdInfo.setPriceStd(BigDecimalUtil.getBigDecimal(priceStd));
                     tProdInfo.setShelfDt(shelfDt);

                     TProdInfo tProdInfo2=tProdInfoMapper.selectByPrimaryKey(prdCode);
                     if(tProdInfo2!=null){
                         tProdInfoMapper.updateByPrimaryKey(tProdInfo);
                         updataNum+=1;
                     }else {
                         tprodlist.add(tProdInfo);
                     }
                 }
                 if(!tprodlist.isEmpty()){
                     List<TProdInfo> unique=new ArrayList<>();
                     tprodlist.stream().filter(LogUtil.distinctByKey(s->s.getPrdCode())).forEach(s->unique.add(s));
                     tProdInfoMapper.excelImport(unique);
                     addNum=unique.size();
                 }
                 LogUtil.setLogOperation(ProdController.class,"prodImport",updataNum,addNum);
             } catch (Exception e) {
                 return ResponseData.failInstance("上传失败");
             }
         }
            return ResponseData.successInstance("上传成功");
        }


    /**
     * 获取产品线名称 下拉框
     */
    @GetMapping(path = "/getProdLnNameModel")
    public ResponseData getProdLnNameModel() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("ProdServeType"));
    }

    /**
     * 获取上架方 下拉框
     */
    @GetMapping(path = "/getShelfOperModel")
    public ResponseData getShelfOperModel() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("ShelfKind"));
    }

    /**
     * 获取 产品货架下拉框
     */
    @GetMapping(path = "/getGoodsShelfModel")
    public ResponseData getGoodsShelfModel() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("ProductShelf"));
    }

    /**
     * 获取 产品大类 下拉框
     */
    @GetMapping(path = "/getProdTpcdModel")
    public ResponseData getProdTpcdModel() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("ProductKind"));
    }

}
