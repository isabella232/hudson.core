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
 *    Kohsuke Kawaguchi, Tom Huybrechts
 *
 *
 *******************************************************************************/ 

package hudson.model;

import hudson.Util;
import static hudson.Util.fixNull;

import hudson.model.labels.LabelAtom;
import hudson.model.labels.LabelExpression;
import hudson.model.labels.LabelExpressionLexer;
import hudson.model.labels.LabelExpressionParser;
import hudson.model.labels.LabelOperatorPrecedence;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.Cloud;
import hudson.util.QuotedStringTokenizer;
import hudson.util.VariableResolver;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.TreeSet;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import hudson.matrix.Axis;
import hudson.matrix.MatrixProject;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

/**
 * Group of {@link Node}s.
 *
 * @author Kohsuke Kawaguchi
 * @see Hudson#getLabels()
 * @see Hudson#getLabel(String)
 */
@ExportedBean
public abstract class Label extends Actionable implements Comparable<Label>, ModelObject {

    /**
     * Display name of this label.
     */
    protected transient final String name;
    private transient volatile Set<Node> nodes;
    private transient volatile Set<Cloud> clouds;
    @Exported
    public transient final LoadStatistics loadStatistics;
    public transient final NodeProvisioner nodeProvisioner;

    public Label(String name) {
        this.name = name;
        // passing these causes an infinite loop - getTotalExecutors(),getBusyExecutors());
        this.loadStatistics = new LoadStatistics(0, 0) {
            @Override
            public int computeIdleExecutors() {
                return Label.this.getIdleExecutors();
            }

            @Override
            public int computeTotalExecutors() {
                return Label.this.getTotalExecutors();
            }

            @Override
            public int computeQueueLength() {
                return Hudson.getInstance().getQueue().countBuildableItemsFor(Label.this);
            }
        };
        this.nodeProvisioner = new NodeProvisioner(this, loadStatistics);
    }

    /**
     * Alias for {@link #getDisplayName()}.
     */
    @Exported
    public final String getName() {
        return getDisplayName();
    }

    /**
     * Returns a human-readable text that represents this label.
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * Returns a label expression that represents this label.
     */
    public abstract String getExpression();

    /**
     * Relative URL from the context path, that ends with '/'.
     */
    public String getUrl() {
        return "label/" + name + '/';
    }

    public String getSearchUrl() {
        return getUrl();
    }

    /**
     * Evaluates whether the label expression is true given the specified value
     * assignment. IOW, returns true if the assignment provided by the resolver
     * matches this label expression.
     */
    public abstract boolean matches(VariableResolver<Boolean> resolver);

