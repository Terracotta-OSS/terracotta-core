package com.tc.logging;

import java.io.File;

/**
 * @author Mathieu Carbou
 */
interface DelegatingTCLogger {

  void setLogDirectory(File theDirectory, int processType);

  void disableLocking();

  TCLogger newLogger(String name);

  void closeFileAppender();

  TCLogger getConsoleLogger();

  TCLogger getOperatorEventLogger();
}
