package com.platform.model;

public class Attachment {
	private Integer id;
	private String attachment_name;
	private String attachment_url;
	private Integer notice_id;
	
	public Integer getNotice_id() {
		return notice_id;
	}
	public void setNotice_id(Integer notice_id) {
		this.notice_id = notice_id;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getAttachment_name() {
		return attachment_name;
	}
	public void setAttachment_name(String attachment_name) {
		this.attachment_name = attachment_name;
	}
	public String getAttachment_url() {
		return attachment_url;
	}
	public void setAttachment_url(String attachment_url) {
		this.attachment_url = attachment_url;
	}
	

}
