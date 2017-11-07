package org.terracotta.passthrough;

import org.terracotta.entity.ActiveInvokeChannel;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityServerException;

public class PassThroughServerActiveInvokeContext<M extends EntityMessage, R extends EntityResponse> extends PassThroughServerInvokeContext
  implements ActiveInvokeContext<R> {
  private final MessageCodec<M, R> codec;
  private final EntityMessage message;
  private final ClientDescriptor descriptor;
  private final IMessageSenderWrapper monitor;
  private final PassthroughRetirementManager retirement;

  public PassThroughServerActiveInvokeContext(M message, ClientDescriptor descriptor, int concurrencyKey, long current, long
    oldest, IMessageSenderWrapper monitor, PassthroughRetirementManager retirement, MessageCodec<M, R> codec) {
    super(descriptor == null ? null : descriptor.getSourceId(), concurrencyKey, current, oldest);
    this.message = message;
    this.descriptor = descriptor;
    this.monitor = monitor;
    this.retirement = retirement;
    this.codec = codec;
  }

  @Override
  public ClientDescriptor getClientDescriptor() {
    return descriptor;
  }

  @Override
  public ActiveInvokeChannel<R> openInvokeChannel() {
    monitor.open();
    return new ActiveInvokeChannel<R>() {
      @Override
      public void sendResponse(R response) {
        try {
          byte[] r = codec.encodeResponse(response);
          PassthroughMessage msg = PassthroughMessageCodec.createMonitorMessage(r, null);
          msg.setTransactionTracking(PassThroughServerActiveInvokeContext.this.getCurrentTransactionId(),PassThroughServerActiveInvokeContext.this.getOldestTransactionId());
          monitor.sendComplete(msg, false);
        } catch (MessageCodecException codec) {
          throw new RuntimeException(codec);
        }
      }

      @Override
      public void sendException(Exception excptn) {
        EntityException exp = (excptn instanceof EntityException) ? (EntityException)excptn : new EntityServerException(null, null, null, excptn);
        PassthroughMessage msg = PassthroughMessageCodec.createMonitorMessage(null, exp);
        msg.setTransactionTracking(PassThroughServerActiveInvokeContext.this.getCurrentTransactionId(), PassThroughServerActiveInvokeContext.this.getOldestTransactionId());
        monitor.sendComplete(msg, false);
      }

      @Override
      public void close() {
        monitor.close();
      }
    };
  }
}