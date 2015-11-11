package org.terracotta.passthrough;


/**
 * In the case where we are an active sending a message to a downstream passive, we use this implementation to provide the
 * basic interlock across the 2 threads.
 */
public class PassthroughInterserverInterlock implements IMessageSenderWrapper {
  private final IMessageSenderWrapper sender;
  private boolean isDone = false;
  
  public PassthroughInterserverInterlock(IMessageSenderWrapper sender) {
    this.sender = sender;
  }

  public synchronized void waitForComplete() {
    while (!this.isDone) {
      try {
        wait();
      } catch (InterruptedException e) {
        Assert.unexpected(e);
      }
    }
  }
  @Override
  public void sendAck(PassthroughMessage ack) {
  }
  @Override
  public synchronized void sendComplete(PassthroughMessage complete) {
    this.isDone = true;
    notifyAll();
  }
  @Override
  public PassthroughClientDescriptor clientDescriptorForID(long clientInstanceID) {
    Assert.unreachable();
    return null;
  }
  @Override
  public PassthroughConnection getClientOrigin() {
    return sender.getClientOrigin();
  }
}