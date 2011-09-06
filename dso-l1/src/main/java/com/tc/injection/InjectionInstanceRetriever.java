/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.injection;

public interface InjectionInstanceRetriever<T> {
  public T getInstanceToInject(Class type);
}