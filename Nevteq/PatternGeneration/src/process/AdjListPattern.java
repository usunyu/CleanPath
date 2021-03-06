package process;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;

import oracle.spatial.geometry.JGeometry;
import oracle.sql.STRUCT;
import objects.*;

public class AdjListPattern {

	/**
	 * @param file
	 */
	static String root = "file";
	static String generatedFileRoot = "../GeneratedFile";
	static String edgeFile = "Edges.csv";
	static String adjListFile = "AdjList_Tuesday.txt";
	static String adjListPatternKML = "AdjList_Pattern.kml";
	/**
	 * @param link
	 */
	static ArrayList<LinkInfo> linkList = new ArrayList<LinkInfo>();
	static ArrayList<LinkInfo> pathList = new ArrayList<LinkInfo>();
	static HashMap<String, LinkInfo> linkMap = new HashMap<String, LinkInfo>();
	static HashMap<String, LinkInfo> nodesStrToLink = new HashMap<String, LinkInfo>();
	static HashMap<LinkInfo, double[]> linkPatternMap = new HashMap<LinkInfo, double[]>();
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		readLinkFile();
		readAdjListFile();
		findLinkPath();
		//generatePatternKML(6);
		//generatePatternKML(8);
		//generatePatternKML(10);
		//generatePatternKML(12);
		//generatePatternKML(14);
		//generatePatternKML(42);
		//generatePatternKML(44);
		//generatePatternKML(46);
		//generatePatternKML(48);
		generatePatternKML(53);
	}
	
	public static void generatePatternKML(int time) {
		System.out.println("generate pattern kml...");
		int error = 0;
		try {
			FileWriter fstream = new FileWriter(root + "/" + UtilClass.getStartTime(time) + "_" + adjListPatternKML);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("<kml><Document>");

			for (int i = 0; i < pathList.size(); i++) {
				error++;
				
				LinkInfo link = pathList.get(i);
				int linkId = link.getLinkId();
				double[] speedArray = linkPatternMap.get(link);
				PairInfo[] nodeList = link.getNodes();

				String patternStr = "";
				if(speedArray != null) {
					for (int j = 0; j < speedArray.length; j++) {
						if (j == 0)
							patternStr = "\r\n" + UtilClass.getStartTime(j) + " : " + speedArray[j];
						else {
							patternStr = patternStr + "\r\n" + UtilClass.getStartTime(j) + " : " + speedArray[j];
						}
					}
				}
				else {
					System.err.println("no speed array");
				}
				
				String colorStr = "#FFFFFFFF";
				if(speedArray != null)
					colorStr = getColor(speedArray[time]);

				String kmlStr = "<Placemark><name>Link:" + linkId + "</name>";
				kmlStr += "<description>";
				kmlStr += "Pattern:";
				kmlStr += patternStr;
				kmlStr += "</description>";
				kmlStr += "<LineString><tessellate>1</tessellate><coordinates>";
				for (int j = 0; j < nodeList.length; j++) {
					PairInfo node = nodeList[j];
					kmlStr += node.getLongi() + "," + node.getLati() + ",0 ";
				}
				kmlStr += "</coordinates></LineString>";
				kmlStr += "<Style><LineStyle>";
				kmlStr += "<color>" + colorStr + "</color>";
				kmlStr += "<width>2</width>";
				kmlStr += "</LineStyle></Style></Placemark>\n";
				out.write(kmlStr);
			}

			out.write("</Document></kml>");
			out.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.err.println("Error Code: " + error);
		}
		System.out.println("generate pattern kml finish!");
	}
	
	private static String getColor(double speed) {
		if (speed < 10) // blue
			return "64000000";
		else if (speed >= 10 && speed < 25) // red
			return "FF1400FF";
		else if (speed >= 25 && speed < 50) // yellow
			return "FF14F0FF";
		else if (speed >= 50) // green
			return "#FF00FF14";
		return "#FFFFFFFF";
	}
	
	private static void findLinkPath() {
		System.out.println("find link path...");
		for (int i = 0; i < linkList.size(); i++) {
			LinkInfo link = linkList.get(i);
			if (link.getFuncClass() == 1 || link.getFuncClass() == 2) {
				pathList.add(link);
			}
		}
		System.out.println("find link path finish!");
	}
	
	public static void readAdjListFile() {
		System.out.println("read adjlist file...");
		int i = 0;
		try {
			FileInputStream fstream = new FileInputStream(generatedFileRoot + "/" + adjListFile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				i++;
				if (strLine.equals("NA")) {
					continue;
				}

				String[] strList = strLine.split(";");
				for (int j = 0; j < strList.length; j++) {
					String[] nodes = strList[j].split(":");
					String[] headList = nodes[0].split("\\(");
					String nrefNode = headList[0].substring(1);
					String[] timeList = nodes[1].split(",");

					int refNodeId = i - 1;

					String nodeId = refNodeId + "," + nrefNode;

					LinkInfo link = nodesStrToLink.get(nodeId);
					
					if (link == null) {
						nodeId = nrefNode+ "," + refNodeId;
						link = nodesStrToLink.get(nodeId);
					}
					
					if(link != null) {
						double[] speedArray = new double[60];
						for (int k = 0; k < 60; k++) {
							PairInfo[] pairNodes = link.getNodes();
							double dis = DistanceCalculator.CalculationByDistance(pairNodes[0], pairNodes[1]);
							double speed = dis / Double.parseDouble(timeList[k]) * 60 * 60 * 1000;
							speedArray[k] = speed;
						}
						linkPatternMap.put(link, speedArray);
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.err.println("Error Code: " + i);
		}
		System.out.println("read adjlist file finish!");
	}
	
	public static void readLinkFile() {
		System.out.println("read link file...");
		try {
			FileInputStream fstream = new FileInputStream(generatedFileRoot + "/" + edgeFile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				String[] nodes = strLine.split(",");
				//String linkId = nodes[0];
				int linkId = Integer.parseInt(nodes[0]);
				int funClass = Integer.parseInt(nodes[1]);
				String name = nodes[2];
				int refId = Integer.parseInt(nodes[3].substring(1));
				int nrefId = Integer.parseInt(nodes[4].substring(1));
				
				String idStr = Integer.toString(linkId) + Integer.toString(refId) + Integer.toString(nrefId);
				PairInfo[] pairNodes = new PairInfo[2];
				pairNodes[0] = new PairInfo(Double.parseDouble(nodes[5]),
						Double.parseDouble(nodes[6]));
				pairNodes[1] = new PairInfo(Double.parseDouble(nodes[7]),
						Double.parseDouble(nodes[8]));

				LinkInfo link = new LinkInfo(idStr, linkId, funClass, name, refId, nrefId, pairNodes, 2);
				linkList.add(link);
				linkMap.put(idStr, link);
				String nodeId = refId + "," + nrefId;
				nodesStrToLink.put(nodeId, link);
				nodeId = nrefId + "," + refId;
				nodesStrToLink.put(nodeId, link);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		System.out.println("read link file finish!");
	}
}