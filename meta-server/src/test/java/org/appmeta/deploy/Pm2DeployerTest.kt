package org.appmeta.deploy

import jakarta.annotation.Resource
import org.appmeta.AppTest
import org.appmeta.component.deploy.LocalPm2Deployer
import org.appmeta.domain.Terminal
import org.junit.jupiter.api.Test
import java.io.File

class Pm2DeployerTest:AppTest() {
    @Resource
    lateinit var deployer: LocalPm2Deployer

    val DEMO = "demo"

    @Test
    fun check(){
        json(deployer.checkRequirement())
    }

    @Test
    fun overview(){
        json(deployer.overview(), true)
    }

    @Test
    fun remove(){
        json(deployer.remove(DEMO))
    }

//    @Test
//    fun detail(){
//        json(deployer.detail(DEMO))
//    }

    @Test
    fun restart(){
        println("启动应用：${deployer.restart(DEMO)}")
    }

    @Test
    fun deploy(){
        json(
            deployer.deploy(
                DEMO,
                File("/Volumes/zengxm/workspace/github/app-meta-example/packages/end-math/dist/end-math.js"),
                with(Terminal()){
                    port   = 10000
                    useDB  = true
                    dbHost = "localhost"
                    dbName = DEMO
                    dbUser = DEMO
                    dbPwd  = DEMO
                    this
                }
            )
        )
    }
}