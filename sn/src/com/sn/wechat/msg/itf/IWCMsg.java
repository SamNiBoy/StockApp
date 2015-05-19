package com.sn.wechat.msg.itf;

public interface IWCMsg {
	public String getMsgType();
	public String getContent();
	public String getFromUserName();
	public String getToUserName();
	public String getMsgId();
	public String getCreateTime();
}
