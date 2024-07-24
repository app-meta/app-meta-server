package org.nerve.boot.domain;

import java.io.Serializable;

public interface ID<S extends Serializable> {
	S id();

	boolean using();
}
