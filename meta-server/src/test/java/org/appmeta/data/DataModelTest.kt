package org.appmeta.data

import org.appmeta.model.*
import org.junit.jupiter.api.Test

class DataModelTest {

    @Test
    fun model(){
        listOf( DataCreateModel(), DataUpdateModel(), DataReadModel(), DataDeleteModel()).forEach {
            println("${it.javaClass} \t ${it.action}")
        }
    }

    @Test
    fun queryItem(){
        val sql = "age LT 50"
//        println(sql.split(" "))
        println(QueryItem(sql))
    }
}