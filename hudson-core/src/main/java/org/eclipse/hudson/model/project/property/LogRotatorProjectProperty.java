/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov
 *
 *******************************************************************************/
package org.eclipse.hudson.model.project.property;

import hudson.tasks.LogRotator;
import org.eclipse.hudson.api.model.IJob;

/**
 * Represents LogRotator property.
 * <p/>
 * Date: 9/27/11
 *
 * @author Nikita Levyankov
 */
public class LogRotatorProjectProperty extends BaseProjectProperty<LogRotator> {
    public LogRotatorProjectProperty(IJob job) {
        super(job);
    }
}