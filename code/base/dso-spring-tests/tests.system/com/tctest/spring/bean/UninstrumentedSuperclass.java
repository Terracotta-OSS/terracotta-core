/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class UninstrumentedSuperclass extends SingletonEvent {

  public UninstrumentedSuperclass(Object source, String message) {
    super(source, message);
  }

}
