/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.options.IOption;
import com.tc.util.ResourceBundleHelper;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;

public class AdminClientContext implements IAdminClientContext, Thread.UncaughtExceptionHandler {
  private static Logger              logger;

  private final AdminClient          client;
  private AdminClientController      controller;
  private final ResourceBundleHelper bundleHelper;
  private final Preferences          prefs;
  private final AbstractNodeFactory  nodeFactory     = AbstractNodeFactory.getFactory();
  private final ExecutorService      executorService = Executors.newCachedThreadPool();
  private final Map<String, IOption> optionMap;

  static {
    try {
      boolean append = true;
      FileHandler fh = new FileHandler("%h/.devconsole.log.%g", 1000000, 3, append);
      fh.setFormatter(new SimpleFormatter());
      logger = Logger.getLogger("DevConsole");
      logger.addHandler(fh);
      logger.setLevel(Level.ALL);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public AdminClientContext(AdminClient client) {
    this.client = client;
    this.bundleHelper = new ResourceBundleHelper(client);
    this.prefs = client.loadPrefs();
    this.optionMap = new LinkedHashMap<String, IOption>();
    Thread.setDefaultUncaughtExceptionHandler(this);
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

  public IOption getOption(String name) {
    return optionMap.get(name);
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
    logger.info(msg);
  }

  public void log(Throwable t) {
    controller.log(t);
    logger.throwing(null, null, t);
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

  public void uncaughtException(Thread t, Throwable e) {
    log(e);

    // print to console for debugging purpose. This is necessary for uncaught exception upon startup where
    // users have no idea where the log might be.
    e.printStackTrace();
  }
}
