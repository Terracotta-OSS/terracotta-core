/*
 * MBeanMirrorFactory.java , Copyright 2007 Eamonn McManus Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
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
