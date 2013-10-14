package tdsp;

import java.io.*;
import java.text.*;
import java.util.*;

import library.*;

import objects.*;
import function.*;

public class RDFTdspGeneration {

	/**
	 * @param file
	 */
	static String root 				= "file";
	static String adjListFile		= "RDF_AdjList.csv";
	static String pathKMLFile 		= "RDF_Path.kml";
	/**
	 * @param args
	 */
	static long startNode 		= 49304020;
	static long endNode 		= 958285311;
	static int startTime 		= 10;
	static int timeInterval 	= 15;
	static int timeRange 		= 60;
	static String[] days = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
	static String[] months = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
	static String SEPARATION		= ",";
	static String VERTICAL		= "|";
	static String COLON			= ":";
	static String SEMICOLON		= ";";
	static String UNKNOWN 			= "Unknown Street";
	static String YES				= "Y";
	static String NO				= "N";
	/**
	 * @param link
	 */
	static HashMap<Long, RDFLinkInfo> linkMap = new HashMap<Long, RDFLinkInfo>();
	/**
	 * @param node
	 */
	static HashMap<Long, RDFNodeInfo> nodeMap = new HashMap<Long, RDFNodeInfo>();
	/**
	 * @param graph
	 */
	static HashMap<Long, ArrayList<RDFToNodeInfo>> adjListMap = new HashMap<Long, ArrayList<RDFToNodeInfo>>();
	/**
	 * @param route
	 */
	static HashMap<String, RDFLinkInfo> nodeToLink = new HashMap<String, RDFLinkInfo>();
	/**
	 * @param path
	 */
	static ArrayList<Long> pathNodeList = new ArrayList<Long>();
	/**
	 * @param sign
	 * <signId, signObject>
	 */
	static HashMap<Long, RDFSignInfo> signMap = new HashMap<Long, RDFSignInfo>();
	
	public static void main(String[] args) {
		// read node
		RDFInput.readNodeFile(nodeMap);
		prepareRoute();
		// read link
		RDFInput.readLinkFile(linkMap, nodeMap, nodeToLink);
		RDFInput.readLinkName(linkMap);
		RDFInput.readLinkGeometry(linkMap);
		RDFInput.readLinkLane(linkMap);
		// read sign
		RDFInput.fetchSignOrigin(linkMap, signMap);
		RDFInput.fetchSignDest(linkMap, signMap);
		RDFInput.fetchSignElement(signMap);
		
		buildAdjList(0);
		tdspGreedy(startNode, endNode, startTime);
		//tdspAStar(startNode, endNode, startTime);
		RDFOutput.generatePathKML(pathNodeList, nodeToLink);
		turnByTurn();
	}
	
	public static void prepareRoute() {
		System.out.println("preparing route...");
		for(RDFNodeInfo node : nodeMap.values()) {
			node.prepareRoute();
		}
	}
	
	/**
	 * @deprecated we use base name instead
	 * @param stName
	 * @return
	 */
	private static String getUniformSTName(String stName) {
		String[] namePart = stName.split(";");
	 	int index = 0;
	 	boolean find = false;
		
	 	if(namePart.length > 1) {
	 		for(; index < namePart.length; index++) {
	 			for(int i = 0; i < namePart[index].length(); i++) {
	 				if(Character.isDigit(namePart[index].charAt(i))) {
	 					find = true;
	 					break;
	 				}
	 			}
	 			if(find)
	 				break;
	 		}
	 	}
	 	if(index == namePart.length)
	 		stName = namePart[0];
	 	else
	 		stName = namePart[index];
	 	return stName;
	 }
	
	public static RDFSignInfo getCorrectSign(long linkId, LinkedList<RDFSignInfo> signList) {
		RDFSignInfo correctSign = null;
		for(RDFSignInfo sign : signList) {
			if(sign.containDestLinkId(linkId)) {
				correctSign = sign;
				break;
			}
		}
		return correctSign;
	}
	
