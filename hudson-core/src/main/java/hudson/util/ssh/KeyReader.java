/********************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi
 *
 *******************************************************************************/

package hudson.util.ssh;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Parses the putty key bit vector, which is an encoded sequence of {@link BigInteger}s.
 *
 * @author Kohsuke Kawaguchi
 */
class KeyReader {

    private final DataInput di;

    KeyReader(byte[] key) {
        this.di = new DataInputStream(new ByteArrayInputStream(key));
    }

    /**
     * Skips an integer without reading it.
     */
    public void skip() {
        try {
            di.skipBytes(di.readInt());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private byte[] read() {
        try {
            int len = di.readInt();
            byte[] r = new byte[len];
            di.readFully(r);
            return r;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Reads the next integer.
     */
    public BigInteger readInt() {
        return new BigInteger(read());
    }
}
