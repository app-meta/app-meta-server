package org.nerve.boot.web.auth;

public class AuthException extends RuntimeException {

    public AuthException(){
        super();
    }

    public AuthException(String msg){
        super(msg);
    }
}
