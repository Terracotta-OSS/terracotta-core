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
package com.tc.l2.state;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TerracottaManagement;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class VoterManagerImpl extends AbstractTerracottaMBean implements VoterManager {

  public static final String DIAGNOSTIC_NAME = "VoterManager";

  private final Set<String> voters = (new ConcurrentHashMap<String, Object>()).keySet();
  private final AtomicReference<String> candidate = new AtomicReference<>();

  public VoterManagerImpl() throws Exception {
    super(VoterManager.class, false);
    ManagementFactory.getPlatformMBeanServer().registerMBean(this,
        TerracottaManagement.createObjectName(null, DIAGNOSTIC_NAME, TerracottaManagement.MBeanDomain.PUBLIC));
  }

  @Override
  public boolean registerVoter(String id) {
    System.out.println("Registered " + id);
    return candidate.compareAndSet(null, id);
  }

  @Override
  public boolean confirmVoter(String id) {
    System.out.println("Confirmed " + id);
    if (candidate.compareAndSet(id, null)) {
      voters.add(id);
      return true;
    }
    return false;
  }

  @Override
  public boolean deregisterVoter(String id) {
    System.out.println("Deregister " + id);
    return voters.remove(id);
  }

  @Override
  public void reset() {
    //
  }
}
