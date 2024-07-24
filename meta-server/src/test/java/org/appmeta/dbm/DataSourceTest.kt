package org.appmeta.dbm

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.Test

class DataSourceTest {

    @Test
    fun createDataSource(){
        HikariDataSource(HikariConfig().also {
            it.username = "root"
            it.password = "rootroot"
            it.jdbcUrl = "jdbc:mysql://127.0.0.1:33006"
            it.driverClassName = "com.mysql.cj.jdbc.Driver"
        }).use {source->
            source.connection.use { conn->
                val result = conn.prepareStatement("SHOW DATABASES").executeQuery()
                while (result.next()){
                    println(result.getString(1))
                }
            }
        }
    }
}