	public static String getSignText(long linkId, LinkedList<RDFSignInfo> signList) {
		RDFSignInfo sign = getCorrectSign(linkId, signList);
		String signInfoStr = null;
		// contain the correct sign
		if(sign != null) {
			RDFSignDestInfo signDest = sign.getSignDest(linkId);
			ArrayList<RDFSignElemInfo> signElemList = signDest.getSignElemList();
			RDFSignElemInfo signElemR = null;
			RDFSignElemInfo signElemT = null;
			for(RDFSignElemInfo signElem : signElemList) {
				if(signElem.getTextType().equals("R")) {
					signElemR = signElem;
				}
				if(signElem.getTextType().equals("T")) {
					signElemT = signElem;
				}
				/* problem */
				if(signElemR != null && signElemT != null) {
					break;
				}
			}
			if(signElemR != null) {
				if( signDest.getExitNumber() != null ) {
					signInfoStr = "Take the exit " + signDest.getExitNumber() + " onto " + signElemR.getText();
				}
				else {
					signInfoStr = "Take the exit onto " + signElemR.getText();
				}
			}
			if(signElemT != null) {
				signInfoStr += " toward " + signElemT.getText();
			}
		}
		return signInfoStr;
	}
	
	public static void turnByTurn() {
		System.out.println("turn by turn...");
		long preNodeId = -1;
		String preBaseName = "";
		String preStreetName = "";
		boolean preExitName = false;
		int preDirIndex = -1;
		double distance = 0;
		boolean firstRoute = true;
		DecimalFormat df = new DecimalFormat("#0.0");
		for(int i = 0; i < pathNodeList.size(); i++) {
			if(i == 0) {
				preNodeId = pathNodeList.get(i);
				continue;
			}
			
			long curNodeId = pathNodeList.get(i);
			String nodeStr = preNodeId + "," + curNodeId;
			RDFLinkInfo link = nodeToLink.get(nodeStr);
			
			long linkId = link.getLinkId();
			int functionalClass = link.getFunctionalClass();
			boolean exitName = link.isExitName();
			
			RDFNodeInfo preNode = nodeMap.get(preNodeId);
			RDFNodeInfo curNode = nodeMap.get(curNodeId);
			
			int curDirIndex = Geometry.getDirectionIndex(preNode.getLocation(), curNode.getLocation());
			
			String curBaseName = link.getBaseName();
			LinkedList<String> curStreetNameList = link.getStreetNameList();
			String curStreetName = curStreetNameList.getFirst();
			
			LinkedList<RDFSignInfo> signList = link.getSignList();
			
			if(i == 1) {	// initial
				preBaseName = curBaseName;
				preStreetName = curStreetName;
				preDirIndex = curDirIndex;
			}
			
			// no turn need, cumulative distance
			if(Geometry.isSameDirection(curDirIndex, preDirIndex) && preBaseName.equals(curBaseName)) {
				// using sign table
				if(signList != null) {
					String signText = getSignText(linkId, signList);
					if(signText != null) {
						if(firstRoute) {
							System.out.println("Head " + Geometry.getDirectionStr(preDirIndex) + " on " + preStreetName + " toward " + curStreetName);
							firstRoute = false;
						}
						System.out.println( df.format(distance) + " miles");
						System.out.println(signText);
						distance = 0;
					}
				}
			}
			else if(!preBaseName.equals(curBaseName) && Geometry.isSameDirection(curDirIndex, preDirIndex)) {	// change road
				if(functionalClass > 2) {
					if(!preStreetName.equals(UNKNOWN) && !preExitName) {
						if(firstRoute) {
							System.out.println("Head " + Geometry.getDirectionStr(preDirIndex) + " on " + preStreetName + " toward " + curStreetName);
							firstRoute = false;
						}
						System.out.println( df.format(distance) + " miles");
						System.out.println("Merge on to " + curStreetName);
						distance = 0;
					}
				}
				else {
					if(signList != null) {
						String signText = getSignText(linkId, signList);
						if(signText != null) {
							if(firstRoute) {
								System.out.println("Head " + Geometry.getDirectionStr(preDirIndex) + " on " + preStreetName + " toward " + curStreetName);
								firstRoute = false;
							}
							System.out.println( df.format(distance) + " miles");
							System.out.println(signText);
							distance = 0;
						}
					}
				}
			}
			else if(preBaseName.equals(curBaseName) && !Geometry.isSameDirection(curDirIndex, preDirIndex)) {	// change direction
				if(functionalClass > 2) {
					if(!preStreetName.equals(UNKNOWN) && !preExitName) {
						if(firstRoute) {
							System.out.println("Head " + Geometry.getDirectionStr(preDirIndex) + " on " + preStreetName + " toward " + curStreetName);
							firstRoute = false;
						}
						System.out.println( df.format(distance) + " miles");
						int turn = Geometry.getTurn(preDirIndex, curDirIndex);
						if(turn == Geometry.LEFT && !exitName)
							System.out.println("Turn left to stay on " + curStreetName);
						if(turn == Geometry.RIGHT && !exitName)
							System.out.println("Turn right to stay on " + curStreetName);
						
						distance = 0;
					}
				}
				else {
					// using sign table
					if(signList != null) {
						String signText = getSignText(linkId, signList);
						if(signText != null) {
							if(firstRoute) {
								System.out.println("Head " + Geometry.getDirectionStr(preDirIndex) + " on " + preStreetName + " toward " + curStreetName);
								firstRoute = false;
							}
							System.out.println( df.format(distance) + " miles");
							System.out.println(signText);
							distance = 0;
						}
					}
				}
			}
			else {	// change direction and road
				if(functionalClass > 2) {
					if(!preStreetName.equals(UNKNOWN) && !preExitName) {
						if(firstRoute) {
							System.out.println("Head " + Geometry.getDirectionStr(preDirIndex) + " on " + preStreetName + " toward " + curStreetName);
							firstRoute = false;
						}
						System.out.println( df.format(distance) + " miles");
						int turn = Geometry.getTurn(preDirIndex, curDirIndex);
						if(turn == Geometry.LEFT && !exitName)
							System.out.println("Turn left on to " + curStreetName);
						if(turn == Geometry.RIGHT && !exitName)
							System.out.println("Turn right on to " + curStreetName);
						distance = 0;
					}
				}
				else {
					// using sign table
					if(signList != null) {
						String signText = getSignText(linkId, signList);
						if(signText != null) {
							if(firstRoute) {
								System.out.println("Head " + Geometry.getDirectionStr(preDirIndex) + " on " + preStreetName + " toward " + curStreetName);
								firstRoute = false;
							}
							System.out.println( df.format(distance) + " miles");
							System.out.println(signText);
							distance = 0;
						}
					}
				}
			}
			distance += Geometry.calculateDistance(link.getPointList());
			
			// arrive destination
			if(i == pathNodeList.size() - 1) {
				if(distance > 0) {
					System.out.println("Go straight on " + curStreetName);
					System.out.println( df.format(distance) + " miles");
				}
				System.out.println("Arrive destination.");
			}
				
			preNodeId = curNodeId;
			preBaseName = curBaseName;
			preStreetName = curStreetName;
			preDirIndex = curDirIndex;
			preExitName = exitName;
		}
		System.out.println("turn by turn finish!");
	}
	
