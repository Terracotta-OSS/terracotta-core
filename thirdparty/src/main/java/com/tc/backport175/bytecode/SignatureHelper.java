/*******************************************************************************************
 * Copyright (c) Jonas Bon4r, Alexandre Vasseur. All rights reserved.                      *
 * http://backport175.codehaus.org                                                         *
 * --------------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of Apache License Version 2.0 *
 * a copy of which has been included with this distribution in the license.txt file.       *
 *******************************************************************************************/
package com.tc.backport175.bytecode;

import com.tc.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * Returns JVM type signature for a members and types.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Boner</a>
 */
public class SignatureHelper {
    /**
     * Returns JVM type signature for a constructor.
     *
     * @param constructor
     * @return
     */
    public static String getConstructorSignature(final Constructor constructor) {
        //TODO: raise issue in ASM for the Type API to accept Constructor
        Class[] paramTypes = constructor.getParameterTypes();
        StringBuffer buf = new StringBuffer();
        buf.append('(');
        for (int i = 0; i < paramTypes.length; i++) {
            buf.append(Type.getDescriptor(paramTypes[i]));
        }
        buf.append(')');
        buf.append(Type.VOID_TYPE.getDescriptor());
        return buf.toString();
    }

    /**
     * Returns JVM type signature for a method.
     *
     * @param method
     * @return
     */
    public static String getMethodSignature(final Method method) {
        return Type.getMethodDescriptor(method);
    }

    /**
     * Returns JVM type signature for a field.
     *
     * @param field
     * @return
     */
    public static String getFieldSignature(final Field field) {
        return Type.getDescriptor(field.getType());
    }

    /**
     * Returns JVM type signature for given class.
     *
     * @param klass
     * @return
     */
    public static String getClassSignature(Class klass) {
        return Type.getDescriptor(klass);
    }

}
