/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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