package org.appmeta.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import org.apache.commons.io.FileUtils
import org.appmeta.*
import org.appmeta.component.AppConfig
import org.appmeta.domain.*
import org.appmeta.model.AppModel
import org.appmeta.tool.FileTool
import org.nerve.boot.Const
import org.nerve.boot.Const.AT
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.cache.CacheManage
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.exception.ServiceException
import org.nerve.boot.module.operation.Operation
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.DateUtil
import org.nerve.boot.util.MD5Util
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.AntPathMatcher
import org.springframework.util.Assert
import org.springframework.util.FileSystemUtils
import org.springframework.util.StreamUtils
import org.springframework.util.StringUtils
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipFile


/*
 * @project app-meta-server
 * @file    org.appmeta.service.AppService
 * CREATE   2022年12月06日 13:33 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class AppLinkService(
    private val appAsync:AppAsync,
    private val appM:AppMapper):BaseService<AppLinkMapper, AppLink>() {

    private fun buildQ(link: AppLink) = Q().eq(F.AID, link.aid).eq(F.UID, link.uid).eq(F.TYPE, link.type)

    /**
     * 根据用户id跟类型查询对应的应用列表
     */
    fun byUid(uid: String, type:Int, formIndex:Int=0, size:Int = 20):List<App> {
        val q = Q().eq(F.UID, uid).eq(F.TYPE, type)
        q.last("LIMIT ${formIndex},$size")

        return list(q).mapNotNull { appM.withCache(it.aid) }
    }

    fun exist(link: AppLink) = count(buildQ(link)) > 0L

    fun create(link: AppLink){
        if(org.apache.commons.lang3.StringUtils.isAnyEmpty(link.aid, link.uid))
            throw ServiceException("关联的应用、用户不能为空")

        if(count(buildQ(link)) > 0L)    return
//            throw ServiceException("重复关联")

        save(link)
        logger.info("创建 ${link.uid} 与应用#${link.aid} 的关联（类型=${link.type}）")

        if(link.type == AppLink.MARK)       appAsync.afterMark(link.aid)
        else if(link.type == AppLink.LIKE)  appAsync.afterLike(link.aid)
    }

    fun remove(link: AppLink) {
        if(link.id>0L){
            if(StringUtils.hasText(link.uid))
                remove(Q().eq(F.UID, link.uid).eq(F.ID, link.id))
            else
                removeById(link.id)

            logger.info("按ID#${link.id} 删除关联")
        }
        else{
            remove(buildQ(link))
            logger.info("删除 ${link.uid} 与应用#${link.aid} 的关联（类型=${link.type}）")

            if(link.type == AppLink.MARK)   appAsync.afterMark(link.aid, true)
        }
    }
}


@Service
class AppVersionService(private val config:AppConfig, private val settingService: SettingService):BaseService<AppVersionMapper, AppVersion>() {
    private val FAIL = "文件检验不通过"
    private val SIGN = "SIGN"

    /**
     * 返回 SIGN 文件中的清单列表
     */
    fun checkZipFile(file:File): List<String> {
        ZipFile(file).use { zipFile->
            //必须包含首页文件
            zipFile.getEntry(config.home)?: throw Exception("资源包下必须包含 ${config.home} 文件")

            val signEntry = zipFile.getEntry(SIGN) ?: throw Exception(FAIL)

            val zis = zipFile.getInputStream(signEntry)
            val sign = StreamUtils.copyToString(zis, StandardCharsets.UTF_8)
            if(sign.trim().isEmpty())   throw Exception(FAIL)

            val c = Calendar.getInstance()
            var fileMd5 = MD5Util.encode("${c[Calendar.YEAR]}-${c[Calendar.MONTH] + 1}-${c[Calendar.DAY_OF_MONTH]}")

            val signList = sign.split("\n").map { it.trim().split(" ") }
            signList.forEach { v->
                val entry = zipFile.getEntry(v[0])
                val code = if(entry == null) MD5Util.encode(v[0]) else MD5Util.encode(zipFile.getInputStream(entry))
                fileMd5 = MD5Util.encode(code + fileMd5)
                if(fileMd5 != v[1])
                    throw Exception("${FAIL}:${v[0]}")
            }

            zipFile.close()
            return signList.map { it[0] }
        }
    }

