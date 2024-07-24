package org.appmeta.service

import org.appmeta.domain.Client
import org.appmeta.domain.ClientMapper
import org.nerve.boot.db.service.BaseService
import org.springframework.stereotype.Service

@Service
class ClientService:BaseService<ClientMapper, Client>() {

}