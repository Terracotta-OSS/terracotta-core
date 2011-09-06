/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy.subpkg;

/**
 * This class is non-public and in a different package as the DelegatingInvoicationHandlerTests. It's sole
 * purpose to make sure we handle the case where the delegation is asked to invoke a method directly on a
 * non-accessible class 
 */
class PackagePrivateClass implements TestInterface {
  private static int count = 0;

  private final int  number;

  PackagePrivateClass() {
    synchronized (PackagePrivateClass.class) {
      number = count++;
    }
  }

  // from the public test interface
  public void method() {
    System.out.println("I am number " + number);
  }
}