/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;

import java.util.Collection;
import java.util.Iterator;

public class InstrumentationLoggerImpl implements InstrumentationLogger {

  private final DSOInstrumentationLoggingOptions opts;
  private final TCLogger                         logger;

  public InstrumentationLoggerImpl(DSOInstrumentationLoggingOptions opts) {
    this.opts = opts;
    this.logger = CustomerLogging.getDSOInstrumentationLogger();
  }

  public boolean classInclusion() {
    return opts.logClass().getBoolean();
  }

  public boolean lockInsertion() {
    return opts.logLocks().getBoolean();
  }

  public boolean transientRootWarning() {
    return opts.logTransientRoot().getBoolean();
  }

  public boolean rootInsertion() {
    return opts.logRoots().getBoolean();
  }

  public boolean distMethodCallInsertion() {
    return opts.logDistributedMethods().getBoolean();
  }

  public void classIncluded(String className) {
    logger.info(className + " included for instrumentation");
  }

  public void subclassOfLogicallyManagedClasses(String className, Collection logicalSuperClasses) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(className).append(" is a subclass of a logically managed superclass : (");

    for (Iterator i = logicalSuperClasses.iterator(); i.hasNext();) {
      buffer.append(i.next());
      if (i.hasNext()) {
        buffer.append(',');
      }
    }

    buffer.append("). This is currently not supported! Perhaps it has overridden a protected method.");
    logger.warn(buffer.toString());

  }

  public void autolockInserted(String className, String methodName, String methodDesc, LockDefinition lockDefinition) {
    String level = lockDefinition.getLockLevel().toString();
    logger.info("Inserting autolocks in method " + className + "." + methodName + methodDesc + ", level: " + level);
  }

  public void lockInserted(String className, String methodName, String methodDesc, LockDefinition[] locks) {
    StringBuffer sb = new StringBuffer();

    String s = locks.length > 1 ? "locks" : "lock";

    sb.append("Inserting named ").append(s).append(" in ").append(className).append('.').append(methodName)
        .append(methodDesc);
    for (int i = 0; i < locks.length; i++) {
      sb.append("\n\tname: ").append(locks[i].getLockName()).append(", level: ").append(locks[i].getLockLevel());
    }

    logger.info(sb.toString());
  }

  public void transientRootWarning(String className, String fieldName) {
    logger.warn("The java transient property is being ignored for root:" + className + "." + fieldName);
  }

  public void rootInserted(String className, String fieldName, String desc, boolean isStatic) {
    logger.info("DSO root inserted for field " + className + "." + fieldName + ", type " + (isStatic ? "static " : "")
                + desc);
  }

  public void distMethodCallInserted(String className, String methodName, String desc) {
    logger.info("Adding distributed call: " + methodName + desc + " to class:" + className);
  }

}
