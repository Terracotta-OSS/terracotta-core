/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
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
package com.tc.l2.logging;

import com.tc.logging.TCAppender;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggingService;
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

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import com.tc.properties.TCPropertiesConsts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
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
 * 
 * @author teck
 */
public class TCLoggingLog4J implements TCLoggingService {
  private static final String       TERRACOTTA_L2_LOG_FILE_NAME        = "terracotta-server.log";

  private static final int          MAX_BUFFERED_LOG_MESSAGES          = 10 * 1000;

  private static final String       MAX_LOG_FILE_SIZE_PROPERTY         = "maxLogFileSize";
  private static final int          DEFAULT_MAX_LOG_FILE_SIZE          = 512;
  private static final String       MAX_BACKUPS_PROPERTY               = "maxBackups";
  private static final int          DEFAULT_MAX_BACKUPS                = 20;
  private static final String       LOG4J_CUSTOM_FILENAME              = ".tc.custom.log4j.properties";
  public static final String        LOG4J_PROPERTIES_FILENAME          = ".tc.dev.log4j.properties";

  private static final String       CONSOLE_PATTERN                    = "%d %p - %m%n";
  public static final String        DUMP_PATTERN                       = "[dump] %m%n";
  private static final String       CONSOLE_PATTERN_DEVELOPMENT        = "%d [%t] %p %c - %m%n";
  // This next pattern is used when we're *only* logging to the console.
  private static final String       CONSOLE_LOGGING_ONLY_PATTERN       = "[TC] %d %p - %m%n";
  public static final String        FILE_AND_JMX_PATTERN               = "%d [%t] %p %c - %m%n";

  private static final String       CONSOLE_LOGGER_NAME                = "org.terracotta.console";
  public static final String        DUMP_LOGGER_NAME                   = "com.tc.dumper.dump";

  public static final String        LOG_CONFIGURATION_PREFIX           = "The configuration read for Logging: ";

  private static final String[]     INTERNAL_LOGGER_NAMESPACES         = new String[] { "com", "org", "tc.operator"};
  
  private static final String       LOCK_FILE_NAME                     = ".terracotta-logging.lock";

  private final static TCLogger     console;
  private final static Appender     consoleAppender;
  private final static Logger[]     allLoggers;  

  private final static DelegatingAppender delegateFileAppender;
  private final static DelegatingAppender delegateBufferingAppender;
  private static boolean            buffering;
  private File               currentLoggingDirectory            = null;
  private FileLock           currentLoggingDirectoryFileLock    = null;
  private boolean            lockingDisabled                    = false;

  private static Properties         loggingProperties;

  public TCLoggingLog4J() {

  }
  /**
   *
   * @param clazz
   * @return
   */
  public TCLogger getLogger(Class<?> clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getLogger(clazz.getName());
  }

  @Override
  public TCLogger getLogger(String name) {
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

  @Override
  public TCLogger getConsoleLogger() {
    return console;
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
      File[] devLoggingLocations = new File[] { new File(System.getProperty("user.home"), LOG4J_PROPERTIES_FILENAME),
          new File(System.getProperty("user.dir"), LOG4J_PROPERTIES_FILENAME) };

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

  @SuppressWarnings("resource")
  public void setLogDirectory(File theDirectory) {
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

    synchronized (TCLoggingLog4J.class) {
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

    String logFilePath = new File(theDirectory, TERRACOTTA_L2_LOG_FILE_NAME).getAbsolutePath();

    synchronized (TCLoggingLog4J.class) {
      try {
        TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(TCPropertiesConsts.LOGGING_CATEGORY);
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

  @Override
  public TCLogger getDumpLogger() {
    return new TCLoggerImpl(DUMP_LOGGER_NAME);
  }

  static {
    ClassLoader prevLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(TCLoggingLog4J.class.getClassLoader());

    Log4jSafeInit.init();

    Logger consoleLogger = Logger.getLogger(CONSOLE_LOGGER_NAME);

    console = new TCLoggerImpl(CONSOLE_LOGGER_NAME);
    consoleAppender = new TCConsoleAppender(new PatternLayout(CONSOLE_PATTERN), ConsoleAppender.SYSTEM_OUT);

    List<Logger> internalLoggers = new ArrayList<Logger>();
    for (String nameSpace : INTERNAL_LOGGER_NAMESPACES) {
      internalLoggers.add(Logger.getLogger(nameSpace));
    }

    /**
     * Don't add consoleLogger to allLoggers because it's a child of customerLogger, so it shouldn't get any appenders.
     * If you DO add consoleLogger here, you'll see duplicate messages in the log file.
     */
    allLoggers = createAllLoggerList(internalLoggers);

    try {
      boolean customLogging = customConfiguration();
      boolean isDev = customLogging ? false : developmentConfiguration();

      if (!customLogging) {
        for (Logger internalLogger : internalLoggers) {
          internalLogger.setLevel(Level.INFO);
        }
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

      delegateFileAppender = new DelegatingAppender(new NullAppender());
      addToAllLoggers(delegateFileAppender);

      BufferingAppender realBufferingAppender;
      realBufferingAppender = new BufferingAppender(MAX_BUFFERED_LOG_MESSAGES);
      realBufferingAppender.setName("buffering appender");
      delegateBufferingAppender = new DelegatingAppender(realBufferingAppender);
      addToAllLoggers(delegateBufferingAppender);
      buffering = true;

      if (!isDev) {
        console.info("New logging session started.");
      }

      writeVersion();
      writePID();
      writeLoggingConfigurations();
    } finally {
      Thread.currentThread().setContextClassLoader(prevLoader);
    }
  }

  private static Logger[] createAllLoggerList(List<Logger> internalLoggers) {
    List<Logger> loggers = new ArrayList<Logger>();
    loggers.addAll(internalLoggers);
//  do not add the customer logger anymore since it is covered by the "com" internal namespace
//  if the namespace for customer ever changes, add it back
//    loggers.add(customerLogger);
    return loggers.toArray(new Logger[] {});
  }

  public static void addToAllLoggers(Appender appender) {
    for (Logger allLogger : allLoggers)
      allLogger.addAppender(appender);
  }

  private static void writeVersion() {
    ProductInfo info = ProductInfo.getInstance();
    TCLogger consoleLogger = console;

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
      console.info("PID is " + pid);
    } catch (Throwable t) {
      // ignore, not fatal if this doesn't work for some reason
    }
  }

  private void writeSystemProperties() {
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

      getLogger(TCLoggingLog4J.class).info(data.toString());
    } catch (Throwable t) {
      // don't let exceptions here be fatal
      t.printStackTrace();
    }
  }

  /**
   * This method will print the logging configurations being used by the logger.
   */
  private static void writeLoggingConfigurations() {
    if (loggingProperties != null) {
      new TCLoggerImpl(TCLoggingLog4J.class.getName()).info(LOG_CONFIGURATION_PREFIX + loggingProperties);
    }
  }

  @Override
  public void setLogLocationAndType(URI location) {
    setLogDirectory(new File(location));
  }

  
}
