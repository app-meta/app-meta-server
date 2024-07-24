package org.nerve.boot.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.nerve.boot.domain.IDString;

public class StringEntity implements IDString {
    @TableId(type = IdType.INPUT)
    protected String id;

    @Override
    public String id() {
        return id;
    }

    public String getId() {
        return id;
    }

    public StringEntity setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean using() {
        return id != null && !id.isEmpty();
    }
}
