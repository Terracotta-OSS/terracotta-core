/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * CheckedZip32OutputArchive.java
 *
 * Created on 29. Juni 2006, 20:58
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

package org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip;


import java.io.*;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.spi.*;
import org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip.Zip32OutputArchive.*;

/**
 * A {@link Zip32OutputArchive} which must be used in conjunction with
 * {@link CheckedZip32InputArchive}.
 * 
 * @deprecated Since TrueZIP 6.4, this class has been made redundant in order
 *             to fix a bug in the archive drivers which relied on it and is
 *             effectively a no-op.
 * @see Zip32OutputArchive
 * @see CheckedZip32InputArchive
 * @see CheckedZip32Driver
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.1
 */
public class CheckedZip32OutputArchive extends Zip32OutputArchive {

    public CheckedZip32OutputArchive(
            OutputStream out,
            String charset,
            Zip32InputArchive source)
    throws  NullPointerException,
            UnsupportedEncodingException,
            IOException {
        super(out, charset, source);
    }
}
