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

import hudson.model.Item;
import hudson.model.Job;
import hudson.security.Permission;
import hudson.security.SecurityRealm;
import hudson.security.SidACL;
import java.util.List;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.eclipse.hudson.security.team.TeamManager.TeamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.acls.sid.Sid;

/**
 * Team based authorization
 *
 * @since 3.1.0
 * @author Winston Prakash
 */
public class TeamBasedACL extends SidACL {

    private transient Logger logger = LoggerFactory.getLogger(TeamBasedACL.class);

    public enum SCOPE {
        GLOBAL, TEAM_MANAGEMENT, TEAM, JOB
    };
    private final SCOPE scope;
    private final TeamManager teamManager;
    private Job job;
    private Team team;

    public TeamBasedACL(TeamManager teamManager, SCOPE scope) {
        this.teamManager = teamManager;
        this.scope = scope;
    }

    public TeamBasedACL(TeamManager teamManager, SCOPE scope, Job job) {
        this(teamManager, scope);
        this.job = job;
    }

    public TeamBasedACL(TeamManager teamManager, SCOPE scope, Team team) {
        this(teamManager, scope);
        this.team = team;
    }

    @Override
    protected Boolean hasPermission(Sid sid, Permission permission) {
        String userName = toString(sid);

        // SysAdmin gets all permission
        if (teamManager.isSysAdmin(userName)) {
            return true;
        }
        
        if (scope == SCOPE.TEAM_MANAGEMENT) {
            //Only Sysadmin gets to do Team Management
            if (teamManager.isSysAdmin(userName)) {
                return true;
            }
        }
        
        if (scope == SCOPE.GLOBAL) {
            //All non team members gets only READ Permission
            if (permission.getImpliedBy() == Permission.READ) {
                return true;
            }
            // Member of any of the team with JOB CREATE Permission can create Job
            if (permission == Item.CREATE) {
                for (Team userTeam : teamManager.findUserTeams(userName)) {
                    TeamMember member = userTeam.findMember(userName);
                    if ((member != null) && member.hasPermission(Item.CREATE)) {
                        return true;
                    }
                }
            }
        }
        if (scope == SCOPE.TEAM) {
            // Sysadmin gets to do all team maintenance operations
            if (teamManager.isSysAdmin(userName)) {
                return true;
            }
            
            for (Team userTeam : teamManager.findUserTeams(userName)) {
                if (userTeam == team) {
                    // Team admin gets to do all team maintenance operations
                    if (userTeam.isAdmin(userName)) {
                        return true;
                    } else if (userTeam.isMember(userName) && permission.getImpliedBy() == Permission.READ) {
                        return true;
                    }
                }
            }
        }
        if (scope == SCOPE.JOB) {
            Team jobTeam = teamManager.findJobOwnerTeam(job.getName());

            if (jobTeam != null) {
                if (jobTeam.isMember(userName)) {
                    // All members of the team get read permission
                        if (permission.getImpliedBy() == Permission.READ) {
                        return true;
                    }
                    if (isTeamAwareSecurityRealm()) {
                        return true; // for now give full permission to all team members
                    } else {
                        TeamMember member = jobTeam.findMember(userName);
                        return member.hasPermission(permission);
                    }
                }
            }
            // Grant Read permission to Public Jobs and jobs based on visibility
            if (permission.getImpliedBy() == Permission.READ) {
                try {
                    Team publicTeam = teamManager.findTeam(PublicTeam.PUBLIC_TEAM_NAME);

                    if (publicTeam.isJobOwner(job.getName())) {
                        if (permission.getImpliedBy() == Permission.READ) {
                            return true;
                        }
                    }
                } catch (TeamNotFoundException ex) {
                    logger.error("The public team must exists.", ex);
                }

                if (jobTeam != null) {
                    TeamJob teamJob = jobTeam.findJob(job.getName());
                    for (Team userTeam : teamManager.findUserTeams(userName)) {
                        if (teamJob.isVisible(userTeam.getName())) {
                            return true;
                        }
                    }
                    if (teamJob.isVisible(PublicTeam.PUBLIC_TEAM_NAME)) {
                        return true;
                    }
                }
            }
        }
        return null;
    }

    private boolean isTeamAwareSecurityRealm() {
        HudsonSecurityManager hudsonSecurityManager = HudsonSecurityEntitiesHolder.getHudsonSecurityManager();
        SecurityRealm securityRealm = null;
        if (hudsonSecurityManager != null) {
            securityRealm = hudsonSecurityManager.getSecurityRealm();
        }
        if ((securityRealm != null) && securityRealm instanceof TeamAwareSecurityRealm) {
            return true;
        }
        return false;
    }
}
