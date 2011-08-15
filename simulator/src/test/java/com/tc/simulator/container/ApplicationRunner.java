/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.container;

import com.tc.simulator.app.Application;
import com.tc.simulator.app.ErrorContext;
import com.tc.simulator.control.Control;
import com.tc.simulator.listener.ResultsListener;
import com.tc.simulator.listener.StatsListener;
import com.tc.simulator.listener.StatsListenerFactory;

import java.util.Properties;

class ApplicationRunner implements Runnable {
  private final ApplicationRunnerConfig config;
  private final Control                 control;
  private final ResultsListener         resultsListener;
  private final Application             application;
  private final StatsListener           statsListener;

  ApplicationRunner(ApplicationRunnerConfig config, Control control, ResultsListener resultsListener,
                    Application application, StatsListenerFactory statsListenerFactory) {
    this.config = config;
    this.control = control;
    this.resultsListener = resultsListener;
    this.application = application;
    Properties properties = new Properties();
    properties.setProperty("sample_name", "application_instance_execution_time");
    properties.setProperty("application_class", application.getClass().getName());
    this.statsListener = statsListenerFactory.newStatsListener(properties);
  }

  public void run() {
    try {
      if (this.config.getStartTimeout() < 1) {
        notifyError(new ApplicationRunnerConfigException("Start timeout must be greater than zero."));
        return;
      }
      this.control.waitForStart();
      long start = System.currentTimeMillis();
      this.application.run();
      long delta = System.currentTimeMillis() - start;
      statsListener.sample(delta, "");
      this.resultsListener.notifyResult(Boolean.TRUE);
    } catch (Throwable t) {
      this.resultsListener.notifyError(new ErrorContext("Caught Throwable from Application.run()", t));
      this.resultsListener.notifyResult(Boolean.FALSE);
    } finally {
      control.notifyComplete();
    }
  }

  private void notifyError(Throwable t) {
    this.resultsListener.notifyError(new ErrorContext(t));
    this.resultsListener.notifyResult(Boolean.FALSE);
  }
}