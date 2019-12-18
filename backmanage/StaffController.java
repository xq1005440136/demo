package com.ccb.ark.backmanage;

import com.ccb.ark.common.LogOperation;
import com.ccb.ark.dto.StaffDTO;
import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.StaffMapper;
import com.ccb.ark.model.DictParmCfg;
import com.ccb.ark.model.Staff;
import com.ccb.ark.service.impl.BackManageService;
import com.ccb.ark.utils.ExcelUtil;
import com.ccb.ark.utils.LogUtil;
import com.ccb.ark.utils.StringUtils;
import com.ccb.ark.vo.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.regex.RegexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@Slf4j
@RequestMapping("/api/staff")
public class StaffController {
    private static final Logger logger = LoggerFactory.getLogger(StaffController.class);
    private static final String STAFF="staff";

    private static final String DATE=RegexUtil.asRegex("^[1-9]{1}[0-9]{3}[-][0-1]{1}[0-9]{1}");

    @Autowired
    private BackManageService backManageService;

    @Resource
    private StaffMapper staffMapper;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;


    /**
     * 人力 新增
     */
    @PostMapping(path = "/hrAdd" )
    public ResponseData addStaff(@RequestBody StaffDTO staffDto) {
        Staff staff = new Staff();
        BeanUtils.copyProperties(staffDto, staff);
        return backManageService.addStaff(staff);
    }
    /**
     * 人力 修改
     */
    @PostMapping(path = "/hrUpdate")
    public ResponseData updateStaff(@RequestBody StaffDTO staffDto) {
        Staff staff = new Staff();
        BeanUtils.copyProperties(staffDto, staff);
        Map<String,Object> map=new HashMap<>();
        map.put(STAFF,staff);
        return backManageService.update(STAFF,map);
    }
    /**
     * 人力 删除
     */
    @PostMapping(path = "/hrDelete")
    public ResponseData deleteStaff(String id) {
        return backManageService.delete(STAFF,id);
    }

    /**
     * 人力 条件查询
     */
    @PostMapping(path = "/hrSearch")
    public ResponseData getStaffList(@RequestBody StaffDTO staffDto) {
        Staff staff = new Staff();
        BeanUtils.copyProperties(staffDto, staff);
        return backManageService.getStaffList(staff);
    }

