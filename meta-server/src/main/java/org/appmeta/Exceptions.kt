package org.appmeta

import java.io.Serializable


/*
 * @project app-meta-server
 * @file    org.appmeta.Exceptions
 * CREATE   2023年12月21日 16:15 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

class Document404Exception(id: Serializable?): Exception("文档#${id} 不存在")