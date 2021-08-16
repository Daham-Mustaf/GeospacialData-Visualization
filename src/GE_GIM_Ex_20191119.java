import java.awt.Dimension;
import javax.swing.JFrame;

import geometry.Envelope;
import viewer.base.MapPanel;
import viewer.symbols.SymbolFactory;

public class GE_GIM_Ex_20191119 {

	public static void main(String[] args) {
		String sUrl = "http://131.220.71.188:8080/geoserver/geo/ows";
		WFSLayer wfsLayer = new WFSLayer(SymbolFactory.DEFAULT_FACTORY, sUrl, "tree");
		
		// generate Panel and Frame 		
		MapPanel panel = new MapPanel();
		panel.getMap().addLayer(wfsLayer, 1);
		JFrame frame = new JFrame("Ex 4 WFS Layer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(panel);
		Dimension size = new Dimension(640, 480);
		frame.setSize(size);
		frame.setPreferredSize(size);
		frame.setVisible(true);
	}

}
