/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * Archive.java
 *
 * Created on 2. Maerz 2006, 00:47
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.ArchiveDriver;

/**
 * Describes general properties of any archive.
 * A single instance of this interface is created for every
 * canonical path name representation of an archive file.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public interface Archive {

    /**
     * Returns the <em>canonical</em> path of the archive file.
     * A canonical path is both absolute and unique.
     * The precise definition depends on the platform, but all elements in
     * a canonical path are separated by {@link java.io.File#separator}s.
     * <p>
     * This property may be used to determine some archive file specific
     * parameters, such as passwords or similar.
     * However, implementations must not assume that the file denoted by the
     * path actually exists as a file in the native file system!
     *
     * @return A valid reference to a {@link String} object
     *         - never <code>null</code>.
     */
    String getPath();
}
