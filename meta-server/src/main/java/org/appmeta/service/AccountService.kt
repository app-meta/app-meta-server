package org.appmeta.service

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONReader.Feature.SupportSmartMatch
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import jakarta.annotation.Resource
import org.apache.commons.lang3.StringUtils.isNoneBlank
import org.appmeta.Caches
import org.appmeta.S
import org.appmeta.component.SettingChangeEvent
import org.appmeta.domain.*
import org.appmeta.model.UserResultModel
import org.mindrot.jbcrypt.BCrypt
import org.nerve.boot.Const.EMPTY
import org.nerve.boot.Result
import org.nerve.boot.cache.CacheManage
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.domain.AuthUser
import org.nerve.boot.domain.UserAuthRecognizer
import org.nerve.boot.exception.ServiceException
import org.nerve.boot.module.auth.RoleMapper
import org.nerve.boot.module.setting.SettingService
import org.nerve.boot.util.AESProvider
import org.nerve.boot.util.MD5Util
import org.nerve.boot.web.auth.UserLoader
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

/*
 * @project app-meta-server
 * @file    org.appmeta.service.AccountService
 * CREATE   2022年12月19日 11:21 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */
@Service
class DepartmentService:ServiceImpl<DepartmentMapper, Department>()

@Service
class AccountPwdService(private val settingS: SettingService) : BaseService<AccountPwdMapper, AccountPwd>() {

    /**
     * 读取数据库的 AUTH_AES_KEY
     */
    fun getKey(): String {
        val key = settingS.value(S.AUTH_AES_KEY)
        return if(StringUtils.hasText(key)) key else "h8Zunv1Z3dW19zpt"
    }

    /**
     * 重设密码
     * uid 加密方式为 AES
     * 密码加密方式为 md5+Base64 后 Bcrypt 加密
     */
    fun reset(uid:String, pwd:String) {
        Assert.isTrue(isNoneBlank(uid, pwd), "用户名及密码不能为空")

        with(AccountPwd()){
            id      = AESProvider().encrypt(uid, getKey())
            value   = BCrypt.hashpw(
                Base64.getEncoder().encodeToString(MD5Util.encode(pwd).toByteArray()),
                BCrypt.gensalt()
            )

            saveOrUpdate(this)
            logger.info("更新 $uid 的密码为 $value")
        }
    }

    /**
     *
     */
    fun check(uid: String, pwd: String):Boolean {
        val id = AESProvider().encrypt(uid, getKey())

        return try{
            BCrypt.checkpw(pwd, baseMapper.selectById(id).value)
        } catch (e:Exception){
            logger.error("验证 $uid 的密码 $pwd 失败...")
            false
        }
    }
}

