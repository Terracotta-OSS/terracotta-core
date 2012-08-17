/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ReadWriteSfxDriver.java
 *
 * Created on 24. Dezember 2005, 00:01
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip;


import java.io.*;

import javax.swing.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;

/**
 * An archive driver which reads and writes Self Executable (SFX/EXE) ZIP
 * files.
 * <p>
 * <b>Warning:</b> Modifying SFX archives usually voids the SFX code in the
 * preamble!
 * This is because most SFX implementations do not tolerate the contents of
 * the archive to be modified (by intention or accident).
 * When executing the SFX code of a modified archive, anything may happen:
 * The SFX code may be terminating with an error message, crash, silently
 * produce corrupted data, or even something more evil.
 * However, an archive modified with this driver is still a valid ZIP file.
 * So you may still extract the modified archive using a regular ZIP utility.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 * @see ReadOnlySfxDriver
 */
public class ReadWriteSfxDriver extends AbstractSfxDriver {
    private static final long serialVersionUID = -937199842631639717L;

    /**
     * Equivalent to {@link #ReadWriteSfxDriver(String, Icon, Icon, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, DEFAULT_LEVEL)}.
     */
    public ReadWriteSfxDriver() {
        this(DEFAULT_CHARSET, null, null, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #ReadWriteSfxDriver(String, Icon, Icon, boolean, int)
     * this(charset, null, null, false, DEFAULT_LEVEL)}.
     */
    public ReadWriteSfxDriver(String charset) {
        this(charset, null, null, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #ReadWriteSfxDriver(String, Icon, Icon, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, level)}.
     */
    public ReadWriteSfxDriver(int level) {
        this(DEFAULT_CHARSET, null, null, false, level);
    }

    /** Constructs a new read-write SFX/EXE driver. */
    public ReadWriteSfxDriver(
            String charset,
            Icon openIcon,
            Icon closedIcon,
            boolean postambled,
            final int level) {
        super(charset, openIcon, closedIcon, postambled, level);
    }
}
