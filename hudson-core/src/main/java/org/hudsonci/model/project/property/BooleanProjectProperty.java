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
package org.hudsonci.model.project.property;

import org.hudsonci.api.model.ICascadingJob;

/**
 * Represents boolean property.
 * <p/>
 * Date: 9/22/11
 *
 * @author Nikita Levyankov
 */
public class BooleanProjectProperty extends BaseProjectProperty<Boolean> {

    public BooleanProjectProperty(ICascadingJob job) {
        super(job);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getOriginalValue() {
        Boolean originalValue = super.getOriginalValue();
        return null != originalValue ? originalValue : getDefaultValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getDefaultValue() {
        return false;
    }
}
