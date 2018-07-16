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
package com.tc.object;

import com.tc.entity.VoltronEntityMessage;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;



/**
 * The minimal interface, provided to the EntityClientEndpoint, to handle invocations to send to the server.
 */
public interface InvocationHandler {
  InFlightMessage invokeAction(EntityID eid, EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> acks, InFlightMonitor monitor, boolean requiresReplication, boolean shouldBlockGetOnRetire, boolean deferred, byte[] payload);
  InFlightMessage invokeActionWithTimeout(EntityID eid, EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> acks, InFlightMonitor monitor, boolean requiresReplication, boolean shouldBlockGetOnRetire, boolean deferred, long invokeTimeout, TimeUnit units, byte[] payload) throws InterruptedException, TimeoutException;
}
