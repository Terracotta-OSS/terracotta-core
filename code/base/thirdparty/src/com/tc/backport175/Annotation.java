/*******************************************************************************************
 * Copyright (c) Jonas Bonér, Alexandre Vasseur. All rights reserved.                      *
 * http://backport175.codehaus.org                                                         *
 * --------------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of Apache License Version 2.0 *
 * a copy of which has been included with this distribution in the license.txt file.       *
 *******************************************************************************************/
package com.tc.backport175;

/**
 * Marker interface for all reader dynamic proxy implementations.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public interface Annotation {

    /**
     * Returns the annotation type, e.g. the annotation interface type.
     *
     * @return the type
     */
    public Class annotationType();
}