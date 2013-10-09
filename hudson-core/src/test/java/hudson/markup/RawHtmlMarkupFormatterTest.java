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

package hudson.markup;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Bob Foster
 */
public class RawHtmlMarkupFormatterTest {
    
    private String format(String in) throws IOException {
        RawHtmlMarkupFormatter formatter = new RawHtmlMarkupFormatter();
        StringWriter writer = new StringWriter();
        formatter.translate(in, writer);
        return writer.toString();
    }

    @Test
    public void testTextIsOk() throws Exception {
        String in = "this is some text";
        String out = format(in);
        assertTrue(in.equals(out));
    }

    @Test
    public void testPIsBalanced() throws Exception {
        // The translation balances all HTML tags
        String in = "<p>this is some text";
        String expect = "<p>this is some text</p>";
        String out = format(in);
        assertTrue(expect.equals(out));
    }
    
    @Test
    public void testPIsCanonical() throws Exception {
        // The translation converts all HTML elements to lowercase
        String in = "<P>this is some text";
        String expect = "<p>this is some text</p>";
        String out = format(in);
        assertTrue(expect.equals(out));
    }

    @Test
    public void scriptsAreRemoved() throws Exception {
        String in = "foo<script>alert('Hi!')</script>bar";
        String expect = "foobar";
        String out = format(in);
        assertTrue(expect.equals(out));
    }

    @Test
    public void scriptsUppercaseAreRemoved() throws Exception {
        String in = "foo<SCRIPT>alert('Hi!')</SCRIPT>bar";
        String expect = "foobar";
        String out = format(in);
        assertTrue(expect.equals(out));
    }

    @Test
    public void tdOutsideTableIsRemoved() throws Exception {
        // NB: Input following disallowed element is swallowed until ended.
        String in = "foo<td>bar";
        String expect = "foo";
        String out = format(in);
        assertTrue(expect.equals(out));
    }
    
    @Test
    public void tdOutsideTableBalanced() throws Exception {
        // NB: Input following disallowed element is swallowed until ended.
        String in = "foo<td>bar</td>baz";
        String expect = "foobaz";
        String out = format(in);
        assertTrue(expect.equals(out));
    }
    
    private static final String[] DISALLOWED = new String[] {
        "th", "td"
    };

    @Test
    public void disallowedTableElementsAreRemoved() throws Exception {
        String expect = "foobaz";
        for (String disallow : DISALLOWED) {
            String in = "foo<"+disallow+">bar</"+disallow+">baz";
            String out = format(in);
            assertTrue("<"+disallow+"> not removed: '"+out+"'", expect.equals(out));
        }
    }
    
    @Test
    public void trTooMuchRemoved() throws Exception {
        // A bug in TagBalancingHtmlStreamEventReceiver inserts a <td> after
        // <tr> but does not terminate the <td> when a </tr> is seen
        // and thus swallows the entire remainder of the string.
        // This bug is ignored since the effect is to remove the offending element(s)
        String in = "foo<tr>bar</tr>baz";
        String out = format(in);
        String expect = "foo";
        assertTrue("<tr> not removed", expect.equals(out));
    }
    
    private static final String[] DISALLOWED_SECTIONS = new String[] {
        "thead", "tfoot", "tbody", "caption", "colgroup"
    };

    @Test
    public void theadEtcTooLittleRemoved() throws Exception {
        // A bug in TagBalancingHtmlStreamEventReceiver inserts a </thead> etc.
        // immediately after <thead> etc. A later </thead> is ignored.
        // Hence, the <thead> is removed, but some inner text, etc.
        // may be retained.
        // This bug is ignored since the effect is to remove the offending element(s)
        String expect = "foobarbaz";
        for (String disallow : DISALLOWED_SECTIONS) {
            String in = "foo<"+disallow+">bar</"+disallow+">baz";
            String out = format(in);
            assertTrue("<"+disallow+"> not removed: '"+out+"'", expect.equals(out));
        }
    }

    @Test
    public void tableIsAllowed() throws Exception {
        String in = "<table><tr><th>Foo</th><th>Bar</th></tr><tr><td>foo</td><td>bar</td></tr></table>";
        String out = format(in);
        String expect = in;
        assertTrue(expect.equals(out));
    }

    @Test
    public void tableIsAllowed2() throws Exception {
        // Table is normalized and balanced
        String in = "<TABLE><tr><th>Foo<TH>Bar<tr><td>foo<TD>bar</TABLE>";
        String expect = "<table><tr><th>Foo</th><th>Bar</th></tr><tr><td>foo</td><td>bar</td></tr></table>";
        String out = format(in);
        assertTrue(expect.equals(out));
    }
}
