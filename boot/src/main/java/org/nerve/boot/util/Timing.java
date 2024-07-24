package org.nerve.boot.util;

import java.text.DecimalFormat;
import java.util.Date;

/**
 * org.nerve.selenium.util
 * Created by zengxm on 2016/4/19 0019.
 */
public class Timing {
	private double start;
	private double used=-1;
	private Date startDate;
	private Date endDate;

	public Timing(){
		start= System.nanoTime();
		startDate=new Date();
	}

	public void stop(){
		this.used();
		this.endDate=new Date();
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	/**
	 * 默认返回的毫秒
	 * @return
	 */
	public double used(){
		if(used==-1)
			used= (System.nanoTime()-start)/1000000;
		return used;
	}

	public double getStart(){
		return start;
	}

	/**
	 *
	 * @return
	 */
	public long toMillSecond(){
		return (long)used();
	}

	public double toSecond(){
		return used()/1000;
	}

	/**
	 * 得到秒数的字符串描述
	 * @return
	 */
	public String toSecondStr(){
		return new DecimalFormat("#.###").format(toSecond());
	}

	public final static String SIMPLE_DATETIME = "yyyyMMdd";
}
