package org.nerve.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class DefaultIpDetector implements IpDetector {

	@Override
	public String getIp() {
		HttpServletRequest request = request();
		return WebUtil.getIp(request);
	}

	protected HttpServletRequest request(){
		RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
		return ((ServletRequestAttributes)requestAttributes).getRequest();
	}

	@Override
	public String getUserAgent() {
		return WebUtil.getUserAgent(request());
	}
}
