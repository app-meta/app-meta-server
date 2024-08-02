package org.appmeta.web.page

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.appmeta.Channels.MOBILE
import org.appmeta.F
import org.appmeta.Role
import org.appmeta.component.PageContentUpdateEvent
import org.appmeta.component.PageDeleteEvent
import org.appmeta.component.PageNameUpdateEvent
import org.appmeta.domain.*
import org.appmeta.domain.Page.Companion.SERVER
import org.appmeta.model.*
import org.appmeta.service.*
import org.nerve.boot.Result
import org.nerve.boot.enums.Fields
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.web.auth.AuthConfig
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths


/*
 * @project app-meta-server
 * @file    org.appmeta.web.app.AppPageCtrl
 * CREATE   2023年02月13日 12:58 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@RestController
@RequestMapping("page")
class PageCtrl(
    private val appAsync: AppAsync,
    private val eventPublisher: ApplicationEventPublisher,
    private val appM:AppMapper,
    private val documentM:DocumentMapper,
    private val documentS:DocumentService,
    private val accountM:AccountMapper,
    private val accountHelper: AccountHelper,
    private val dataBatchM:DataBatchMapper,
    private val authConfig: AuthConfig,
    private val dataS:DataService,
    private val linkS:PageLinkService,
    private val service:PageService) : BasicPageCtrl() {

    private val modifyFields = arrayListOf(
        F.ACTIVE,
        F.MAIN,
        F.SEARCH,
        Fields.NAME.value(),
        F.SERVICE_AUTH,
        F.EDIT_AUTH
    )


    @PostMapping("create", name = "新建页面")
    fun create(@RequestBody page: Page) = resultWithData {
        val user = authHolder.get()

        appM.withCache(page.aid)?: throw Exception("应用#${page.aid} 不存在")

        //对于 server 类型，每个应用只能创建一个
        if(page.template == SERVER){
            if(pageM.selectCount(QueryWrapper<Page>().eq(F.AID, page.aid).eq(F.TEMPLATE, SERVER)) > 0)
                throw Exception("每个应用仅能创建唯一的后端服务")
        }

        page.of(user)

        page.addOn = System.currentTimeMillis()
        pageM.insert(page)

        cacheRefresh.pageList()
        ""
    }

    @PostMapping("modify", name = "修改页面属性")
    fun modifyField(@RequestBody model: FieldModel):Result {
        Assert.isTrue(modifyFields.contains(model.key), "${model.key} 字段不允许通过该接口修改")

        return _checkEditResult(model.id) { page, _ ->
            val wrapper = UpdateWrapper<Page>()

            if(model.key == F.MAIN){
                if(model.value == true){
                    //修改其他页面为非主要
                    pageM.update(null, wrapper.eq(F.AID, page.aid).eq(F.MAIN, true).set(F.MAIN, false))
                    page.main   = true
                }
                else{
                    page.main   = false
                }

                pageM.updateById(page)

                opLog("修改#${model.id}/${page.name} 对于应用（${page.aid}）的主页属性为 ${model.value}", page)
            }
            else {
                wrapper.eq(F.ID, model.id)
                pageM.update(null, wrapper.set(model.key, model.value))

                opLog("修改页面#${model.id} 的属性 ${model.key}=${model.value}", null)

                if(model.key == Fields.NAME.value()){
                    page.name = model.value.toString()
                    if(logger.isDebugEnabled) logger.debug("发布 #${page.id} 名称变动事件 PageNameChangeEvent")
                    eventPublisher.publishEvent(PageNameUpdateEvent(page))
                }
            }

            cacheRefresh.pageList()
        }
    }

    @PostMapping("delete", name = "删除页面")
    fun delete(@RequestBody model: IdModel) = result {
        val page = pageM.selectById(model.id)
        val user = authHolder.get()
        Assert.isTrue(user.hasRole(Role.ADMIN) || page.uid == user.id, "您没有权限删除该页面")

        pageM.deleteById(model.id)

        eventPublisher.publishEvent(PageDeleteEvent(model.id))
        //对于小程序，则删除文件夹，但是保留了历史文件，可用于还原
        if(page.template == Page.H5){
            val location = "${appConfig.resAppPath}/${page.aid}-${page.id}"
            FileUtils.deleteDirectory(Paths.get(location).toFile())
            logger.info("删除H5小程序目录 $location")
        }
        opLog("删除页面#${page.id}/${page.name}", page, Operation.DELETE)

        cacheRefresh.pageList()
    }

    @PostMapping("list", name = "页面列表")
    fun list(@RequestBody model: QueryModel) = resultWithData { service.list(model) }

    private fun _authableList(model: QueryModel) = authHolder.get().let { user->
        service.list(model)
            .filter { authHelper.checkService(it, user) }
            .onEach {
                // 标记是否已经关注（没有更多的字段了哈哈，active 为筛选字段，故可以用）
                it.active = linkS.check("${it.id}", user.id)
            }
    }

    /**
     * 同时返回 app 信息及已经授权的页面清单
     */
    @PostMapping("authable", name = "应用详情及已授权页面")
    fun listOfAuthableByApp(@RequestBody model: IdStringModel) = resultWithData {
        val app = appM.withCache(model.id)?: throw Exception("应用[${model.id}]不存在")

        mapOf(
            "app"   to app,
            "pages" to _authableList(
                QueryModel().also {
                    it.form["EQ_aid"]       = app.id
                    it.form["EQ_active"]    = true
                }
            )
        )
    }

    @PostMapping("list-authable", name = "页面列表（已授权）")
    fun listOfAuthable(@RequestBody model: QueryModel) = resultWithData {
        model.form["EQ_active"] = true
        //不显示后端服务、FaaS
        model.form["NOT_template"] = arrayOf(Page.SERVER, Page.FAAS)

        _authableList(model)
    }

    @PostMapping("list-editable", name = "页面列表（维护授权）")
    fun listOfEditable(@RequestBody model: QueryModel) = resultWithData {
        val user = authHolder.get()
        model.form["EQ_active"] = true
        model.form["NE_uid"] = user.id

        service.list(model)
            .filter { authHelper.checkEdit(it, user) }
            .onEach {p->
                appM.withCache(p.aid)?.let { p.content = "${it.id}/${it.name}" }
            }
    }

    /**
     * 获取页面的详细信息，如果具备访问权限，则加载 content
     *
     * 同时进行以下处理：
     *  1、返回文档列表
     *  2、进行 onLaunch 处理（记录应用的访问次数）
     *
     *  此接口通常用于一次性返回所有数据的场景（如移动终端，避免多次请求）
     */
    @RequestMapping("detail", name = "获取页面内容")
    fun detail(@RequestBody model:PageModel) = resultWithData {
        val page = pageM.selectById(model.id)?: return@resultWithData null
        val user = authHolder.get()
        model.channel = getChannel()

        val canServie = page.active && authHelper.checkService(page, user)

        appAsync.afterLaunch(
            model.also {
                it.channel = getChannel()
                it.pid = "${page.id}"
                it.aid = page.aid
            },
            user.id,
            requestIP
        )

        //最简返回
        val map= mutableMapOf(
            F.ID        to page.id,
            F.AID       to page.aid,
            F.NAME      to page.name,
            F.TEMPLATE  to page.template,
            F.ACTIVE    to page.active,
            F.CONTENT   to if(canServie) { service.buildContent(page, true) } else "",
            "documents" to if(canServie) documentS.listByPage("${page.id}") else null
        )
        //对于移动终端，直接返回 User 信息（减少请求次数）
        if(model.channel == MOBILE && canServie)
            map[F.USER] = accountHelper.buildUserBean(request.getHeader(authConfig.tokenName))
        map
    }

    /**
     * 如果 id 指定则加载特定的页面
     */
    @PostMapping("main", name = "获取应用的主页面")
    fun findMain(@RequestBody model: PageModel) = resultWithData {
        val wrapper = QueryWrapper<Page>().eq(F.AID, model.aid)
        if(StringUtils.hasText(model.pid))
            wrapper.eq(F.ID, model.pid)
        else
            wrapper.eq(F.MAIN, true)

        PageResultModel(appM.withCache(model.aid), pageM.selectOne(wrapper))
    }

    /**
     * 通常是 PC 端使用该接口
     */
    @GetMapping("content", name = "获取页面内容")
    fun loadContent(id:Long) = _checkServiceResult(id) { page, _ ->
        if(page.template == Page.H5){
            val location = "${appConfig.resAppPath}/${page.aid}-${id}/${appConfig.home}"
            val resource = if("file".equals(appConfig.resProtocol, true)) FileSystemResource(location) else ClassPathResource(location)

            if(!resource.exists()) {
                logger.error("小程序[${page.name}]未部署资源文件 $resource")
                throw Exception("小程序[${page.name}]未部署资源文件，请联系管理员")
            }

            if(logger.isDebugEnabled)   logger.debug("小程序部署地址：$resource")
            "${request.contextPath}/${appConfig.resAppContext}/${page.aid}-${id}/${appConfig.home}"
        }
        else
            service.buildContent(page, false)
    }

    @PostMapping("content", name = "更新页面内容")
    fun updateContent(@RequestBody model: FieldModel) = _checkEditResult(model.id) { page, _ ->
        pageM.update(
            null,
            UpdateWrapper<Page>().eq(F.ID, model.id)
                .set(F.CONTENT, model.value)
                .set(F.UPDATE_ON, System.currentTimeMillis())
        )
        // 刷新后端服务配置
        if(page.template == SERVER) cacheRefresh.pageServer(page.aid)

        eventPublisher.publishEvent(PageContentUpdateEvent(page))

        opLog("更新页面#${model.id} 的内容", page)
    }

    /**
     *
     */
    @PostMapping("query", name = "内容检索")
    fun query(@RequestBody model:QueryModel) = result {
        model.form["EQ_${F.SEARCH}"] = true

        it.data = service.query(model)
        if(!model.countOnly)
            it.total = model.pagination.total
    }

    /*
    ---------------------------------------------------------------------------------------------------------------
    附件相关
    ---------------------------------------------------------------------------------------------------------------
     */

    @PostMapping("document-list", name = "获取页面附件列表")
    fun documentList(@RequestBody model:IdStringModel) = resultWithData {
        documentS.listByPage(model.id)
    }

    @PostMapping("document-upload", name = "上传页面附件")
    fun documentUpload(@RequestParam("file") file: MultipartFile, model:PageModel)  = _checkEditResult(model.pid) { page, user->
        val isOver = model.id != null

        val document = if(isOver)
            documentS.overwrite(model, file.inputStream, file.originalFilename!!)
        else
            documentS.store(file.inputStream, file.originalFilename!!) { doc->
                doc.of(user)
                doc.aid = page.aid
                doc.pid = "${page.id}"
            }

        cacheRefresh.pageDocumentList(model.pid)

        opLog("${if(isOver) "覆盖" else "上传"}附件 ${model.toText()} >> ${document.path}", document, Operation.IMPORT)
        document
    }

    @PostMapping("document-del", name = "删除页面附件")
    fun documentDel(@RequestBody model: PageModel) = _checkEditResult(model.pid) { _, _ ->
        val doc = documentS.delete(model.id!!)

        opLog("删除附件 #${doc.id}/${doc.filename}", doc, Operation.DELETE)
    }

    @PostMapping("document-edit", name = "修改附件信息")
    fun documentModify(@RequestBody doc:Document) = _checkEditResult(doc.pid) { _, _ ->
        val q = UpdateWrapper<Document>().eq(F.ID, doc.id).eq(F.PID, doc.pid)
        if(StringUtils.hasText(doc.filename))
            q.set("filename", doc.filename)
        if(StringUtils.hasText(doc.summary))
            q.set(F.SUMMARY, doc.summary)
        documentS.update(null, q)

        cacheRefresh.pageDocumentList(doc.pid)

        opLog("修改附件信息 ${q.targetSql}", doc, Operation.MODIFY)
    }

    @RequestMapping("document-download", name = "下载页面附件")
    fun documentDownload(model: PageModel, response:HttpServletResponse) {
        val page = pageM.selectById(model.pid)
        val user = authHolder.get()
        if(!authHelper.checkService(page, user))  throw Exception(HttpStatus.UNAUTHORIZED.name)

        val doc = documentS.getById(model.id)
        val isPreview = model.aid == "preview"
        logger.info("${user.showName} 尝试${if(isPreview) "预览" else "下载"}文件 ${doc.filename}")

        val path = Paths.get(doc.path)
        if(Files.notExists(path))   throw Exception("附件 ${doc.filename}（${doc.path}）不存在")

        downloadFile(response, path.toFile(), doc.filename) {
            if(!isPreview)  documentM.afterDownload(doc.id)
        }
    }


    /*
    ---------------------------------------------------------------------------------------------------------------
    数据批次相关
    ---------------------------------------------------------------------------------------------------------------
     */
    @PostMapping("batch-list", name = "获取批次清单")
    fun batchList(@RequestBody model:PageModel) = resultWithData {
        dataBatchM.selectList(QueryWrapper<DataBatch>().eq(F.PID, model.pid).eq(F.AID, model.aid).orderByDesc(F.ID))
    }

    @PostMapping("batch-clear", name = "删除批次数据")
    fun batchDelData(@RequestBody model: IdModel) = resultWithData {
        val batch = dataBatchM.selectById(model.id) ?:  return@resultWithData -1
        if(!StringUtils.hasText(batch.pid) || !batch.active)
            return@resultWithData -1

        val page = pageM.selectById(batch.pid)
        val user = authHolder.get()
        if(authHelper.checkService(page, user)) {
            logger.info("${user.showName} 请求删除批次#${model.id} （PID=${batch.pid}） 的应用数据")

            dataS.deleteWithBatch(batch)
        }
        else
            -1
    }
}