/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver;

import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.appevent.ReadOnlyObjectEvent;
import com.tc.object.appevent.UnlockedSharedObjectEvent;
import com.tc.object.msg.JMXMessage;
import com.tc.stats.AbstractNotifyingMBean;

import javax.management.NotCompliantMBeanException;

public class DSOApplicationEvents extends AbstractNotifyingMBean implements DSOApplicationEventsMBean {

  public DSOApplicationEvents() throws NotCompliantMBeanException {
    super(DSOApplicationEventsMBean.class);
  }

  public void addMessage(final JMXMessage msg) throws UnsupportedMessageTypeException {
    Object obj = msg.getJMXObject();
    String notifyType = null;
    
    if (obj instanceof NonPortableObjectEvent) {
      notifyType = NON_PORTABLE_OBJECT_EVENT;
    } else if (obj instanceof UnlockedSharedObjectEvent) {
      notifyType = UNLOCKED_SHARED_OBJECT_EVENT;
    } else if (obj instanceof ReadOnlyObjectEvent) {
      notifyType = READ_ONLY_OBJECT_EVENT;
    }
    
    if(notifyType != null) {
      sendNotification(notifyType, obj);
    }
  }

  public void reset() {
    // nothing to reset
  }

}
