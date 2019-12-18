package com.ccb.ark.backmanage;

import com.ccb.ark.common.enums.NotificationStatus;
import com.ccb.ark.model.TNotification;
import com.ccb.ark.repo.ITNotificationRepo;
import com.ccb.ark.vo.NotificationData;
import com.ccb.ark.vo.ResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private ITNotificationRepo notificationRepo;
    /**
     * APP-更新推送通知状态
     * @param notification
     * @return
     */
    @PostMapping("/update")
    public ResponseData updateNotification(@RequestBody NotificationData notification) {
        if (!NotificationStatus.contains(notification.getStatus())) {
            return ResponseData.failInstance("输入的Notification状态值非法，合法的取值范围：0-未读，1-已读");
        }

        Optional<TNotification> notificationPo = notificationRepo.findById(notification.getId());
        if (!notificationPo.isPresent()) {
            return ResponseData.failInstance("未找到对应的Notification：" + notification.getId());
        }

        notificationPo.get().setReadStatus(Byte.valueOf(notification.getStatus()));
        notificationPo.get().setUpdateTime(new Date(System.currentTimeMillis()));
        notificationRepo.save(notificationPo.get());

        return ResponseData.successInstance("通知状态更新为：" + NotificationStatus.getName(notification.getStatus()));
    }
}
