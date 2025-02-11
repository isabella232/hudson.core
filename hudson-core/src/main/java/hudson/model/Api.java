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
 *    Kohsuke Kawaguchi, Seiji Sogabe
 *
 *
 *******************************************************************************/ 

package hudson.model;

import hudson.util.IOException2;
import org.dom4j.CharacterData;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hudsonci.xpath.XPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import org.hudsonci.xpath.XFunctionFilter;
import org.hudsonci.xpath.XPathException;

/**
 * Used to expose remote access API for ".../api/"
 *
 * <p> If the parent object has a <tt>_api.jelly</tt> view, it will be included
 * in the api index page.
 *
 * @author Kohsuke Kawaguchi
 * @see Exported
 */
public class Api extends AbstractModelObject {

    /**
     * Model object to be exposed as XML/JSON/etc.
     */
    //TODO: review and check whether we can do it private
    public final Object bean;
    
    private static class ApiFunctionFilter implements XFunctionFilter {
        public boolean accept(String namespaceURI, String prefix, String localName) {
            if ("document".equals(localName)) {
                return false;
            }
            return true;
        }
    }

    public Api(Object bean) {
        this.bean = bean;
    }

    public String getDisplayName() {
        return "API";
    }

    public String getSearchUrl() {
        return "api";
    }

    public Object getBean() {
        return bean;
    }

    /**
     * Exposes the bean as XML.
     */
    public void doXml(StaplerRequest req, StaplerResponse rsp,
            @QueryParameter String xpath,
            @QueryParameter String wrapper,
            @QueryParameter int depth) throws IOException, ServletException {
        String[] excludes = req.getParameterValues("exclude");

        if (xpath == null && excludes == null) {
            // serve the whole thing
            rsp.serveExposedBean(req, bean, Flavor.XML);
            return;
        }

        StringWriter sw = new StringWriter();

        // first write to String
        Model p = MODEL_BUILDER.get(bean.getClass());
        p.writeTo(bean, depth, Flavor.XML.createDataWriter(bean, sw));
        
        // apply XPath
        Object result;
        try {
            Document dom = new SAXReader().read(new StringReader(sw.toString()));

            // apply exclusions
            if (excludes != null) {
                for (String exclude : excludes) {
                    XPath ex = new XPath(exclude);
                    ex.setFunctionFilter(new ApiFunctionFilter());
                    List<org.dom4j.Node> list = (List<org.dom4j.Node>) ex.selectNodes(dom);
                    for (org.dom4j.Node n : list) {
                        Element parent = n.getParent();
                        if (parent != null) {
                            parent.remove(n);
                        }
                    }
                }
            }

            if (xpath == null) {
                result = dom;
            } else {
                XPath ex = new XPath(xpath);
                ex.setFunctionFilter(new ApiFunctionFilter());
                List list = ex.selectNodes(dom);
                if (wrapper != null) {
                    Element root = DocumentFactory.getInstance().createElement(wrapper);
                    for (Object o : list) {
                        if (o instanceof String) {
                            root.addText(o.toString());
                        } else {
                            root.add(((org.dom4j.Node) o).detach());
                        }
                    }
                    result = root;
                } else if (list.isEmpty()) {
                    rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    rsp.getWriter().print(Messages.Api_NoXPathMatch(xpath));
                    return;
                } else if (list.size() > 1) {
                    rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    rsp.getWriter().print(Messages.Api_MultipleMatch(xpath, list.size()));
                    return;
                } else {
                    result = list.get(0);
                }
            }

        } catch (XPathException e) {
            throw new IOException2(e);
        } catch (DocumentException e) {
            throw new IOException2(e);
        }

        OutputStream o = rsp.getCompressedOutputStream(req);
        try {
            if (result instanceof CharacterData) {
                rsp.setContentType("text/plain;charset=UTF-8");
                o.write(((CharacterData) result).getText().getBytes("UTF-8"));
                return;
            }

            if (result instanceof String || result instanceof Number || result instanceof Boolean) {
                rsp.setContentType("text/plain;charset=UTF-8");
                o.write(result.toString().getBytes("UTF-8"));
                return;
            }

            // otherwise XML
            rsp.setContentType("application/xml;charset=UTF-8");
            new XMLWriter(o).write(result);
        } finally {
            o.close();
        }
    }

    /**
     * Generate schema.
     */
    public void doSchema(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        rsp.setContentType("application/xml");
        StreamResult r = new StreamResult(rsp.getOutputStream());
        new SchemaGenerator(new ModelBuilder().get(bean.getClass())).generateSchema(r);
        r.getOutputStream().close();
    }

    /**
     * Exposes the bean as JSON.
     */
    public void doJson(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        rsp.serveExposedBean(req, bean, Flavor.JSON);
    }
    private static final ModelBuilder MODEL_BUILDER = new ModelBuilder();
}
