package org.nerve.boot.module.setting;

import com.baomidou.mybatisplus.annotation.TableName;
import org.nerve.boot.db.StringEntity;
import org.nerve.boot.db.Table;
import org.nerve.boot.enums.Form;

@Table(name = "sys_setting")
@TableName("sys_setting")
public class Setting extends StringEntity {
    String title;
    String summary;
    String defaultContent;
    String content;
    Form form = Form.TEXT;
    String formValue;
    String category;
    int sort = -1;			//排序，越小越靠前

    public String getTitle() {
        return title;
    }

    public Setting setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public Setting setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public String getDefaultContent() {
        return defaultContent;
    }

    public Setting setDefaultContent(String defaultContent) {
        this.defaultContent = defaultContent;
        return this;
    }

    public String getContent() {
        return content;
    }

    public Setting setContent(String content) {
        this.content = content;
        return this;
    }

    public Form getForm() {
        return form;
    }

    public Setting setForm(Form form) {
        this.form = form;
        return this;
    }

    public String getFormValue() {
        return formValue;
    }

    public Setting setFormValue(String formValue) {
        this.formValue = formValue;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public Setting setCategory(String category) {
        this.category = category;
        return this;
    }

    public int getSort() {
        return sort;
    }

    public Setting setSort(int sort) {
        this.sort = sort;
        return this;
    }
}
