package org.nerve.boot.module.operation;

import org.nerve.boot.db.service.BaseService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OperationService extends BaseService<OperationMapper, Operation> {
    @Async
    public void saveAsync(Operation op) {
        save(op);
    }
}
