package org.nerve.boot.db;

import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zengxm on 2015/4/1.
 */
public final class DBHelper {
	private static Logger logger = LoggerFactory.getLogger(DBHelper.class);

	private static Map<String, String> tableMap = new HashMap<String, String>();

	/**
	 * <pre>
	 *根据实体类名获取真实数据库中的表名
	 *
	 * 若没有指定 @Table(name)
	 * 则返回小写转换后的类名
	 *</pre>
	 * @param cls
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static String getTableName(final Class cls){
		String clsName = cls.getName();
		if(tableMap.containsKey(clsName))
			return tableMap.get(clsName);

		Field[] fields = cls.getDeclaredFields();
		fields[0].getAnnotation(Table.class);

		Annotation[] annos = cls.getDeclaredAnnotations();
		for(int i=0;i<annos.length;i++){
			Class annoType = annos[i].annotationType();
			if(annoType == Table.class || annoType == TableName.class){
				String tableName = annoType == Table.class? ((Table) annos[i]).name(): ((TableName) annos[i]).value();
				if(StringUtils.isEmpty(tableName))
					tableName = StringUtils.uncapitalize(cls.getSimpleName());
				tableMap.put(clsName, tableName);
				if(logger.isDebugEnabled()) logger.debug("缓存表名[key="+clsName+", tableName="+tableName+"]");
				return tableName;
			}
		}
		return cls.getSimpleName().toLowerCase();
	}
}
