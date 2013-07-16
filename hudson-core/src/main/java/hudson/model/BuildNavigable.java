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

/**
 * A build that offloads the traversal of builds to an external
 * navigator object. 
 * 
 * <p>This is so that the build does not need to hold references
 * to other builds and garbage collection of build objects can be centrally 
 * managed by the navigator.
 * </p>
 * 
 * @author Roy Varghese
 */
public interface BuildNavigable {
    
    BuildNavigator getBuildNavigator();
    
    void setBuildNavigator(BuildNavigator buildNavigator);
    
}
