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