@Service
class AccountService(
    private val settingS: SettingService,
    private val recognizer: UserAuthRecognizer,
    private val departS:DepartmentService,
    private val roleM:RoleMapper):BaseService<AccountMapper, Account>() {

    @Resource
    lateinit var restTemplate: RestTemplate

    fun toAuthUser(uid: String, ip:String=""):AuthUser = CacheManage.get(
        "AUTH-$uid-$ip",
        {
            val account = getById(uid)?:throw ServiceException("ACCOUNT #${uid} INVALID")

            AuthUser().also {
                it.id = uid
                it.ip = ip
                it.name   = account.name
                it.roles  = recognizer.loadRoleByUser(it)
            }
        },
        30 * 60
    )

    @Cacheable(Caches.ACCOUNT_ALL)
    fun listOfAll() = mapOf(
        "accounts"       to baseMapper.selectList(null),
        "departments"    to departS.list(),
        "roles"          to roleM.selectList(null)
    )

    @Cacheable(Caches.ACCOUNT)
    fun listOfAccount() = baseMapper.selectList(null)

    @Cacheable(Caches.ACCOUNT_ID)
    fun getNameById(id:String) = baseMapper.selectById(id)?.name

    fun listOfDepart() = departS.list()

    fun listOfRole() = roleM.selectList(null)

    /**
     * 解析的内容示例：
     *
     * {"success":true,"data":[["用户ID","用户名","部门ID 部门名称"]]}
     *
     * 参数可以是：
     * ① 远程地址，返回特定格式的内容
     * ② 文件路径
     * ③ JSON 格式的字符串（以 { 或者 [ 开头）
     */
    fun refreshFromRemote(remote:String): String {
        val lines = (
                if(remote.startsWith("http")){
                    val response = restTemplate.getForEntity(remote, String::class.java)
                    logger.info("从 $remote 获取到响应数据 [CODE=${response.statusCode}]")

                    if(response.statusCode != HttpStatus.OK)    throw Exception("获取远程用户信息（$remote）失败：${response.statusCode}")

                    //如果使用 restTemplate 转换返回的是 ArrayList，故统一使用 JSON2 进行转换
                    response.body!!
                }
                else if(remote.trim()[0].let { it == '{' || it=='[' }){
                    remote
                }
                else{
//                    val file = File(remote)
//                    if(!(file.exists() && file.isFile)) throw Exception("读取用户信息文件（$remote）失败：文件不存在或不可读")
//
//                    JSON.parseObject<Result>(FileInputStream(file), Result::class.java, SupportSmartMatch).data
                    Paths.get(remote).let {
                        if(!(it.exists() && it.isRegularFile())) throw Exception("读取用户信息文件（$remote）失败：文件不存在或不可读")

                        Files.readString(it, Charsets.UTF_8)
                    }
                }
            ).let {
                if(it.startsWith(("[")))
                    JSON.parseArray(it)
                else
                    JSON.parseObject(it, Result::class.java, SupportSmartMatch).data
            }

        Assert.isTrue(lines is JSONArray, "用户清单必须是数组格式（当前格式为 ${lines.javaClass.simpleName}）")

        val users   = mutableListOf<Account>()
        val departs = mutableMapOf<String, Department>()

        with(lines as JSONArray) {
            logger.info("从 $remote 读取到用户数据 $size 条")
            forEachIndexed { i, v ->
                val items = v as JSONArray
                if(items.size < 3)  {
                    logger.info("第 ${i+1} 行数据长度不足 3 跳过...")
                    return@forEachIndexed
                }
                //处理部门信息
                val d = items.getString(2).split(" ")
                val depart = Department(d[0], d[1])
                if(!departs.containsKey(depart.id)) departs[depart.id] = depart

                //处理用户
                users.add(Account(items.getString(0), items.getString(1), depart.id))
            }
        }

        if(users.isNotEmpty() && departs.isNotEmpty()){
            logger.info("解析到 ${users.size} 个用户、${departs.size} 个部门信息，即将覆盖...")
            saveOrUpdateBatch(users)

            departS.saveOrUpdateBatch(departs.values)
            return "更新用户${users.size}个、部门${departs.size}个"
        }
        return "用户${users.size}个、部门${departs.size}个（存在0不进行更新）"
    }

    @Async
    @EventListener(SettingChangeEvent::class, condition = "#e.setting.id=='SYS_ACCOUNT_REMOTE'")
    fun onSettingChange(e:SettingChangeEvent) = e.setting.content.let {
        if(StringUtils.hasText(it) && settingS.intValue(S.SYS_ACCOUNT_INTERVAL, 0) > 0){
            logger.info("检测到用户同步地址变更，即将进行数据同步...")
            // 立即进行用户数据同步
            refreshFromRemote(it)
        }
    }
}

@Component
class AccountHelper(
    private val userLoader: UserLoader,
    private val departmentM:DepartmentMapper) {

    /**
     * 根据 TOKEN 构建用户信息
     */
    fun buildUserBean(token:String) = userLoader.from(token).let { user->
        if(user == null)    return@let null

        UserResultModel(
            user.id,
            user.name,
            user.ip,
            if(StringUtils.hasText(user.id)) departmentM.loadByUser(user.id) else null,
            user.roles
        )
    }

    /**
     * 返回部门描述信息
     * id-name
     */
    fun getDepartByUid(uid: String): String {
        val d = departmentM.loadByUser(uid)
        return if(d==null) EMPTY else "${d.id}-${d.name}"
    }
}