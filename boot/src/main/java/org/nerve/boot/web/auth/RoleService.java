package org.nerve.boot.web.auth;

import org.nerve.boot.db.service.BaseService;
import org.nerve.boot.module.auth.RoleMapper;
import org.springframework.stereotype.Service;

@Service
public class RoleService extends BaseService<RoleMapper, Role> {
}
