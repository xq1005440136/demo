package com.ccb.ark.backmanage;

import com.ccb.ark.mapper.DictParmCfgMapper;
import com.ccb.ark.mapper.TLogInfoMapper;
import com.ccb.ark.po.UserPo;
import com.ccb.ark.service.impl.BackManageService;
import com.ccb.ark.service.impl.UserService;
import com.ccb.ark.vo.ResponseData;
import com.ccb.ark.vo.UserData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static com.ccb.ark.utils.MessageConstants.PASSWORD_INCORRECT_ERROR;
import static com.ccb.ark.utils.MessageConstants.PASSWORD_UNKNOWN_ERROR;


@RestController
@Slf4j
@RequestMapping("/api/user")
public class UserPoController {

    @Autowired
    private UserService userService;

    @Autowired
    private BackManageService backManageService;

    @Autowired
    private PasswordEncoder encoder;

    @Resource
    private DictParmCfgMapper dictParmCfgMapper;

    @Resource
    private TLogInfoMapper tLogInfoMapper;

    /**
     * 用户个人设置 修改
     */
    @PostMapping(path = "/updateSetting")
    public ResponseData updatePersonalSetting(@RequestBody UserPo userPo) {
        if (userService.updateNameAndPasswordByPrimaryKey(userPo) > 0) {
            return ResponseData.successInstance("修改成功");
        } else {
            return ResponseData.failInstance("修改失败");
        }
    }

    /**
     * 用户信息 条件查询
     */
    @GetMapping(path = "/search")
    public ResponseData getUserPoList(UserPo userPo) {
        return backManageService.getUserPoList(userPo);
    }

    /**
     * 用户信息 新增
     */
    @PostMapping(path = "/add" )
    public ResponseData addUserPo(@RequestBody UserPo userPo) {
        return backManageService.addUser(userPo);
    }

    /**
     * 用户信息 修改
     */
    @PostMapping(path = "/update")
    public ResponseData updateUserPo(@RequestBody UserPo userPo) {
        Map<String,Object> map=new HashMap<>();
//        userPo.setPassword(encoder.encode(userPo.getPassword()));   //密码encode
        map.put("user",userPo);
        return backManageService.update("user",map);
    }

    /**
     * 用户信息 删除
     */
    @PostMapping(path = "/delete")
    public ResponseData deleteUserPo(String usercode) {
        return backManageService.delete("user",usercode);
    }


    /**
     * 获取机构 下拉框
     */
    @GetMapping(path = "/getDeptcodeModel")
    public ResponseData getPosModel() {
        return ResponseData.successInstance(dictParmCfgMapper.findByParmTpcd("meetingType"));
    }

    /**
     * APP- 修改个人密码
     */
    @PostMapping(path = "/updatePassword")
    public ResponseData updatePassword(@RequestBody UserData userData) {
        int result = userService.updatePassword(userData);
        if (result == PASSWORD_INCORRECT_ERROR) {
            return ResponseData.failInstance("原密码输入错误");
        } else if (result == PASSWORD_UNKNOWN_ERROR) {
            return ResponseData.failInstance("未知错误");
        } else {
            return ResponseData.successInstance("密码修改成功");
        }
    }
}
