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
import hudson.security.SidACL;
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

        GLOBAL, TEAM, JOB
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
        if (scope == SCOPE.GLOBAL) {
            //All non team members gets only READ Permission
            if (permission.getImpliedBy() == Permission.READ) {
                return true;
            }
            // Member of any of the team get JOB CREATE Permission
            if (permission == Item.CREATE) {
                Team team = teamManager.findUserTeam(userName);
                if (team != null) {
                    return true;
                }
            }
        }
        if (scope == SCOPE.TEAM) {
            // Sysadmin gets to do all team maintenance operations
            if (teamManager.isSysAdmin(userName)) {
                return true;
            }
            Team team = teamManager.findUserTeam(userName);

            if (team != null) {
                // Team admin gets to do all team maintenance operations
                if (team.isAdmin(userName)) {
                    return true;
                } else if (team.isMember(userName) && permission.getImpliedBy() == Permission.READ) {
                    return true;
                }
            }
        }
        if (scope == SCOPE.JOB) {
            Team jobTeam = teamManager.findJobOwnerTeam(job.getId());

            if (jobTeam != null) {
                if (jobTeam.isMember(userName)) {
                    // Team admin gets full permission on all jobs
                    if (jobTeam.isAdmin(userName)) {
                        return true;
                    } else {
                        //TODO:
                        //    Check permissions based on job scope, 
                        //    Grant all permission to job owner if it is marked private
                        //    No permission to all other team members for private jobs
                        //    Grant member specific permissions 

                        return true; // for now give full permission to all team members
                    }
                }
            }
            // Grant Read permission to 
            Team publicTeam;
            try {
                publicTeam = teamManager.findTeam(PublicTeam.PUBLIC_TEAM_NAME);

                if (publicTeam.isJobOwner(job.getId())) {
                    if (permission.getImpliedBy() == Permission.READ) {
                        return true;
                    } 
                }
            } catch (TeamNotFoundException ex) {
                logger.error("The public team must exists.", ex);
            }
            // TODO: Grant read permission to jobs marked as public scoped in all teams
        }
        return false;

    }
}