    /**
     * 持久化新的资源版本文件包
     * 如果 id 为空，则为更新主程序的资源文件
     */
    fun saveVersionFile(ver: AppVersion, fis:InputStream): String {
        val id = if(StringUtils.hasText(ver.aid)) {
            if(!StringUtils.hasText(ver.pid))    throw Exception("请指定小程序 ID")
            "${ver.aid}-${ver.pid}"
        }
        else
            EMPTY

        val isMicroPage = id.isNotEmpty()
        val root = Paths.get(config.resHistoryPath, id, "${DateUtil.getDateTimeSimple()}.zip")

        val versionFile = root.toFile()
        logger.debug("$root $versionFile")
        if(!Files.exists(root.parent))
            Files.createDirectories(root.parent)

        FileUtils.copyToFile(fis, versionFile)

        //小程序不做检查
        if(!isMicroPage && config.resZipCheck){
            try{
                checkZipFile(versionFile)
            }
            catch (e:Exception) {
                logger.info("资源包校验失败：{}", e.message)
                FileUtils.deleteQuietly(versionFile)
                throw e
            }
        }

        logger.info("版本文件保存到 $root")

        ver.path = versionFile.path
        ver.size = versionFile.length()
        if(!StringUtils.hasText(ver.version))   ver.version = H.buildVersion()
        ver.addOn = System.currentTimeMillis()

        baseMapper.insert(ver)

        val msg = unzipToDeploy(id, versionFile)

        // 对于小程序，需要注入特定的内容
        if(isMicroPage){
            FileTool.injectText(
                Paths.get(config.resAppPath, id, config.home).toFile(),
                0,
                settingService.value(S.APP_MICRO_INJECT)
            )
        }

        return msg
    }

    /**
     *
     */
    fun unzipToDeploy(id:String, originFile:File, linkStr:String="<br>"): String {
        val targetPath = with(id) {
            var dir = config.resPath
            if (StringUtils.hasText(id))
                dir = "./${config.resAppPath}/${id}"

            logger.info("资源解压到 {}", dir)
            Paths.get(dir)
        }

        FileSystemUtils.deleteRecursively(targetPath)
        return FileTool.unzip(originFile, targetPath).joinToString(linkStr)
    }
}

/**
 * 应用权限管理模块
 */
@Service
class AppRoleService(private val roleM:AppRoleMapper, private val linkM:AppRoleLinkMapper) {
    val logger = LoggerFactory.getLogger(javaClass)

    private val UUID_REGEX = Regex("[0-9a-zA-Z_.]+")
    private fun RQ(aid: String, uuid: String?=null) = QueryWrapper<AppRole>().eq(F.AID, aid).eq(uuid!=null, F.UUID, uuid)
    private fun LQ(aid: String, uid:String) = QueryWrapper<AppRoleLink>().eq(F.AID, aid).eq(F.UID, uid)

    private fun roleCacheKey(aid: String, uid: String) = "APP-ROLE-$aid-$uid"

    /**
     * 清空相应的缓存
     */
    fun cleanCache(aid: String, uid: String?=null) =
        CacheManage.clearWithPrefix("${aid}$AT${if(uid != null) "${uid}${AT}" else ""}")

    fun roleList(aid: String) = roleM.selectList(RQ(aid))

    fun addRole(role:AppRole) {
        Assert.hasText(role.aid, "应用ID不能为空")
        Assert.hasText(role.uuid, "角色ID不能为空")

        //角色ID只能：0-9,a-z,A-Z,_,. 等组合
        Assert.isTrue(UUID_REGEX.matches(role.uuid), "角色ID只能包含数字、字母、下划线、英文点")

        //判断是否存在重复
        Assert.isTrue(roleM.selectCount(RQ(role.aid, role.uuid)) <= 0L, "应用（${role.aid}）下已存在角色${role.uuid}")

        if(role.addOn <=0)
            role.addOn = System.currentTimeMillis()

        roleM.insert(role)
        logger.info("新增应用（${role.aid}）角色：${role.uuid}/${role.name}-${role.summary}")

        cleanCache(role.aid)
    }

    fun removeRole(aid:String, uuid:String):Int =
        roleM.delete(RQ(aid, uuid)).also {
            if(it>0)    cleanCache(aid)
            logger.info("删除应用（${aid}）角色：${uuid}（结果=${it}）")
        }

    /**
     * 仅能更新授权信息、名称、描述信息
     * 返回更新数量
     */
    fun updateRole(role: AppRole) =
        UpdateWrapper<AppRole>().eq(F.AID, role.aid).eq(F.UUID, role.uuid).let { q->
            q.set(StringUtils.hasText(role.name), F.NAME, role.name)
            q.set(StringUtils.hasText(role.auth), F.AUTH, role.auth)
            q.set(StringUtils.hasText(role.summary), F.SUMMARY, role.summary)

            roleM.update(q).also {
                if(it>0)    cleanCache(role.aid, role.uuid)
            }
        }

    /**
     * 查询某个用户的应用权限
     */
    fun loadLink(aid: String, uid: String) = linkM.load(aid, uid)

    fun updateLink(link: AppRoleLink) {
        Assert.hasText(link.aid, "应用ID不能为空")
        Assert.hasText(link.uid, "用户ID不能为空")

        val old = linkM.load(link.aid, link.uid)
        if(old != null){
            if(old.role == link.role) return

            linkM.update(UpdateWrapper<AppRoleLink>().eq(F.AID, link.aid).eq(F.UID, link.uid).set(F.ROLE, link.role))
        }
        else{
            linkM.insert(link)
        }
        logger.info("分配${link.uid}在应用${link.aid}下的角色：${link.role}")

        cleanCache(link.aid, link.uid)
        CacheManage.clear(roleCacheKey(link.aid, link.uid))
    }

