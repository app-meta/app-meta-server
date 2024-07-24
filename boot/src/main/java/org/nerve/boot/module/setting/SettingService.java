package org.nerve.boot.module.setting;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.nerve.boot.db.service.BaseService;
import org.nerve.boot.enums.Fields;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static org.nerve.boot.Const.COMMA;

@Service
public class SettingService extends BaseService<SettingMapper, Setting> {

	protected QueryWrapper<Setting> byId(String uuid){
		return Q().eq(Fields.ID.value(), uuid);
	}

	public boolean existsByUUID(String uuid){
		return count(byId(uuid))>0;
	}

	public Setting load(String uuid) {
		return getById(uuid);
	}

	public Setting load(Enum e) {
		return load(e.name());
	}

	@Cacheable("settings")
	public String value(String uuid) {
		Setting setting = load(uuid);
		return Objects.isNull(setting)? null : setting.getContent();
	}

	@Cacheable("settings")
	public String value(Enum e) {
		return value(e.name());
	}

	@Cacheable("settings")
	public String[] valueOfList(Enum e) {
		String text = value(e.name());
		return text != null ? text.trim().split(COMMA) : null;
	}

	@Cacheable("settings")
	public int intValue(Enum uuid, int defaultValue) {
		String value = value(uuid);
		return StringUtils.isEmpty(value)? defaultValue : Integer.valueOf(value);
	}

	@Cacheable("settings")
	public float floatValue(Enum uuid, float defaultValue) {
		String value = value(uuid);
		return StringUtils.isEmpty(value)? defaultValue : Float.valueOf(value);
	}

	@Cacheable("settings")
	public boolean booleanValue(Enum uuid, boolean defaultValue) {
		String value = value(uuid);
		return StringUtils.isEmpty(value)? defaultValue : Boolean.valueOf(value);
	}

	@Cacheable("settings")
	public List<Setting> loadByCategory(String category) {
		return list(Q().eq("category", category));
	}
}
