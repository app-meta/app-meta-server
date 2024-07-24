package org.nerve.boot;


import java.util.Objects;

/**
 * 通用的分页工具
 */

public class Pagination {
	public final static String ORDER_DIRECTION_ASC = "ASC";
	public final static String ORDER_DIRECTION_DESC = "DESC";

	/**
	 * 默认每页记录数
	 */
	public static final int DEFAULT_PAGE_SIZE = 20;

	/**
	 * 当前页码
	 */
	protected int page=1;
	protected int pageSize = DEFAULT_PAGE_SIZE;
	protected long total = 0;    //最大值

	public Pagination(){}

	public Pagination(int size){
		if(size > 0)
			pageSize = size;
	}

	public Pagination(int page, int size){
		this(size);
		if(page>0)
			this.page = page;
	}

	public int getOffset(){
		return pageSize * (page - 1);
	}

	public int getPage() {
		return page;
	}

	public Pagination setPage(int page) {
		this.page = page;
		return this;
	}

	public int getPageSize() {
		return pageSize;
	}

	public Pagination setPageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public long getTotal() {
		return total;
	}

	public Pagination setTotal(long total) {
		this.total = total;
		return this;
	}

	/**
	 * 转化成 LIMIT 语句，支持 MySQL、PostgreSQL
	 * @return
	 */
	public String toLimit(){
		return String.format(" LIMIT %d,%d", getOffset(), pageSize);
	}

	@Override
	public String toString() {
		return "Pagination{" +
				"page=" + page +
				", pageSize=" + pageSize +
				", total=" + total +
				'}';
	}

	@Override
	public int hashCode() {
		return Objects.hash(page, pageSize);
	}
}