	public static void tdspGreedy(long startNode, long endNode, int startTime) {
		System.out.println("start finding the path...");
		PriorityQueue<RDFNodeInfo> priorityQ = new PriorityQueue<RDFNodeInfo>(
				20, new Comparator<RDFNodeInfo>() {
			public int compare(RDFNodeInfo n1, RDFNodeInfo n2) {
				return n1.getCost() - n2.getCost();
			}
		});
		
		RDFNodeInfo current = nodeMap.get(startNode);	// get start node
		if(current == null) {
			System.err.println("cannot find start node, program exit!");
			System.exit(-1);
		}
		
		current.setCost(0);	// set start cost to 0
		
		priorityQ.offer(current);
		
		while ((current = priorityQ.poll()) != null) {
			long nodeId = current.getNodeId();
			
			if(nodeId == endNode)	// find the end
				break;
			
			int timeIndex = startTime + current.getCost() / 60 / timeInterval;
			
			if (timeIndex > timeRange - 1)	// time [6am - 9 pm], we regard times after 9pm as constant edge weights
				timeIndex = timeRange - 1;
			
			ArrayList<RDFToNodeInfo> adjNodeList = adjListMap.get(nodeId);
			if(adjNodeList == null)
				continue;
			
			int arrTime = current.getCost();
			
			for(RDFToNodeInfo toNode : adjNodeList) {
				long toNodeId = toNode.getNodeId();
				
				RDFNodeInfo toNodeRoute = nodeMap.get(toNodeId);
				
				if(toNodeRoute.isVisited())	// if the node is visited, we bypass it
					continue;
				
				int travelTime;
				if(toNode.isFix())	// fix time
					travelTime = toNode.getTravelTime();
				else	// fetch from time array
					travelTime = toNode.getTravelTimeArray()[timeIndex];
				
				// if we find a node with updated distance, just insert it to the priority queue
				// even we pop out another node with same id later, we know that it was visited and will ignore it
				int totalTime = arrTime + travelTime;
				if (totalTime < toNodeRoute.getCost()) {
					toNodeRoute.setCost(totalTime);
					toNodeRoute.setParentId(nodeId);
					priorityQ.offer(toNodeRoute);
				}
			}
		}
		
		if (startNode == endNode)
			System.out.println("start node is the same as end node.");
		else {
			current = nodeMap.get(endNode);
			if(current == null) {
				System.err.println("cannot find end node, program exit!");
				System.exit(-1);
			}
			pathNodeList.add(endNode);	// add end node
			
			while(current.getParentId() != -1 && current.getParentId() != startNode) {
				current = nodeMap.get(current.getParentId());
				if(current == null) {
					System.err.println("cannot find intermediate node, program exit!");
					System.exit(-1);
				}
				pathNodeList.add(current.getNodeId());	// add intermediate node
			}
			
			if(current.getParentId() == -1) {
				System.err.println("cannot find the path, program exit!");
				System.exit(-1);
			}
			
			if(current.getParentId() == startNode)
				pathNodeList.add(startNode);	// add start node
			
			Collections.reverse(pathNodeList);	// reverse the path list
		}
		// prepare for next routing
		prepareRoute();
		System.out.println("find the path successful!");
	}
	
