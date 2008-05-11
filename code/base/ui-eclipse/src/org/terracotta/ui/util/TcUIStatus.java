/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.terracotta.dso.TcPlugin;

/**
 * Convenience class for error exceptions thrown inside TcPlugin.
 */
public class TcUIStatus extends Status {

  private TcUIStatus(int severity, int code, String message, Throwable throwable) {
    super(severity, TcPlugin.getPluginId(), code, message, throwable);
  }

  public static IStatus createError(int code, Throwable throwable) {
    String message = throwable.getMessage();
    if (message == null) {
      message = throwable.getClass().getName();
    }
    return new TcUIStatus(IStatus.ERROR, code, message, throwable);
  }

  public static IStatus createError(int code, String message, Throwable throwable) {
    return new TcUIStatus(IStatus.ERROR, code, message, throwable);
  }

  public static IStatus createWarning(int code, String message, Throwable throwable) {
    return new TcUIStatus(IStatus.WARNING, code, message, throwable);
  }

  public static IStatus createInfo(int code, String message, Throwable throwable) {
    return new TcUIStatus(IStatus.INFO, code, message, throwable);
  }
}
