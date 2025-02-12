/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object;

import com.tc.exception.EntityBusyException;
import com.tc.exception.EntityReferencedException;
import com.tc.exception.ServerException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityServerException;
import org.terracotta.exception.EntityServerUncaughtException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.exception.PermanentEntityException;

public class ExceptionUtils {
  public static void throwEntityException(Exception exp) throws EntityException {
    if (exp instanceof ConnectionClosedException) {
      throw rewriteConnectionClosed((ConnectionClosedException)exp);
    } else if (exp instanceof RuntimeException) {
      throw addCallerStackTraceToRuntime((RuntimeException)exp);
    } else {
      Exception converted = convert(exp);
      if (converted instanceof EntityException) {
        throw (EntityException) converted;
      } else if (converted instanceof RuntimeException) {
        throw (RuntimeException) converted;
      } else {
        throw new RuntimeException(converted);
      }
    }
  }

  private static ConnectionClosedException rewriteConnectionClosed(ConnectionClosedException exp) {
    ConnectionClosedException local = exp.getClassName() != null ? new ConnectionClosedException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), !exp.messageWasNotSent(), exp)
        : new ConnectionClosedException(!exp.messageWasNotSent(), exp.getDescription(), exp);

    // remove this class from exception stack
    local.setStackTrace(Arrays.asList(local.getStackTrace()).stream().filter(e->!e.getClassName().equals(ExceptionUtils.class.getName())).toArray(size->new StackTraceElement[size]));
    return local;
  }

  private static RuntimeException addCallerStackTraceToRuntime(RuntimeException runtime) {
    List<StackTraceElement> newStack = new LinkedList<>(Arrays.asList(Thread.currentThread().getStackTrace()));
    // remove getStackTrace
    newStack.remove(0);
    // remove this classes stacks
    newStack.removeIf(e->e.getClassName().equals(ExceptionUtils.class.getName()));
    newStack.add(new StackTraceElement("##########  trace of exception cause", "starts here   ###########", null, -1));
    newStack.addAll(Arrays.asList(runtime.getStackTrace()));
    runtime.setStackTrace(newStack.toArray(new StackTraceElement[0]));
    return runtime;
  }
  
  public static Exception convert(Exception server) {
    if (server instanceof ServerException) {
      return convertServerException((ServerException)server);
    } else {
      server.addSuppressed(new RuntimeException("caller local trace"));
      return server;
    }
  }
  
  private static Exception convertServerException(ServerException exp) {
    switch (exp.getType()) {
      case CONNECTION_CLOSED:
      case CONNECTION_SHUTDOWN:
        return new ConnectionClosedException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), true, exp);
      case ENTITY_ALREADY_EXISTS:
        return new EntityAlreadyExistsException(exp.getClassName(), exp.getEntityName(), exp.getCause());
      case ENTITY_BUSY_EXCEPTION:
        return new EntityBusyException(exp.getClassName(), exp.getEntityName(), exp.getCause());
      case ENTITY_CONFIGURATION:
        return new EntityConfigurationException(exp.getClassName(), exp.getEntityName(), exp.getCause());
      case ENTITY_NOT_FOUND:
        return new EntityNotFoundException(exp.getClassName(), exp.getEntityName(), exp.getCause());
      case ENTITY_NOT_PROVIDED:
        return new EntityNotProvidedException(exp.getClassName(), exp.getEntityName(), exp.getCause());
      case ENTITY_REFERENCED:
        return new EntityReferencedException(exp.getClassName(), exp.getEntityName());
      case ENTITY_SERVER:
        return new EntityServerException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause());
      case ENTITY_SERVER_UNCAUGHT:
        return new EntityServerUncaughtException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause());
      case ENTITY_USER_EXCEPTION:
        return new EntityServerException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause());
      case ENTITY_VERSION_MISMATCH:
        return new EntityVersionMismatchException(exp.getClassName(), exp.getEntityName(), 0, 0, exp.getCause());
      case MESSAGE_CODEC:
        return new EntityServerException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause());
      case PERMANENT_ENTITY:
        return new PermanentEntityException(exp.getClassName(), exp.getEntityName(), exp.getCause());
      case PERMISSION_DENIED:
        return new EntityServerException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause());
      case RECONNECT_REJECTED:
        return new EntityServerException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause());
      case WRAPPED_EXCEPTION:
        return new EntityServerException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause());
      default:
        return null;
    }
  }
}
