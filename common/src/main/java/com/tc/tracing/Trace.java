package com.tc.tracing;

import com.tc.entity.VoltronEntityMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trace {

  private static final Logger LOGGER = LoggerFactory.getLogger(Trace.class);

  private static final ThreadLocal<Trace> ACTIVE_TRACE = new ThreadLocal<Trace>();
  private static final Trace DUMMY = new Trace("DummyID", "DummyComponent");

  private final String id;
  private final String componentName;
  private final Trace parent;

  private long startTime;

  public Trace(String id, String componentName) {
    this(id, componentName, null);
  }

  public Trace(String id, String componentName, Trace parent) {
    this.id = id;
    this.componentName = componentName;
    this.parent = parent;
  }

  public Trace subTrace(String subComponentName) {
    return new Trace(id, componentName + ":" + subComponentName, this);
  }

  public void log(String message) {
    LOGGER.trace("[trace - {}] {} - {}", id, componentName, message);
  }

  public void start() {
    this.startTime = System.nanoTime();
    LOGGER.trace("[trace - {}] start trace for componentName - {}", id, this.componentName);
    ACTIVE_TRACE.set(this);
  }

  public void end() {
    LOGGER.trace("[trace - {}] end trace for componentName - {}, elapsed {} ns", id, componentName, System.nanoTime() - startTime);
    if(parent != null) {
      ACTIVE_TRACE.set(parent);
    } else {
      ACTIVE_TRACE.set(null);
    }
  }

  public String getId() {
    return id;
  }

  public static Trace activeTrace() {
    Trace trace = ACTIVE_TRACE.get();
    return trace != null ? trace : DUMMY;
  }

  public static Trace newTrace(VoltronEntityMessage message, String componentName) {
    try {
      return new Trace(message.getSource().toLong() + ":" + message.getTransactionID().toLong(), componentName);
    } catch (Exception e) {
      return DUMMY;
    }
  }
}
