/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
  //public static final ObjectName TOPOLOGY_MBEAN;

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