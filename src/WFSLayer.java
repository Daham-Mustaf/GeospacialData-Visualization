import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.util.IteratorIterable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import viewer.base.Layer;
import viewer.symbols.SymbolFactory;
import geometry.Envelope;
import io.structures.Feature;

public class WFSLayer extends Layer {
	private String wfsUrl;
	private String type;

	public WFSLayer(SymbolFactory s, String wfsUrl, String type) {
		super(s);
		this.wfsUrl = wfsUrl;
		this.type = type;
		this.setExtent(getExtentFromWFS());
	}

	public Envelope getExtentFromWFS() {
		Envelope env = null;
		try {
			String query = wfsUrl + "?request=GetCapabilities&version=1.1.0&service=wfs";
			URL url = new URL(query);
			HttpURLConnection conn;
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(conn.getInputStream());
			Element root = doc.getRootElement();
			Namespace ows = Namespace.getNamespace("http://www.opengis.net/ows");
			IteratorIterable<Element> list = root.getDescendants(new ElementFilter("WGS84BoundingBox", ows));
			while (list.hasNext()) {
				Element element = list.next();
				String lc = element.getChild("LowerCorner", ows).getText();
				String uc = element.getChild("UpperCorner", ows).getText();

				String[] lowerCorner = lc.split(" ");
				String[] upperCorner = uc.split(" ");

				Double lCoX = Double.parseDouble(lowerCorner[0]);
				Double lCoY = Double.parseDouble(lowerCorner[1]);
				Double UpcoX = Double.parseDouble(upperCorner[0]);
				Double UpcoY = Double.parseDouble(upperCorner[1]);
				System.out.println(lCoX + " ," + lCoY + " ," + UpcoX + " ," + UpcoY);
				Point lower = point_from4326_to25832(lCoY, lCoX);
				Point upper = point_from4326_to25832(UpcoY, UpcoX);
				env = new Envelope(lower.getX(), upper.getX(), lower.getY(), upper.getY());
			}
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		return env;
	}

	@Override
	public List<viewer.symbols.Symbol> query(Envelope searchEnv) {
		List<viewer.symbols.Symbol> result = new LinkedList<viewer.symbols.Symbol>();
		String query = wfsUrl + "?SERVICE=WFS&VERSION=1.1.0&REQUEST=GetFeature&TYPENAME=" + type;
		URL url;
		try {
			url = new URL(query);
			HttpURLConnection conn;
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			SAXBuilder builder = new SAXBuilder();
			Namespace gml = Namespace.getNamespace("http://www.opengis.net/gml");
			Document doc = builder.build(conn.getInputStream());
			Element root = doc.getRootElement();
			IteratorIterable<Element> list = root.getDescendants(new ElementFilter("Point", gml));
			while (list.hasNext()) {
				Element element = list.next();
				String Points = element.getChild("pos", gml).getText();
				String[] point = Points.split(" ");
				double pointXs = Double.parseDouble(point[0]);
				double pointYs = Double.parseDouble(point[1]);
				Coordinate coor = new Coordinate(pointXs, pointYs);
				Geometry points = new GeometryFactory().createPoint(coor);
				Feature feature = new Feature(points);
				result.add(mySymbolFactory.createSymbol(feature));

			}

		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * transforms point from epsg:4326 to epsg:25832
	 * @param lat
	 * @param lon
	 * @return
	 */
	public org.locationtech.jts.geom.Point point_from4326_to25832(double lat, double lon) {
		org.locationtech.jts.geom.Point p_25832 = null;
		try {
			CoordinateReferenceSystem sourceCRS = org.geotools.referencing.CRS.decode("EPSG:4326");
			CoordinateReferenceSystem targetCRS = org.geotools.referencing.CRS.decode("EPSG:25832");
			MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
			org.locationtech.jts.geom.GeometryFactory factory = new org.locationtech.jts.geom.GeometryFactory();

			org.locationtech.jts.geom.Coordinate[] coordinates = { new org.locationtech.jts.geom.Coordinate(lat, lon) };
			org.locationtech.jts.geom.CoordinateSequence coordinateSequence = new org.locationtech.jts.geom.impl.CoordinateArraySequence(
					coordinates);
			org.locationtech.jts.geom.Point p_4326 = new org.locationtech.jts.geom.Point(coordinateSequence, factory);

			p_25832 = (org.locationtech.jts.geom.Point) JTS.transform(p_4326, transform);
		} catch (MismatchedDimensionException | TransformException | FactoryException e) {
			e.printStackTrace();
		}
		return p_25832;
	}

}
