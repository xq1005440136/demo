package com.ccb.ark.backmanage;

import com.ccb.alg.core.vo.ReqPageableVo;
import com.ccb.ark.dto.TDraftRecordDTO;
import com.ccb.ark.dto.TMeetingsDTO;
import com.ccb.ark.model.TDraftRecord;
import com.ccb.ark.model.TFile;
import com.ccb.ark.model.TMeetings;
import com.ccb.ark.repo.ITFileRepo;
import com.ccb.ark.service.impl.MeetingService;
import com.ccb.ark.service.storage.StorageException;
import com.ccb.ark.service.storage.StorageService;
import com.ccb.ark.utils.StringUtils;
import com.ccb.ark.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/meetings")
public class MeetingsController {

    @Autowired
    private StorageService storageService;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private ITFileRepo fileRepo;

    /**
     * APP-根据会议状态查询（近期会议status:0或历史会议status:1）
     * @param status
     * @return
     */
    @GetMapping("/getByStatus")
    public ResponseData getByStatus(@RequestParam String status, @RequestParam String pageNo, @RequestParam String pageSize) {
        ReqPageableVo.Pagination pagination = new ReqPageableVo.Pagination();
        pagination.setPageNo(Integer.valueOf(pageNo));
        pagination.setPageSize(Integer.valueOf(pageSize));
        return meetingService.getByStatus(status, pagination);
    }

    /**
     * APP && VUE后台管理-根据会议唯一ID查询
     * @param id
     * @return
     */
    @GetMapping(path = "/{id}")
    public ResponseData getByMeetingId(@PathVariable String id) {
        return meetingService.getByMeetingId(id);
    }

    /**
     * VUE-后台管理-根据会议唯一ID查询会议表决议案
     * @param id
     * @return
     */
    @GetMapping(path = "/{id}/decision")
    public ResponseData getDecisionBMeetingId(@PathVariable String id) {
        return meetingService.getDecisionByMeetingId(id);
    }

    /**
     * APP-根据会议唯一ID及议案类型，获取会议对应的上传文件列表
     */
    @GetMapping(path = "/{id}/{type}/attachments")
    public ResponseData getDraftsByMeetingIdAndType(@PathVariable String id, @PathVariable String type) {
        return meetingService.getDraftsByMeetingIdAndType(id, type);
    }

    /**
     * APP-根据会议唯一ID及会议类型，获取评论列表
     * @param id 会议唯一ID
     * @param type 会议类型
     * @return
     */
    @GetMapping(path = "/{id}/{type}/comments")
    public ResponseData getCommentsByMeetingIdAndType(@PathVariable String id, @PathVariable String type) {
        return meetingService.getCommentsByMeetingIdAndType(id, type);
    }

    /**
     * VUE后台管理-发送会议通知
     * @param id
     * @return
     */
    @GetMapping(path = "/{id}/push")
    public ResponseData sendPushByMeetingId(@PathVariable String id) {
        if(StringUtils.isEmpty(id)) {
            return ResponseData.failInstance("会议ID不能为空");
        }
        return meetingService.sendPushByMeetingId(id);
    }


    /**
     * VUE后台管理-对指定用户发送会议通知
     * @param pushData
     * @return
     */
    @PostMapping(path = "/push")
    public ResponseData sendPushByMeetingIdAndEmpCode(@RequestBody PushData pushData) {
        if(StringUtils.isEmpty(pushData.getId())) {
            return ResponseData.failInstance("会议ID不能为空");
        }
        return meetingService.sendPushByMeetingIdAndEmpCode(pushData.getId(), pushData.getEmpCodeList());
    }

    /**
     * APP-提交议案评论内容
     * @param comments
     * @return
     */
    @PostMapping("/comments")
    public ResponseData addComments(@RequestBody CommentsData comments) {
        return meetingService.addComments(comments);
    }

    /**
     * APP-议案投票
     * @param draftRecordDto
     * @return
     */
    @PostMapping("/vote")
    public ResponseData addVote(@RequestBody TDraftRecordDTO draftRecordDto) {
        TDraftRecord draftRecord = new TDraftRecord();
        BeanUtils.copyProperties(draftRecordDto, draftRecord);
        return meetingService.addVote(draftRecord);
    }

