package org.nerve.boot.db;

import org.nerve.boot.domain.IDLong;

import java.util.Date;

public class DateEntity extends IDLong {
	protected Date addDate;

	public Date getAddDate() {
		return addDate;
	}

	public DateEntity setAddDate(Date addDate) {
		this.addDate = addDate;
		return this;
	}
}
