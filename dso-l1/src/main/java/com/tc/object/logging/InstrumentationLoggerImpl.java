/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.logging;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;

import java.util.Collection;
import java.util.Iterator;

public class InstrumentationLoggerImpl implements InstrumentationLogger {

  private final TCLogger logger;

  private boolean        classInclusion;
  private boolean        lockInsertion;
  private boolean        transientRootWarning;
  private boolean        rootInsertion;
  private boolean        distMethodCallInsertion;

  public InstrumentationLoggerImpl(final DSOInstrumentationLoggingOptions opts) {
    this.logger = CustomerLogging.getDSOInstrumentationLogger();

    if (opts != null) {
      this.classInclusion = opts.logClass();
      this.lockInsertion = opts.logLocks();
      this.transientRootWarning = opts.logTransientRoot();
      this.rootInsertion = opts.logRoots();
      this.distMethodCallInsertion = opts.logDistributedMethods();
    }
  }

  public void setClassInclusion(final boolean classInclusion) {
    this.classInclusion = classInclusion;
  }

  public boolean getClassInclusion() {
    return this.classInclusion;
  }

  public void setLockInsertion(final boolean lockInsertion) {
    this.lockInsertion = lockInsertion;
  }

  public boolean getLockInsertion() {
    return this.lockInsertion;
  }

  public void setTransientRootWarning(final boolean transientRootWarning) {
    this.transientRootWarning = transientRootWarning;
  }

  public boolean getTransientRootWarning() {
    return this.transientRootWarning;
  }

  public void setRootInsertion(final boolean rootInsertion) {
    this.rootInsertion = rootInsertion;
  }

  public boolean getRootInsertion() {
    return this.rootInsertion;
  }

  public void setDistMethodCallInsertion(final boolean distMethodCallInsertion) {
    this.distMethodCallInsertion = distMethodCallInsertion;
  }

  public boolean getDistMethodCallInsertion() {
    return this.distMethodCallInsertion;
  }

  public void classIncluded(final String className) {
    logger.info(className + " included for instrumentation");
  }

  public void subclassOfLogicallyManagedClasses(final String className, final Collection logicalSuperClasses) {
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

  public void autolockInserted(final String className, final String methodName, final String methodDesc, final LockDefinition lockDefinition) {
    String level = lockDefinition.getLockLevel().toString();
    logger.info("Inserting autolocks in method " + className + "." + methodName + methodDesc + ", level: " + level);
  }

  public void lockInserted(final String className, final String methodName, final String methodDesc, final LockDefinition[] locks) {
    StringBuffer sb = new StringBuffer();

    String s = locks.length > 1 ? "locks" : "lock";

    sb.append("Inserting named ").append(s).append(" in ").append(className).append('.').append(methodName)
        .append(methodDesc);
    for (int i = 0; i < locks.length; i++) {
      sb.append("\n\tname: ").append(locks[i].getLockName()).append(", level: ").append(locks[i].getLockLevel());
    }

    logger.info(sb.toString());
  }

  public void transientRootWarning(final String className, final String fieldName) {
    logger.warn("The java transient property is being ignored for root:" + className + "." + fieldName);
  }

  public void rootInserted(final String className, final String fieldName, final String desc, final boolean isStatic) {
    logger.info("DSO root inserted for field " + className + "." + fieldName + ", type " + (isStatic ? "static " : "")
                + desc);
  }

  public void distMethodCallInserted(String className, String methodName, String desc) {
    logger.info("Adding distributed call: " + methodName + desc + " to class:" + className);
  }

}
