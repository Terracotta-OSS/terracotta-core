package com.tc.object;

import com.tc.exception.EntityBusyException;
import com.tc.exception.VoltronWrapperException;
import com.tc.util.Assert;

import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityUserException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.exception.PermanentEntityException;
import org.terracotta.exception.RuntimeEntityException;

import java.util.Arrays;


public class ExceptionUtils {
  public static EntityException addLocalStackTraceToEntityException(EntityException e) throws RuntimeEntityException {
    EntityException wrappedException;
    if(e instanceof EntityUserException) {
      wrappedException = new EntityUserException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof EntityNotFoundException) {
      wrappedException = new EntityNotFoundException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof VoltronWrapperException) {
      throwRuntimeExceptionWithLocalStack(((VoltronWrapperException)e).getWrappedException());
      // We won't reach this point.
      wrappedException = null;
    } else if(e instanceof EntityNotProvidedException) {
      wrappedException = new EntityNotProvidedException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof EntityVersionMismatchException) {
      EntityVersionMismatchException vme = (EntityVersionMismatchException) e;
      wrappedException = new EntityVersionMismatchException(e.getClassName(), e.getEntityName(), vme.getExpectedVersion(), vme.getAttemptedVersion(), e);
    } else if(e instanceof EntityAlreadyExistsException) {
      wrappedException = new EntityAlreadyExistsException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof EntityConfigurationException) {
      wrappedException = new EntityConfigurationException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof EntityBusyException) {
      wrappedException = new EntityBusyException(e.getClassName(), e.getEntityName(), e);
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

  private static void throwRuntimeExceptionWithLocalStack(RuntimeEntityException wrappedException) throws RuntimeEntityException {
    if (wrappedException instanceof PermanentEntityException) {
      throw new PermanentEntityException(wrappedException.getClassName(), wrappedException.getEntityName(), wrappedException);
    } else if (wrappedException instanceof ConnectionClosedException) {
      throw new ConnectionClosedException(wrappedException.getDescription(), wrappedException);
    } else {
      // Unknown exception - this must be populated.
      Assert.fail("Unhandled runtime exception type");
    }
  }
}
