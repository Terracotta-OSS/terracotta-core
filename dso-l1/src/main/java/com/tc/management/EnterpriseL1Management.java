/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.management;

import com.tc.exception.TCRuntimeException;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.beans.MBeanNames;
import com.tc.management.beans.TerracottaOperatorEventsMBean;
import com.tc.management.beans.object.EnterpriseTCClientMbean;
import com.tc.management.beans.object.EnterpriseTCClientMbeanImpl;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;

import java.io.IOException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class EnterpriseL1Management extends L1Management {
  private final EnterpriseTCClientMbean       enterpriseTCClientMbean;
  private final TerracottaOperatorEventsMBean l1OperatorEventsBean;

  public EnterpriseL1Management(TunnelingEventHandler tunnelingHandler, String rawConfigText, TCClient client) {
    super(tunnelingHandler, rawConfigText, client);
    try {
      this.enterpriseTCClientMbean = new EnterpriseTCClientMbeanImpl(client);
      this.l1OperatorEventsBean = new TerracottaOperatorEventsMBeanImpl();
    } catch (NotCompliantMBeanException e) {
      throw new TCRuntimeException("Could not create EnterpriseTCClientMbean: " + e);
    }
  }

  @Override
  public Object findMBean(ObjectName objectName, Class<?> mBeanInterface) throws IOException {
    if (objectName.equals(L1MBeanNames.ENTERPRISE_TC_CLIENT)) return this.enterpriseTCClientMbean;
    if (objectName.equals(MBeanNames.OPERATOR_EVENTS_PUBLIC)) return this.l1OperatorEventsBean;
    return super.findMBean(objectName, mBeanInterface);

  }

  @Override
  protected void registerMBeans() throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException, MalformedObjectNameException {
    registerMBean(enterpriseTCClientMbean, L1MBeanNames.ENTERPRISE_TC_CLIENT);
    super.registerMBeans();
    registerMBean(l1OperatorEventsBean, MBeanNames.OPERATOR_EVENTS_PUBLIC);
  }

}
