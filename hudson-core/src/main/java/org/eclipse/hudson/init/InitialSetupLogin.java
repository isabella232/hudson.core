/**
 * *****************************************************************************
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
 * Winston Prakash
 *
 ******************************************************************************
 */
package org.eclipse.hudson.init;

import hudson.security.Permission;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

/**
 * Provides support for initial setup login if Hudson security is already set
 *
 * @author Winston Prakash
 */
final public class InitialSetupLogin {

    private HudsonSecurityManager hudsonSecurityManager;
    private InitialSetup initialSetup;

    public InitialSetupLogin(InitialSetup initSetup) {
        initialSetup = initSetup;
        hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
    }

    public HudsonSecurityManager getHudsonSecurityManager() {
        return hudsonSecurityManager;
    }
    
    public boolean canFinish(){
        return initialSetup.canFinish();
    }

    public HttpResponse doFinish() {
        return initialSetup.doCheckFinish();
    }

    public HttpResponse doContinue() {
        if (!hudsonSecurityManager.hasPermission(Permission.HUDSON_ADMINISTER)) {
            return HttpResponses.forbidden();
        }
        initialSetup.getServletContext().setAttribute("app", initialSetup);
        return HttpResponses.ok();
    }
}
