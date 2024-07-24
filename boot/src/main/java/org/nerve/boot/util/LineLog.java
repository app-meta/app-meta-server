package org.nerve.boot.util;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 按照行记录日志的工具类
 *
 * com.zeus.util
 * Created by zengxm on 2017/9/3.
 */
public final class LineLog {
	private static String NEW_LINE = System.getProperty("line.separator");

	private StringBuilder sb = new StringBuilder();
	private SimpleDateFormat dateFormat;

	public LineLog(){
		this("yyyy-MM-dd HH:mm:ss:sss");
	}

	public LineLog(String formater){
		dateFormat = new SimpleDateFormat(formater);
	}

	/**
	 * 增加日志记录
	 * @param msg
	 * @return
	 */
	public LineLog add(String msg){
		sb.append(format(msg));
		return this;
	}

	public LineLog add(String msg, Throwable e){
		add(msg);
		sb.append(ExceptionUtils.getStackTrace(e));
		sb.append(NEW_LINE);

		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	private String format(String msg){
		return String.format("%s %s%s",dateFormat.format(new Date()), msg, NEW_LINE);
	}
}
