/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
