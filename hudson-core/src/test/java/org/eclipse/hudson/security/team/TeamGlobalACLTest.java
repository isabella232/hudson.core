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
import hudson.security.ACL;
import hudson.security.Permission;
import java.io.File;
import java.io.IOException;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.acls.sid.Sid;

/**
 * Test class for TeamBasedACL
 *
 * @author Winston Prakash
 */
public class TeamGlobalACLTest {

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
    public void testGlobalSysAdminPermission() throws IOException {
        //Paul should not get global configure permission before adding as Sysadmin
        Sid sid = new PrincipalSid("Paul");
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.GLOBAL);
        Assert.assertNull("Paul should not have global CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission));

        //Now Paul should get global configure permission since added as Sysadmin
        teamManager.addSysAdmin("Paul");
        Assert.assertTrue("Paul should have global CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission).booleanValue());

    }

    @Test
    public void testGlobalNonSysAdminPermission() {
        //Chris, a non SysAdmin, should not get global create permission 
        Sid sid = new PrincipalSid("Chris");
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.GLOBAL);
        Assert.assertNull("Chris should not have global CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission));

        //But Chris should get READ permission
        Assert.assertTrue("Chris should have global READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

    }

    @Test
    public void testGlobalAnonymousPermission() {
        //Anonymous, should not get global create permission 
        Sid sid = ACL.ANONYMOUS;
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.GLOBAL);
        Assert.assertNull("Anonymous should not have global CONFIGURE permission", teamBasedACL.hasPermission(sid, configurePermission));

        //Anonymous should get READ permission
        Assert.assertTrue("Anonymous should have global READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

    }

    @Test
    public void testGlobalEveryonePermission() {
        //Evru one, should not get global create permission 
        Sid sid = ACL.EVERYONE;
        TeamBasedACL teamBasedACL = new TeamBasedACL(teamManager, TeamBasedACL.SCOPE.GLOBAL);
        Assert.assertNull("Every one should not have global CREATE permission", teamBasedACL.hasPermission(sid, configurePermission));

        //Every one should get READ permission
        Assert.assertTrue("Every one should have global READ permission", teamBasedACL.hasPermission(sid, readPermission).booleanValue());

    }
}