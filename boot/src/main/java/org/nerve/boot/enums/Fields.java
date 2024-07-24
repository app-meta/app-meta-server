package org.nerve.boot.enums;

/**
 * 通用的实体字段名
 * com.zeus.enums
 * Created by zengxm on 4/5/2017.
 */
public enum Fields {
	TRASH("trash"),
	STATUS("status"),
	CATEGORY("category"),
	EXTRAS("extras"),
	UUID("uuid"),
	ADD_DATE("addDate"),
	PASSWORD("password"),
	UPDATE_DATE("updateDate"),
	NAME("name"),
	SUMMARY("summary"),
	ID("id"),
	UID("uid"),
	UNAME("uname"),
	CREATE_ON("createOn"),
	KEY("key"),
	ROLES("roles.$id")
	;

	private String n;

	Fields(String n){
		this.n = n;
	}

	@Override
	public String toString() {
		return n;
	}

	public String value(){
		return n;
	}
}
