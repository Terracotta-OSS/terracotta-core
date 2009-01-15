/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.options.IOption;
import com.tc.util.ResourceBundleHelper;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;

public class AdminClientContext implements IAdminClientContext {
  private AdminClient               client;
  private AdminClientController     controller;
  private ResourceBundleHelper      bundleHelper;
  private Preferences               prefs;
  private final AbstractNodeFactory nodeFactory     = AbstractNodeFactory.getFactory();
  private final ExecutorService     executorService = Executors.newCachedThreadPool();
  private Map<String, IOption>      optionMap;

  public AdminClientContext(AdminClient client) {
    this.client = client;
    this.bundleHelper = new ResourceBundleHelper(client.getClass());
    this.prefs = client.loadPrefs();
    this.optionMap = new LinkedHashMap<String, IOption>();
  }

  public void setAdminClientController(AdminClientController controller) {
    this.controller = controller;
  }
  
  public AdminClientController getAdminClientController() {
    return this.controller;
  }

  public ApplicationController getApplicationController() {
    return getAdminClientController();
  }
  
  public AdminClient getClient() {
    return this.client;
  }

  public ResourceBundleHelper getBundleHelper() {
    return this.bundleHelper;
  }

  public AbstractNodeFactory getNodeFactory() {
    return this.nodeFactory;
  }

  public Preferences getPrefs() {
    return this.prefs;
  }

  public void registerOption(IOption option) {
    optionMap.put(option.getName(), option);
  }
  
  public Iterator<IOption> options() {
    return optionMap.values().iterator();
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
}
