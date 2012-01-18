/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.test.schema;

/**
 * Allows you to build valid config for the L2s. This class <strong>MUST NOT</strong> invoke the actual XML beans to do
 * its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class L2SConfigBuilder extends BaseConfigBuilder {

  private L2ConfigBuilder[]        l2s;
  private HaConfigBuilder          ha;
  private GroupsConfigBuilder      groups;
  private UpdateCheckConfigBuilder updateCheck;

  public L2SConfigBuilder() {
    super(1, new String[] { "l2s", "ha", "groups", "update-check" });
  }

  public void setL2s(L2ConfigBuilder[] l2s) {
    this.l2s = l2s;
    setProperty("l2s", l2s);
  }

  public void setHa(HaConfigBuilder ha) {
    this.ha = ha;
    setProperty("ha", ha);
  }

  public void setGroups(GroupsConfigBuilder groups) {
    this.groups = groups;
    setProperty("groups", groups);
  }

  public void setUpdateCheck(UpdateCheckConfigBuilder updateCheck) {
    this.updateCheck = updateCheck;
    setProperty("update-check", updateCheck);
  }

  public L2ConfigBuilder[] getL2s() {
    return l2s;
  }

  public HaConfigBuilder getHa() {
    return ha;
  }

  public GroupsConfigBuilder getGroups() {
    return groups;
  }

  public UpdateCheckConfigBuilder getUpdateCheck() {
    return updateCheck;
  }

  public String toString() {
    String out = "";
    if (isSet("l2s")) {
      out += l2sToString();
    }
    if (isSet("groups")) {
      out += groups.toString();
    }
    if (isSet("ha")) {
      out += ha.toString();
    }
    if (isSet("update-check")) {
      out += updateCheck.toString();
    }
    return out;
  }

  private String l2sToString() {
    String val = "";
    for (int i = 0; i < l2s.length; i++) {
      val += l2s[i].toString();
    }
    return val;
  }

  public static L2SConfigBuilder newMinimalInstance() {
    L2ConfigBuilder l2 = new L2ConfigBuilder();
    L2SConfigBuilder out = new L2SConfigBuilder();
    out.setL2s(new L2ConfigBuilder[] { l2 });
    return out;
  }

}
