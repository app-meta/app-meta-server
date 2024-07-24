package org.nerve.boot.db;

import java.util.Date;

public class UpdateDateEntity extends DateEntity{
	protected Date updateDate;

	public Date getUpdateDate() {
		return updateDate;
	}

	public UpdateDateEntity setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
		return this;
	}
}
