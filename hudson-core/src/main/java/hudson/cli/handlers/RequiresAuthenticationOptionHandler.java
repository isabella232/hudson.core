/*
 * Copyright (c) 2013 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */

package hudson.cli.handlers;

import hudson.model.TopLevelItem;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Setter;

/**
 * Works around a design flaw in option handlers. Certain parameters may require
 * an authenticated user to succeed; other parameters, e.g., --username and
 * --password are used to authenticate users.
 * Thus, <code>parseArguments</code> may require authentication and be required by
 * authentication.
 * @author Bob Foster
 */
public abstract class RequiresAuthenticationOptionHandler<T> extends OptionHandler<T>  {

    public RequiresAuthenticationOptionHandler(CmdLineParser parser, OptionDef option, Setter<T> setter) {
        super(parser, option, setter);
    }
    
    private static boolean isAuthenticated;
    
    public static boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    public static void setIsAuthenticated(boolean isAuthenticated) {
        RequiresAuthenticationOptionHandler.isAuthenticated = isAuthenticated;
    }
}
