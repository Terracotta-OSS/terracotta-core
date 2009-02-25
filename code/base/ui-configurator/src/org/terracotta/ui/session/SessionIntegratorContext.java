/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.ApplicationController;
import com.tc.admin.common.ApplicationContext;
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

public class SessionIntegratorContext implements ApplicationContext {
  public SessionIntegrator           client;
  public ResourceBundleHelper        bundleHelper;
  public Preferences                 prefs;
  private final ExecutorService      executorService = Executors.newCachedThreadPool();
  private final Map<String, IOption> optionMap;

  SessionIntegratorContext(SessionIntegrator client) {
    this.client = client;
    this.bundleHelper = new ResourceBundleHelper(client.getClass());
    this.prefs = client.loadPrefs();
    this.optionMap = new LinkedHashMap<String, IOption>();
  }

  public SessionIntegrator getClient() {
    return this.client;
  }

  public String getMessage(String id) {
    return getString(id);
  }

  public String getString(String id) {
    return bundleHelper.getString(id);
  }

  public String[] getMessages(String[] ids) {
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

  public void registerOption(IOption option) {
    optionMap.put(option.getName(), option);
  }

  public Iterator<IOption> options() {
    return optionMap.values().iterator();
  }

  public IOption getOption(String name) {
    return optionMap.get(name);
  }

  public Object getObject(String id) {
    return bundleHelper.getObject(id);
  }

  public void clearStatus() {
    /**/
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

  public String format(final String key, Object... args) {
    return bundleHelper.format(key, args);
  }

  public Preferences getPrefs() {
    return prefs;
  }

  public void storePrefs() {
    getClient().storePrefs();
  }

  public void log(String msg) {
    /**/
  }

  public void log(Throwable t) {
    /**/
  }

  public void setStatus(String msg) {
    /**/
  }

  public void block() {
    /**/
  }

  public void unblock() {
    /**/
  }

  public ApplicationController getApplicationController() {
    return null;
  }
}
