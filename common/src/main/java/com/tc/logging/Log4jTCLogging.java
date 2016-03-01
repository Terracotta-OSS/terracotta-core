/*
 *
 *  The contents of this file are subject to the Terracotta  License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.logging;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import com.tc.util.io.FileUtils;
import com.tc.util.io.IOUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.varia.NullAppender;

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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Factory class for obtaining TCLogger instances.
 * <p>
 * Update: feb. 2016: replaces old TCLogging
 *
 * @author teck
 */
class Log4jTCLogging implements DelegatingTCLogger  {

  private static final DelegatingTCLogger DELEGATE = new Log4jTCLogging();

  public static DelegatingTCLogger getDelegate() {
    return DELEGATE;
  }

  private Log4jTCLogging() {
  }

  @Override
  public void closeFileAppender() {
    LazyLoad.closeFileAppender();
  }

  @Override
  public void setLogDirectory(File theDirectory, int processType) {
    LazyLoad.setLogDirectory(theDirectory, processType);
  }

  @Override
  public void disableLocking() {
    LazyLoad.disableLocking();
  }

  @Override
  public TCLogger newLogger(String name) {
    return new Log4jTCLogger(name);
  }

  @Override
  public TCLogger getConsoleLogger() {
    return LazyLoad.getConsoleLogger();
  }

  @Override
  public TCLogger getOperatorEventLogger() {
    return LazyLoad.getOperatorEventLogger();
  }

  static class LazyLoad {

    public static final String LOG_CONFIGURATION_PREFIX = TCLogging.LOG_CONFIGURATION_PREFIX;

    private static final int MAX_BUFFERED_LOG_MESSAGES = 10 * 1000;

    private static final String TERRACOTTA_L1_LOG_FILE_NAME = "terracotta-client.log";
    private static final String TERRACOTTA_L2_LOG_FILE_NAME = "terracotta-server.log";
    private static final String TERRACOTTA_GENERIC_LOG_FILE_NAME = "terracotta-generic.log";

    private static final String LOCK_FILE_NAME = ".terracotta-logging.lock";

    private static final String[] INTERNAL_LOGGER_NAMESPACES = TCLogging.INTERNAL_LOGGER_NAMESPACES;

    private static final String CUSTOMER_LOGGER_NAMESPACE = TCLogging.CUSTOMER_LOGGER_NAMESPACE;
    private static final String CUSTOMER_LOGGER_NAMESPACE_WITH_DOT = TCLogging.CUSTOMER_LOGGER_NAMESPACE_WITH_DOT;

    private static final String CONSOLE_LOGGER_NAME = TCLogging.CONSOLE_LOGGER_NAME;
    public static final String DUMP_LOGGER_NAME = TCLogging.DUMP_LOGGER_NAME;
    private static final String OPERATOR_EVENT_LOGGER_NAME = TCLogging.OPERATOR_EVENT_LOGGER_NAME;

    private static final String LOGGING_PROPERTIES_SECTION = "logging";
    private static final String MAX_LOG_FILE_SIZE_PROPERTY = "maxLogFileSize";
    private static final int DEFAULT_MAX_LOG_FILE_SIZE = 512;
    private static final String MAX_BACKUPS_PROPERTY = "maxBackups";
    private static final int DEFAULT_MAX_BACKUPS = 20;
    private static final String LOG4J_CUSTOM_FILENAME = ".tc.custom.log4j.properties";
    public static final String LOG4J_PROPERTIES_FILENAME = TCLogging.LOG4J_PROPERTIES_FILENAME;

    private static final String CONSOLE_PATTERN = "%d %p - %m%n";
    public static final String DUMP_PATTERN = TCLogging.DUMP_PATTERN;
    public static final String DERBY_PATTERN = TCLogging.DERBY_PATTERN;
    private static final String CONSOLE_PATTERN_DEVELOPMENT = "%d [%t] %p %c - %m%n";
    // This next pattern is used when we're *only* logging to the console.
    private static final String CONSOLE_LOGGING_ONLY_PATTERN = "[TC] %d %p - %m%n";
    public static final String FILE_AND_JMX_PATTERN = TCLogging.FILE_AND_JMX_PATTERN;

    private static final TCLogger console;
    private static final TCLogger operatorEventLogger;
    private static final Appender consoleAppender;
    private static final Logger[] allLoggers;

