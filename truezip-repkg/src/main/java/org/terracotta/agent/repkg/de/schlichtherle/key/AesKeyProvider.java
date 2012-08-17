/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

package org.terracotta.agent.repkg.de.schlichtherle.key;

/**
 * A {@link KeyProvider} which allows to select the cipher key strength
 * when creating a new AES encrypted resource or replacing the entire
 * contents of an already existing AES encrypted resource.
 * The cipher key strength for the AES encryption may be either 128, 192 or
 * 256 bits.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.1
 */
public interface AesKeyProvider extends KeyProvider {

    /** Identifier for a 128 bit ciphering key. */
    int KEY_STRENGTH_128 = 0;

    /** Identifier for a 192 bit ciphering key. */
    int KEY_STRENGTH_192 = 1;

    /** Identifier for a 256 bit ciphering key. */
    int KEY_STRENGTH_256 = 2;

    /**
     * Returns the cipher key strength for the AES encryption.
     * 
     * @return One of <code>KEY_STRENGTH_128</code>,
     *        <code>KEY_STRENGTH_192</code> or <code>KEY_STRENGTH_256</code>.
     */
    int getKeyStrength();

    /**
     * Sets the cipher key strength for the AES encryption.
     * 
     * @param keyStrength One of <code>KEY_STRENGTH_128</code>,
     *        <code>KEY_STRENGTH_192</code> or <code>KEY_STRENGTH_256</code>.
     * @throws IllegalArgumentException If the preconditions for the parameter
     *         do not hold.
     */
    void setKeyStrength(int keyStrength);
}
