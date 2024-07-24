package org.appmeta.component

import org.appmeta.domain.Page
import org.appmeta.domain.RobotLog
import org.nerve.boot.module.setting.Setting
import org.springframework.context.ApplicationEvent
import java.io.Serializable

class PageDeleteEvent(val id:Serializable):ApplicationEvent(id)
class PageNameUpdateEvent(val page:Page):ApplicationEvent(page)
class PageContentUpdateEvent(val page: Page):ApplicationEvent(page)

class SettingChangeEvent(val setting:Setting): ApplicationEvent(setting)

/**
 * 机器人执行完成后触发的事件
 */
class RobotFinishEvent(val robotLog: RobotLog):ApplicationEvent(robotLog)