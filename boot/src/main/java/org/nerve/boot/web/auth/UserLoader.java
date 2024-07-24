package org.nerve.boot.web.auth;

import org.nerve.boot.domain.AuthUser;

public interface UserLoader {
    AuthUser from(String text) throws Exception;
}
