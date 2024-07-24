package org.nerve.boot.domain;

import java.util.Objects;

public interface IDString extends ID<String>{

    default boolean using(){
        return Objects.nonNull(id()) && id().trim().length()>0;
    }

}
