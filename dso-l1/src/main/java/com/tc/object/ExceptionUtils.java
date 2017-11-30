package com.tc.object;

import com.tc.exception.EntityBusyException;
import com.tc.exception.VoltronWrapperException;
import com.tc.exception.VoltronEntityUserExceptionWrapper;
import com.tc.util.Assert;

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
import org.terracotta.exception.RuntimeEntityException;

import java.util.Arrays;


public class ExceptionUtils {
  public static EntityException addLocalStackTraceToEntityException(EntityID eid, EntityException e) throws RuntimeEntityException {
    EntityException wrappedException;
    String entityType = eid.getClassName();
    String enityName = eid.getEntityName();
    if(e instanceof VoltronEntityUserExceptionWrapper) {
      //should we add local stack trace here?
      wrappedException = new EntityServerException(entityType, enityName, e.getDescription(), e.getCause());
    } else if(e instanceof EntityNotFoundException) {
      wrappedException = new EntityNotFoundException(entityType, enityName, e);
    } else if(e instanceof VoltronWrapperException) {
      throwRuntimeExceptionWithLocalStack(eid, ((VoltronWrapperException)e).getWrappedException());
      // We won't reach this point.
      wrappedException = null;
    } else if(e instanceof EntityNotProvidedException) {
      wrappedException = new EntityNotProvidedException(entityType, enityName, e);
    } else if(e instanceof EntityVersionMismatchException) {
      EntityVersionMismatchException vme = (EntityVersionMismatchException) e;
      wrappedException = new EntityVersionMismatchException(entityType, enityName, vme.getExpectedVersion(), vme.getAttemptedVersion(), e);
    } else if(e instanceof EntityAlreadyExistsException) {
      wrappedException = new EntityAlreadyExistsException(entityType, enityName, e);
    } else if(e instanceof EntityConfigurationException) {
      wrappedException = new EntityConfigurationException(entityType, enityName, e);
    } else if(e instanceof EntityBusyException) {
      wrappedException = new EntityBusyException(entityType, enityName, e);
    } else {
//  just return the remote exception with remote stack, don't want to hide the type of the 
//  exception in these cases.
      return e;
    }
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    //strip last two recent elements - getStackTrace() and this method
    wrappedException.setStackTrace(Arrays.copyOfRange(stackTrace, 2, stackTrace.length));
    return wrappedException;
  }

  private static void throwRuntimeExceptionWithLocalStack(EntityID eid, RuntimeEntityException wrappedException) throws RuntimeEntityException {
    String entityType = eid.getClassName();
    String enityName = eid.getEntityName();
    if (wrappedException instanceof PermanentEntityException) {
      throw new PermanentEntityException(entityType, enityName, wrappedException);
    } else if (wrappedException instanceof ConnectionClosedException) {
      throw new LocalConnectionClosedException(eid, wrappedException.getDescription(), wrappedException);
    } else if (wrappedException instanceof EntityServerUncaughtException) {
      throw new EntityServerUncaughtException(entityType, enityName, wrappedException.getDescription(), wrappedException);
    } else {
      // Unknown exception - this must be populated.
      Assert.fail("Unhandled runtime exception type");
    }
  }
}
