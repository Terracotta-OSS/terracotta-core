/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver;

import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.msg.JMXMessage;
import com.tc.stats.AbstractNotifyingMBean;
import com.tc.util.Assert;

import javax.management.NotCompliantMBeanException;

public class DSOApplicationEvents extends AbstractNotifyingMBean implements DSOApplicationEventsMBean {

  public DSOApplicationEvents() throws NotCompliantMBeanException {
    super(DSOApplicationEventsMBean.class);
  }

  public void addMessage(final JMXMessage msg) throws UnsupportedMessageTypeException {
    Object obj = msg.getJMXObject();
    // XXX: dispatch differently based on the type of the serialized object
    Assert.assertTrue(obj instanceof NonPortableObjectEvent);
    NonPortableObjectEvent event = (NonPortableObjectEvent) obj;
    sendNotification(NON_PORTABLE_OBJECT_EVENT, event);
  }
  
  public void reset() {
    // nothing to reset
  }

}
