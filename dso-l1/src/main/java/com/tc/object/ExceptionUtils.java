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
 */
package com.tc.object;

import com.tc.exception.EntityBusyException;
import com.tc.exception.EntityReferencedException;
import com.tc.exception.ServerException;
import com.tc.exception.WrappedEntityException;

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
  public static EntityException throwEntityException(Exception exp) {
    if (exp instanceof RuntimeException) {
      throw (RuntimeException)exp;
    } else {
      return convert(exp);
    }
  }
  
  public static EntityException convert(Exception server) {
    if (server instanceof ServerException) {
      return convertServerException((ServerException)server);
    } else if (server instanceof EntityException) {
      return (EntityException)server;
    } else {
      return new WrappedEntityException("", "", server.getMessage(), server);
    }
  }
  
  private static EntityException convertServerException(ServerException exp) {
    switch (exp.getType()) {
      case CONNECTION_CLOSED:
      case CONNECTION_SHUTDOWN:
        return new WrappedEntityException(new ConnectionClosedException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), true, exp));
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
        return new WrappedEntityException(new EntityServerUncaughtException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause()));
      case ENTITY_USER_EXCEPTION:
        return new EntityServerException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause());
      case ENTITY_VERSION_MISMATCH:
        return new EntityVersionMismatchException(exp.getClassName(), exp.getEntityName(), 0, 0, exp.getCause());
      case MESSAGE_CODEC:
        return new EntityServerException(exp.getClassName(), exp.getEntityName(), exp.getDescription(), exp.getCause());
      case PERMANENT_ENTITY:
        return new WrappedEntityException(new PermanentEntityException(exp.getClassName(), exp.getEntityName(), exp.getCause()));
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
