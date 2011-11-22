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
package hudson.model;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Represents integer property.
 * <p/>
 * Date: 9/22/11
 *
 * @author Nikita Levyankov
 */
public class IntegerProjectProperty extends BaseProjectProperty<Integer> {
    /**
     * {@inheritDoc}
     */
    protected boolean allowOverrideValue(Integer cascadingValue, Integer candidateValue) {
        return !ObjectUtils.equals(cascadingValue, candidateValue);
    }
}
