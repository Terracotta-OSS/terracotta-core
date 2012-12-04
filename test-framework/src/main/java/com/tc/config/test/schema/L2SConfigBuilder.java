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

  private L2ConfigBuilder[]              l2s;
  private GroupsConfigBuilder            groups;
  private UpdateCheckConfigBuilder       updateCheck;
  private GarbageCollectionConfigBuilder gc;
  private boolean                        restartable = false;

  public L2SConfigBuilder() {
    super(1, new String[] { "l2s", "ha", "groups", "update-check", "garbage-collection", "client-reconnect-window",
        "restartable" });
  }

  public void setL2s(L2ConfigBuilder[] l2s) {
    this.l2s = l2s;
    setProperty("l2s", l2s);
  }

  public void setGroups(GroupsConfigBuilder groups) {
    this.groups = groups;
    setProperty("groups", groups);
  }

  public void setUpdateCheck(UpdateCheckConfigBuilder updateCheck) {
    this.updateCheck = updateCheck;
    setProperty("update-check", updateCheck);
  }

  public void setGarbageCollection(GarbageCollectionConfigBuilder gc) {
    this.gc = gc;
    setProperty("garbage-collection", gc);
  }

  public void setRestartable(boolean data) {
    setProperty("restartable", data);
    restartable = data;
  }

  public void setReconnectWindowForPrevConnectedClients(int secs) {
    setProperty("client-reconnect-window", secs);
  }

  public L2ConfigBuilder[] getL2s() {
    return l2s;
  }

  public GroupsConfigBuilder getGroups() {
    return groups;
  }

  public UpdateCheckConfigBuilder getUpdateCheck() {
    return updateCheck;
  }

  @Override
  public String toString() {
    String out = "";
    if (isSet("l2s")) {
      out += l2sToString();
    }
    if (isSet("groups")) {
      out += groups.toString();
    }
    if (isSet("update-check")) {
      out += updateCheck.toString();
    }

    if (isSet("garbage-collection")) {
      out += gc.toString();
    }

    out += getRestartable();

    if (isSet("client-reconnect-window")) {
      out += element("client-reconnect-window");
    }

    return out;
  }

  private String getRestartable() {
    if (!restartable) return "\n";
    return "\n<restartable enabled=\"" + restartable + "\"/>\n";
  }

  private String l2sToString() {
    String val = "";
    for (L2ConfigBuilder l2 : l2s) {
      val += l2.toString();
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
