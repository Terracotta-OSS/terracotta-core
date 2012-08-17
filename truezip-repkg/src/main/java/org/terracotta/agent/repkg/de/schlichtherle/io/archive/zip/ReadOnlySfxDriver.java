/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ReadOnlySfxDriver.java
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
 * An archive driver which reads Self Executable (SFX/EXE) ZIP files,
 * but doesn't support to create or update them.
 * <p>
 * Instances of this class are immutable.
 * 
 * @see CheckedReadOnlySfxDriver
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 * @see ReadWriteSfxDriver
 */
public class ReadOnlySfxDriver extends AbstractSfxDriver {
    private static final long serialVersionUID = -993451557140046215L;

    /**
     * Equivalent to {@link #ReadOnlySfxDriver(String, Icon, Icon, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, DEFAULT_LEVEL)}.
     */
    public ReadOnlySfxDriver() {
        this(DEFAULT_CHARSET, null, null, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #ReadOnlySfxDriver(String, Icon, Icon, boolean, int)
     * this(charset, null, null, false, DEFAULT_LEVEL)}.
     */
    public ReadOnlySfxDriver(String charset) {
        this(charset, null, null, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #ReadOnlySfxDriver(String, Icon, Icon, boolean, int)
     * this(DEFAULT_CHARSET, null, null, false, level)}.
     */
    public ReadOnlySfxDriver(int level) {
        this(DEFAULT_CHARSET, null, null, false, level);
    }

    /** Constructs a new read-only SFX/EXE driver. */
    public ReadOnlySfxDriver(
            String charset,
            Icon openIcon,
            Icon closedIcon,
            boolean postambled,
            final int level) {
        super(charset, openIcon, closedIcon, postambled, level);
    }

    protected Zip32OutputArchive createZip32OutputArchive(
            Archive archive,
            OutputStream out,
            Zip32InputArchive source)
    throws IOException {
        throw new FileNotFoundException(
                "driver class does not support creating or modifying SFX archives");
    }
}
