package org.nerve.boot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

abstract public class IDLong implements ID<Long> {

    @TableId(type = IdType.AUTO)
    protected long id;

    @Override
    public Long id() {
        return id;
    }

    public Long getId(){
        return id;
    }

    public IDLong setId(long id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean using() {
        return id>0;
    }
}
