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
package com.tc.management.beans;

import com.tc.management.TerracottaManagement;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class L2MBeanNames {

  public static final ObjectName TC_SERVER_INFO;
  public static final ObjectName DSO;

  //XXX: this for test, to read server data when active-active is running.
  public static final ObjectName DUMPER;

  static {
    try {
      TC_SERVER_INFO = TerracottaManagement.createObjectName(null, "TerracottaServer", TerracottaManagement.MBeanDomain.PUBLIC);
      DSO = TerracottaManagement.createObjectName(null, "DSO", TerracottaManagement.MBeanDomain.PUBLIC);
      DUMPER = TerracottaManagement.createObjectName(null, "L2Dumper", TerracottaManagement.MBeanDomain.PUBLIC);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }
}