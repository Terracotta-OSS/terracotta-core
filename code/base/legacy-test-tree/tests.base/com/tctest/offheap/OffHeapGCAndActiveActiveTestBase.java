/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.offheap;

import com.tc.objectserver.api.GCStats;
import com.tc.objectserver.control.ServerControl;
import com.tc.stats.DGCMBean;
import com.tc.test.GroupData;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.TestConfigObject;
import com.tc.test.activeactive.ActiveActiveServerManager;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.GCConfigurationHelper;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestIface;
import com.tctest.runner.PostAction;

import java.util.ArrayList;
import java.util.List;

public abstract class OffHeapGCAndActiveActiveTestBase extends OffHeapActiveActiveTransparentTestBase implements
    TestConfigurator {
  protected GCConfigurationHelper gcConfigHelper  = new GCConfigurationHelper();
  private GroupData[]             groupsData;
  private ServerControl[]         serverControls;
  public final static String      GROUPS_DATA     = "groups-data";
  public final static String      SERVER_CONTROLS = "server-controls";
  public final static int         NODE_COUNT      = 2;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.getTransparentAppConfig().setAttribute(GROUPS_DATA, groupsData);
    t.getTransparentAppConfig().setAttribute(SERVER_CONTROLS, serverControls);
    t.initializeTestRunner();
  }

  @Override
  protected boolean canRun() {
    return mode().endsWith(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_ACTIVE);
  }

  protected void setUpMultipleServersTest(PortChooser portChooser, ArrayList jvmArgs) throws Exception {
    super.setUpMultipleServersTest(portChooser, jvmArgs);
    ActiveActiveServerManager manager = (ActiveActiveServerManager) multipleServerManager;
    groupsData = manager.getGroupsData();
    serverControls = manager.getServerControls();
  }

  protected boolean isRunNoCrashMode() {
    return (multipleServerManager.getMultipleServersTestSetupManager().getServerCrashMode() == MultipleServersCrashMode.NO_CRASH);
  }

  protected void customizeActiveActiveTest(ActiveActiveServerManager manager) throws Exception {
    super.customizeActiveActiveTest(manager);
  }

  protected static class VerifyDGCPostAction implements PostAction {

    private List<List<DGCMBean>> dgcMBeans;

    public VerifyDGCPostAction(List<List<DGCMBean>> dgcMBeans) {
      this.dgcMBeans = dgcMBeans;
    }

    public void execute() {
      synchronized (this.dgcMBeans) {
        for (int group = 0; group < this.dgcMBeans.size(); ++group) {
          List<DGCMBean> groupBean = this.dgcMBeans.get(group);
          for (int server = 0; server < groupBean.size(); ++server) {
            DGCMBean mbean = groupBean.get(server);
            GCStats[] stats = mbean.getGarbageCollectorStats();
            int totalActiveObjects = 0;
            int gcObjects = 0;
            for (int i = 0; i < stats.length; ++i) {
              gcObjects += Long.valueOf(stats[i].getActualGarbageCount());
            }
            System.out.println("server: " + server);
            if (server == 0) {
              Assert.assertTrue("No garbage collected!", gcObjects > 0);
              totalActiveObjects = gcObjects;
            } else {
              Assert.assertEquals(totalActiveObjects, gcObjects);
            }
          }

        }
      }
    }
  }

}