    /**
     * 人力 excel导入
     */
     @RequestMapping(value="/import",method= RequestMethod.POST)
     @LogOperation( logSystem = "人力", logType = "excel导入")
     public ResponseData hrImport(HttpServletRequest request, @RequestParam("file") MultipartFile file){
         // 判断文件是否为空
         if (!file.isEmpty()) {
             logger.info("文件描述" + file.getContentType());
             logger.info("文件描述" + file.getName());
             logger.info("文件原名" + file.getOriginalFilename());
             String filename=file.getOriginalFilename();
             //判断文件是否是Excel(2003、2007)
             String suffix = filename.substring(filename.lastIndexOf('.'),filename.length());
             // 2003后缀或2007后缀
             if (!(".xls".equals(suffix) || ".xlsx".equals(suffix))) {
                 return ResponseData.failInstance("文件格式不正确,请上传Excel文件!");
             }
             // 导入xlsx文件数据
             try {
                 List<List<Object>> list= ExcelUtil.readExcel(file.getInputStream(),suffix);
                 if(!"所属机构".equals(String.valueOf(list.get(0).get(0)))&& !"所属部门".equals(String.valueOf(list.get(0).get(1)))&&
                    !"姓名".equals(String.valueOf(list.get(0).get(2)))&& !"员编号工".equals(String.valueOf(list.get(0).get(3)))){
                     return ResponseData.failInstance("请上传人力表!");
                 }
                 List<Staff> stafflist=new ArrayList<>() ;
                 int updataNum=0;
                 int addNum=0;

                 for(int i=1;i<list.size();i++){
                     Staff staff=new Staff();
                     List<Object> l=list.get(i);

                     staff.setId(StringUtils.getUUID());
                     String areaVal=String.valueOf(l.get(0)).trim().replace(" ","");
                     if(areaVal==null||areaVal.length()==0){
                         return ResponseData.failInstance("Excel第"+(i+1)+"行:所属机构不能为空");
                     }
                     String area= backManageService.getParmCd("posModel",areaVal);
                     if(area!=null){
                         staff.setArea(area);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:所属机构：'"+areaVal+"' 不 存在");
                     }
                     String depart=String.valueOf(l.get(1));
                     if(depart==null||depart.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:所属部门不能为空");
                     }

                     String name=String.valueOf(l.get(2));
                     if(name==null||name.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:姓名不能为空");
                     }

                     String espace=String.valueOf(l.get(3)).trim().replace(" ","");
                     if(espace==null||espace.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:员工编号不能为空");
                     }
                     if(!espace.matches(RegexUtil.asRegex("[1-9](\\d+)?"))&&espace.length()!=8){
                         return ResponseData.failInstance("第"+(i+1)+"行:请输入8位数字");
                     }

                     String sex=String.valueOf(l.get(4));
                     if(sex==null||sex.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:性别不能为空");
                     }

                     String birthday=String.valueOf(l.get(5)).trim().replace(" ","");
                     if(birthday==null||birthday.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:'生日'不能为空");
                     }
                     if(birthday!=null&&birthday.length()>0&&birthday.length()!=10){
                         return ResponseData.failInstance("第"+(i+1)+"行:生日：'"+birthday+"' 格式不正确！请使用yyyy-MM-dd格式");
                     }
                     staff.setDepart(depart);
                     staff.setName(name);
                     staff.setEspace(espace);
                     staff.setSex(sex);
                     staff.setBirthday(birthday);
                     staff.setPost(String.valueOf(l.get(6)));
                     staff.setLevel(String.valueOf(l.get(7)));

                     String politicalVal=String.valueOf(l.get(8)).trim().replace(" ","");
                     if(politicalVal==null||politicalVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:政治面貌不能为空");
                     }
                     String political =backManageService.getParmCd("PoliticalStatus",politicalVal);
                     if(political!=null){
                         staff.setPolitical(politicalVal);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:请输入正确的政治面貌");
                     }

                     String degreeVal=String.valueOf(l.get(9)).trim().replace(" ","");
                     if(degreeVal==null||degreeVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:学历不能为空");
                     }
                     staff.setDegree(degreeVal);

                     String major=String.valueOf(l.get(10)).trim().replace(" ","");
                     staff.setMajor(major);

                     String university=String.valueOf(l.get(11)).trim().replace(" ","");
                     staff.setUniversity(university);

                     String stateVal=String.valueOf(l.get(12)).trim().replace(" ","");
                     if(stateVal==null||stateVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:当前状态不能为空");
                     }
                     String state= backManageService.getParmCd("HrStatus",stateVal);
                     if(state!=null){
                         staff.setJobState(state);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:当前状态：'"+stateVal+"' 不 存在");
                     }

                     staff.setLeaveReason(String.valueOf(l.get(13)));
                     staff.setLeaveTo(String.valueOf(l.get(14)));

                     String ccbDate=String.valueOf(l.get(15)).trim().replace(" ","");
                     if(ccbDate==null||ccbDate.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘入职时间’不能为空");
                     }else {
                         if(!ccbDate.matches(DATE)){
                             return ResponseData.failInstance("第"+(i+1)+"行:入职时间：'"+ccbDate+"'格式 不正确！请使用 yyyy-MM格式");
                         }
                         if(Integer.valueOf(ccbDate.substring(5))>12){
                             return ResponseData.failInstance("第"+(i+1)+"行:入职时间：'"+ccbDate+"'请使用正确的yyyy-MM格式");
                         }
                     }

                     String resignDate=String.valueOf(l.get(16)).trim().replace(" ","");
                     if(("离职".equals(stateVal)||"退休".equals(stateVal))&&(resignDate==null||resignDate.length()==0)){
                         return ResponseData.failInstance("第"+(i+1)+"行:当前状态为 ‘离职’ 或 ‘退休’时，''离退休时间不能为空");
                     }

                     if(resignDate!=null&&resignDate.length()!=0){
                         if(!resignDate.matches(DATE)){
                             return ResponseData.failInstance("第"+(i+1)+"行:离退休时间：'"+resignDate+"'格式不正确！请使用yyyy-MM格式");
                         }
                         if(Integer.valueOf(resignDate.substring(5))>12){
                             return ResponseData.failInstance("第"+(i+1)+"行:离退休时间：'"+resignDate+"'请使用正确的yyyy-MM格式");
                         }
                     }

                     String jobDate=String.valueOf(l.get(17)).trim().replace(" ","");
                     if(jobDate==null||jobDate.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘参加工作时间’不能为空");
                     }else {
                         if(!jobDate.matches(DATE)){
                             return ResponseData.failInstance("第"+(i+1)+"行:参加工作时间：'"+jobDate+"'格式 不正确！请使用 yyyy-MM 格式");
                         }
                         if(Integer.valueOf(jobDate.substring(5))>12){
                             return ResponseData.failInstance("第"+(i+1)+"行:参加工作时间：'"+jobDate+"'请 使用 正确的yyyy-MM格式");
                         }
                     }


                     staff.setCcbDate(ccbDate);
                     staff.setResignDate(resignDate);
                     staff.setJobDate(jobDate);
                     staff.setPos(String.valueOf(l.get(18)));

                     String posTypeVal=String.valueOf(l.get(19)).trim().replace(" ","");
                     if(posTypeVal==null||posTypeVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘职位类别’不能为空");
                     }else {
                         String posType= backManageService.getParmCd("PositionCata",posTypeVal);
                         if(posType!=null){
                             staff.setPosType(posTypeVal);
                         }else {
                             return ResponseData.failInstance("第"+(i+1)+"行:职位类别：'"+posTypeVal+"' 不存在");
                         }
                     }


                     String posSeqVal=String.valueOf(l.get(20)).trim().replace(" ","");
                     if(posSeqVal==null||posSeqVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘职位序列’不能为空");
                     }
                     String posSeq= backManageService.getParmCd("PositionSeq",posSeqVal);
                     if(posSeq!=null){
                         DictParmCfg d= dictParmCfgMapper.selectByPrimaryKey("PositionSeq",posSeq);
                         if(posTypeVal.equals(d.getParmTpnm())){
                             staff.setPosSeq(posSeq);
                         }else {
                             return ResponseData.failInstance("第"+(i+1)+"行:职位序列：'"+posSeqVal+"' 不在‘"+posTypeVal+"’分类下");
                         }
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:职位序列：'"+posSeqVal+"' 不存在");
                     }

                     String posLevel=String.valueOf(l.get(21)).trim().replace(" ","");
                     if(posLevel==null||posLevel.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:‘职级’不能为空");

                     }
                     if(!posLevel.matches("^[1-9](\\d+)?")){
                         return ResponseData.failInstance("第"+(i+1)+"行:职级：'"+posLevel+"'数据错误！请输入1-21之间数字");
                     }
                     if(Integer.valueOf(posLevel)>20){
                         return ResponseData.failInstance("第"+(i+1)+"行:职级：'"+posLevel+"'数据错误！请输入1-21之间数字");
                     }
                     staff.setPosLevel(posLevel);

                     String businessVal=String.valueOf(l.get(22)).trim().replace(" ","");
                     if(businessVal!=null&&businessVal.length()!=0){
                         String business= backManageService.getParmCd("businessModel",businessVal);
                         if(business!=null){
                             staff.setBusinessAbility(business);
                         }else {
                             return ResponseData.failInstance("第"+(i+1)+"行:专业能力：'"+businessVal+"' 不存 在");
                         }
                     }else {
                         staff.setBusinessAbility(businessVal);
                     }

                     String techAbilityVal=String.valueOf(l.get(23)).trim().replace(" ","");
                     if(techAbilityVal==null||techAbilityVal.length()==0){
                         staff.setTechAbility(techAbilityVal);
                     }else {
                         String techAbility =backManageService.getParmCd("abilityModel",techAbilityVal);
                         if(techAbility!=null){
                             staff.setTechAbility(techAbility);
                         }else {
                             return ResponseData.failInstance("第"+(i+1)+"行:技术能力：'"+techAbilityVal+"' 不存 在");
                         }
                     }

                     String categoryVal=String.valueOf(l.get(24)).trim().replace(" ","");
                     if(categoryVal==null||categoryVal.length()==0){
                         return ResponseData.failInstance("第"+(i+1)+"行:'员工类别'不能为空");
                     }
                     String category= backManageService.getParmCd("HrCategory",categoryVal);
                     if(category!=null){
                         staff.setCategory(categoryVal);
                     }else {
                         return ResponseData.failInstance("第"+(i+1)+"行:员工类别：'"+categoryVal+"' 不 存 在");
                     }

                     Staff staff2=staffMapper.findbyEspace(espace);
                     if(staff2!=null){
                         staff.setId(staff2.getId());
                         staffMapper.updateByPrimaryKey(staff);
                         updataNum+=1;
                     }else {
                         staff.setUpdateTime(new Date());
                         staff.setCreateTime(new Date());
                         stafflist.add(staff);
                     }
                 }
                 if(stafflist.size()>0){
                     List<Staff>  unique=new ArrayList<>();
                     stafflist.stream().filter(LogUtil.distinctByKey(s -> s.getEspace())).forEach(s->unique.add(s));
                     staffMapper.excelImport(unique);
                     addNum=unique.size();
                 }
                 LogUtil.setLogOperation(StaffController.class,"hrImport",updataNum,addNum);

             } catch (Exception e) {
                 return ResponseData.failInstance("数据错误");
             }
         }
            return ResponseData.successInstance("上传成功");
        }



