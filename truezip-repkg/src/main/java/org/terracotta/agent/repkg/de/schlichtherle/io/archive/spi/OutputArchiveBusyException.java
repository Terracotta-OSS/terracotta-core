/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ArchiveDriverBusyException.java
 *
 * Created on 7. Maerz 2006, 21:55
 */
/*
 * Copyright 2006 Schlichtherle IT Services
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi;

import org.terracotta.agent.repkg.de.schlichtherle.io.FileBusyException;

/**
 * Thrown to indicate that the {@link OutputArchive#getOutputStream} method
 * failed because the archive is already busy on output.
 * This exception is guaranteed to be recoverable,
 * meaning it must be possible to write the same entry again as soon as the
 * archive is not busy on output anymore, unless another exceptional condition
 * applies.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public class OutputArchiveBusyException extends FileBusyException {
    
    /**
     * Constructs an instance of <code>ArchiveDriverBusyException</code> with
     * the specified archive entry.
     * 
     * @param entry The archive entry which was tried to write while
     *        its associated {@link OutputArchive} was busy.
     */
    public OutputArchiveBusyException(ArchiveEntry entry) {
        super(entry.getName());
    }
}
