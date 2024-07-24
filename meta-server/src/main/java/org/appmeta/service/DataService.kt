package org.appmeta.service

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy
import com.alibaba.fastjson2.JSON
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import org.appmeta.F
import org.appmeta.domain.*
import org.appmeta.model.*
import org.nerve.boot.Const.COMMA
import org.nerve.boot.db.service.BaseService
import org.nerve.boot.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import java.io.OutputStream


/*
 * @project app-meta-server
 * @file    org.appmeta.service.DataService
 * CREATE   2022年12月16日 10:27 上午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

@Service
class DataService(private val batchM:DataBatchMapper, private val blockM:DataBlockMapper) : BaseService<DataMapper, Data>() {

    private fun _info(msg:String) = logger.info("[DATA] $msg")

    @Transactional
    fun create(model: DataCreateModel):Int {
        val (aid, pid, uid) = model
        val isBatch = StringUtils.hasText(model.batch)
        val objs = model.objs.ifEmpty {
            if (model.obj.isEmpty()) throw Exception("插入数据对象不能为空")

            listOf(model.obj)
        }

        var counter = 0
        objs.forEach { d->
            with(Data(aid, uid, model.channel)) {
                this.pid    = pid

                this.v      = if(isBatch) {
                    val dMap = mutableMapOf<String, Any>(F.BATCH_ to model.batch)
                    dMap.putAll(d)
                    dMap
                }
                else
                    d

                save(this)
                counter ++
                if(logger.isDebugEnabled)   logger.debug("新增数据 #$aid $d")
            }
        }
        if(isBatch){
            with(DataBatch(model)){
                size    = counter
                origin  = model.origin
                batch   = model.batch

                batchM.insert(this)
            }
        }

        _info("${if(isBatch) "[按批次 ${model.batch}]" else ""} 新增 #$aid 数据 ${objs.size} 条（成功 $counter 条）")
        return counter
    }

    /**
     * 根据 ID 跟 aid 更新指定的对象
     *
     * 两个方案：
     *      1、利用 id+aid 查询出对象，然后更新 v 属性，再保存到数据库
     *
     *      2、利用 id+aid，配合 JSON_SET 更新字段（存在 SQL 注入风险）
     */
    fun update(model: DataUpdateModel) {
        val (aid, pid, _, obj) = model
        Assert.isTrue(model.id > 0L, "ID 须指定")
        Assert.isTrue(obj.isNotEmpty(), "修改值不能为空")

        val wrapper = U()
            .apply("${F.ID}={0}", model.id)
            .apply("${F.AID}={0}", aid)
            .apply(StringUtils.hasText(pid), "${F.PID}={0}", pid)

        val data = getOne(wrapper)?: throw ServiceException("#${model.id} 数据不存在")
        //目前仅支持修改数据内容
        if(model.merge)
            data.v += obj
        else
            data.v = obj

        updateById(data)

        if(logger.isDebugEnabled)   logger.debug("更新数据 #${model.id} $obj")
        _info("更新 ID=${model.id}(AID=${aid}) 的数据...")

//        val set = StringBuilder("JSON_SET(v,")
//        /*
//        直接拼凑 JSON_SET 语句，存在 SQL 注入风险
//        日后考虑优化
//         */
//        obj.forEach { (k, v) -> set.append("$.${k}", v)}
//        set.append(")")
//
//        wrapper.set("v", set.toString())
//
//        update(wrapper)
    }

    fun readBy(id:Long, aid:String) = read(DataReadModel.by(id, aid)).firstOrNull()

    private fun _buildQueryFromModel(model: DataModel): QueryWrapper<Data> {
        val ( aid, pid, uid ) = model

        val wrapper = Q()
            .apply("${F.AID}={0}", aid)
            .apply(StringUtils.hasText(uid), "${F.UID}={0}", uid)
            .apply(model.timeFrom > 0L, "${F.ADD_ON}>={0}", model.timeFrom)
            .apply(model.timeEnd > 0L, "${F.ADD_ON}<={0}", model.timeEnd)
        //优先使用 pids
        if(model.pids.isNotEmpty())
            wrapper.`in`(F.PID, model.pids)
        else
            wrapper.apply(StringUtils.hasText(pid), "${F.PID}={0}", pid)

        //限定查询字段
        wrapper.select(F.ID, F.PID, F.AID, F.UID, F.CHANNEL, F.V, F.ADD_ON)

        if(model.id>0L){
            wrapper.apply("${F.ID}={0}", model.id)
        }
        else if(model.match.isNotEmpty()){
            model.match.forEach { item->
                val column = "v->'\$.\"${item.field}\"'"        //给 field 加上双引号以支持中文 key
                // IN、NOT IN 的特殊操作
                if(item.op == QueryFilter.IN || item.op == QueryFilter.NIN){
                    if(item.value !is List<*>)  throw ServiceException("对属性 ${item.field} 进行 IN 查询必须传递数组或者列表")

                    if(item.op == QueryFilter.IN)
                        wrapper.`in`(column, item.value as List<Any>)
                    else
                        wrapper.notIn(column, item.value as List<Any>)
                }
                else if(item.op == QueryFilter.LIKE)
                    wrapper.like(column, item.value)
                else
                    wrapper.apply("$column ${item.op.action} {0}", item.value)
            }
        }
        return wrapper
    }

    fun read(model: DataReadModel):List<Data> {
        val wrapper = _buildQueryFromModel(model)
        if(model.desc)
            wrapper.orderByDesc(F.ID)

        if(model.page > 0){
            //分页查询
            val p = Page.of<Data>(model.page, model.pageSize)
            page(p, wrapper)
            model.total = p.total
            return p.records
        }

        wrapper.last(model.toLimit())
        return list(wrapper)
    }

    fun delete(model: DataDeleteModel):Int {
        val wrapper = _buildQueryFromModel(model)

        return baseMapper.delete(wrapper)
    }

    fun export(model:DataExportModel, outStream: OutputStream) {
        EasyExcel.write(outStream)
            .head(model.headers.map { listOf(it.label) })
            .registerWriteHandler(LongestMatchColumnWidthStyleStrategy())
            .build()
            .let { writer->
                val sheet = EasyExcel.writerSheet(model.sheetName).build()
                val fields = model.headerFields()

                logger.info("开始导出：$fields")

                var count = 0
                baseMapper.loadWithStream(_buildQueryFromModel(model)) { row ->
                    val json = JSON.parseObject(row.resultObject["v"] as String)

                    writer.write(listOf(fields.map { json[it] }), sheet)
                    count ++
                }

                writer.finish()
                logger.info("数据导出完成，共 $count 条...")
            }
    }

    /**
     * CSV 格式数据
     */
    fun exportToCSV(model: DataExportModel, outStream: OutputStream) {
        var count = 0
        val fields = model.headerFields()
        outStream.write("${model.headers.joinToString(COMMA) { it.label }}\n".toByteArray())
        baseMapper.loadWithStream(_buildQueryFromModel(model)) { row ->
            val json = JSON.parseObject(row.resultObject["v"] as String)

            outStream.write("${fields.map { json[it] }.joinToString(COMMA)}\n".toByteArray())
            count ++
        }
    }

    fun deleteWithBatch(bean: DataBatch):Int {
        val model   = DataDeleteModel()
        model.aid   = bean.aid
        model.pid   = bean.pid

        model.match.add(QueryItem(F.BATCH_, QueryFilter.EQ, bean.batch))

        val count = delete(model)
        batchM.update(null, UpdateWrapper<DataBatch>().eq(F.ID, bean.id).set(F.ACTIVE, false))
        return count
    }


    private fun _QBlock(block: DataBlock) = QueryWrapper<DataBlock>().eq(F.AID, block.aid).eq(F.UUID, block.uuid)

    //================================= START 数据块管理 START =================================

    /**
     *
     */
    fun getBlockBy(block: DataBlock) = blockM.selectOne(_QBlock(block))

    /**
     * 如果 block.text 为空，则删除该条记录
     */
    fun setBlockTo(block: DataBlock) {
        if(logger.isDebugEnabled)   logger.debug("更新数据块 AID=${block.aid} UUID=${block.uuid} TEXT=${block.text}")

        if(StringUtils.hasText(block.text)){
            val oldB = getBlockBy(block)
            if(oldB == null){
                blockM.insert(block)
                _info("新增数据块 ${block.info()}")
            }
            else{
                oldB.text = block.text
                block.id  = oldB.id
                blockM.updateById(oldB)
                _info("更新数据块 ${block.info()} TEXT=${block.text}")
            }
        }
        else {
            blockM.delete(_QBlock(block))
            _info("删除数据块 ${block.info()}")
        }
    }

    fun getBlockList(aid: String) = blockM.selectList(QueryWrapper<DataBlock>().eq(F.AID, aid))
    //================================= END 数据块管理 END =================================
}
