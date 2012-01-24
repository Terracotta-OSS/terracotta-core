/*******************************************************************************************
 * Copyright (c) Jonas Boner, Alexandre Vasseur. All rights reserved.                      *
 * http://backport175.codehaus.org                                                         *
 * --------------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of Apache License Version 2.0 *
 * a copy of which has been included with this distribution in the license.txt file.       *
 *******************************************************************************************/
package com.tc.backport175.bytecode.spi;

import java.io.IOException;

/**
 * Callback interface that all vendors that wants to be able to control which bytecode is read when retrieving the
 * annotations should implement.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Boner</a>
 */
public interface BytecodeProvider {

    /**
     * Returns the bytecode for a specific class.
     *
     * @param className the fully qualified name of the class
     * @param loader    the class loader that has loaded the class
     * @return the bytecode
     * @throws Exception upon failure
     */
    byte[] getBytecode(String className, ClassLoader loader) throws ClassNotFoundException, IOException;
}
