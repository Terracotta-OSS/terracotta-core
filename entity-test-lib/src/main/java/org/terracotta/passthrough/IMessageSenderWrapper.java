package org.terracotta.passthrough;


/**
 * An interface used to represent the party who sent a message to a server.
 * This exists so that the core message handling code can work the same way, whether the sender of the message was a client
 * or an active.
 * Specifically, the meaning of different messages is exposed via different methods since some implementations don't want to
 * use the message, only understand why it was sent.
 */
public interface IMessageSenderWrapper {
  void sendAck(PassthroughMessage ack);
  void sendComplete(PassthroughMessage complete);
  PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID);
  /**
   * Returns the underlying connection on the client.
   * NOTE:  This can only be used for identity-matching purposes (could be replaced with a client-unique ID, later on).
   */
  PassthroughConnection getClientOrigin();
}
