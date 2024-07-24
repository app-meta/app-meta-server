package org.nerve.boot.web;

/**
 * Created by zengxm on 2017/8/23.
 */
public interface IpDetector {
	String getIp();

	/**
	 * 返回当前请求的 UA
	 * @return
	 */
	String getUserAgent();
}
