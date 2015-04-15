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

import org.junit.Test;

import java.lang.management.ManagementFactory;

import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlainMBeanMirrorTest {
  private MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

  @Test
  public void testDynamicMBeanInfoTest() throws Exception {
    ObjectName objectName = new ObjectName("org.terracotta:node=DynamicMBeanInfoTest");
    DynamicMBean dynamicMBean = mock(DynamicMBean.class);
    when(dynamicMBean.getMBeanInfo()).thenReturn(new MBeanInfo("foo", "bar", new MBeanAttributeInfo[0],
        new MBeanConstructorInfo[0], new MBeanOperationInfo[0], new MBeanNotificationInfo[0]));
    mBeanServer.registerMBean(dynamicMBean, objectName);

    PlainMBeanMirror plainMBeanMirror = new PlainMBeanMirror(mBeanServer, objectName, objectName);
    assertThat(plainMBeanMirror.getMBeanInfo().getAttributes().length, is(0));

    MBeanAttributeInfo mBeanAttributeInfo = new MBeanAttributeInfo("foo", "Object", "bar", true, false, false);
    when(dynamicMBean.getMBeanInfo()).thenReturn(new MBeanInfo("foo", "bar", new MBeanAttributeInfo[] {
        mBeanAttributeInfo }, new MBeanConstructorInfo[0],
        new MBeanOperationInfo[0], new MBeanNotificationInfo[0]));

    assertThat(plainMBeanMirror.getMBeanInfo().getAttributes()[0], is(mBeanAttributeInfo));
  }
}