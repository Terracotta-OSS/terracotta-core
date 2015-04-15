/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
