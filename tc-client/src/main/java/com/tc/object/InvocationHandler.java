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

import org.terracotta.entity.Invocation;
import org.terracotta.entity.InvocationCallback;

import java.util.Set;



/**
 * The minimal interface, provided to the EntityClientEndpoint, to handle invocations to send to the server.
 */
public interface InvocationHandler {
  Invocation.Task invokeAction(EntityID eid, EntityDescriptor entityDescriptor, Set<InvocationCallback.Types> callbacks, InvocationCallback<byte[]> callback, boolean requiresReplication, byte[] payload);
}
