package com.tctest.spring.integrationtests.framework;

public interface Stoppable {

  void start() throws Exception;

  void stop() throws Exception;

  public void stopIgnoringExceptions();

  boolean isStopped();

}
