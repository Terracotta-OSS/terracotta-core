/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.entity.VoltronEntityMessage;

import java.util.Set;
import java.util.concurrent.Future;


/**
 * The minimal interface, provided to the EntityClientEndpoint, to handle invocations to send to the server.
 */
public interface InvocationHandler {
  Future<byte[]> invokeAction(EntityDescriptor entityDescriptor, Set<VoltronEntityMessage.Acks> acks, boolean requiresReplication, byte[] payload);
}
