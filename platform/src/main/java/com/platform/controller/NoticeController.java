package com.platform.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.platform.data.ApiResult;
import com.platform.data.ApiResultFactory;
import com.platform.data.ApiResultInfo;
import com.platform.model.BasicResponse;
import com.platform.rmodel.notice.AttachmentInfo;
import com.platform.rmodel.notice.CreateNoticeRequest;
import com.platform.rmodel.notice.CreateNoticeResponse;
import com.platform.rmodel.notice.NoticeDeleteRequest;
import com.platform.rmodel.notice.NoticeDetailRequest;
import com.platform.rmodel.notice.NoticeDetailResponse;
import com.platform.rmodel.notice.NoticeListResponse;
import com.platform.service.notice.NoticeService;
import com.platform.service.upload.QiNiuService;
import com.platform.util.DataTypePaserUtil;
import com.platform.util.NamedByTime;
import com.platform.util.RedisUtil;
import com.platform.util.RequestUtil;

@Controller
public class NoticeController {
	private Logger logger = Logger.getLogger(NoticeController.class);
	@Autowired
	private NoticeService noticeService;
	@Autowired
	private QiNiuService qiNiuService;

	public QiNiuService getQiNiuService() {
		return qiNiuService;
	}

	public void setQiNiuService(QiNiuService qiNiuService) {
		this.qiNiuService = qiNiuService;
	}

	public NoticeService getNoticeService() {
		return noticeService;
	}

	public void setNoticeService(NoticeService noticeService) {
		this.noticeService = noticeService;
	}

	@RequestMapping("notices")
	public @ResponseBody ApiResult getAllNotices(HttpServletResponse responseHttp) {
		responseHttp.setHeader("Access-Control-Allow-Origin", "*");
		logger.debug("start to get all notices");
		NoticeListResponse response = null;
		try {
			response = noticeService.getAllNoticesInfo();
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("fail to get response", e);
			return ApiResultFactory.getServerError();
		}
		// 判断服务是否正常返回
		if (response == null) {
			logger.error("response is null");
			return ApiResultFactory.getServerError();
		}

		// 通过返回码的数值，判断服务结果是否为正确的结果
		if (response.getCode() != 0) {

			logger.error("there are errors in service");
			return new ApiResult(response.getCode(), response.getMsg());
		}
		return new ApiResult(response);
	}

	@RequestMapping("notice/detail")
	public @ResponseBody ApiResult getNoticeDetail(HttpServletRequest requestHttp) {
		Map<String, String> requestParams = RequestUtil.getParameterMap(requestHttp);
		String[] paras = { "notice_id" };
		boolean flag = RequestUtil.validate(paras, requestParams);
		if (flag == false) {
			logger.error(ApiResultInfo.ResultMsg.RequiredParasError);
			return ApiResultFactory.getLackParasError();
		}
		NoticeDetailRequest request = new NoticeDetailRequest();
		request.setNotice_id(DataTypePaserUtil.StringToInteger(requestParams.get(paras[0])));

		NoticeDetailResponse response = null;
		try {
			logger.debug(" start to get notice detail using noticeService");
			response = noticeService.getNoticeDetail(request);
		} catch (Exception e) {
			// TODO: handle exception
			logger.error(ApiResultInfo.ResultMsg.ServerError);
			return ApiResultFactory.getServerError();
		}
		// 检查服务返回是否正常
		if (response == null) {
			logger.debug("fail to get the notice detail response");
			return ApiResultFactory.getServerError();
		}
		//// 通过返回码的数值，判断服务结果是否为正确的结果
		if (response.getCode() != 0) {

			logger.error("there are errors in service");
			return new ApiResult(response.getCode(), response.getMsg());
		}
		return new ApiResult(response);

	}

	@RequestMapping("notices/delete")
	private @ResponseBody ApiResult deleteNotices(HttpServletRequest requestHttp, HttpServletResponse responseHttp,
			@RequestParam(value = "deleteList[]", required = false) List<String> noticeIdList) throws IOException {

		NoticeDeleteRequest request = new NoticeDeleteRequest();
		Integer noticeIdListLength = noticeIdList.size();
		List<Integer> idList = new ArrayList<Integer>();
		for (int i = 0; i < noticeIdListLength; i++) {
			idList.add(DataTypePaserUtil.StringToInteger(noticeIdList.get(i)));
		}
		request.setNoticeIdList(idList);
		BasicResponse response = null;
		try {
			logger.debug(" start to delete notice  using noticeService");
			response = noticeService.deleteNotice(request);
		} catch (Exception e) {
			// TODO: handle exception
			logger.error(ApiResultInfo.ResultMsg.ServerError);
			return ApiResultFactory.getServerError();
		}
		// 检查服务返回是否正常
		if (response == null) {
			logger.debug("fail to delete the notice response");
			return ApiResultFactory.getServerError();
		}
		//// 通过返回码的数值，判断服务结果是否为正确的结果
		if (response.getCode() != 0) {

			logger.error("there are errors in service");
			return new ApiResult(response.getCode(), response.getMsg());
		}
		return new ApiResult(response);

	}

	@RequestMapping("notice/create")
	private @ResponseBody ApiResult createNotice(HttpServletRequest requestHttp, HttpServletResponse responseHttp,
			@RequestParam(value = "files[]", required = false) List<MultipartFile> files) throws IOException {
		Map<String, String> requestParams = RequestUtil.getParameterMap(requestHttp);
		CreateNoticeRequest request = new CreateNoticeRequest();
		String publisher = RedisUtil.get(requestParams.get("ticket"));
		request.setPublisher(publisher);
		request.setTitle(requestParams.get("noticeTitle"));
		request.setContent(requestParams.get("noticeContent"));
		List<String> fileNewNameList = new ArrayList<String>();
		List<AttachmentInfo> attachmentList = new ArrayList<AttachmentInfo>();
		if (!(files.isEmpty())) {
			for (int i = 0; i < files.size(); i++) {
				AttachmentInfo attachment = new AttachmentInfo();
				String orgFileName = files.get(i).getOriginalFilename();
				int index = orgFileName.lastIndexOf(".");
				String newName = NamedByTime.getQiNiuFileName() + "." + orgFileName.substring(index + 1);
				attachment.setAttachment_name(orgFileName);
				attachment.setAttachment_url(newName);
				attachmentList.add(attachment);
				fileNewNameList.add(newName);
			}
		} else {
			logger.debug("get the attachmentfiles error");
			return ApiResultFactory.getLackParasError();
		}
		try {
			qiNiuService.uploadAsyMutip(files, fileNewNameList);
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("qiNiuService excute error", e);
			return ApiResultFactory.getUploadFileError();
		}
		request.setAttachmentList(attachmentList);
		CreateNoticeResponse response = null;
		try {
			logger.debug("start to create a new notice using noticeService");
			response = noticeService.createNotice(request);
		} catch (Exception e) {
			// TODO: handle exception
			logger.error(ApiResultInfo.ResultMsg.ServerError);
			return ApiResultFactory.getServerError();
		}
		if (response == null) {
			logger.debug("fail to delete the notice response");
			return ApiResultFactory.getServerError();
		}
		//// 通过返回码的数值，判断服务结果是否为正确的结果
		if (response.getCode() != 0) {

			logger.error("there are errors in service");
			return new ApiResult(response.getCode(), response.getMsg());
		}
		return new ApiResult(response);

	}
}