    /**
     * Evaluates whether the label expression is true when an entity owns the
     * given set of {@link LabelAtom}s.
     */
    public final boolean matches(final Collection<LabelAtom> labels) {
        return matches(new VariableResolver<Boolean>() {
            public Boolean resolve(String name) {
                for (LabelAtom a : labels) {
                    if (a.getName().equals(name)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public final boolean matches(Node n) {
        return matches(n.getAssignedLabels());
    }

    /**
     * Returns true if this label is a "self label", which means the label is
     * the name of a {@link Node}.
     */
    public boolean isSelfLabel() {
        Set<Node> nodes = getNodes();
        return nodes.size() == 1 && nodes.iterator().next().getSelfLabel() == this;
    }

    /**
     * Gets all {@link Node}s that belong to this label.
     */
    @Exported
    public synchronized Set<Node> getNodes() {
        Set<Node> nodes = this.nodes;
        if (nodes != null) {
            return nodes;
        }

        Set<Node> r = new HashSet<Node>();
        Hudson h = Hudson.getInstance();
        if (this.matches(h)) {
            r.add(h);
        }
        for (Node n : h.getNodes()) {
            if (this.matches(n)) {
                r.add(n);
            }
        }
        return this.nodes = Collections.unmodifiableSet(r);
    }

    /**
     * Gets all {@link Cloud}s that can launch for this label.
     */
    @Exported
    public synchronized Set<Cloud> getClouds() {
        if (clouds == null) {
            Set<Cloud> r = new HashSet<Cloud>();
            Hudson h = Hudson.getInstance();
            for (Cloud c : h.clouds) {
                if (c.canProvision(this)) {
                    r.add(c);
                }
            }
            clouds = Collections.unmodifiableSet(r);
        }
        return clouds;
    }

    /**
     * Can jobs be assigned to this label? <p> The answer is yes if there is a
     * reasonable basis to believe that Hudson can have an executor under this
     * label, given the current configuration. This includes situations such as
     * (1) there are offline slaves that have this label (2) clouds exist that
     * can provision slaves that have this label.
     */
    public boolean isAssignable() {
        for (Node n : getNodes()) {
            if (n.getNumExecutors() > 0) {
                return true;
            }
        }
        return !getClouds().isEmpty();
    }

    /**
     * Number of total {@link Executor}s that belong to this label. <p> This
     * includes executors that belong to offline nodes, so the result can be
     * thought of as a potential capacity, whereas {@link #getTotalExecutors()}
     * is the currently functioning total number of executors. <p> This method
     * doesn't take the dynamically allocatable nodes (via {@link Cloud}) into
     * account. If you just want to test if there's some executors, use
     * {@link #isAssignable()}.
     */
    public int getTotalConfiguredExecutors() {
        int r = 0;
        for (Node n : getNodes()) {
            r += n.getNumExecutors();
        }
        return r;
    }

    /**
     * Number of total {@link Executor}s that belong to this label that are
     * functioning. <p> This excludes executors that belong to offline nodes.
     */
    @Exported
    public int getTotalExecutors() {
        int r = 0;
        for (Node n : getNodes()) {
            Computer c = n.toComputer();
            if (c != null && c.isOnline()) {
                r += c.countExecutors();
            }
        }
        return r;
    }

    /**
     * Number of busy {@link Executor}s that are carrying out some work right
     * now.
     */
    @Exported
    public int getBusyExecutors() {
        int r = 0;
        for (Node n : getNodes()) {
            Computer c = n.toComputer();
            if (c != null && c.isOnline()) {
                r += c.countBusy();
            }
        }
        return r;
    }

    /**
     * Number of idle {@link Executor}s that can start working immediately.
     */
    @Exported
    public int getIdleExecutors() {
        int r = 0;
        for (Node n : getNodes()) {
            Computer c = n.toComputer();
            if (c != null && (c.isOnline() || c.isConnecting())) {
                r += c.countIdle();
            }
        }
        return r;
    }

    /**
     * Returns true if all the nodes of this label is offline.
     */
    @Exported
    public boolean isOffline() {
        for (Node n : getNodes()) {
            if (n.toComputer() != null && !n.toComputer().isOffline()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a human readable text that explains this label.
     */
    @Exported
    public String getDescription() {
        Set<Node> nodes = getNodes();
        if (nodes.isEmpty()) {
            Set<Cloud> clouds = getClouds();
            if (clouds.isEmpty()) {
                return Messages.Label_InvalidLabel();
            }

            return Messages.Label_ProvisionedFrom(toString(clouds));
        }

        if (nodes.size() == 1) {
            return nodes.iterator().next().getNodeDescription();
        }

        return Messages.Label_GroupOf(toString(nodes));
    }

    private String toString(Collection<? extends ModelObject> model) {
        boolean first = true;
        StringBuilder buf = new StringBuilder();
        for (ModelObject c : model) {
            if (buf.length() > 80) {
                buf.append(",...");
                break;
            }
            if (!first) {
                buf.append(',');
            } else {
                first = false;
            }
            buf.append(c.getDisplayName());
        }
        return buf.toString();
    }

    /**
     * Returns projects that are tied on this node.
     */
    @Exported
    public List<AbstractProject> getTiedJobs() {
        List<AbstractProject> r = new ArrayList<AbstractProject>();
        for (AbstractProject p : Util.filter(Hudson.getInstance().getItems(), AbstractProject.class)) {
            if (this.equals(p.getAssignedLabel())) {
                r.add(p);
            }
        }
        for (MatrixProject p : Util.filter(Hudson.getInstance().getItems(), MatrixProject.class)) {
            for (Axis axis : p.getAxes()) {
                if (axis.getValues().contains(getName())){
                    r.add(p);
                }
                 
            }
        }
        return r;
    }

    public boolean contains(Node node) {
        return getNodes().contains(node);
    }

    /**
     * If there's no such label defined in {@link Node} or {@link Cloud}. This
     * is usually used as a signal that this label is invalid.
     */
    public boolean isEmpty() {
        return getNodes().isEmpty() && getClouds().isEmpty();
    }

    /*package*/ void reset() {
        nodes = null;
        clouds = null;
    }

    /**
     * Expose this object to the remote API.
     */
    public Api getApi() {
        return new Api(this);
    }

    /**
     * Returns the label that represents "this&amp;rhs"
     */
    public Label and(Label rhs) {
        return new LabelExpression.And(this, rhs);
    }

    /**
     * Returns the label that represents "this|rhs"
     */
    public Label or(Label rhs) {
        return new LabelExpression.Or(this, rhs);
    }

    /**
     * Returns the label that represents "this&lt;->rhs"
     */
    public Label iff(Label rhs) {
        return new LabelExpression.Iff(this, rhs);
    }

    /**
     * Returns the label that represents "this->rhs"
     */
    public Label implies(Label rhs) {
        return new LabelExpression.Implies(this, rhs);
    }

    /**
     * Returns the label that represents "!this"
     */
    public Label not() {
        return new LabelExpression.Not(this);
    }

    /**
     * Returns the label that represents "(this)" This is a pointless operation
     * for machines, but useful for humans who find the additional parenthesis
     * often useful
     */
    public Label paren() {
        return new LabelExpression.Paren(this);
    }

    /**
     * Precedence of the top most operator.
     */
    public abstract LabelOperatorPrecedence precedence();

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }

        return name.equals(((Label) that).name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public int compareTo(Label that) {
        return this.name.compareTo(that.name);
    }

    @Override
    public String toString() {
        return name;
    }

    public static final class ConverterImpl implements Converter {

        public ConverterImpl() {
        }

        public boolean canConvert(Class type) {
            return Label.class.isAssignableFrom(type);
        }

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            Label src = (Label) source;
            writer.setValue(src.getExpression());
        }

        public Object unmarshal(HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return Hudson.getInstance().getLabel(reader.getValue());
        }
    }

    /**
     * Convers a whitespace-separate list of tokens into a set of
     * {@link Label}s.
     *
     * @param labels Strings like "abc def ghi". Can be empty or null.
     * @return Can be empty but never null. A new writable set is always
     * returned, so that the caller can add more to the set.
     * @since 1.308
     */
    public static Set<LabelAtom> parse(String labels) {
        Set<LabelAtom> r = new TreeSet<LabelAtom>();
        labels = fixNull(labels);
        if (labels.length() > 0) {
            for (String l : new QuotedStringTokenizer(labels).toArray()) {
                r.add(Hudson.getInstance().getLabelAtom(l));
            }
        }
        return r;
    }

    /**
     * Obtains a label by its {@linkplain #getName() name}.
     */
    public static Label get(String l) {
        return Hudson.getInstance().getLabel(l);
    }

    /**
     * Parses the expression into a label expression tree.
     *
     * TODO: replace this with a real parser later
     */
    public static Label parseExpression(String labelExpression) throws RecognitionException {
        LabelExpressionLexer lexer = new LabelExpressionLexer(new ANTLRStringStream(labelExpression));
        return new LabelExpressionParser(new CommonTokenStream(lexer)).expr();
    }
}
