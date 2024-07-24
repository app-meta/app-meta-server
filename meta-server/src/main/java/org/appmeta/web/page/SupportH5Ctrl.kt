package org.appmeta.web.page

import org.appmeta.component.AppConfig
import org.appmeta.domain.AppVersion
import org.appmeta.domain.AppVersionMapper
import org.appmeta.model.PageModel
import org.appmeta.service.AppVersionService
import org.nerve.boot.module.operation.Operation
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
@RequestMapping("page/h5")
class SupportH5Ctrl(
    private val mapper:AppVersionMapper,
    private val service: AppVersionService,
    private val config:AppConfig) : BasicPageCtrl() {

    @PostMapping("deploy", name = "更新前端资源")
    fun deploy(@RequestPart("file") file: MultipartFile, version: AppVersion) = _checkEditResult(version.pid) { _, user->
        version.uid = user.id

        opLog("部署前端资源#${version.aid} ${version.version} ${version.summary}", version, Operation.IMPORT)
        service.saveVersionFile(version, file.inputStream).also {
            updateDateById(version.pid)
        }
    }

    @RequestMapping("list", name = "版本列表（MAIN/小程序页面）")
    fun list(@RequestBody model: PageModel) = resultWithData {
        mapper.latestBy(model.aid, model.pid, config.resZipLimit)
    }
}