/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.runner;

/**
 * action after an app has completed its action.
 */
public interface PostAction {
  
  public void execute() throws Exception;

}
