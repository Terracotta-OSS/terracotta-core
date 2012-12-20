/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.test.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows you to build valid config for the L2s. This class <strong>MUST NOT</strong> invoke the actual XML beans to do
 * its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class L2SConfigBuilder extends BaseConfigBuilder {

  private GroupConfigBuilder[]           groups;
  private UpdateCheckConfigBuilder       updateCheck;
  private GarbageCollectionConfigBuilder gc;
  private boolean                        restartable = false;

  public L2SConfigBuilder() {
    super(1, new String[] { "groups", "update-check", "garbage-collection", "client-reconnect-window", "restartable" });
  }

  public void setGroups(GroupConfigBuilder[] groups) {
    if (this.groups != null) { throw new IllegalStateException("groups already set"); }
    this.groups = groups;
    setProperty("groups", groups);
  }

  public L2ConfigBuilder[] getL2s() {
    List<L2ConfigBuilder> l2s = new ArrayList<L2ConfigBuilder>();

    if (groups != null) {
      for (GroupConfigBuilder group : groups) {
        for (L2ConfigBuilder l2 : group.getL2s()) {
          l2s.add(l2);
        }
      }
    }

    return l2s.toArray(new L2ConfigBuilder[l2s.size()]);
  }

  public void setL2s(L2ConfigBuilder[] l2Builder) {
    if (groups != null) {
      //
      throw new IllegalStateException("groups have already been set. The L2s must be set in the groups config");
    }

    GroupConfigBuilder group = new GroupConfigBuilder("auto-generated");
    group.setL2s(l2Builder);
    setGroups(new GroupConfigBuilder[] { group });
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

  public GroupConfigBuilder[] getGroups() {
    return groups;
  }

  public UpdateCheckConfigBuilder getUpdateCheck() {
    return updateCheck;
  }

  @Override
  public String toString() {
    String out = "";

    if (isSet("groups")) {
      for (GroupConfigBuilder group : groups) {
        out += group.toString();
      }
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

  public static L2SConfigBuilder newMinimalInstance() {
    return new L2SConfigBuilder();
  }

}
