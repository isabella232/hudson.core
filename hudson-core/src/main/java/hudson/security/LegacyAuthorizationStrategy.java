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
 *
 *******************************************************************************/ 

package hudson.security;

import hudson.RestrictedSince;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

/**
 * {@link AuthorizationStrategy} implementation that emulates the legacy behavior.
 *
 * @author Kohsuke Kawaguchi
 * @deprecated as of 2.2.0
 *             This strategy was removed due to <a href='http://issues.hudson-ci.org/browse/HUDSON-8944'>HUDSON-8944</a>
 */
@Restricted(DoNotUse.class)
@RestrictedSince("2.2.0")
public final class LegacyAuthorizationStrategy extends FullControlOnceLoggedInAuthorizationStrategy {
}