    private static Log4jDelegatingAppender delegateFileAppender;
    private static Log4jDelegatingAppender delegateBufferingAppender;
    private static boolean buffering;
    private static File currentLoggingDirectory = null;
    private static FileLock currentLoggingDirectoryFileLock = null;
    private static boolean lockingDisabled = false;

    private static Properties loggingProperties;

    public static TCLogger getLogger(Class<?> clazz) {
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

      return new Log4jTCLogger(name);
    }

    /**
     * This method lets you get a logger w/o any name restrictions. FOR TESTS ONLY (ie. not for shipping code)
     */
    public static TCLogger getTestingLogger(String name) {
      if (name == null) { throw new IllegalArgumentException("Name cannot be null"); }
      return new Log4jTCLogger(name);
    }

    /**
     * This method lets you get a logger w/o any name restrictions. FOR TESTS ONLY (ie. not for shipping code)
     */
    public static TCLogger getTestingLogger(Class<?> clazz) {
      if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
      return getTestingLogger(clazz.getName());
    }

    // You want to look at CustomerLogging to get customer facing logger instances
    static TCLogger getCustomerLogger(String name) {
      if (name == null) { throw new IllegalArgumentException("name cannot be null"); }

      name = CUSTOMER_LOGGER_NAMESPACE_WITH_DOT + name;

      if (CONSOLE_LOGGER_NAME.equals(name)) { throw new IllegalArgumentException("Illegal name: " + name); }

      return new Log4jTCLogger(name);
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
        List<InputStream> configStreams = new ArrayList<InputStream>();
        // Specify the order of LEAST importance; last one in wins
        File[] devLoggingLocations = new File[]{new File(System.getProperty("user.home"), LOG4J_PROPERTIES_FILENAME),
            new File(System.getProperty("user.dir"), LOG4J_PROPERTIES_FILENAME)};

        InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(LOG4J_PROPERTIES_FILENAME);
        if (stream != null) {
          configStreams.add(stream);
        }

        for (File propFile : devLoggingLocations) {
          if (propFile.isFile() && propFile.canRead()) {
            configStreams.add(new FileInputStream(propFile));
          }
        }

        Properties developerProps = layerDevelopmentConfiguration(configStreams);
        if (developerProps != null) {
          Logger.getRootLogger().setLevel(Level.INFO);
          PropertyConfigurator.configure(developerProps);
          loggingProperties = developerProps;
          return true;
        }
      } catch (Exception e) {
        reportLoggingError(e);
      }

      return false;
    }

    static Properties layerDevelopmentConfiguration(Collection<? extends InputStream> list) {
      if (list.isEmpty()) {
        return null;
      }
      Properties isDev = new Properties();
      for (InputStream in : list) {
        try {
          isDev.load(in);
        } catch (IOException ioe) {
          reportLoggingError(ioe);
        }
      }
      return isDev;
    }

    private static boolean customConfiguration() {
      try {
        // First one wins:
        List<File> locations = new ArrayList<File>();
        if (System.getenv("TC_INSTALL_DIR") != null) {
          locations.add(new File(System.getenv("TC_INSTALL_DIR"), LOG4J_CUSTOM_FILENAME));
        }
        locations.add(new File(System.getProperty("user.home"), LOG4J_CUSTOM_FILENAME));
        locations.add(new File(System.getProperty("user.dir"), LOG4J_CUSTOM_FILENAME));

        for (File propFile : locations) {
          if (propFile.isFile() && propFile.canRead()) {

            Properties properties = new Properties();
            FileInputStream fis = null;
            try {
              fis = new FileInputStream(propFile);
              properties.load(fis);
            } finally {
              IOUtils.closeQuietly(fis);
            }

            PropertyConfigurator.configure(properties);
            loggingProperties = properties;
            return true;
          }
        }

        return false;
      } catch (Exception e) {
        reportLoggingError(e);
        return false;
      }
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
    public static final int PROCESS_TYPE_L1 = 1;
    public static final int PROCESS_TYPE_L2 = 2;

    @SuppressWarnings("resource")
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
          Log4jBufferingAppender realBufferingAppender = (Log4jBufferingAppender) delegateBufferingAppender
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
          lockFile.createNewFile();
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
          newFileAppender = new Log4jTCRollingFileAppender(new PatternLayout(FILE_AND_JMX_PATTERN), logFilePath, true);
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
            Log4jBufferingAppender realBufferingAppender = (Log4jBufferingAppender) delegateBufferingAppender
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
      return new Log4jTCLogger(DUMP_LOGGER_NAME);
    }

