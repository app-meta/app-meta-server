package org.appmeta.component

import org.appmeta.service.AccountService
import org.appmeta.tool.JWTTool
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.web.auth.UserLoader
import org.springframework.stereotype.Component

@Component
class CustomUserLoader(private val jwtTool: JWTTool,private val accountService: AccountService):UserLoader {

    override fun from(text: String?): AuthUser? {
        if(text == null)    return null
        val info = jwtTool.verify(text)

        return if(info.first == "") null else accountService.toAuthUser(info.first, info.second)
    }
}
