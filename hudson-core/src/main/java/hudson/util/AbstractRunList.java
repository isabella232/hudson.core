/*
 * Copyright (c) 2013 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */
package hudson.util;

import hudson.model.Node;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author rovarghe
 */
public abstract class AbstractRunList<R> extends ArrayList<R> {

    public AbstractRunList() {
    }

    public AbstractRunList(Collection<? extends R> c) {
        super(c);
    }

    /**
     * Filter the list by timestamp.
     *
     * {@code s&lt=;e}.
     */
    public abstract AbstractRunList<R> byTimestamp(long start, long end);

    /**
     * Filter the list to non-successful builds only.
     */
    public abstract AbstractRunList<R> failureOnly();

    public abstract R getFirstBuild();

    public abstract R getLastBuild();

    /**
     * Reduce the size of the list by only leaving relatively new ones. This
     * also removes on-going builds, as RSS cannot be used to publish
     * information if it changes.
     */
    public abstract AbstractRunList<R> newBuilds();

    /**
     * Filter the list to builds on a single node only
     */
    public abstract AbstractRunList<R> node(Node node);

    /**
     * Filter the list to regression builds only.
     */
    public abstract AbstractRunList<R> regressionOnly();
    
}
