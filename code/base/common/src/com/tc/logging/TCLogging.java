/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.varia.NullAppender;

import com.tc.util.Assert;
import com.tc.util.ProductInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;

/**
 * Factory class for obtaining TCLogger instances.
 *
 * @author teck
 */
public class TCLogging {

  private static final int          MAX_BUFFERED_LOG_MESSAGES          = 10 * 1000;

  private static final String       TERRACOTTA_L1_LOG_FILE_NAME        = "terracotta-client.log";
  private static final String       TERRACOTTA_L2_LOG_FILE_NAME        = "terracotta-server.log";
  private static final String       TERRACOTTA_GENERIC_LOG_FILE_NAME   = "terracotta-generic.log";

  private static final String       LOCK_FILE_NAME                     = ".terracotta-logging.lock";

  private static final String       INTERNAL_LOGGER_NAMESPACE          = "com.tc";
  private static final String       INTERNAL_LOGGER_NAMESPACE_WITH_DOT = INTERNAL_LOGGER_NAMESPACE + ".";

  private static final String       CUSTOMER_LOGGER_NAMESPACE          = "com.terracottatech";
  private static final String       CUSTOMER_LOGGER_NAMESPACE_WITH_DOT = CUSTOMER_LOGGER_NAMESPACE + ".";

  private static final String       CONSOLE_LOGGER_NAME                = CUSTOMER_LOGGER_NAMESPACE + ".console";

  private static final String       BENCH_LOGGER_NAME                  = "terracotta.bench";
  private static final String       MAX_LOG_FILE_SIZE                  = "512MB";
  private static final int          MAX_BACKUPS                        = 20;
  private static final String       LOG4J_PROPERTIES_FILENAME          = ".tc.dev.log4j.properties";

  private static final String       CONSOLE_PATTERN                    = "%d %p - %m%n";
  private static final String       CONSOLE_PATTERN_DEVELOPMENT        = "%d [%t] %p %c - %m%n";
  // This next pattern is used when we're *only* logging to the console.
  private static final String       CONSOLE_LOGGING_ONLY_PATTERN       = "[TC] %d %p - %m%n";
  private static final String       FILE_AND_JMX_PATTERN               = "%d [%t] %p %c - %m%n";

  private static TCLogger           console;
  private static JMXAppender         jmxAppender;
  private static Appender           consoleAppender;
  private static String             logFile;
  private static DelegatingAppender delegateFileAppender;
  private static DelegatingAppender delegateBufferingAppender;
  private static boolean            buffering;

  private static Logger[]           allLoggers;

  private static File               currentLoggingDirectory;
  private static FileLock           currentLoggingDirectoryFileLock    = null;
  private static boolean            lockingDisabled                    = false;

  public static JMXAppender getJMXAppender() {
    return jmxAppender;
  }

  public static String getLogFileLocation() {
    return logFile;
  }

  public static TCLogger getLogger(Class clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getLogger(clazz.getName());
  }

  public static TCLogger getLogger(String name) {
    if ((name == null) || !name.startsWith(INTERNAL_LOGGER_NAMESPACE_WITH_DOT)) {
      // this comment here to make formatter sane
      throw new IllegalArgumentException("Logger not in valid namepsace (ie. '" + INTERNAL_LOGGER_NAMESPACE_WITH_DOT
          + "' ): " + name);
    }

    return new TCLoggerImpl(name);
  }

  /**
   * This method lets you get a logger w/o any name restrictoins. FOR TESTS ONLY (ie. not for shipping code)
   */
  public static TCLogger getTestingLogger(String name) {
    if (name == null) { throw new IllegalArgumentException("Name cannot be null"); }
    return new TCLoggerImpl(name);
  }

  /**
   * This method lets you get a logger w/o any name restrictoins. FOR TESTS ONLY (ie. not for shipping code)
   */
  public static TCLogger getTestingLogger(Class clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getTestingLogger(clazz.getName());
  }

  public static TCLogger getBenchLogger() {
    return new TCLoggerImpl(BENCH_LOGGER_NAME);
  }

