/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * InputIOException.java
 *
 * Created on 8. Januar 2006, 19:41
 */
/*
 * Copyright 2006-2007 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.agent.repkg.de.schlichtherle.io;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Thrown if an {@link IOException} happened on the input side rather than
 * the output side when copying an InputStream to an OutputStream.
 * This exception is always initialized with an <code>IOException</code> as
 * its cause, so it is safe to cast the return value of
 * {@link Throwable#getCause} to an <code>IOException</code>.
 *
 * @author Christian Schlichtherle
 * @version @version@
 */
// TODO: Review: Rename this to IException?
public class InputIOException extends IOException {

    /**
     * Constructs a new <code>InputIOException</code>.
     *
     * @param cause A valid <code>IOException</code>.
     *        This must not be <code>null</code> and must not be an instance
     *        of {@link FileNotFoundException} (which means that they cannot
     *        not be masked).
     * @throws IllegalArgumentException If <code>cause</code> is an instance of
     *         <code>FileNotFoundException</code>.
     */
    public InputIOException(final IOException cause) {
        super(cause != null ? cause.toString() : null);
        if (cause instanceof FileNotFoundException) {
            final IllegalArgumentException iae = new IllegalArgumentException();
            iae.initCause(cause);
            throw iae;
        }
        initCause(cause);
    }
}
