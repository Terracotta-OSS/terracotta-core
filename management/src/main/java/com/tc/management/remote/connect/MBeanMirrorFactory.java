/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.connect;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationBroadcaster;
import javax.management.ObjectName;

public class MBeanMirrorFactory {
  private MBeanMirrorFactory() {
    // there are no instances of this class
  }

  public static MBeanMirror newMBeanMirror(MBeanServerConnection mbsc, ObjectName objectName, ObjectName localObjectName)
      throws IOException, InstanceNotFoundException, IntrospectionException {
    MBeanMirror mirror = new PlainMBeanMirror(mbsc, objectName, localObjectName);
    if (mbsc.isInstanceOf(objectName, NotificationBroadcaster.class.getName())) mirror = new NotifyingMBeanMirror(
                                                                                                                  mirror);
    return mirror;
  }
}
