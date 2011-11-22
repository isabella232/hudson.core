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
 *    Anton Kozak
 *
 *******************************************************************************/
package hudson.model;

import org.eclipse.hudson.api.model.IJob;

/**
 * Represents {@link Result} property.
 * <p/>
 * Date: 9/23/11
 *
 * @author Anton Kozak
 */
//TODO try to replace it with generics
public class ResultProjectProperty extends BaseProjectProperty<Result> {

    public ResultProjectProperty(IJob job) {
        super(job);
    }
}