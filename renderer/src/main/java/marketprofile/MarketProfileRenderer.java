package marketprofile;

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.HighLowItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;

public class MarketProfileRenderer extends AbstractXYItemRenderer implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {
    private static final long serialVersionUID = -5957948865988978949L;
    private BigDecimal tickSize;
    private boolean tickSizeConst;
    private double domainTickSize;
    private boolean drawVolume;
    private double lowerBound;
    private Paint profilePaint;
    private Paint volumePaint;
    private double maxVolume = 0.0D;
    private List<ChartItemsSeriesCollection> chartItemsSeriesCollections;



    public MarketProfileRenderer(Date startDateTime, boolean drawVolume) {
        this.lowerBound = (double) startDateTime.getTime();
        this.drawVolume = drawVolume;
        this.volumePaint = Color.GRAY;
        this.profilePaint = Color.GRAY;
        this.tickSizeConst = false;
        this.setDefaultToolTipGenerator(new HighLowItemLabelGenerator());
    }

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset xyDataset, PlotRenderingInfo info) {
        if(!tickSizeConst) {
            NumberAxis rangeAxis = (NumberAxis)plot.getRangeAxis();
            double tick = rangeAxis.getTickUnit().getSize();
            tickSize = new BigDecimal(Double.toString(tick));
        }

        DateAxis dateAxis = (DateAxis)plot.getDomainAxis();
        domainTickSize = dateAxis.getTickUnit().getSize();

        chartItemsSeriesCollections = new ArrayList<>();
        Map<BigDecimal, Double> xValsMap = new HashMap<>();
        char symbol = '#';
        OHLCDataset dataset = (OHLCDataset) xyDataset;

        for(int series = 0; series < dataset.getSeriesCount(); series ++) {
            List<ChartItemsCollection> chartItemsCollections = new ArrayList<>();
            for(int item = 0; item < dataset.getItemCount(series); item++) {

                symbol = getSymbol(symbol);


                double low = dataset.getLowValue(series, item);
                BigDecimal lowBD = roundValueWithTickSize(low);

                double high = dataset.getHighValue(series, item);
                BigDecimal highBD = roundValueWithTickSize(high);

                List<ChartItem> chartItemsList = new ArrayList<>();

                while (highBD.compareTo(lowBD) > 0) {
                    xValsMap.putIfAbsent(lowBD, lowerBound);
                    double xCurr = xValsMap.get(lowBD);
                    chartItemsList.add(new ChartItem(xCurr, lowBD.doubleValue(), lowBD.doubleValue() + tickSize.doubleValue(), symbol));

                    double nextXVal = xCurr + domainTickSize;
                    xValsMap.put(lowBD, nextXVal);
                    lowBD = lowBD.add(tickSize);
                }
                chartItemsCollections.add(new ChartItemsCollection(chartItemsList));


                if(drawVolume) {
                    double volume = dataset.getVolumeValue(series, item);
                    if (volume > this.maxVolume) {
                        this.maxVolume = volume;
                    }
                }
            }
            chartItemsSeriesCollections.add(new ChartItemsSeriesCollection(chartItemsCollections));
        }
        return new XYItemRendererState(info);
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
                         PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
                         XYDataset xyDataset, int series, int item, CrosshairState crosshairState, int pass)
    {
        RectangleEdge rangeEdge = plot.getRangeAxisEdge();
        RectangleEdge domainEdge = plot.getDomainAxisEdge();
        OHLCDataset dataset = (OHLCDataset) xyDataset;

        List<ChartItem> chartItemList = chartItemsSeriesCollections.get(series).getSeriesData().get(item).getData();
        for(ChartItem chartItem : chartItemList) {
            drawRectangle(chartItem, g2, domainEdge, rangeEdge, dataArea, domainAxis, rangeAxis, dataset, info, series, item);
            drawChartItem(chartItem, g2, domainEdge, rangeEdge, dataArea, domainAxis, rangeAxis);
        }

        if(drawVolume) {
            drawVolumeItem(dataset, dataArea, g2, domainEdge, domainAxis, series, item);
        }
    }



    private void drawChartItem(ChartItem chartItem, Graphics2D g2, RectangleEdge domainEdge, RectangleEdge rangeEdge,
                               Rectangle2D dataArea, ValueAxis domainAxis, ValueAxis rangeAxis)
    {
        setUpFont(g2);

        double y = (chartItem.getLow() + chartItem.getHigh())/2;
        double x = (chartItem.getXValue() + chartItem.getXValue() + domainTickSize)/2;
        String text = Character.toString(chartItem.getSymbol());

        double yJ2D = rangeAxis.valueToJava2D(y, dataArea, rangeEdge);
        double xJ2D = domainAxis.valueToJava2D(x, dataArea, domainEdge);

        g2.drawString(text, (float) xJ2D, (float) yJ2D);
    }


    private void drawRectangle( ChartItem chartItem, Graphics2D g2, RectangleEdge domainEdge, RectangleEdge rangeEdge,
                                Rectangle2D dataArea, ValueAxis domainAxis, ValueAxis rangeAxis,
                                OHLCDataset dataset, PlotRenderingInfo info, int series, int item)
    {
        double lowJ2D = rangeAxis.valueToJava2D(chartItem.getLow(), dataArea, rangeEdge);
        double highJ2D = rangeAxis.valueToJava2D(chartItem.getHigh(), dataArea, rangeEdge);

        double x1J2D = domainAxis.valueToJava2D(chartItem.getXValue(), dataArea, domainEdge);
        double x2J2D = domainAxis.lengthToJava2D(domainTickSize, dataArea, domainEdge);

        Rectangle2D.Double body = new Rectangle2D.Double(x1J2D, highJ2D, x2J2D, lowJ2D - highJ2D);
        Rectangle2D.Double hotspot = new Rectangle2D.Double(x1J2D, highJ2D, x2J2D, lowJ2D - highJ2D);

        Paint paint = this.getProfilePaint();
        g2.setPaint(paint);
        g2.fill(body);

        g2.draw(body);

        EntityCollection entities = null;
        if (info != null) {
            entities = info.getOwner().getEntityCollection();
        }

        if(entities != null) {
            this.addEntity(entities, hotspot, dataset, series, item, 0.0D, 0.0D);
        }
    }

    private void drawVolumeItem(OHLCDataset dataset, Rectangle2D dataArea, Graphics2D g2, RectangleEdge domainEdge, ValueAxis domainAxis, int series, int item) {
        double volume = dataset.getVolumeValue(series, item);
        double volumeHeight = volume/maxVolume;
        double min = dataArea.getMinY();
        double max = dataArea.getMaxY();

        double volumeY = volumeHeight * (max - min);
        double volumeYY = max - volumeY;

        double x = dataset.getXValue(series, item);
        double xJ2D = domainAxis.valueToJava2D(x, dataArea, domainEdge);

        g2.setPaint(this.getVolumePaint());
        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(3, 0.3F));

        double volumeWidth = 3.0D;
        g2.fill(new Rectangle2D.Double(xJ2D, volumeYY, volumeWidth, volumeY));
        g2.setComposite(originalComposite);
    }

    private class ChartItem {
        private double xValue;
        private double low;
        private double high;
        private char symbol;

        public ChartItem(double xValue, double low, double high, char symbol) {
            this.xValue = xValue;
            this.low = low;
            this.high = high;
            this.symbol = symbol;
        }

        public char getSymbol() {
            return symbol;
        }

        public double getXValue() {
            return xValue;
        }

        public double getLow() {
            return low;
        }

        public double getHigh() {
            return high;
        }
    }

    private class ChartItemsCollection {
        private List<ChartItem> data;
        public ChartItemsCollection(List<ChartItem> data) {
            this.data = data;
        }

        public List<ChartItem> getData() {
            return data;
        }
    }

    private class ChartItemsSeriesCollection {
        private List<ChartItemsCollection> seriesData;

        public ChartItemsSeriesCollection(List<ChartItemsCollection> seriesData) {
            this.seriesData = seriesData;
        }

        public List<ChartItemsCollection> getSeriesData() {
            return seriesData;
        }
    }

    private BigDecimal roundValueWithTickSize(double val) {
        double round = Math.round(val/tickSize.doubleValue()) * tickSize.doubleValue();
        BigDecimal bd = new BigDecimal(round);
        bd = bd.setScale(tickSize.scale(), RoundingMode.HALF_UP);
        return bd;
    }

    public void setTickSize(double tick) {
        tickSizeConst = true;
        this.tickSize = new BigDecimal(Double.toString(tick));
    }

    private void setUpFont(Graphics2D g2) {
        String fontName = g2.getFont().toString();
        int fontSize = g2.getFont().getSize();
        g2.setFont(new Font(fontName, Font.BOLD, fontSize));
        g2.setColor(Color.black);
    }

    private char getSymbol(char symbol) {
        if(symbol == '#') {
            return 'A';
        }

        if(symbol >= 'A' && symbol < 'Z') {
            return (char) (symbol + 1);
        } else if(symbol >= 'a' && symbol < 'z') {
            return (char) (symbol + 1);
        }

        if(symbol == 'Z') {
            return 'a';
        } else if(symbol == 'z') {
            return 'A';
        }

        return '#';
    }

    public boolean getDrawVolume() {
        return this.drawVolume;
    }

    public void setDrawVolume(boolean drawVolume) {
        if (this.drawVolume != drawVolume) {
            this.drawVolume = drawVolume;
            this.fireChangeEvent();
        }
    }

    public Paint getProfilePaint() {
        return profilePaint;
    }

    public void setProfilePaint(Paint profilePaint) {
        this.profilePaint = profilePaint;
    }

    public Paint getVolumePaint() {
        return this.volumePaint;
    }

    public void setVolumePaint(Paint paint) {
        Args.nullNotPermitted(paint, "paint");
        this.volumePaint = paint;
        this.fireChangeEvent();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}