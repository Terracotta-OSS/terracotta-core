/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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