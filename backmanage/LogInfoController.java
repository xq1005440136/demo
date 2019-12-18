package com.ccb.ark.backmanage;


import com.ccb.alg.core.swaparea.DataSwapArea;
import com.ccb.alg.security.consts.AlgSecurityConsts;
import com.ccb.ark.common.MyResponseBodyAdvice;
import com.ccb.ark.dto.TLogInfoDTO;
import com.ccb.ark.mapper.TLogInfoMapper;
import com.ccb.ark.model.TLogInfo;
import com.ccb.ark.utils.PageBean;
import com.ccb.ark.utils.StringUtils;
import com.ccb.ark.vo.LogData;
import com.ccb.ark.vo.ResponseData;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/logInfo")
public class LogInfoController {
    @Resource
    private TLogInfoMapper tLogInfoMapper;

    /**
     * 日志查询
     */
    @PostMapping("/search")
    public ResponseData getLogInfoList(@RequestBody TLogInfoDTO logDto) {
        try {
            TLogInfo tLogInfo = new TLogInfo();
            BeanUtils.copyProperties(logDto, tLogInfo);
            Integer pageNum = tLogInfo.getPage();
            Integer pageSize = tLogInfo.getLimit();
            PageBean<TLogInfo> pageBean = new PageBean<>();
            pageBean.setPageNum(pageNum == null || pageNum < 0 ? 1 : pageNum);
            pageBean.setPageSize(pageSize == null || pageNum < 1 ? 10 : pageSize);
            pageBean.setOrderBy("log_time");
            pageBean.setOrderType("desc");
            PageHelper.startPage(pageBean.getPageNum(), pageBean.getPageSize(), pageBean.getOrderBy() + " " + pageBean.getOrderType());
            List<TLogInfo> tLogInfoList = tLogInfoMapper.getLogInfoList(tLogInfo);
            Page page = (Page) tLogInfoList;
            pageBean.setList(tLogInfoList);
            pageBean.setPages(page.getPages());
            pageBean.setTotal(page.getTotal());
            return ResponseData.successInstance(pageBean);
        } catch (Exception e) {
            return ResponseData.failInstance("查询失败");
        }
    }

    @PostMapping("/delete")
    public ResponseData deleteLogInfo(@RequestBody String[] ids) {
        try {
            if (ids.length != 0) {
                tLogInfoMapper.deleteLogInfoList(ids);
            }
            return ResponseData.successInstance("删除成功");
        } catch (Exception e) {
            return ResponseData.failInstance("删除失败");
        }
    }

    /**
     * APP关键日志记录
     */
    @PostMapping("/record")
    public ResponseData record(HttpServletRequest request, @RequestBody LogData logData) {
        final Map userInfo = DataSwapArea.getValue(AlgSecurityConsts.JWT_PAYLOAD_KEY_PAYLOAD, Map.class);
        String userId = (String) userInfo.get("userId");
        String userName = (String) userInfo.get("username");
        String userIp = new MyResponseBodyAdvice().getIpAddr(request);

        TLogInfo log = new TLogInfo();
        log.setId(StringUtils.getUUID());
        log.setUserIp(userIp);
        log.setUserName(userName + " - " + userId);
        log.setLogType(logData.getEvent());
        log.setLogSystem(logData.getPlatform());
        log.setLogResult(logData.getEvent());
        log.setLogTime(new Date(System.currentTimeMillis()));
        try {
            tLogInfoMapper.insert(log);
            return ResponseData.successInstance("日志记录成功");
        } catch (Exception e) {
            return ResponseData.failInstance("日志记录错误");
        }
    }

}
