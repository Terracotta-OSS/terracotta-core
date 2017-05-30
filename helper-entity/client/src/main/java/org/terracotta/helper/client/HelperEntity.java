package org.terracotta.helper.client;

import org.terracotta.connection.entity.Entity;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.StateDumpable;
import org.terracotta.entity.StateDumper;
import org.terracotta.exception.EntityException;
import org.terracotta.helper.common.HelperEntityMessage;
import org.terracotta.helper.common.HelperEntityMessageType;
import org.terracotta.helper.common.HelperEntityResponse;

public class HelperEntity implements Entity, StateDumpable {
  
  private final EntityClientEndpoint<HelperEntityMessage, HelperEntityResponse> entityClientEndpoint;
  private volatile Runnable collectState;
  
  public HelperEntity(final EntityClientEndpoint<HelperEntityMessage, HelperEntityResponse> entityClientEndpoint) {
    this.entityClientEndpoint = entityClientEndpoint;
    entityClientEndpoint.setDelegate(new EndpointDelegate() {
      @Override
      public void handleMessage(final EntityResponse entityResponse) {
        if(entityResponse instanceof HelperEntityResponse) {
          HelperEntityMessageType type = ((HelperEntityResponse) entityResponse).getType();
          
          switch (type) {
            case DUMP:
              ThreadDumpUtil.getThreadDump();
              if(collectState != null) {
                collectState.run();
              }
              break;
            default:
              throw new IllegalArgumentException("Unknown message type: " + type);
          }
        }
      }

      @Override
      public byte[] createExtendedReconnectData() {
        return new byte[0];
      }

      @Override
      public void didDisconnectUnexpectedly() {

      }
    });
  }
  
  public void dumpState() {
    try {
      InvokeFuture<HelperEntityResponse> invokeFuture = entityClientEndpoint.beginInvoke()
          .replicate(true)
          .message(new HelperEntityMessage(HelperEntityMessageType.DUMP))
          .invoke();
      invokeFuture.get();
    } catch (MessageCodecException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (EntityException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  
  @Override
  public void close() {
    entityClientEndpoint.close();
  }

  @Override
  public void dumpStateTo(final StateDumper stateDumper) {
    stateDumper.dumpState("key", "value");
  }

  public void setCollectState(final Runnable collectState) {
    this.collectState = collectState;
  }
}
