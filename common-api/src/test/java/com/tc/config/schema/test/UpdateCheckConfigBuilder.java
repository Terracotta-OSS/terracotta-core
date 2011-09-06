/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

public class UpdateCheckConfigBuilder extends BaseConfigBuilder {

  public UpdateCheckConfigBuilder() {
    super(3, new String[] { "enabled", "period-days" });
  }

  public void setEnabled(boolean enabled) {
    setProperty("enabled", enabled);
  }

  public void setPeriodDays(int periodDays) {
    setProperty("period-days", periodDays);
  }

  public String toString() {
    String out = "";

    out += openElement("update-check");
    out += element("enabled");
    out += element("period-days");
    out += closeElement("update-check");

    return out;
  }
}
