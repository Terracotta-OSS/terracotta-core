/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.runner;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.Application;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.listener.ListenerProvider;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractTransparentApp implements Application {

  private final TransparentAppCoordinator coordinator;
  private final int                       intensity;
  private final ListenerProvider          listenerProvider;
  private final Set                       appIds = new HashSet();

  public AbstractTransparentApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    synchronized (appIds) {
      if (!appIds.add(appId)) { throw new AssertionError("You've created me with the same global ID as someone else: "
                                                         + appId); }
    }
    this.listenerProvider = listenerProvider;
    this.intensity = config.getIntensity();
    this.coordinator = new TransparentAppCoordinator(appId, config.getGlobalParticipantCount());
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

  protected void moveToStage(int stage) {
    coordinator.moveToStage(stage);
  }

  protected void moveToStageAndWait(int stage) {
    coordinator.moveToStageAndWait(stage);
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

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.addIncludePattern(AbstractTransparentApp.class.getName());
    config.addRoot("AbstractTransparentAppAppIds", AbstractTransparentApp.class.getName() + ".appIds");
    config.addWriteAutolock("* " + AbstractTransparentApp.class.getName() + ".*(..)");

    TransparencyClassSpec spec = config.getOrCreateSpec(TransparentAppCoordinator.class.getName());
    spec.addRoot("participants", "participants");
    config.addWriteAutolock("* " + TransparentAppCoordinator.class.getName() + ".*(..)");
  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    config.addIncludePattern(AbstractTransparentApp.class.getName());
    config.addRoot("AbstractTransparentAppAppIds", AbstractTransparentApp.class.getName() + ".appIds");
    config.addWriteAutolock("* " + AbstractTransparentApp.class.getName() + ".*(..)");

    config.addIncludePattern(TransparentAppCoordinator.class.getName());
    config.addRoot("participants", TransparentAppCoordinator.class.getName() + ".participants");
    config.addWriteAutolock("* " + TransparentAppCoordinator.class.getName() + ".*(..)");
  }

  public void notifyResult(Boolean result) {
    this.listenerProvider.getResultsListener().notifyResult(result);
  }

  public boolean interpretResult(Object result) {
    return result instanceof Boolean && ((Boolean) result).booleanValue();
  }
}