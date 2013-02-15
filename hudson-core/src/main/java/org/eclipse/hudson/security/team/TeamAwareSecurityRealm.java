/*
 * Copyright (c) 2013 Oracle Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Winston Prakash
 */
package org.eclipse.hudson.security.team;

import hudson.security.SecurityRealm;

/**
 * A security realm which is team aware and handles its own team.
 * This is particularly useful if this security realm uses pre authentication.
 *
 * This security realm can provide its own UI to manage team or  it can dynamically 
 * create team. But it should use TeamManager (Hudson.getInstance().getTeamManager())
 * so that the team can be persisted correctly
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public abstract class TeamAwareSecurityRealm extends SecurityRealm {
    
    public abstract Team GetCurrentUserTeam();
        
}
