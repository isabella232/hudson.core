/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     
 *
 *******************************************************************************/ 

package hudson.maven;

import java.io.File;
import java.io.IOException;

/**
 * Exists solely for backward compatibility
 * 
 * @author Winston Prakash
 * @see org.eclipse.hudson.legacy.maven.plugin.MavenModuleSetBuild
 */
public class MavenModuleSetBuild extends org.eclipse.hudson.legacy.maven.plugin.MavenModuleSetBuild {

	public MavenModuleSetBuild(MavenModuleSet job) throws IOException {
		super(job);
	}

	public MavenModuleSetBuild(MavenModuleSet project, File buildDir)
			throws IOException {
		super(project, buildDir);
	}

}
