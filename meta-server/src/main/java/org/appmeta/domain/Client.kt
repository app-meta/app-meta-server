package org.appmeta.domain

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.nerve.boot.annotation.CN
import org.nerve.boot.db.StringEntity

@CN("客户端")
class Client:StringEntity() {
    var name        = ""
    var ip          = ""
    var key         = ""
    var updateOn    = 0L
    var addOn       = 0L
}

@Mapper
interface ClientMapper:BaseMapper<Client>