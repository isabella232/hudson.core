/*******************************************************************************
 *
 * Copyright (c) 2012 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *
 *  Winston Prakash
 *     
 *******************************************************************************/ 
package hudson.util;

import hudson.Functions;

/**
 * Model object used to display error page if any Hudson Security initialization error occurs.
 *
 * <p>
 * <tt>index.jelly</tt> would display a nice friendly error page.
 *
 * @author Winston Prakash
 */
public class SecurityFailedToInit extends ErrorObject {
    public final Throwable exception;

    public SecurityFailedToInit(Throwable exception) {
        this.exception = exception;
    }

    public String getStackTrace() {
        return Functions.printThrowable(exception);
    }
}