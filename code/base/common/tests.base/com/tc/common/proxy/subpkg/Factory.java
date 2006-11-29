/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.common.proxy.subpkg;

/**
 * A public way to get at instances of PackagePrivateClass from outside the com.tc.common.proxy.subpkg package
 */
public class Factory {

  public static TestInterface getInstance() {
    return new PackagePrivateClass();
  }
}