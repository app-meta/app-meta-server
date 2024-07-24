package org.nerve.boot.web.auth;

import org.nerve.boot.domain.AuthUser;
import org.springframework.stereotype.Component;

@Component
public final class AuthHolder {

    private ThreadLocal<AuthUser> userLocal = new ThreadLocal<>();

    public void set(AuthUser user){
        userLocal.set(user);
    }

    public AuthUser get(){
        return userLocal.get();
    }

    public void clean(){
        userLocal.remove();
    }
}
