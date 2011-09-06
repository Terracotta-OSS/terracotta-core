/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

public class GroupException extends Exception {

  public GroupException(Exception reason) {
    super(reason);
  }

  public GroupException(String reason) {
    super(reason);
  }

}
