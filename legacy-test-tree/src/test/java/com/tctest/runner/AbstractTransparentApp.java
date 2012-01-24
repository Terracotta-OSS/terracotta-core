/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.runner;

import com.tc.logging.LogLevel;
import com.tc.logging.TCLogging;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.Application;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.JMXUtils;

import java.util.Map;
import java.util.Map.Entry;

import javax.management.remote.JMXConnector;

public abstract class AbstractTransparentApp implements Application {

  public static final String              L1_LOG_LEVELS = "l1LogLevels";

  private final TransparentAppCoordinator coordinator;
  private final int                       intensity;
  private final ListenerProvider          listenerProvider;

  public AbstractTransparentApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    this.listenerProvider = listenerProvider;
    this.intensity = config.getIntensity();
    this.coordinator = new TransparentAppCoordinator(appId, config.getGlobalParticipantCount());

    Map<Class<?>, LogLevel> logLevels = (Map<Class<?>, LogLevel>) config.getAttributeObject(L1_LOG_LEVELS);
    if (logLevels != null) {
      for (Entry<Class<?>, LogLevel> logLevelEntry : logLevels.entrySet()) {
        TCLogging.getLogger(logLevelEntry.getKey()).setLevel(logLevelEntry.getValue());
      }
    }
  }

  protected int getIntensity() {
    return this.intensity;
  }

  protected int getParticipantCount() {
    return coordinator.getParticipantCount();
  }

  public String getApplicationId() {
    return coordinator.getGlobalId();
  }

  protected void notifyError(String msg) {
    listenerProvider.getResultsListener().notifyError(new ErrorContext(msg, new Error()));
  }

  protected void notifyError(ErrorContext context) {
    listenerProvider.getResultsListener().notifyError(context);
  }

  protected void notifyError(Throwable t) {
    listenerProvider.getResultsListener().notifyError(new ErrorContext(t));
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config, Map optionalAttributes) {
    visitL1DSOConfig(visitor, config);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addIncludePattern(AbstractTransparentApp.class.getName());
    config.addIncludePattern(TransparentAppCoordinator.class.getName());
    config.addWriteAutolock("* " + AbstractTransparentApp.class.getName() + ".*(..)");
    config.addWriteAutolock("* " + TransparentAppCoordinator.class.getName() + ".*(..)");
  }

  public void notifyResult(Boolean result) {
    this.listenerProvider.getResultsListener().notifyResult(result);
  }

  public boolean interpretResult(Object result) {
    return result instanceof Boolean && ((Boolean) result).booleanValue();
  }

  public static JMXConnector getJMXConnector(String host, int jmxPort) {
    try {
      return JMXUtils.getJMXConnector(host, jmxPort);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
