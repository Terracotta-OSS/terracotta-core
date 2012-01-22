/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

abstract class AbstractEmbeddedOSGiRuntime implements EmbeddedOSGiRuntime {

  static class Message {

    static final Message INSTALLING_BUNDLE              = new Message("installing.bundle");
    static final Message BUNDLE_INSTALLED               = new Message("bundle.installed");
    static final Message UNINSTALLING_BUNDLE            = new Message("uninstalling.bundle");
    static final Message BUNDLE_UNINSTALLED             = new Message("bundle.uninstalled");
    static final Message STARTING_BUNDLE                = new Message("starting.bundle");
    static final Message BUNDLE_STARTED                 = new Message("bundle.started");
    static final Message STOPPING_BUNDLE                = new Message("stopping.bundle");
    static final Message BUNDLE_STOPPED                 = new Message("bundle.stopped");
    static final Message REGISTERING_SERVICE            = new Message("registering.service");
    static final Message SERVICE_REGISTERED             = new Message("service.registered");
    static final Message STOPPING_FRAMEWORK             = new Message("stopping.framework");
    static final Message SHUTDOWN                       = new Message("framework.shutdown");

    static final Message WARN_MISSING_REPOSITORY        = new Message("warn.missing.repository");
    static final Message WARN_SKIPPED_ALREADY_ACTIVE    = new Message("warn.skipped.activation");
    static final Message WARN_SKIPPED_FILE_INSTALLATION = new Message("warn.skipped.file.installation");
    static final Message WARN_SKIPPED_FILE_UNREADABLE   = new Message("warn.skipped.file.unreadable");
    static final Message WARN_SKIPPED_MISNAMED_FILE     = new Message("warn.skipped.misnamed.file");
    static final Message ERROR_INVALID_REPOSITORY       = new Message("error.missing.repository");
    static final Message ERROR_BUNDLE_INACCESSIBLE      = new Message("error.bundle.inaccessible");
    static final Message ERROR_BUNDLE_NOT_FOUND         = new Message("error.bundle.not.found");
    static final Message ERROR_BUNDLE_URL_UNRESOLVABLE  = new Message("error.bundle.unresolved");
    static final Message ERROR_REQUIRED_BUNDLE_MISSING  = new Message("error.bundle.dependency.missing");

    private final String resourceBundleKey;

    private Message(final String resourceBundleKey) {
      this.resourceBundleKey = resourceBundleKey;
    }

    String key() {
      return resourceBundleKey;
    }
  }

  private static final TCLogger       logger        = CustomerLogging.getDSORuntimeLogger();

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

  protected final void warn(final Message message, final Object[] arguments) {
    warn(formatMessage(message, arguments));
  }

  protected void warn(final String message) {
    logger.warn(message);
  }

  protected final void fatal(final String message) throws BundleException {
    logger.fatal(message);
    throw new BundleException(message);
  }

  protected final void exception(final Message message, final Object[] arguments, final Throwable t)
      throws BundleException {
    final String msg = formatMessage(message, arguments);
    logger.error(msg, t);
    throw new BundleException(msg, t);
  }

  private final static String formatMessage(final Message message, final Object[] arguments) {
    return MessageFormat.format(resourceBundle.getString(message.key()), arguments);
  }

}
