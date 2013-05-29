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
import hudson.security.ACL;
import hudson.security.Permission;
import java.io.File;
import java.io.IOException;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.eclipse.hudson.security.team.TeamManager.TeamNotFoundException;
import org.junit.After;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.acls.sid.Sid;

/**
 * Test class for TeamBasedACL
 *
 * @author Winston Prakash
 */
public class TeamJobACLTest {

    private Permission configurePermission = Item.CONFIGURE;
    private Permission readPermission = Item.READ;
    
    private File homeDir = FileUtils.getTempDirectory();
    private File teamsFolder = new File(homeDir, "teams");
    private final String teamsConfigFileName = "teams.xml";
    private File teamsStore = new File(teamsFolder, teamsConfigFileName);
    private TeamManager teamManager;

    @Before
    public void setUp() {
        if (teamsStore.exists()) {
            teamsStore.delete();
        }
        teamManager = new TeamManager(homeDir);
        teamManager.setUseBulkSaveFlag(false);
    }

    @After
    public void tearDown() {
        if (teamsStore.exists()) {
            teamsStore.delete();
        }
    }

    @Test
    public void testJobPermission() throws IOException, TeamManager.TeamAlreadyExistsException {
        String teamName = "team1";
        teamManager.createTeam(teamName);
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");
        try {
            teamManager.addUser(teamName, "Paul");
            teamManager.addJobToUserTeam("Paul", freeStyleJob.getName());
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }

        Sid sid = new PrincipalSid("Paul");
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertTrue("Paul is a team member and should have Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());

        Sid sid2 = new PrincipalSid("Chris");
        Assert.assertFalse("Chris is not a team member and should not have Job CONFIGURE permission", teamBasedACL.hasPermission(sid2, configurePermission).booleanValue());
        Assert.assertFalse("Chris is not a team member and should not have Job READ permission", teamBasedACL.hasPermission(sid2, readPermission).booleanValue());

    }

    @Test
    public void testPublicJobPermission() throws IOException {
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");
        try {
            teamManager.getPublicTeam().addJob(freeStyleJob.getName());
        } catch (TeamNotFoundException ex) {
            fail("Public Team must exist");
        }
        Sid sid = new PrincipalSid("Paul");
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertFalse("Paul is not a SysAdmin and should not have public Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());
        Assert.assertTrue("Paul should have pubic Job READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

        teamManager.addSysAdmin("Paul");
        Assert.assertTrue("Paul is a SysAdmin and should have public Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());
    }

    @Test
    public void testAnonymousPublicJobPermission() throws IOException {
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");
        try {
            teamManager.getPublicTeam().addJob(freeStyleJob.getName());
        } catch (TeamNotFoundException ex) {
            fail("Public Team must exist");
        }

        Sid sid = ACL.ANONYMOUS;
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertFalse("Anonymous should not have public Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());
        Assert.assertTrue("Anonymous should have public Job READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

    }

    @Test
    public void testEveryonePublicJobPermission() throws IOException {
        FreeStyleProject freeStyleJob = new FreeStyleProjectMock("testJob");
        try {
            teamManager.getPublicTeam().addJob(freeStyleJob.getName());
        } catch (TeamNotFoundException ex) {
            fail("Public Team must exist");
        }

        Sid sid = ACL.EVERYONE;
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.JOB, freeStyleJob);
        Assert.assertFalse("Every one should not have public Job CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());
        Assert.assertTrue("Every one should have piublic Job READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

    }
}