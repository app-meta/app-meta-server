package org.appmeta.module.dbm


class DbmModel {
    companion object {
        const val SQL = "SQL"
    }

    var sourceId            = 0L
    var db                  = ""
    var table               = ""
    var action              = SQL
    var sql:String?         = null
    var batch               = false
    var condition:String?   = null  //筛选条件
    var columns:String?     = null  //查询列
    var obj:Map<String,Any>?= null

    override fun toString() = "SOURCE=#$sourceId DB=${db} TABLE=$table ACTION=$action"
}