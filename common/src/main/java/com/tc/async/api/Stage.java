/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

import com.tc.text.PrettyPrintable;

/**
 * @author steve
 */
public interface Stage extends PrettyPrintable {

  public void destroy();

  public Sink getSink();

  public void start(ConfigurationContext context);

}
