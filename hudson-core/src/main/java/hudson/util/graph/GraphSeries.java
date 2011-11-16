/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.util.graph;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Data and definition of a chart GraphSeries  
 * @author winstonp
 */
public class GraphSeries<E> {

    private Color color;
    private String caption;
    //Base URL to construct full URL for data Click on Graph
    private String baseURL;
    private List<E> data = new ArrayList<E>();
    // Should the label displayed inside the data point display area
    private boolean labelInside = false;

    public GraphSeries(String label) {
        this.caption = label;
    }
    
    public GraphSeries(String label, Color color) {
        this.caption = label;
        this.color = color;
    }
    
    public GraphSeries(String label, Color color, boolean labelInside) {
        this.caption = label;
        this.color = color;
        this.labelInside = labelInside;
    }

    public List<E> getData() {
        return data;
    }

    public void setData(List<E> data) {
        this.data = data;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String label) {
        this.caption = label;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void add(E element) {
        data.add(element);
    }
    
    public boolean isLabelInside() {
        return labelInside;
    }

    public void setLabelInside(boolean labelInside) {
        this.labelInside = labelInside;
    }
}
