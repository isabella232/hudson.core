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

import hudson.security.Permission;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple model to hold team members information
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public class TeamMember {

    private String name;
    private boolean isTeamAdmin;
    private Set<Permission> grantedPermissions = new HashSet<Permission>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isTeamAdmin() {
        return isTeamAdmin;
    }

    public void setAsTeamAdmin(boolean teamAdmin) {
        this.isTeamAdmin = teamAdmin;
    }

    public void addPermission(Permission permission) {
        if (!grantedPermissions.contains(permission)) {
            grantedPermissions.add(permission);
        }
    }

    public void removePermission(Permission permission) {
        if (grantedPermissions.contains(permission)) {
            grantedPermissions.remove(permission);
        }
    }

    public boolean hasPermission(Permission permission) {
        return grantedPermissions.contains(permission);
    }
}
