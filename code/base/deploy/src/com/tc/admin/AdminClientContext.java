/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.dijon.ComponentResource;
import org.dijon.DictionaryResource;
import org.dijon.Resource;

import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IServer;
import com.tc.util.ResourceBundleHelper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;

public class AdminClientContext {
  private AdminClient           client;
  private AdminClientController controller;
  private ResourceBundleHelper  bundleHelper;
  private DictionaryResource    topRes;
  private AbstractNodeFactory   nodeFactory;
  private Preferences           prefs;
  private ExecutorService       executorService;

  AdminClientContext(AdminClient client, ResourceBundleHelper bundleHelper, DictionaryResource topRes,
                     AbstractNodeFactory nodeFactory, Preferences prefs, ExecutorService executorService) {
    this.client = client;
    this.bundleHelper = bundleHelper;
    this.topRes = topRes;
    this.nodeFactory = nodeFactory;
    this.prefs = prefs;
    this.executorService = executorService;
  }

  public void setController(AdminClientController controller) {
    this.controller = controller;
  }

  public AdminClient getClient() {
    return this.client;
  }

  public AdminClientController getController() {
    return this.controller;
  }

  public ResourceBundleHelper getBundleHelper() {
    return this.bundleHelper;
  }

  public DictionaryResource getTopRes() {
    return this.topRes;
  }

  public AbstractNodeFactory getNodeFactory() {
    return this.nodeFactory;
  }

  public Preferences getPrefs() {
    return this.prefs;
  }

  public void execute(Runnable r) {
    this.executorService.execute(r);
  }

  public <T> Future<T> submitTask(Callable<T> task) {
    return this.executorService.submit(task);
  }

  public Future<?> submit(Runnable task) {
    return this.executorService.submit(task);
  }

  public ComponentResource getComponent(String path) {
    return this.topRes.getComponent(path);
  }

  public Resource childResource(String tag) {
    return this.topRes.child(tag);
  }

  public Object resolveResource(String path) {
    return this.topRes.resolve(path);
  }

  public String getMessage(String id) {
    return getString(id);
  }

  public String getString(String id) {
    return bundleHelper.getString(id);
  }

  public String format(final String key, Object... args) {
    return bundleHelper.format(key, args);
  }

  public String[] getMessages(final String[] ids) {
    String[] result = null;

    if (ids != null && ids.length > 0) {
      int count = ids.length;
      result = new String[count];
      for (int i = 0; i < count; i++) {
        result[i] = getMessage(ids[i]);
      }
    }

    return result;
  }

  public Object getObject(String id) {
    return bundleHelper.getObject(id);
  }

  public void log(String msg) {
    controller.log(msg);
  }

  public void log(Throwable t) {
    controller.log(t);
  }

  public void setStatus(String msg) {
    controller.setStatus(msg);
  }

  public void clearStatus() {
    this.controller.clearStatus();
  }

  public void storePrefs() {
    getClient().storePrefs();
  }

  public void block() {
    if (this.controller != null) {
      this.controller.block();
    }
  }

  public void unblock() {
    if (this.controller != null) {
      this.controller.unblock();
    }
  }

  public boolean isExpanded(XTreeNode node) {
    return this.controller != null ? this.controller.isExpanded(node) : false;
  }

  public void expand(XTreeNode node) {
    if (this.controller != null) {
      this.controller.expand(node);
    }
  }

  public void nodeChanged(XTreeNode node) {
    if (this.controller != null) {
      this.controller.nodeChanged(node);
    }
  }

  public void nodeStructureChanged(XTreeNode node) {
    if (this.controller != null) {
      this.controller.nodeStructureChanged(node);
    }
  }

  public void remove(XTreeNode node) {
    if (this.controller != null) {
      this.controller.remove(node);
    }
  }

  public void select(XTreeNode node) {
    if (this.controller != null) {
      this.controller.select(node);
    }
  }

  public void removeServerLog(IServer server) {
    if (this.controller != null) {
      this.controller.removeServerLog(server);
    }
  }

  public void addServerLog(IServer server) {
    if (this.controller != null) {
      this.controller.addServerLog(server);
    }
  }

  public boolean testServerMatch(ClusterNode node) {
    return this.controller.testServerMatch(node);
  }

  public boolean testServerMatch(ServerNode node) {
    return this.controller.testServerMatch(node);
  }

  public void updateServerPrefs() {
    this.controller.updateServerPrefs();
  }
}
