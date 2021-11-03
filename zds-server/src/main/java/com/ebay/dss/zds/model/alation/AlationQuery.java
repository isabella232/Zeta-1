package com.ebay.dss.zds.model.alation;

import java.util.List;

public class AlationQuery extends AlationQueryMeta {
  private String autosave_content;
  private String content;
  private int datasource_id;
  private String title;
  private boolean published;
  private String description;
  private List<AlationSchedule> schedules;

  public AlationQuery(int id, String author) {
    super(id, author);
  }

  public AlationQuery() {
    super();
  }

  public String getAutosave_content() {
    return autosave_content;
  }

  public void setAutosave_content(String autosave_content) {
    this.autosave_content = autosave_content;
  }

  public boolean isPublished() {
    return published;
  }

  public void setPublished(boolean published) {
    this.published = published;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public int getDatasource_id() {
    return datasource_id;
  }

  public void setDatasource_id(int datasource_id) {
    this.datasource_id = datasource_id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<AlationSchedule> getSchedules() {
    return schedules;
  }

  public void setSchedules(List<AlationSchedule> schedules) {
    this.schedules = schedules;
  }

  public AlationSchedule getSchedule() {
    if (this.schedules != null && this.schedules.size() >=1) {
      return this.schedules.get(0);
    }
    return null;
  }

}
