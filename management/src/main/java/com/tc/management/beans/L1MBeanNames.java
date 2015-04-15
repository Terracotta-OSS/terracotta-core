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

public class L1MBeanNames {

  public static final ObjectName CLUSTER_BEAN_PUBLIC;
  public static final ObjectName L1INFO_PUBLIC;
  public static final ObjectName ENTERPRISE_TC_CLIENT;

  static {
    try {
      CLUSTER_BEAN_PUBLIC = TerracottaManagement.createObjectName(Type.Cluster, Subsystem.None, null,
                                                                  "Terracotta Cluster Bean",
                                                                  TerracottaManagement.MBeanDomain.PUBLIC);
      L1INFO_PUBLIC = TerracottaManagement.createObjectName(Type.DsoClient, Subsystem.None, null, "L1 Info Bean",
                                                            TerracottaManagement.MBeanDomain.PUBLIC);
      ENTERPRISE_TC_CLIENT = TerracottaManagement.createObjectName(Type.DsoClient, Subsystem.Logging, null,
                                                                   "Terracotta Enterprise Bean",
                                                                   TerracottaManagement.MBeanDomain.PUBLIC);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }

}
