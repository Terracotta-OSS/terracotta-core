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
package com.tc.management.beans;

import com.tc.management.TerracottaManagement;
import com.tc.management.TerracottaManagement.Subsystem;
import com.tc.management.TerracottaManagement.Type;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class L2MBeanNames {

  public static final ObjectName TC_SERVER_INFO;
  public static final ObjectName LOGGER;
  public static final ObjectName DSO;

  //XXX: this for test, to read server data when active-active is running.
  public static final ObjectName LOCAL_DGC_STATS;
  public static final ObjectName OBJECT_MANAGEMENT;
  public static final ObjectName DUMPER;
  public static final ObjectName SERVER_DB_BACKUP;
  public static final ObjectName ENTERPRISE_TC_SERVER;

  static {
    try {
      TC_SERVER_INFO = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Terracotta Server", TerracottaManagement.MBeanDomain.INTERNAL);
      LOGGER = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Logger", TerracottaManagement.MBeanDomain.INTERNAL);
      DSO = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "DSO", TerracottaManagement.MBeanDomain.PUBLIC);
      LOCAL_DGC_STATS = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "DSO Server", TerracottaManagement.MBeanDomain.PUBLIC);
      OBJECT_MANAGEMENT = TerracottaManagement.createObjectName(Type.Server, Subsystem.ObjectManagement, null, "ObjectManagement", TerracottaManagement.MBeanDomain.PUBLIC);
      DUMPER = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "L2Dumper", TerracottaManagement.MBeanDomain.INTERNAL);
      SERVER_DB_BACKUP = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Terracotta Server Backup", TerracottaManagement.MBeanDomain.INTERNAL);
      ENTERPRISE_TC_SERVER = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Enterprise Terracotta Server", TerracottaManagement.MBeanDomain.INTERNAL);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }
}