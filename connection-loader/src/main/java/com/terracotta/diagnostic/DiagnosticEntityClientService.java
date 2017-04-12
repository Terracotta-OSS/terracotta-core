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
package com.terracotta.diagnostic;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;


public class DiagnosticEntityClientService implements EntityClientService<Diagnostics, Object, EntityMessage, EntityResponse, Void>{

  @Override
  public boolean handlesEntityType(Class<Diagnostics> type) {
    return type == Diagnostics.class;
  }

  @Override
  public byte[] serializeConfiguration(Object c) {
    return new byte[] {};
  }

  @Override
  public Object deserializeConfiguration(byte[] bytes) {
    return new Object();
  }

  @Override
  public Diagnostics create(final EntityClientEndpoint<EntityMessage, EntityResponse> ece, Void ignore) {
    return (Diagnostics)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {Diagnostics.class},
            new java.lang.reflect.InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, final Object[] args) throws Throwable {
        try {
          final String methodName = method.getName();
          InvokeFuture returnValue = ece.beginInvoke().message(new EntityMessage() {
            @Override
            public String toString() {
              if (methodName.equals("get")) {
                return "getJMX " + args[0] + " " + args[1];
              } else if (methodName.equals("set")) {
                return "setJMX " + args[0] + " " + args[1] + " " + args[2];
              } else if (methodName.equals("invoke")) {
                return "invokeJMX " + args[0] + " " + args[1];
              } else {
                return methodName;
              }
            }
          }).invoke();
          // if the server is terminating, never going to get a message back.  just return null
          if (!methodName.equals("terminateServer") && !methodName.equals("forceTerminateServer")) {
            return returnValue.get().toString();
          }
        } catch (EntityException ee) {
          
        } catch (InterruptedException ie) {
          
        } catch (MessageCodecException code) {
          
        }
        return null;
      }}
    );
      
  }

  @Override
  public MessageCodec<EntityMessage, EntityResponse> getMessageCodec() {
    final Charset charset = Charset.forName("UTF-8");
    
    return new MessageCodec<EntityMessage, EntityResponse>() {
      @Override
      public byte[] encodeMessage(EntityMessage m) throws MessageCodecException {
        return m.toString().getBytes(charset);
      }

      @Override
      public EntityMessage decodeMessage(final byte[] bytes) throws MessageCodecException {
        return new EntityMessage() {
          @Override
          public String toString() {
            return new String(bytes, charset);
          }
        };
      }

      @Override
      public byte[] encodeResponse(EntityResponse r) throws MessageCodecException {
        return r.toString().getBytes(charset);
      }

      @Override
      public EntityResponse decodeResponse(final byte[] bytes) throws MessageCodecException {
        return new EntityResponse() {
          @Override
          public String toString() {
            return new String(bytes, charset);
          }
        };
      }
    };
  }
  
}
