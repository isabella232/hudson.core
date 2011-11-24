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

package org.eclipse.hudson.api.model.project.property;

import org.eclipse.hudson.api.model.IJob;

/**
 * Property for triggers in case of we should use child project trigger
 * instead of parent project if they are equals.
 */
public class TriggerProjectProperty<T> extends ExternalProjectProperty<T> {
    public TriggerProjectProperty(IJob job) {
        super(job);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void clearOriginalValue(T originalValue) {
        setOriginalValue(originalValue, false);
    }
}
