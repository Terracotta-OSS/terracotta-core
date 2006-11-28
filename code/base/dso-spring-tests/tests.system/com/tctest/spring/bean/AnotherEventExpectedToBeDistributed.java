/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public class AnotherEventExpectedToBeDistributed extends UninstrumentedSuperclass {

  public AnotherEventExpectedToBeDistributed(Object source, String message) {
    super(source, message);
  }

}
