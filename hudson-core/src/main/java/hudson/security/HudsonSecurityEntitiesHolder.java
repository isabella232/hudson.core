/**
 * ******************************************************************************
 *
 * Copyright (c) 2012 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *  Winston Prakash
 *
 ******************************************************************************
 */
package hudson.security;

/**
 * This is a Container class to set and fetch the Security related entities such
 * as Hudson Security Manager and Hudson Security Filter
 *
 * The main reason for the existence of this class is to create and set and
 * fetch the Security entities outside of Hudson main context to be used in
 * initialization context such as InitialSetup, where Hudson could be setup
 * initially before it starts.
 *
 * Hudson Security Manager will be created and set in the Servlet Context
 * Listener
 *
 * @since 3.0.0
 *
 * @author Winston Prakash
 */
public class HudsonSecurityEntitiesHolder {

    private static HudsonSecurityManager hudsonSecurityManager;
    private static HudsonFilter hudsonSecurityFilter;

    public static HudsonFilter getHudsonSecurityFilter() {
        return hudsonSecurityFilter;
    }

    public static void setHudsonSecurityFilter(HudsonFilter filter) {
        hudsonSecurityFilter = filter;
    }

    public static HudsonSecurityManager getHudsonSecurityManager() {
        return hudsonSecurityManager;
    }

    public static void setHudsonSecurityManager(HudsonSecurityManager securityManager) {
        hudsonSecurityManager = securityManager;
    }
}
