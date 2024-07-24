package org.appmeta.web.app

import org.appmeta.Role
import org.appmeta.component.AppConfig
import org.appmeta.domain.AppVersion
import org.appmeta.domain.AppVersionMapper
import org.appmeta.model.PageModel
import org.appmeta.service.AppVersionService
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.web.ctrl.BasicController
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


/*
 * @project app-meta-server
 * @file    org.appmeta.web.app.AppVersionCtrl
 * CREATE   2023年02月10日 12:00 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("app/version")
class AppVersionCtrl(
    private val mapper:AppVersionMapper,
    private val service: AppVersionService,
    private val config:AppConfig) : BasicController() {

    @PostMapping("upload", name = "更新前端资源")
    fun deploy(@RequestPart("file") file: MultipartFile, version: AppVersion) = result {
        val user = authHolder.get()
        if(!StringUtils.hasText(version.aid)){
            Assert.isTrue(user.hasRole(Role.ADMIN), "主程序资源更新仅限管理员")
        }

        version.uid = user.id

        it.data = service.saveVersionFile(version, file.inputStream)
        opLog("部署前端资源#${version.aid} ${version.version} ${version.summary}", version, Operation.IMPORT)

    }

    @RequestMapping("list", name = "版本列表（MAIN/小程序页面）")
    fun list(@RequestBody model: PageModel) = resultWithData {
        mapper.latestBy(model.aid, model.pid, config.resZipLimit)
    }
}