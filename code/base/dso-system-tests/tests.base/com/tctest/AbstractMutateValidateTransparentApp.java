/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public abstract class AbstractMutateValidateTransparentApp extends AbstractErrorCatchingTransparentApp {
  public static final String     VALIDATOR_COUNT = "validator-count";
  public static final String     MUTATOR_COUNT   = "mutator-count";
  private static final boolean   DEBUG           = true;

  protected final int            validatorCount;
  protected final int            mutatorCount;
  private final boolean          isMutator;
  private final ListenerProvider listenerProvider;
  private final String           appId;

  public AbstractMutateValidateTransparentApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appId = appId;
    this.listenerProvider = listenerProvider;

    validatorCount = cfg.getValidatorCount();
    mutatorCount = cfg.getGlobalParticipantCount();
    isMutator = Boolean.valueOf(cfg.getAttribute(appId)).booleanValue();

    debugPrintln("***** appId=[" + appId + "]:  isMutator=[" + isMutator + "]");
  }

  protected void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  // mutator: mutate, wait until all mutation is done, then validate
  // validator: wait until all mutation is done, then validate
  public void runTest() throws Throwable {
    if (isMutator) {
      debugPrintln("***** appId[" + appId + "]: starting mutate");
      mutate();
      debugPrintln("***** appId[" + appId + "]: finished mutate");
      notifyMutationComplete();
      debugPrintln("***** appId[" + appId + "]: notified mutate-listener... waiting for mutate stage to finish");
      waitForMutationComplete();
      debugPrintln("***** appId[" + appId + "]: mutate stage complete");
    }

    Thread.sleep(5000);

    notifyValidationStart();
    debugPrintln("***** appId[" + appId + "]: notified mutate-listener... waiting for validat stage to start");
    waitForValidationStart();

    debugPrintln("***** appId[" + appId + "]: starting validate");
    validate();
    debugPrintln("***** appId[" + appId + "]: finished validate");
  }

  private final void waitForValidationStart() throws Exception {
    listenerProvider.getMutationCompletionListener().waitForValidationStartTestWide();
  }

  private final void notifyMutationComplete() {
    listenerProvider.getMutationCompletionListener().notifyMutationComplete();
  }

  private final void notifyValidationStart() {
    listenerProvider.getMutationCompletionListener().notifyValidationStart();
  }

  private final void waitForMutationComplete() throws Exception {
    listenerProvider.getMutationCompletionListener().waitForMutationCompleteTestWide();
  }

  protected boolean isMutator() {
    return isMutator;
  }

  protected abstract void mutate() throws Throwable;

  protected abstract void validate() throws Throwable;

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = AbstractMutateValidateTransparentApp.class.getName();
    config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
  }

}
