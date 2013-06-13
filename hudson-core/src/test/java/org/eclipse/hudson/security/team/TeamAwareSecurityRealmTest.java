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

import hudson.model.FreeStyleProject;
import hudson.model.FreeStyleProjectMock;
import hudson.model.Item;
import hudson.security.Permission;
import java.io.File;
import java.io.IOException;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.eclipse.hudson.security.HudsonSecurityEntitiesHolder;
import org.eclipse.hudson.security.HudsonSecurityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.acls.sid.Sid;

/**
 * Test class for TeamAwareSecurityRealmTest
 *
 * @author Winston Prakash
 */
public class TeamAwareSecurityRealmTest {

    private Permission configurePermission = Item.CONFIGURE;
    private Permission readPermission = Item.READ;
    
    private File homeDir = FileUtils.getTempDirectory();
    private File teamsFolder = new File(homeDir, "teams");
    private final String teamsConfigFileName = "teams.xml";
    private File teamsStore = new File(teamsFolder, teamsConfigFileName);
    private TeamManager teamManager;
    private HudsonSecurityManager hudsonSecurityManager;

    @Before
    public void setUp() throws IOException {
        if (teamsStore.exists()) {
            teamsStore.delete();
        }
        teamManager = new TeamManager(homeDir);
        teamManager.setUseBulkSaveFlag(false);
        hudsonSecurityManager = new HudsonSecurityManager(homeDir);
        HudsonSecurityEntitiesHolder.setHudsonSecurityManager(hudsonSecurityManager);
    }

    @After
    public void tearDown() {
        if (teamsStore.exists()) {
            teamsStore.delete();
        }
    }

    @Test
    public void testGlobalSysAdminPermission() throws IOException, TeamManager.TeamAlreadyExistsException{
        Team newTeam = teamManager.createTeam("team1");
        hudsonSecurityManager.setSecurityRealm(new TeamAwareSecurityRealmImpl(newTeam, false, false));

        //Dummy Sid
        Sid sid = new PrincipalSid("Paul");
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.GLOBAL);
        Assert.assertFalse("Current user should not have global CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());
        Assert.assertTrue("Current user should have global READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

        hudsonSecurityManager.setSecurityRealm(new TeamAwareSecurityRealmImpl(newTeam, true, false));
        Assert.assertTrue("Current user should have global CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());

    }

    @Test
    public void testJobPermission() throws IOException, TeamManager.TeamAlreadyExistsException {
        Team team1 = teamManager.createTeam("team1");
        hudsonSecurityManager.setSecurityRealm(new TeamAwareSecurityRealmImpl(team1, false, false));
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");
        team1.addJob(new TeamJob(freeStyleJob.getName()));

        //Dummy Sid
        Sid sid = new PrincipalSid("Paul");
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertTrue("Current user should have Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());

        Team team2 = teamManager.createTeam("team2");
        freeStyleJob = new FreeStyleProjectMock("testJob2");
        team2.addJob(new TeamJob(freeStyleJob.getName()));

        teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertFalse("Current user should not have Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());
        Assert.assertFalse("Current user should not have Job READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());
    }

    @Test
    public void testPublicJobPermission() throws IOException, TeamManager.TeamNotFoundException, TeamManager.TeamAlreadyExistsException {
        String teamName = "team1";
        Team newTeam = teamManager.createTeam(teamName);
        hudsonSecurityManager.setSecurityRealm(new TeamAwareSecurityRealmImpl(newTeam, false, false));

        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");
        teamManager.getPublicTeam().addJob(new TeamJob(freeStyleJob.getName()));

        //Dummy Sid
        Sid sid = new PrincipalSid("Paul");
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertFalse("Current user should not have Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());
        Assert.assertTrue("Current user should have Job READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());
    }

    public class TeamAwareSecurityRealmImpl extends TeamAwareSecurityRealm {

        private Team team;
        private boolean isSysAdmin;
        private boolean isTeamAdmin;

        TeamAwareSecurityRealmImpl(Team team, boolean isSysAdmin, boolean isTeamAdmin) {
            this.team = team;
            this.isSysAdmin = isSysAdmin;
            this.isTeamAdmin = isTeamAdmin;
        }

        @Override
        public Team GetCurrentUserTeam() {
            return team;
        }

        @Override
        public boolean isCurrentUserSysAdmin() {
            return isSysAdmin;
        }

        @Override
        public boolean isCurrentUserTeamAdmin() {
            return isTeamAdmin;
        }

        @Override
        public SecurityComponents createSecurityComponents() {
            return null;
        }
    }
}