	public static void tdspAStar(long startNode, long endNode, int startTime) {
		System.out.println("start finding the path...");
		
		PriorityQueue<RDFNodeInfo> priorityQ = new PriorityQueue<RDFNodeInfo>(
				20, new Comparator<RDFNodeInfo>() {
			public int compare(RDFNodeInfo n1, RDFNodeInfo n2) {
				return n1.getCost() - n2.getCost();
			}
		});
		
		RDFNodeInfo current = nodeMap.get(startNode);	// get start node
		if(current == null) {
			System.err.println("cannot find start node, program exit!");
			System.exit(-1);
		}
		
		current.setCost(0);	// set start cost to 0
		
		priorityQ.offer(current);
		
		while ((current = priorityQ.poll()) != null) {
			long nodeId = current.getNodeId();
			
			if(nodeId == endNode)	// find the end
				break;
			
			int timeIndex = startTime + current.getCost() / 60 / timeInterval;
			
			if (timeIndex > timeRange - 1)	// time [6am - 9 pm], we regard times after 9pm as constant edge weights
				timeIndex = timeRange - 1;
			
			ArrayList<RDFToNodeInfo> adjNodeList = adjListMap.get(nodeId);
			if(adjNodeList == null)
				continue;
			
			int arrTime = current.getCost();
			
			for(RDFToNodeInfo toNode : adjNodeList) {
				long toNodeId = toNode.getNodeId();
				
				RDFNodeInfo toNodeRoute = nodeMap.get(toNodeId);
				
				if(toNodeRoute.isVisited())	// if the node is visited, we bypass it
					continue;
				
				int travelTime;
				if(toNode.isFix())	// fix time
					travelTime = toNode.getTravelTime();
				else	// fetch from time array
					travelTime = toNode.getTravelTimeArray()[timeIndex];
				
				// if we find a node with updated distance, just insert it to the priority queue
				// even we pop out another node with same id later, we know that it was visited and will ignore it
				int totalTime = arrTime + travelTime;
				if (totalTime < toNodeRoute.getCost()) {
					toNodeRoute.setCost(totalTime);
					toNodeRoute.setParentId(nodeId);
					priorityQ.offer(toNodeRoute);
				}
			}
		}
		
		if (startNode == endNode)
			System.out.println("start node is the same as end node.");
		else {
			current = nodeMap.get(endNode);
			if(current == null) {
				System.err.println("cannot find end node, program exit!");
				System.exit(-1);
			}
			pathNodeList.add(endNode);	// add end node
			
			while(current.getParentId() != -1 && current.getParentId() != startNode) {
				current = nodeMap.get(current.getParentId());
				if(current == null) {
					System.err.println("cannot find intermediate node, program exit!");
					System.exit(-1);
				}
				pathNodeList.add(current.getNodeId());	// add intermediate node
			}
			
			if(current.getParentId() == -1) {
				System.err.println("cannot find the path, program exit!");
				System.exit(-1);
			}
			
			if(current.getParentId() == startNode)
				pathNodeList.add(startNode);	// add start node
			
			Collections.reverse(pathNodeList);	// reverse the path list
		}
		System.out.println("start finding the path finish!");
	}
	
