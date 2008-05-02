/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.externall1;

/**
 * This is just a dummy class which is supposed to be present in the classpath of the system class loader Used for
 * testing shared objects - the shared object's class is defined by the system class loader in a web-app and accessed in
 * a standalone java application.
 */
public class StandardClasspathDummyClass {
  // empty
}