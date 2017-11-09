package org.terracotta.passthrough;

import java.util.concurrent.Executor;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeMonitor;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;

/**
 *
 */
public class PassthroughMonitor<R extends EntityResponse> {
  private final MessageCodec<?, R> codec;
  private final InvokeMonitor<R> monitor;
  private final Executor executor;

  public PassthroughMonitor(MessageCodec<?, R> codec, InvokeMonitor<R> monitor, Executor e) {
    this.codec = codec;
    this.monitor = monitor;
    this.executor = e;
  }
  
  public void sendResponse(byte[] data) {
    R response = null;
    try {
      response = codec.decodeResponse(data);
    } catch (MessageCodecException codec) {
      throw new RuntimeException(codec);
    }
    final R lock = response;
    if (monitor != null) {
      if (executor != null) {
        executor.execute(()->monitor.accept(lock));
      } else {
        monitor.accept(lock);
      }
    }
  }
  
  public void sendException(EntityException exp) {
    if (monitor != null) {
      if (executor != null) {
        executor.execute(()->monitor.exception(exp));
      } else {
        monitor.exception(exp);
      }
    }
  }
  
}
