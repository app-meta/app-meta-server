package org.appmeta.domain

import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.appmeta.Caches
import org.nerve.boot.annotation.CN
import org.nerve.boot.db.StringEntity
import org.springframework.cache.annotation.Cacheable

@CN("用户")
class Account: NameBean {
    var did     = ""

    constructor()
    constructor(id:String, name:String, did:String) {
        this.id     = id
        this.name   = name
        this.did    = did
    }

    override fun toString() = "$name($id)/D=$did"
}

@CN("用户凭证")
@TableName("account_pwd")
class AccountPwd: StringEntity(){
    var value   = ""
}


@CN("部门")
class Department: NameBean {

    constructor()
    constructor(id:String, name:String){
        this.id     = id
        this.name   = name
    }
}

@Mapper
interface AccountMapper:BaseMapper<Account>
@Mapper
interface AccountPwdMapper:BaseMapper<AccountPwd>
@Mapper
interface DepartmentMapper:BaseMapper<Department> {

    @Cacheable(Caches.DEPARTMENT)
    @Select("SELECT d.* FROM department d WHERE d.id=(SELECT a.did FROM account a WHERE a.id=#{0}) LIMIT 1")
    fun loadByUser(uid:String):Department?
}