    /**
     * VUE后台管理-会议数据条件查询
     */
    @PostMapping("/search")
    public ResponseData getMeetingsList(@RequestBody TMeetingsDTO meetingDto) {
        TMeetings meeting = new TMeetings();
        BeanUtils.copyProperties(meetingDto, meeting);
        return meetingService.getMeetingsList(meeting);
    }

    /**
     * VUE后台管理-查询会议参与人的推送详情
     */
    @GetMapping(path = "/{id}/notifyDetail")
    public ResponseData getNotifyDetail(@PathVariable String id) {
        return meetingService.getMeetingNotifyDetail(id);
    }

    /**
     * VUE后台管理-新增会议
     */
    @PostMapping("/create")
    public ResponseData addMeetings(@RequestBody MeetingDetailData meeting) {
        return meetingService.addMeetings(meeting);
    }

    /**
     * VUE后台管理-会议数据更新
     */
    @PostMapping("/update")
    public ResponseData updateMeetings(@RequestBody MeetingDetailData meeting) {
        return meetingService.updateMeetings(meeting);
    }

    /**
     * VUE后台管理-会议(会议表决)数据更新
     */
    @PostMapping("/decision/update")
    public ResponseData updateDecisionMeetings(@RequestBody MeetingDetailData meeting) {
        return meetingService.updateDecisonMeetings(meeting);
    }
    /**
     * VUE后台管理-会议(会议表决)-某一条议案是否能投票通道关闭
     * @param id    议案Id
     */
    @GetMapping(path = "/decision/{id}/ifClose")
    public ResponseData judgeIfCouldCloseDraftChannel(@PathVariable String id) {
        return meetingService.judgeIfCouldCloseDraftChannel(id);
    }

    /**
     * VUE后台管理-会议归档，status：0 未归档；status：1 归档
     * @param id
     * @param status
     * @return
     */
    @GetMapping(path = "/archive/{id}/{status}")
    public ResponseData archiveMeetings(@PathVariable String id, @PathVariable String status) {
        return meetingService.archiveMeetings(id, status);
    }

    @GetMapping(path = "/delete/{id}")
    public ResponseData deleteMeetings(@PathVariable String id) {
        return meetingService.deleteMeetings(id);
    }

    /**
     * 后台管理-获取会议附件类型下拉框
     * @return
     */
    @GetMapping("/model")
    public ResponseData getMeetingsModel() {
        return meetingService.getByParmTpcd("meetingAttached");
    }

    /**
     * 后台管理-获取雇员下拉框
     * @return
     */
    @GetMapping("/people")
    public ResponseData getMeetingsPeople() {
        return meetingService.getMeetingsPeople();
    }

    /**
     * 后台管理-文件上传
     * @param files 上传文件
     * @return
     */
    @PostMapping("/file/upload")
    public ResponseData handleFileUpload(@RequestParam("files") MultipartFile[] files) {
        try {
            List<FileUploadData> idList = storageService.store(files);
            return ResponseData.successInstance(idList);
        } catch (StorageException e) {
            return ResponseData.failInstance(e.getMessage());
        }
    }

    /**
     * APP&后台-预览
     * @param id File唯一ID
     * @return
     */
    @GetMapping(path = "/file/preview/{id}")
    public ResponseEntity<Resource> previewAttachments(@PathVariable String id) throws Exception{

        Optional<TFile> fileEntity = fileRepo.findById(id);
        if (fileEntity.isPresent()) {
            String path= fileEntity.get().getUploadPath();
            String filename = fileEntity.get().getFileName();
            Resource file = storageService.returnResourceFile(path,filename);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + URLEncoder.encode(filename, "utf-8") + "\"");
            return ResponseEntity.ok().headers(headers).body(file);
        }
        return ResponseEntity.badRequest().body(null);

    }

    /**
     * 后台管理-删除附件，不物理删除
     * @param id
     * @return
     */
    @GetMapping(path = "/file/delete/{id}")
    public ResponseData handleFileDelete(@PathVariable String id) {
        if (StringUtils.isEmpty(id)) {
            return ResponseData.failInstance("要删除的File对应的id不能为空");
        }

        if (!fileRepo.existsById(id)) {
            return ResponseData.failInstance("要删除的File对应的id不存在: " + id);
        }

        fileRepo.deleteById(id);
        return ResponseData.successInstance("成功删除附件!");
    }
}
