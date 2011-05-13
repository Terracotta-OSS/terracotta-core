/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.varia.NullAppender;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

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

  private static final String[]     INTERNAL_LOGGER_NAMESPACES         = new String[] { "com.tc", "com.terracotta",
      "com.terracottatech", "org.terracotta", "tc.operator"           };

  private static final String       CUSTOMER_LOGGER_NAMESPACE          = "com.terracottatech";
  private static final String       CUSTOMER_LOGGER_NAMESPACE_WITH_DOT = CUSTOMER_LOGGER_NAMESPACE + ".";

  private static final String       CONSOLE_LOGGER_NAME                = CUSTOMER_LOGGER_NAMESPACE + ".console";
  public static final String        DUMP_LOGGER_NAME                   = "com.tc.dumper.dump";
  public static final String        DERBY_LOGGER_NAME                  = "com.tc.derby.log";
  private static final String       OPERATOR_EVENT_LOGGER_NAME         = "tc.operator.event";

  private static final String       LOGGING_PROPERTIES_SECTION         = "logging";
  private static final String       MAX_LOG_FILE_SIZE_PROPERTY         = "maxLogFileSize";
  private static final int          DEFAULT_MAX_LOG_FILE_SIZE          = 512;
  private static final String       MAX_BACKUPS_PROPERTY               = "maxBackups";
  private static final int          DEFAULT_MAX_BACKUPS                = 20;
  private static final String       LOG4J_PROPERTIES_FILENAME          = ".tc.dev.log4j.properties";

  private static final String       CONSOLE_PATTERN                    = "%d %p - %m%n";
  public static final String        DUMP_PATTERN                       = "[dump] %m%n";
  public static final String        DERBY_PATTERN                      = "[derby.log] %m%n";
  private static final String       CONSOLE_PATTERN_DEVELOPMENT        = "%d [%t] %p %c - %m%n";
  // This next pattern is used when we're *only* logging to the console.
  private static final String       CONSOLE_LOGGING_ONLY_PATTERN       = "[TC] %d %p - %m%n";
  public static final String        FILE_AND_JMX_PATTERN               = "%d [%t] %p %c - %m%n";

  private static TCLogger           console;
  private static TCLogger           operatorEventLogger;
  private static Appender           consoleAppender;
  private static DelegatingAppender delegateFileAppender;
  private static DelegatingAppender delegateBufferingAppender;
  private static boolean            buffering;

  private static Logger[]           allLoggers;

  private static File               currentLoggingDirectory;
  private static FileLock           currentLoggingDirectoryFileLock    = null;
  private static boolean            lockingDisabled                    = false;

  public static TCLogger getLogger(Class clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getLogger(clazz.getName());
  }

  public static TCLogger getLogger(String name) {
    if (name == null) { throw new NullPointerException("Logger cannot be null"); }

    boolean allowedName = false;
    for (String namespace : INTERNAL_LOGGER_NAMESPACES) {
      String withDot = namespace + ".";
      if (name.startsWith(withDot)) {
        allowedName = true;
        break;
      }
    }

    if (!allowedName) {
      //
      throw new IllegalArgumentException("Logger name (" + name + ") not in valid namespace: "
                                         + Arrays.asList(INTERNAL_LOGGER_NAMESPACES));
    }

    return new TCLoggerImpl(name);
  }

  /**
   * This method lets you get a logger w/o any name restrictions. FOR TESTS ONLY (ie. not for shipping code)
   */
  public static TCLogger getTestingLogger(String name) {
    if (name == null) { throw new IllegalArgumentException("Name cannot be null"); }
    return new TCLoggerImpl(name);
  }

  /**
   * This method lets you get a logger w/o any name restrictions. FOR TESTS ONLY (ie. not for shipping code)
   */
  public static TCLogger getTestingLogger(Class clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getTestingLogger(clazz.getName());
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

  static TCLogger getOperatorEventLogger() {
    return operatorEventLogger;
  }

  private static void reportLoggingError(Exception e) {
    reportLoggingError(null, e);
  }

  private static void reportLoggingError(String message, Exception e) {
    String newLine = System.getProperty("line.separator");
    StringBuffer errorMsg = new StringBuffer(newLine);

    if (message != null) {
      errorMsg.append("WARN: ").append(message).append(newLine);
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
      Properties devLoggingProperties = new Properties();

      // Specify the order of LEAST importantance; last one in wins
      File[] devLoggingLocations = new File[] { new File(System.getProperty("user.home"), LOG4J_PROPERTIES_FILENAME),
          new File(System.getProperty("user.dir"), LOG4J_PROPERTIES_FILENAME) };

      boolean devLog4JPropsFilePresent = false;
      for (int pos = 0; pos < devLoggingLocations.length; ++pos) {
        File propFile = devLoggingLocations[pos];
        if (propFile.isFile() && propFile.canRead()) {
          devLog4JPropsFilePresent = true;
          InputStream in = new FileInputStream(propFile);
          try {
            devLoggingProperties.load(in);
          } finally {
            in.close();
          }
        }
      }
      if (devLog4JPropsFilePresent) {
        Logger.getRootLogger().setLevel(Level.INFO);
        PropertyConfigurator.configure(devLoggingProperties);
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
        currentLoggingDirectoryFileLock.channel().close();
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
        if (!lockFile.exists()) {
          if (!lockFile.createNewFile()) {
            Assert.fail("Failed to create lock file");
          }
        }
        Assert.eval(lockFile.exists());
        FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
        thisDirectoryLock = channel.tryLock();

        if (thisDirectoryLock == null) {
          reportLoggingError("The log directory, '" + theDirectory.getAbsolutePath()
                             + "', is already in use by another "
                             + "Terracotta process. Logging will proceed to the console only.", null);
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
        TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(LOGGING_PROPERTIES_SECTION);
        newFileAppender = new TCRollingFileAppender(new PatternLayout(FILE_AND_JMX_PATTERN), logFilePath, true);
        newFileAppender.setName("file appender");
        int maxLogFileSize = props.getInt(MAX_LOG_FILE_SIZE_PROPERTY, DEFAULT_MAX_LOG_FILE_SIZE);
        newFileAppender.setMaxFileSize(maxLogFileSize + "MB");
        newFileAppender.setMaxBackupIndex(props.getInt(MAX_BACKUPS_PROPERTY, DEFAULT_MAX_BACKUPS));

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

  public static TCLogger getDumpLogger() {
    return new TCLoggerImpl(DUMP_LOGGER_NAME);
  }

  public static TCLogger getDerbyLogger() {
    return new TCLoggerImpl(DERBY_LOGGER_NAME);
  }

  static {
    ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(TCLogging.class.getClassLoader());

    Log4jSafeInit.init();

    try {
      currentLoggingDirectory = null;

      Logger jettyLogger = Logger.getLogger("org.mortbay");
      jettyLogger.setLevel(Level.OFF);

      List<Logger> internalLoggers = new ArrayList<Logger>();
      for (String nameSpace : INTERNAL_LOGGER_NAMESPACES) {
        internalLoggers.add(Logger.getLogger(nameSpace));
      }
      Logger customerLogger = Logger.getLogger(CUSTOMER_LOGGER_NAMESPACE);
      Logger consoleLogger = Logger.getLogger(CONSOLE_LOGGER_NAME);

      /**
       * Don't add consoleLogger to allLoggers because it's a child of customerLogger, so it shouldn't get any
       * appenders. If you DO add consoleLogger here, you'll see duplicate messages in the log file.
       */
      allLoggers = createAllLoggerList(internalLoggers, customerLogger);

      console = new TCLoggerImpl(CONSOLE_LOGGER_NAME);

      operatorEventLogger = new TCLoggerImpl(OPERATOR_EVENT_LOGGER_NAME);

      for (Logger internalLogger : internalLoggers) {
        internalLogger.setLevel(Level.INFO);
      }
      customerLogger.setLevel(Level.INFO);
      consoleLogger.setLevel(Level.INFO);

      boolean isDev = developmentConfiguration();

      consoleAppender = new TCConsoleAppender(new PatternLayout(CONSOLE_PATTERN), ConsoleAppender.SYSTEM_ERR);

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
  public static Log4JAppenderToTCAppender addAppender(String loggerName, TCAppender appender) {
    Log4JAppenderToTCAppender wrappedAppender = new Log4JAppenderToTCAppender(appender);
    new TCLoggerImpl(loggerName).getLogger().addAppender(wrappedAppender);
    return wrappedAppender;
  }

  public static void removeAppender(String loggerName, Log4JAppenderToTCAppender appender) {
    new TCLoggerImpl(loggerName).getLogger().removeAppender(appender);
  }

  private static Logger[] createAllLoggerList(List<Logger> internalLoggers, Logger customerLogger) {
    List<Logger> loggers = new ArrayList<Logger>();
    loggers.addAll(internalLoggers);
    loggers.add(customerLogger);
    return loggers.toArray(new Logger[] {});
  }

  public static void addToAllLoggers(Appender appender) {
    for (int i = 0; i < allLoggers.length; ++i)
      allLoggers[i].addAppender(appender);
  }

  private static void writeVersion() {
    ProductInfo info = ProductInfo.getInstance();
    TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

    // Write build info always
    String longProductString = info.toLongString();
    consoleLogger.info(longProductString);

    // Write patch info, if any
    if (info.isPatched()) {
      String longPatchString = info.toLongPatchString();
      consoleLogger.info(longPatchString);
    }
  }

  private static void writeSystemProperties() {
    try {
      Properties properties = System.getProperties();
      int maxKeyLength = 1;

      ArrayList keys = new ArrayList();
      Iterator iter = properties.entrySet().iterator();
      while (iter.hasNext()) {
        Entry entry = (Entry) iter.next();
        Object objKey = entry.getKey();
        Object objValue = entry.getValue();

        // Filter out any bad non-String keys or values in system properties
        if (objKey instanceof String && objValue instanceof String) {
          String key = (String) objKey;
          keys.add(key);
          maxKeyLength = Math.max(maxKeyLength, key.length());
        }
      }

      String inputArguments = null;
      try {
        RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
        inputArguments = mxbean.getInputArguments().toString();
      } catch (SecurityException se) {
        inputArguments = "unknown";
      }
      String nl = System.getProperty("line.separator");
      StringBuffer data = new StringBuffer();
      data.append("All Java System Properties for this Terracotta instance:");
      data.append(nl);
      data.append("========================================================================");
      data.append(nl);
      data.append("JVM arguments: " + inputArguments);
      data.append(nl);

      String[] sortedKeys = (String[]) keys.toArray(new String[keys.size()]);
      Arrays.sort(sortedKeys);
      for (int i = 0; i < sortedKeys.length; ++i) {
        String key = sortedKeys[i];
        data.append(StringUtils.rightPad(key, maxKeyLength));
        data.append(": ");
        data.append(properties.get(key));
        data.append(nl);
      }
      data.append("========================================================================");

      getLogger(TCLogging.class).info(data.toString());
    } catch (Throwable t) {
      // don't let exceptions here be fatal
      t.printStackTrace();
    }
  }

  // This method for use in tests only
  public static void closeFileAppender() {
    if (delegateFileAppender != null) delegateFileAppender.close();
  }

}
