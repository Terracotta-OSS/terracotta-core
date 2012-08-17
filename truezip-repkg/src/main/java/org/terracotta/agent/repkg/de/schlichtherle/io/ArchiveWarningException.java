/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ZipUpdateConflictException.java
 *
 * Created on 30. Oktober 2004, 13:26
 */
/*
 * Copyright 2005 Schlichtherle IT Services
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


import java.io.IOException;

/**
 * Represents a chain of exceptions thrown by the {@link File#umount} and
 * {@link File#update} methods to indicate an error condition which
 * does <em>not</em> incur loss of data and may be ignored.
 * 
 * <p>Both methods catch any exceptions occuring throughout their processing
 * and store them in an exception chain until all archive files have been
 * updated.
 * Finally, if the exception chain is not empty, it's reordered and thrown
 * so that if its head is an instance of <code>ArchiveWarningException</code>,
 * only instances of this class or its subclasses are in the chain, but no
 * instances of <code>ArchiveException</code> or its subclasses (except
 * <code>ArchiveWarningException</code>, of course).
 *
 * <p>This enables client applications to do a simple case distinction with a
 * try-catch-block like this to react selectively:</p>
 * <pre><code>
 * try {
 *     File.umount();
 * } catch (ArchiveWarningException warning) {
 *     // Only warnings have occured and no data has been lost - ignore this.
 * } catch (ArchiveException error) {
 *     // Some data has been lost - panic!
 *     error.printStackTrace();
 * }
 * </code></pre>
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public class ArchiveWarningException extends ArchiveException {
    
    // TODO: Make this constructor package private!
    public ArchiveWarningException(
            ArchiveException priorZipException,
            String message) {
        super(priorZipException, message);
    }

    // TODO: Make this constructor package private!
    public ArchiveWarningException(
            ArchiveException priorZipException,
            String message,
            IOException cause) {
        super(priorZipException, message, cause);
    }

    // TODO: Make this constructor package private!
    public ArchiveWarningException(
            ArchiveException priorZipException,
            IOException cause) {
        super(priorZipException, cause);
    }
    
    public int getPriority() {
        return -1;
    }
}