    static {
      ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(TCLogging.class.getClassLoader());

      Log4jSafeInit.init();

      Logger customerLogger = Logger.getLogger(CUSTOMER_LOGGER_NAMESPACE);
      Logger consoleLogger = Logger.getLogger(CONSOLE_LOGGER_NAME);

      console = new Log4jTCLogger(CONSOLE_LOGGER_NAME);
      consoleAppender = new Log4jTCConsoleAppender(new PatternLayout(CONSOLE_PATTERN), ConsoleAppender.SYSTEM_OUT);
      operatorEventLogger = new Log4jTCLogger(OPERATOR_EVENT_LOGGER_NAME);

      List<Logger> internalLoggers = new ArrayList<Logger>();
      for (String nameSpace : INTERNAL_LOGGER_NAMESPACES) {
        internalLoggers.add(Logger.getLogger(nameSpace));
      }

      /**
       * Don't add consoleLogger to allLoggers because it's a child of customerLogger, so it shouldn't get any appenders.
       * If you DO add consoleLogger here, you'll see duplicate messages in the log file.
       */
      allLoggers = createAllLoggerList(internalLoggers, customerLogger);

      try {
        boolean customLogging = customConfiguration();
        boolean isDev = customLogging ? false : developmentConfiguration();

        if (!customLogging) {
          for (Logger internalLogger : internalLoggers) {
            internalLogger.setLevel(Level.INFO);
          }
          customerLogger.setLevel(Level.INFO);
          consoleLogger.setLevel(Level.INFO);

          if (!isDev) {
            // Only the console logger goes to the console (by default)
            consoleLogger.addAppender(consoleAppender);
          } else {
            consoleAppender.setLayout(new PatternLayout(CONSOLE_PATTERN_DEVELOPMENT));
            // For non-customer environments, send all logging to the console...
            Logger.getRootLogger().addAppender(consoleAppender);
          }
        }

        delegateFileAppender = new Log4jDelegatingAppender(new NullAppender());
        addToAllLoggers(delegateFileAppender);

        Log4jBufferingAppender realBufferingAppender;
        realBufferingAppender = new Log4jBufferingAppender(MAX_BUFFERED_LOG_MESSAGES);
        realBufferingAppender.setName("buffering appender");
        delegateBufferingAppender = new Log4jDelegatingAppender(realBufferingAppender);
        addToAllLoggers(delegateBufferingAppender);
        buffering = true;

        if (!isDev) {
          CustomerLogging.getGenericCustomerLogger().info("New logging session started.");
        }

        writeVersion();
        writePID();
        writeLoggingConfigurations();
      } catch (Exception e) {
        reportLoggingError(e);
      } finally {
        Thread.currentThread().setContextClassLoader(prevLoader);
      }
    }

    // for test use only!
    public static Log4JAppenderToTCAppender addAppender(String loggerName, TCAppender appender) {
      Log4JAppenderToTCAppender wrappedAppender = new Log4JAppenderToTCAppender(appender);
      new Log4jTCLogger(loggerName).getLogger().addAppender(wrappedAppender);
      return wrappedAppender;
    }

    private static Logger[] createAllLoggerList(List<Logger> internalLoggers, Logger customerLogger) {
      List<Logger> loggers = new ArrayList<Logger>();
      loggers.addAll(internalLoggers);
      loggers.add(customerLogger);
      return loggers.toArray(new Logger[]{});
    }

    public static void addToAllLoggers(Appender appender) {
      for (Logger allLogger : allLoggers)
        allLogger.addAppender(appender);
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

    private static void writePID() {
      try {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        long pid = Long.parseLong(processName.split("@")[0]);
        getLogger(TCLogging.class).info("PID is " + pid);
      } catch (Throwable t) {
        // ignore, not fatal if this doesn't work for some reason
      }
    }

    private static void writeSystemProperties() {
      try {
        Properties properties = System.getProperties();
        int maxKeyLength = 1;

        List<String> keys = new ArrayList<String>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
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

        String[] sortedKeys = keys.toArray(new String[keys.size()]);
        Arrays.sort(sortedKeys);
        for (String key : sortedKeys) {
          data.append(key);
          for (int i = 0; i < maxKeyLength - key.length(); i++) {
            data.append(' ');
          }
          data.append(" : ");
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


    /**
     * This method will print the logging configurations being used by the logger.
     */
    private static void writeLoggingConfigurations() {
      if (loggingProperties != null) {
        getLogger(TCLogging.class).info(LOG_CONFIGURATION_PREFIX + loggingProperties);
      }
    }
  }

}
