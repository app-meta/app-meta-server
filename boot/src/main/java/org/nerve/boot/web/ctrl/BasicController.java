package org.nerve.boot.web.ctrl;
/*
 * @project boot-starter
 * @file    org.nerve.boot.web.ctrl.BasicController
 * CREATE   2022年11月21日 15:02 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import jakarta.annotation.Resource;
import org.nerve.boot.domain.AuthUser;
import org.nerve.boot.domain.ID;
import org.nerve.boot.module.operation.Operation;
import org.nerve.boot.module.operation.OperationService;

public abstract class BasicController extends BaseController {

    @Resource
    protected OperationService opService;

    protected void opLog(String msg, ID<?> bean, int opType) {
        AuthUser user = authHolder.get();
        Operation op = new Operation(user, bean, opType, String.format("%s %s", user!=null?user.getShowName():"", msg));
        opService.saveAsync(op);
        logger.info(op.getSummary());
    }

    protected void opLog(String msg, ID<?> bean) {
        opLog(msg, bean, Operation.MODIFY);
    }
}