    /**
     * 获取机构 下拉框
     */
    @GetMapping(path = "/getPosModel")
    public ResponseData getPosModel() {
        return ResponseData.successInstance(dictParmCfgMapper.getPosModel());
    }

    /**
     * 获取专业能力 下拉框
     */
    @GetMapping(path = "/getBusinessModel")
    public ResponseData getBusinessModel() {
        return ResponseData.successInstance(dictParmCfgMapper.getBusinessModel());
    }

    /**
     * 获取技术能力 下拉框
     */
    @GetMapping(path = "/getAbilityModel")
    public ResponseData getAbilityModel() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("abilityModel"));
    }
    /**
     * 获取 行内职等 下拉框
     */
    @GetMapping(path = "/getDutyGrade")
    public ResponseData getDutyGrade() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("DutyGrade"));
    }
    /**
     * 获取 政治面貌 下拉框
     */
    @GetMapping(path = "/getPoliticalStatus")
    public ResponseData getHrPoliticalStatus() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("PoliticalStatus"));
    }

    /**
     * 获取 学历 下拉框
     */
    @GetMapping(path = "/getEducationGrade")
    public ResponseData getEducationGrade() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("EducationGrade"));
    }

    /**
     * 获取 职位序列 下拉框
     */
    @GetMapping(path = "/getPositionSeq")
    public ResponseData getPositionSeq(String parmTpnm) {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("PositionSeq"));
    }
    /**
     * 获取 职位类别 下拉框
     */
    @GetMapping(path = "/getPositionCata")
    public ResponseData getPositionCata() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("PositionCata"));
    }

    /**
     * 获取 当前状态 下拉框
     */
    @GetMapping(path = "/getHrStatusModel")
    public ResponseData getHrStatusModel() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("HrStatus"));
    }
    /**
     * 获取 员工类别 下拉框
     */
    @GetMapping(path = "/getHrCategory")
    public ResponseData getHrCategory() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("HrCategory"));
    }
}
