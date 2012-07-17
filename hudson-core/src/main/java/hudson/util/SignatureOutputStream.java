/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Kohsuke Kawaguchi
 *
 *******************************************************************************/ 

package hudson.util;

import org.apache.commons.io.output.NullOutputStream;

import java.security.Signature;
import java.security.SignatureException;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class SignatureOutputStream extends FilterOutputStream {

    private final Signature sig;

    public SignatureOutputStream(OutputStream out, Signature sig) {
        super(out);
        this.sig = sig;
    }

    public SignatureOutputStream(Signature sig) {
        this(new NullOutputStream(), sig);
    }

    @Override
    public void write(int b) throws IOException {
        try {
            sig.update((byte) b);
            out.write(b);
        } catch (SignatureException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            sig.update(b, off, len);
            out.write(b, off, len);
        } catch (SignatureException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }
}
