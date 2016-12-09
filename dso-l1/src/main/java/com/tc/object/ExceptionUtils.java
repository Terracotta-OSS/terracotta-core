package com.tc.object;

import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityUserException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.exception.PermanentEntityException;

import java.util.Arrays;
/**
 * @author vmad
 */
public class ExceptionUtils {
  public static EntityException addLocalStackTraceToEntityException(EntityException e) {
    EntityException wrappedException;
    if(e instanceof EntityUserException) {
      wrappedException = new EntityUserException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof EntityNotFoundException) {
      wrappedException = new EntityNotFoundException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof PermanentEntityException) {
      wrappedException = new PermanentEntityException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof EntityNotProvidedException) {
      wrappedException = new EntityNotProvidedException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof EntityVersionMismatchException) {
      EntityVersionMismatchException vme = (EntityVersionMismatchException) e;
      wrappedException = new EntityVersionMismatchException(e.getClassName(), e.getEntityName(), vme.getExpectedVersion(), vme.getAttemptedVersion(), e);
    } else if(e instanceof EntityAlreadyExistsException) {
      wrappedException = new EntityAlreadyExistsException(e.getClassName(), e.getEntityName(), e);
    } else if(e instanceof EntityConfigurationException) {
      wrappedException = new EntityConfigurationException(e.getClassName(), e.getEntityName(), e);
    } else {
      wrappedException = new EntityException(e.getClassName(), e.getClassName(), e.getDescription(), e) {};
    }
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    //strip last two recent elements - getStackTrace() and this method
    wrappedException.setStackTrace(Arrays.copyOfRange(stackTrace, 2, stackTrace.length));
    return wrappedException;
  }
}
