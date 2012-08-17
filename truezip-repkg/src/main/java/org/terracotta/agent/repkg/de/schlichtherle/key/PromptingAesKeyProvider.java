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
 * An implementation of {@link AesKeyProvider} which prompts the user for
 * a key and allows to select the cipher key strength when creating a new
 * AES encrypted resource or replacing the entire contents of an already
 * existing AES encrypted resource.
 * <p>
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.0
 */
public class PromptingAesKeyProvider
        extends PromptingKeyProvider
        implements AesKeyProvider {

    private int keyStrength = KEY_STRENGTH_256;

    public int getKeyStrength() {
        assert keyStrength == KEY_STRENGTH_128
                || keyStrength == KEY_STRENGTH_192
                || keyStrength == KEY_STRENGTH_256;
        return keyStrength;
    }

    public void setKeyStrength(int keyStrength) {
        if (keyStrength != KEY_STRENGTH_128
                && keyStrength != KEY_STRENGTH_192
                && keyStrength != KEY_STRENGTH_256)
            throw new IllegalArgumentException();
        this.keyStrength = keyStrength;
    }

    protected String getUIClassID() {
        return "PromptingAesKeyProvider"; // support code obfuscation!
    }

    /**
     * Resets the key strength to 256 bits.
     */
    protected void onReset() {
        keyStrength = KEY_STRENGTH_256;
    }

    /**
     * Returns the current key strength.
     */
    public String toString() {
        return "" + (128 + keyStrength * 64);
    }
}