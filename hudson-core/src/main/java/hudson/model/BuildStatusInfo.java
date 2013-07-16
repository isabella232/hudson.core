/*******************************************************************************
 *
 * Copyright (c) 2013 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Roy Varghese
 *
 *******************************************************************************/
package hudson.model;

import java.util.List;

/**
 * Track status of all builds related to a Project.
 * 
 * @author Roy Varghese
 */
public interface BuildStatusInfo<J extends Job<J,R>,R extends Run<J,R>> {
    
    public R getLastBuild();

    public R getFirstBuild();

    public R getLastSuccessfulBuild();

    public R getLastUnsuccessfulBuild();

    public R getLastUnstableBuild();

    public R getLastStableBuild();

    public R getLastFailedBuild();

    public R getLastCompletedBuild();

    public List<R> getLastBuildsOverThreshold(int numberOfBuilds, Result threshold);

}
