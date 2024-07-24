package org.nerve.boot.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.nerve.boot.domain.IDLong;

public class IdEntity extends IDLong {

    @TableId(type = IdType.AUTO)
    protected long id;

    public Long id() {
        return id;
    }

    public IdEntity setId(long id) {
        this.id = id;
        return this;
    }

    @Override
    public Long getId() {
        return id;
    }
}
