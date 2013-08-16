/*******************************************************************************
 *
 * Copyright (c) 2004-2010, Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 *
 *
 *******************************************************************************/ 

package hudson.cli;

import hudson.model.Hudson;
import hudson.Extension;
import static hudson.cli.UpdateJobCommand.ensureJobInTeam;
import static hudson.cli.UpdateJobCommand.validateTeam;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.eclipse.hudson.security.team.Team;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Creates a new job by reading stdin as a configuration XML file.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class CreateJobCommand extends CLICommand {

    @Override
    public String getShortDescription() {
        return "Creates a new job by reading stdin or file as a configuration XML file";
    }
    @Argument(metaVar = "NAME", usage = "Name of the job to create. The job name should not be team  qualified. Ex: job1.", required = true)
    public String name;
    @Argument(metaVar = "TEAM", usage = "Team to create the job in. Optional.", index = 1, required = false)
    public String team;
    @Option(name = "-file", usage = "Read config.xml from file rather than standard input.")
    public String file;

    protected int run() throws Exception {
        Hudson h = Hudson.getInstance();
        h.checkPermission(Item.CREATE);
        Team targetTeam = validateTeam(team, true, stderr);

        if (team != null && targetTeam == null) {
            return -1;
        }

        if (h.getItem(name) != null) {
            stderr.println("Job '" + name + "' already exists");
            return -1;
        }
        
        InputStream xml = stdin;
        if (file != null) {
            File inputFile = new File(file);
            if (!inputFile.exists()) {
                stderr.println("File '" + file + "' does not exist");
                return -1;
            }
            if (!inputFile.isFile()) {
                stderr.println("File '" + file + "' is not a file");
                return -1;
            }
            try {
                xml = new FileInputStream(inputFile);
            } catch (FileNotFoundException e) {
                stderr.println("File '" + file + "' not found");
                return -1;
            }
        }
        
        TopLevelItem newItem = h.createProjectFromXML(name, xml);
        newItem = h.getItem(newItem.getName());
        ensureJobInTeam(newItem, targetTeam, name, stderr);
        return 0;
    }
}
