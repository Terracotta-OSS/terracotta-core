package com.tc.util;

import org.omg.SendingContext.RunTime;
import org.terracotta.exception.EntityException;

/**
 * @author vmad
 */
public class Throwables {
    public static Throwable getRootCause(Throwable throwable) {
        Throwable tmp;
        while((tmp = throwable.getCause()) != null) {
            throwable = tmp;
        }
        return throwable;
    }

    public static RuntimeException propagate(Throwable throwable) throws RuntimeException, Error {
        propagateIfInstanceOf(throwable, RuntimeException.class);
        propagateIfInstanceOf(throwable, Error.class);
        return new RuntimeException(throwable);
    }

    public static <X extends Throwable> void propagateIfInstanceOf(Throwable throwable, Class<X> clazz) throws X {
        if(throwable != null && clazz.isInstance(throwable)) {
            throw clazz.cast(throwable);
        }
    }
}
