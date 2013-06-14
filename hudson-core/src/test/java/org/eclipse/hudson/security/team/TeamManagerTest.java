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
import java.io.File;
import java.io.IOException;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.eclipse.hudson.security.team.TeamManager.TeamNotFoundException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for TeamManager
 *
 * @author Winston Prakash
 */
public class TeamManagerTest {

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

    /**
     * Test of createTeam method, of class TeamManager.
     */
    @Test
    public void testCreateTeam() throws IOException, TeamManager.TeamAlreadyExistsException {
        String teamName = "team1";
        teamManager.createTeam(teamName);
        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
    }

    /**
     * Test of removeTeam method, of class TeamManager.
     */
    @Test
    public void testRemoveTeam() throws Exception {
        String teamName = "team1";
        teamManager.createTeam(teamName);

        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
            teamManager.findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }

        teamManager.removeTeam(teamName);

        teamManager = new TeamManager(homeDir);
        try {
            teamManager.findTeam(teamName);
            fail("Team must not exist");
        } catch (TeamNotFoundException ex) {
            // Correct. There should not be a team
        }
    }

    /**
     * Test of findUserTeam method, of class TeamManager.
     */
    @Test
    public void testFindUserTeam() throws IOException, TeamManager.TeamAlreadyExistsException {
        String teamName = "team1";
        teamManager.createTeam(teamName);

        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
            teamManager.findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }

        Team team = teamManager.findUserTeam("chris");
        Assert.assertTrue(teamName.equals(team.getName()));
    }

    /**
     * Test of addJobToUserTeam method, of class TeamManager.
     */
    @Test
    public void testAddJobToUserTeam() throws Exception {
        String teamName = "team1";
        teamManager.createTeam(teamName);

        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
            teamManager.findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }

        teamManager.addJobToUserTeam("chris", "job1");
        Team team = teamManager.findTeam(teamName);
        Assert.assertTrue(team.isJobOwner("job1"));
    }

    /**
     * Test of findJobOwnerTeam method, of class TeamManager.
     */
    @Test
    public void testFindJobOwnerTeam() throws IOException, TeamManager.TeamAlreadyExistsException, TeamNotFoundException {
        String teamName = "team1";
        teamManager.createTeam(teamName);

        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
            teamManager.findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }

        teamManager.addJobToUserTeam("chris", "job1");
        Team team = teamManager.findJobOwnerTeam("job1");
        Assert.assertTrue(teamName.equals(team.getName()));
    }

    /**
     * Test of addJob method, of class TeamManager.
     */
    @Test
    public void testAddJob() throws Exception {
        String teamName = "team1";
        teamManager.createTeam(teamName);

        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
            teamManager.findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
        Team team = teamManager.findTeam(teamName);
        teamManager.addJob(team, "job1");
        Team jobTeam = teamManager.findJobOwnerTeam("job1");
        Assert.assertTrue(teamName.equals(jobTeam.getName()));
    }

    /**
     * Test of removeJob method, of class TeamManager.
     */
    @Test
    public void testRemoveJob() throws Exception {
        String teamName = "team1";
        teamManager.createTeam(teamName);

        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
            teamManager.findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
        teamManager.addJobToUserTeam("chris", "job1");
        Team team = teamManager.findTeam(teamName);
        teamManager.removeJob(team, teamName);
        Assert.assertFalse(team.isJobOwner(teamName));
    }

    /**
     * Test of removeJobFromUserTeam method, of class TeamManager.
     */
    @Test
    public void testRemoveJobFromUserTeam() throws Exception {
        String teamName = "team1";
        teamManager.createTeam(teamName);

        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
            teamManager.findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
        Team team = teamManager.findTeam(teamName);
        teamManager.addJob(team, "job1");
        teamManager.removeJobFromUserTeam("paul", "job1");
        Assert.assertFalse(team.isJobOwner("job1"));
    }

    /**
     * Test of renameJobInUserTeam method, of class TeamManager.
     */
    @Test
    public void testRenameJobInUserTeam() throws Exception {
        String teamName = "team1";
        teamManager.createTeam(teamName);

        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
            Team team = teamManager.findTeam(teamName);
            team.addJob(new TeamJob("job1"));
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
        teamManager.renameJobInUserTeam("paul", "job1", "job2");
        Team team = teamManager.findTeam(teamName);
        Assert.assertFalse(" team1 must not be a owner of job1", team.isJobOwner("job1"));
        Assert.assertTrue(" team1 must be a job owner of job2", team.isJobOwner("job2"));
    }

    /**
     * Test of renameJob method, of class TeamManager.
     */
    @Test
    public void testRenameJob() throws Exception {
        String teamName = "team1";
        teamManager.createTeam(teamName);

        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
            Team team = teamManager.findTeam(teamName);
            team.addJob(new TeamJob("job1"));
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
        Team team = teamManager.findTeam(teamName);
        teamManager.renameJob(team, "job1", "job2");
        Team jobTeam = teamManager.findJobOwnerTeam("job2");
        Assert.assertFalse(team.isJobOwner("job1"));
        Assert.assertTrue(teamName.equals(jobTeam.getName()));
    }

    /**
     * Test of save method, of class TeamManager.
     */
    @Test
    public void testSave() throws Exception {
        String teamName = "team1";
        teamManager.createTeam(teamName);
        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
        // Save is called automatically
        Assert.assertTrue(teamsStore.exists());
        // This re-reads from persistence
        teamManager = new TeamManager(homeDir);
        try {
            teamManager.findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
    }

    /**
     * Test of load method, of class TeamManager.
     */
    @Test
    public void testLoad() throws IOException, TeamManager.TeamAlreadyExistsException {
        String teamName = "team1";
        teamManager.createTeam(teamName);
        try {
            teamManager.addUser(teamName, "chris");
            teamManager.addUser(teamName, "paul");
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
        // Save is called automatically
        Assert.assertTrue(teamsStore.exists());
        // This re-reads from persistence
        teamManager = new TeamManager(homeDir);
        // load() must be called automatically
        try {
            teamManager.findTeam(teamName);
        } catch (TeamNotFoundException ex) {
            fail("Team must exist");
        }
    }

    @Test
    public void testTeamPersistence() throws IOException, TeamManager.TeamAlreadyExistsException, TeamNotFoundException {

        teamManager.setUseBulkSaveFlag(false);
        teamManager.addSysAdmin("admin1");
        teamManager.addSysAdmin("Paul");
        Team team = teamManager.createTeam("Team1", "Team1 Description");
        TeamMember member = new TeamMember();
        member.setName("member1");
        member.setAsTeamAdmin(true);
        member.addPermission(Permission.DELETE);
        team.addMember(member);
        team.addJob(new TeamJob("job1"));

        teamManager = new TeamManager(homeDir);
        teamManager.setUseBulkSaveFlag(false);
        if (!teamManager.isSysAdmin("admin1")) {
            fail("SysAdmin admin1 must exists");
        }
        team = teamManager.findTeam("Team1");
        if (team == null) {
            fail("The team Team1 must exists");
        } else {
            if (!"Team1 Description".equals(team.getDescription())) {
                fail("The team Team1 must have description 'Team1 Description' ");
            }
            if (!team.isJobOwner("job1")) {
                fail("The team Team1 must b ethe owner of job1");
            }
            member = team.findMember("member1");
            if (member == null) {
                fail("The member member1 must exists in Team1 ");
            } else {
                if (!member.canDelete()) {
                    fail("The member member1 must have the permission delete");
                }
            }
        }
    }
}
