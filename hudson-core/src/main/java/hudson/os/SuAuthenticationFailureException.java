/**
 * *****************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
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
 ******************************************************************************
 */
package hudson.os;

import java.io.IOException;

/**
 * 'su' failed to authenticate the given credential.
 *
 * <p> Wrong password, invalid user name, that sort of things.
 *
 * @author Kohsuke Kawaguchi
 */
public class SuAuthenticationFailureException extends IOException {

    public SuAuthenticationFailureException(String message) {
        super(message);
    }
}
