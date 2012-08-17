/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * FileBusyException.java
 *
 * Created on 11. September 2005, 18:10
 */
/*
 * Copyright 2005-2007 Schlichtherle IT Services
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


import java.io.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;

/**
 * Thrown if an archive entry cannot get accessed because either
 * (a) the client application is trying to input or output to the same archive
 * file concurrently and the respective archive driver does not support this,
 * or
 * (b) the archive file needs an implicit unmount which cannot get performed
 * because the client application is still using some other open streams for
 * the same archive file.
 * <p>
 * In order to recover from this exception, client applications may call
 * {@link File#umount()} or {@link File#update()} in order to force all
 * entry streams for all archive files to close and prepare to catch the
 * resulting {@link ArchiveBusyWarningException}.
 * A subsequent try to create the archive entry stream will then succeed
 * unless other exceptional conditions apply.
 * However, if the client application is still using a disconnected stream,
 * it will receive an {@link ArchiveEntryStreamClosedException} on the next
 * call to any other method than <code>close()</code>.
 *
 * @see <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @see FileInputStream
 * @see FileOutputStream
 * @author Christian Schlichtherle
 * @version @version@
 */
public class FileBusyException extends FileNotFoundException {

    /**
     * For use by
     * {@link org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.InputArchiveBusyException} and
     * {@link org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.OutputArchiveBusyException} only.
     */
    protected FileBusyException(final String msg) {
        super(msg);
    }

    // TODO: Remove this.
    /**
     * @deprecated You should not use this constructor.
     *             It will vanish in the next major version.
     */
    public FileBusyException(InputArchiveBusyException cause) {
        super(cause != null ? cause.toString() : null);
        initCause(cause);
    }

    // TODO: Remove this.
    /**
     * @deprecated You should not use this constructor.
     *             It will vanish in the next major version.
     */
    public FileBusyException(OutputArchiveBusyException cause) {
        super(cause != null ? cause.toString() : null);
        initCause(cause);
    }

    // TODO: Make this package private.
    /**
     * @deprecated You should not use this constructor.
     *             It will have package private access in the next major version.
     */
    public FileBusyException(ArchiveBusyException cause) {
        super(cause != null ? cause.toString() : null);
        initCause(cause);
    }
}