	public static void buildAdjList(int day) {
		String[] file = adjListFile.split("\\.");
		String tempAdjListFile = file[0] + "_" + days[day] + "." + file[1];
		
		System.out.println("loading adjlist file: " + tempAdjListFile);
		int debug = 0;
		try {
			FileInputStream fstream = new FileInputStream(root + "/" + tempAdjListFile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			System.out.println("building graph, please wait...");

			while ((strLine = br.readLine()) != null) {
				debug++;
				if (debug % 100000 == 0)
					System.out.println("completed " + debug + " lines.");

				String[] splitStr = strLine.split("\\" + VERTICAL);
				long startNode = Long.parseLong(splitStr[0].substring(1));

				ArrayList<RDFToNodeInfo> toNodeList = new ArrayList<RDFToNodeInfo>();
				String[] nodeListStr = splitStr[1].split(SEMICOLON);
				for (int i = 0; i < nodeListStr.length; i++) {
					String nodeStr = nodeListStr[i];
					long toNode = Long.parseLong(nodeStr.substring(nodeStr.indexOf('n') + 1, nodeStr.indexOf('(')));
					String fixStr = nodeStr.substring(nodeStr.indexOf('(') + 1, nodeStr.indexOf(')'));
					RDFToNodeInfo toNodeInfo;
					if (fixStr.equals("F")) { // fixed
						int travelTime = Integer.parseInt(nodeStr.substring(nodeStr.indexOf(':') + 1));
						toNodeInfo = new RDFToNodeInfo(toNode, travelTime);
					} else { // variable
						String timeListStr = nodeStr.substring(nodeStr.indexOf(':') + 1);
						String[] timeValueStr = timeListStr.split(SEPARATION);
						int[] travelTimeArray = new int[timeValueStr.length];
						for (int j = 0; j < timeValueStr.length; j++)
							travelTimeArray[j] = Integer.parseInt(timeValueStr[j]);
						toNodeInfo = new RDFToNodeInfo(toNode, travelTimeArray);
					}
					toNodeList.add(toNodeInfo);
				}
				adjListMap.put(startNode, toNodeList);
			}

			br.close();
			in.close();
			fstream.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.err.println("buildList: debug code: " + debug);
		}
		System.out.println("building list finish!");
	}
}