  // You want to look at CustomerLogging to get customer facing logger instances
  static TCLogger getCustomerLogger(String name) {
    if (name == null) { throw new IllegalArgumentException("name cannot be null"); }

    name = CUSTOMER_LOGGER_NAMESPACE_WITH_DOT + name;

    if (CONSOLE_LOGGER_NAME.equals(name)) { throw new IllegalArgumentException("Illegal name: " + name); }

    return new TCLoggerImpl(name);
  }

  // this method not public on purpose, use CustomerLogging.getConsoleLogger() instead
  static TCLogger getConsoleLogger() {
    return console;
  }

  private static void reportLoggingError(Exception e) {
    reportLoggingError(null, e);
  }

  private static void reportLoggingError(String message, Exception e) {
    StringBuffer errorMsg = new StringBuffer("\n");

    if (message != null) {
      errorMsg.append("WARN: ").append(message).append("\n");
    }

    if (e != null) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      errorMsg.append(sw.toString());
    }

    System.err.println(errorMsg.toString());
  }

  private static boolean developmentConfiguration() {
    try {
      File props = new File(System.getProperty("user.dir"), LOG4J_PROPERTIES_FILENAME);

      if (props.canRead() && props.isFile()) {
        Logger.getRootLogger().setLevel(Level.INFO);
        PropertyConfigurator.configure(props.getAbsolutePath());
        return true;
      }
    } catch (Exception e) {
      reportLoggingError(e);
    }

    return false;
  }

  /**
   * <strong>FOR TESTS ONLY</strong>. This allows tests to successfully blow away directories containing log files on
   * Windows. This is a bit of a hack, but working around it is otherwise an enormous pain &mdash; tests only fail on
   * Windows, and you must then very carefully go around, figure out exactly why, and then work around it. Use of this
   * method makes everything a great deal simpler.
   */
  public static synchronized void disableLocking() {
    lockingDisabled = true;

    if (currentLoggingDirectoryFileLock != null) {
      try {
        currentLoggingDirectoryFileLock.release();
        currentLoggingDirectoryFileLock = null;
      } catch (IOException ioe) {
        throw Assert.failure("Unable to release file lock?", ioe);
      }
    }
  }

  public static final int PROCESS_TYPE_GENERIC = 0;
  public static final int PROCESS_TYPE_L1      = 1;
  public static final int PROCESS_TYPE_L2      = 2;

  public static void setLogDirectory(File theDirectory, int processType) {
    Assert.assertNotNull(theDirectory);

    if (theDirectory.getName().trim().equalsIgnoreCase("stdout:")
        || theDirectory.getName().trim().equalsIgnoreCase("stderr:")) {
      if (currentLoggingDirectory != null
          && currentLoggingDirectory.getName().trim().equalsIgnoreCase(theDirectory.getName())) {
        // Nothing to do; great!
        return;
      }

      delegateFileAppender.setDelegate(new NullAppender());
      consoleAppender.setLayout(new PatternLayout(CONSOLE_LOGGING_ONLY_PATTERN));

      // Logger.addAppender() doesn't double-add, so this is safe
      Logger.getRootLogger().addAppender(consoleAppender);

      if (buffering) {
        BufferingAppender realBufferingAppender = (BufferingAppender) delegateBufferingAppender
            .setDelegate(new NullAppender());
        realBufferingAppender.stopAndSendContentsTo(consoleAppender);
        realBufferingAppender.close();
        buffering = false;
      }

      boolean stdout = theDirectory.getName().trim().equalsIgnoreCase("stdout:");
      getConsoleLogger().info("All logging information now output to standard " + (stdout ? "output" : "error") + ".");

      return;
    }

    synchronized (TCLogging.class) {
      if (currentLoggingDirectory != null) {
        try {
          if (theDirectory.getCanonicalPath().equals(currentLoggingDirectory.getCanonicalPath())) { return; }
        } catch (IOException ioe) {
          // oh, well -- what can we do? we'll continue on.
        }
      }
    }

    try {
      FileUtils.forceMkdir(theDirectory);
    } catch (IOException ioe) {
      reportLoggingError("We can't create the directory '" + theDirectory.getAbsolutePath()
          + "' that you specified for your logs.", ioe);
      return;
    }

    if (!theDirectory.canWrite()) {
      // formatting
      reportLoggingError("The log directory, '" + theDirectory.getAbsolutePath() + "', can't be written to.", null);
      return;
    }

    FileLock thisDirectoryLock = null;

    if (!lockingDisabled) {
      File lockFile = new File(theDirectory, LOCK_FILE_NAME);

      try {
        if (!lockFile.exists()) lockFile.createNewFile();
        Assert.eval(lockFile.exists());
        FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
        thisDirectoryLock = channel.tryLock();

        if (thisDirectoryLock == null) {
          reportLoggingError("The log directory, '" + theDirectory.getAbsolutePath()
              + "', is already in use by another " + "Terracotta process. Logging will proceed to the console only.",
              null);
          return;
        }

      } catch (OverlappingFileLockException ofle) {
        // This VM already holds the lock; no problem
      } catch (IOException ioe) {
        reportLoggingError("We can't lock the file '" + lockFile.getAbsolutePath() + "', to make sure that only one "
            + "Terracotta process is using this directory for logging. This may be a permission "
            + "issue, or some unexpected error. Logging will proceed to the console only.", ioe);
        return;
      }
    }

    RollingFileAppender newFileAppender;

    String logFileName;

    switch (processType) {
      case PROCESS_TYPE_L1:
        logFileName = TERRACOTTA_L1_LOG_FILE_NAME;
        break;

      case PROCESS_TYPE_L2:
        logFileName = TERRACOTTA_L2_LOG_FILE_NAME;
        break;

      case PROCESS_TYPE_GENERIC:
        logFileName = TERRACOTTA_GENERIC_LOG_FILE_NAME;
        break;

      default:
        throw Assert.failure("Unknown process type: " + processType);
    }

    String logFilePath = new File(theDirectory, logFileName).getAbsolutePath();

    synchronized (TCLogging.class) {
      try {
        newFileAppender = new RollingFileAppender(new PatternLayout(FILE_AND_JMX_PATTERN), logFilePath, true);
        newFileAppender.setName("file appender");
        newFileAppender.setMaxFileSize(MAX_LOG_FILE_SIZE);
        newFileAppender.setMaxBackupIndex(MAX_BACKUPS);

        // This makes us start with a new file each time.
        newFileAppender.rollOver();

        // Note: order of operations is very important here. We start the new appender before we close and remove the
        // old one so that you don't drop any log records.
        Appender oldFileAppender = delegateFileAppender.setDelegate(newFileAppender);

        if (oldFileAppender != null) {
          oldFileAppender.close();
        }

        if (buffering) {
          BufferingAppender realBufferingAppender = (BufferingAppender) delegateBufferingAppender
              .setDelegate(new NullAppender());
          realBufferingAppender.stopAndSendContentsTo(delegateFileAppender);
          realBufferingAppender.close();
          buffering = false;
        }

        currentLoggingDirectory = theDirectory;

        if (currentLoggingDirectoryFileLock != null) currentLoggingDirectoryFileLock.release();
        currentLoggingDirectoryFileLock = thisDirectoryLock;
      } catch (IOException ioe) {
        reportLoggingError("We were unable to switch the logging system to log to '" + logFilePath + "'.", ioe);
      }
    }

    getConsoleLogger().info("Log file: '" + logFilePath + "'.");
    writeSystemProperties();
  }

  static {
    ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(TCLogging.class.getClassLoader());

    try {
      currentLoggingDirectory = null;

      Logger jettyLogger = Logger.getLogger("org.mortbay");
      jettyLogger.setLevel(Level.OFF);

      Logger internalLogger = Logger.getLogger(INTERNAL_LOGGER_NAMESPACE);
      Logger customerLogger = Logger.getLogger(CUSTOMER_LOGGER_NAMESPACE);
      Logger consoleLogger = Logger.getLogger(CONSOLE_LOGGER_NAME);
      Logger benchLogger = Logger.getLogger(BENCH_LOGGER_NAME);

      allLoggers = new Logger[] { internalLogger, customerLogger, consoleLogger, benchLogger };

      console = new TCLoggerImpl(CONSOLE_LOGGER_NAME);

      internalLogger.setLevel(Level.INFO);
      customerLogger.setLevel(Level.INFO);
      consoleLogger.setLevel(Level.INFO);
      benchLogger.setLevel(Level.INFO);

      boolean isDev = developmentConfiguration();

      consoleAppender = new ConsoleAppender(new PatternLayout(CONSOLE_PATTERN), ConsoleAppender.SYSTEM_ERR);

      if (!isDev) {
        // Only the console logger goes to the console (by default)
        consoleLogger.addAppender(consoleAppender);
      } else {
        consoleAppender.setLayout(new PatternLayout(CONSOLE_PATTERN_DEVELOPMENT));
        // For non-customer environments, send all logging to the console...
        Logger.getRootLogger().addAppender(consoleAppender);
      }

      delegateFileAppender = new DelegatingAppender(new NullAppender());
      addToAllLoggers(delegateFileAppender);

      BufferingAppender realBufferingAppender;
      realBufferingAppender = new BufferingAppender(MAX_BUFFERED_LOG_MESSAGES);
      realBufferingAppender.setName("buffering appender");
      delegateBufferingAppender = new DelegatingAppender(realBufferingAppender);
      addToAllLoggers(delegateBufferingAppender);
      buffering = true;

      // all logging goes to JMX based appender
      jmxAppender = new JMXAppender();
      jmxAppender.setLayout(new PatternLayout(FILE_AND_JMX_PATTERN));
      jmxAppender.setName("JMX appender");
      addToAllLoggers(jmxAppender);

      if (!isDev) {
        CustomerLogging.getGenericCustomerLogger().info("New logging session started.");
      }

      writeVersion();
    } catch (Exception e) {
      reportLoggingError(e);
    } finally {
      Thread.currentThread().setContextClassLoader(prevLoader);
    }
  }

  // for test use only!
  public static void addAppender(String loggerName, TCAppender appender) {
    new TCLoggerImpl(loggerName).getLogger().addAppender(new Log4JAappenderToTCAppender(appender));
  }

  private static void addToAllLoggers(Appender appender) {
    for (int i = 0; i < allLoggers.length; ++i)
      allLoggers[i].addAppender(appender);
  }

  private static void writeVersion() {
    ProductInfo info = ProductInfo.getThisProductInfo();
    String moniker = info.moniker();

    CustomerLogging.getConsoleLogger().info(
        moniker + ", version " + info.rawVersion() + " as of " + info.buildTimestampAsString() + ".");
    getLogger(TCLogging.class).info(moniker + " version: " + info.toLongString());
  }

  private static void writeSystemProperties() {
    Properties properties = System.getProperties();
    int maxKeyLength = 1;

    Iterator iter = properties.keySet().iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      maxKeyLength = Math.max(maxKeyLength, key == null ? 0 : key.length());
    }

    StringBuffer data = new StringBuffer();
    data.append("========================================================================\n");
    data.append("All Java System Properties for this Terracotta instance:\n");

    String[] keys = (String[]) properties.keySet().toArray(new String[properties.size()]);
    Arrays.sort(keys);
    for (int i = 0; i < keys.length; ++i) {
      String key = keys[i];
      String value = (String) properties.get(key);

      while (key.length() < maxKeyLength)
        key += " ";

      data.append(key + ": " + value + "\n");
    }

    data.append("========================================================================\n");

    getLogger(TCLogging.class).info(data.toString());
  }

  // This method for use in tests only
  public static void closeFileAppender() {
    if (delegateFileAppender != null) delegateFileAppender.close();
  }

}
