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

    public static final int TYPE_AREA = 1;
    public static final int TYPE_BAR = 2;
    public static final int TYPE_LINE = 3;
    
    private Color color;
    
    /** Caption for the series to display in the legend */
    private String caption;
    
    /** Base URL to construct full URL for data Click on Graph */
    private String baseURL;
    
    /** The series data */
    private List<E> data = new ArrayList<E>();
    
    /** Should the label displayed inside the data point display area useful in Bar, Area */
    private boolean labelInside = true;
    
    /** How this series should be displayed - BAR, AREA, LINE etc */
    private int type = TYPE_BAR;
    
    /** Should the value label be displayed */
    private boolean valueLabelDisplayed = true;
    
    /** Should this chart series be stacked over previous series*/
    private boolean stacked = true;
    
    public GraphSeries(String caption){
        this.caption = caption;
    }
    
    public GraphSeries(int type, String caption, Color color){
        this(type, caption, color, true, true);
    }
    
    public GraphSeries(int type, String caption, Color color, boolean valueLabelDisplayed) {
        this(type, caption, color, valueLabelDisplayed, true);
    }
    
    public GraphSeries(int type, String caption, Color color, boolean valueLabelDisplayed, boolean labelInside) {
        this.type = type; 
        this.caption = caption;
        this.color = color;
        this.valueLabelDisplayed = valueLabelDisplayed;
        this.labelInside = labelInside;
    }
    
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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
    
     public boolean isStacked() {
        return stacked;
    }

    public void setStacked(boolean stacked) {
        this.stacked = stacked;
    }
    
    public boolean isLabelInside() {
        return labelInside;
    }

    public void setLabelInside(boolean labelInside) {
        this.labelInside = labelInside;
    }
    
    public boolean isValueLabelDisplayed() {
        return valueLabelDisplayed;
    }

    public void setValueLabelDisplayed(boolean valueLabelDisplayed) {
        this.valueLabelDisplayed = valueLabelDisplayed;
    }
}
