package org.nerve.boot.exception;

/**
 * Created by zengxm on 2015/3/10.
 */
public class ServiceException extends RuntimeException {
	public ServiceException(){
		super();
	}

	public ServiceException(String message){
		super(message);
	}

	public ServiceException(Throwable cause){
		super(cause);
	}

	public ServiceException(String message, Throwable cause){
		super(message, cause);
	}
}
