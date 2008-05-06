/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

/**
 * This is a mild hack, but the purpose of this class is indicate whether Terracotta code is available to a particular
 * loader. As it's name inidicates, this class should NEVER be in the boot jar since classes in the boot jar are always
 * available
 */
public class NotInBootJar {

  private NotInBootJar() {
    //
  }

}
