/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

abstract class AbstractEmbeddedOSGiRuntime implements EmbeddedOSGiRuntime {

  static class Message {

    static final Message INSTALLING_BUNDLE   = new Message("installing.bundle");
    static final Message BUNDLE_INSTALLED    = new Message("bundle.installed");
    static final Message UNINSTALLING_BUNDLE = new Message("uninstalling.bundle");
    static final Message BUNDLE_UNINSTALLED  = new Message("bundle.uninstalled");
    static final Message STARTING_BUNDLE     = new Message("starting.bundle");
    static final Message BUNDLE_STARTED      = new Message("bundle.started");
    static final Message STOPPING_BUNDLE     = new Message("stopping.bundle");
    static final Message BUNDLE_STOPPED      = new Message("bundle.stopped");
    static final Message REGISTERING_SERVICE = new Message("registering.service");
    static final Message SERVICE_REGISTERED  = new Message("service.registered");
    static final Message STOPPING_FRAMEWORK  = new Message("stopping.framework");
    static final Message SHUTDOWN            = new Message("framework.shutdown");

    private final String resourceBundleKey;

    private Message(final String resourceBundleKey) {
      this.resourceBundleKey = resourceBundleKey;
    }

    String key() {
      return resourceBundleKey;
    }
  }

  private static final TCLogger       logger = CustomerLogging.getDSORuntimeLogger();
  private static final ResourceBundle resourceBundle;

  static {
    try {
      resourceBundle = ResourceBundle.getBundle(EmbeddedOSGiRuntime.class.getName(), Locale.getDefault(),
          EmbeddedOSGiRuntime.class.getClassLoader());
    } catch (MissingResourceException mre) {
      throw new RuntimeException("No resource bundle exists for " + EmbeddedOSGiRuntime.class.getName());
    } catch (Throwable t) {
      throw new RuntimeException("Unexpected error loading resource bundle for " + EmbeddedOSGiRuntime.class.getName(),
          t);
    }
  }

  protected final void info(final Message message, final Object[] arguments) {
    logger.info(formatMessage(message, arguments));
  }

  private final static String formatMessage(final Message message, final Object[] arguments) {
    return MessageFormat.format(resourceBundle.getString(message.key()), arguments);
  }

}
