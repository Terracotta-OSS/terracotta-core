/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import com.tc.admin.ApplicationController;
import com.tc.admin.options.IOption;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;

public interface ApplicationContext {
  IApplication getClient();

  ApplicationController getApplicationController();

  Preferences getPrefs();

  void execute(Runnable r);

  <T> Future<T> submitTask(Callable<T> task);

  Future<?> submit(Runnable task);

  String getMessage(String id);

  String getString(String id);

  Object getObject(String id);

  String format(final String key, Object... args);

  String[] getMessages(final String[] ids);

  void log(String msg);

  void log(Throwable t);

  void setStatus(String msg);

  void clearStatus();

  void storePrefs();

  void block();

  void unblock();

  void registerOption(IOption option);

  IOption getOption(String name);

  Iterator<IOption> options();
}