    /**
     * 判断是否存在指定路径的访问权限
     */
    fun checkAuth(aid: String, user: AuthUser, url:String):Boolean = CacheManage.get(
        listOf(aid, user.id, user.ip, url).joinToString(AT),    // "${user.id}${AT}${aid}${AT}${url}"
        {
            val roles = roleM.selectList(RQ(aid))
            // 如果未配置任何角色，则视为不作拦截
            if(roles.isEmpty()) return@get true

            val links = linkM.load(aid, user.id)?: return@get false
            val urls = mutableSetOf<String>()
            links.roleList().also { userRoles->
                roles.forEach { r->
                    if(userRoles.contains(r.uuid))  {
                        val ips = r.ipList()
                        // 判断是否为有效的IP范围（精准匹配）
                        if(ips.isEmpty() || ips.contains(user.ip))
                            urls.addAll(r.authList())
                    }
                }
            }
            if(logger.isDebugEnabled)   logger.debug("${user.id}(IP=${user.ip}) 在应用${aid}内的授权：${urls}")
            AntPathMatcher().let { m-> urls.any { m.match(it, url) } }
        },
        3600*10
    )

    /**
     * 返回用户在指定应用下的：
     *  1、应用角色列表（List）
     *  2、授权URL
     *  3、用户被分配的角色列表（String格式，以英文逗号隔开）
     */
    fun loadRoleAndAuthOfUser(aid: String, uid: String): Triple<List<String>, List<String>, String> = CacheManage.get(roleCacheKey(aid, uid)) {
        val roles = roleM.selectList(RQ(aid))
        if(roles.isEmpty())
            Triple(emptyList(), emptyList(), EMPTY)
        else{
            //计算授权的地址
            val link = linkM.load(aid, uid)
            Triple(
                roles.map { it.uuid },
                if(link == null)
                    emptyList()
                else
                    link.roleList().let { userRoles->
                        val auths = mutableListOf<String>()
                        roles.filter { userRoles.contains(it.uuid) }.forEach { auths.addAll(it.authList()) }
                        auths
                    }
                ,
                link?.role ?: EMPTY
            )
        }
    }

    /**
     * 获取用户的角色列表
     * 返回的是字符串
     */
    fun loadRoleOfUser(aid: String, uid: String) = loadRoleAndAuthOfUser(aid, uid).third
}

@Service
class AppService(
    private val pageM:PageMapper,
    private val dataM:DataMapper,
    private val refresh: CacheRefresh,
    private val cfg: AppConfig,
    private val propertyM:AppPropertyMapper) : BaseService<AppMapper, App>() {

    fun detailOf(id: String): Map<String, Any> {
        val app = getById(id) ?: throw ServiceException("应用#${id}不存在")

        return mapOf(
            "app" to app,
            "property" to propertyM.selectById(id)
        )
    }

    @Transactional
    fun create(model: AppModel) {
        val (app, property) = model

        if (!Regex(cfg.appIdRegex).matches(app.id))
            throw Exception("应用编号[${app.id}]不合规，必须是长度在3-20间的字母数字下划线组合")
        if (count(Q().eq(F.ID, app.id)) > 0) throw ServiceException("应用编号[${app.id}]已存在")

        app.addOn = System.currentTimeMillis()
        save(app)

        property.bind(app)
        propertyM.insert(property)
    }

    /**
     * 未来考虑增加数据更新情况
     *
     * 参考 https://blog.csdn.net/Fupengyao/article/details/118599666
     */
    fun update(model: AppModel) {
        val (app, property) = model

        if (!baseMapper.exists(Q().eq(F.ID, app.id)))
            throw Exception("应用编号[${app.id}]不存在")

        updateById(app)

        property.bind(app)
        propertyM.updateById(property)

        refresh.app(app.id)
    }

    fun remove(id: String, user: AuthUser):App {
        val app = getById(id)
        //仅限应用创建者及管理员
        if (app.uid == user.id || user.hasRole(Role.ADMIN)) {
            val dataCount = dataM.selectCount(QueryWrapper<Data>().eq(F.AID, id))
            if(dataCount > 0L){
                throw ServiceException("应用[${id}/${app.name}]下有 $dataCount 条数据，请先处理再操作")
            }
            val pageCount = pageM.selectCount(QueryWrapper<Page>().eq(F.AID, id))
            if(pageCount > 0L)
                throw ServiceException("应用[${id}/${app.name}]下有 $pageCount 个页面，请先处理再操作")

            propertyM.deleteById(id)
            removeById(id)
            refresh.app(id)

            return app
        }
        else
            throw Exception("仅限创建者或管理员能够删除应用")
    }
}