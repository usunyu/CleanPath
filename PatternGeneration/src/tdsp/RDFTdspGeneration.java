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
	
	public static String getTurnText(int turn) {
		String turnStr = "";
		switch(turn) {
			case Geometry.LEFT: 
				turnStr = "Turn left ";
				break;
			case Geometry.RIGHT:
				turnStr = "Turn right ";
				break;
			case Geometry.SLIGHTLEFT:
				turnStr = "Take slight left ";
				break;
			case Geometry.SLIGHTRIGHT:
				turnStr = "Take slight right ";
				break;
			case Geometry.SHARPLEFT:
				turnStr = "Take sharp  left ";
				break;
			case Geometry.SHARPRIGHT:
				turnStr = "Take sharp right ";
				break;
			case Geometry.UTURN:
				turnStr = "Take u-turn ";
				break;
		}
		return turnStr;
	}
	
	public static boolean isArterialClass(int functionalClass) {
		if(functionalClass >= 3 && functionalClass <= 5)
			return true;
		else
			return false;
	}
	
	public static boolean isHighwayClass(int functionalClass) {
		if(functionalClass >= 1 && functionalClass <= 2)
			return true;
		else
			return false;
	}
	
	public static RDFSignInfo getTargetSign(long linkId, LinkedList<RDFSignInfo> signList) {
		RDFSignInfo targetSign = null;
		if(signList == null)
			return targetSign;
		for(RDFSignInfo sign : signList) {
			if(sign.containDestLinkId(linkId)) {
				targetSign = sign;
				break;
			}
		}
		return targetSign;
	}

	public static HashSet<String> getSignText(RDFSignDestInfo signDest) {
		HashSet<String> signTextSet = null;
		ArrayList<RDFSignElemInfo> signElemList = signDest.getSignElemList();
		for(RDFSignElemInfo signElem : signElemList) {
			if(signElem.getTextType().equals("R")) {
				if(signTextSet == null) {
					signTextSet = new HashSet<String>();
				}
				signTextSet.add(signElem.getText());
			}
		}
		return signTextSet;
	}
	
	/**
	 * search forward to find if the road name contained in the available sign text, if so, chose that for routing
	 * @param currentIndex
	 * @param signTextSet
	 * @return
	 */
	public static String searchPathSign(int currentIndex, HashSet<String> signTextSet) {
		String signText = null;
		long preNodeId = -1;
		for(int i = currentIndex; i < pathNodeList.size(); i++) {
			if(i == currentIndex) {
				preNodeId = pathNodeList.get(i);
				continue;
			}
			long curNodeId = pathNodeList.get(i);
			String nodeStr = preNodeId + "," + curNodeId;
			RDFLinkInfo link = nodeToLink.get(nodeStr);
			String baseName = link.getBaseName();
			if(signTextSet.contains(baseName)) {
				signText = baseName;
				break;
			}
			preNodeId = curNodeId;
		}
		return signText;
	}
	
	public static void turnByTurn() {
		System.out.println("turn by turn...");
		int debug = 0;
		try {
			long preNodeId = -1;
			
			String preBaseName = "";
			String preStreetName = "";
			boolean preExitName = false;
			int preFunctionalClass = -1;
			int preDirIndex = -1;
			String lastRouteText = UNKNOWN;
			
			double distance = 0;
			boolean firstRoute = true;
			DecimalFormat df = new DecimalFormat("#0.0");
			
			for(int i = 0; i < pathNodeList.size(); i++) {
				debug++;
				
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
				
				if(i == 1) {	// initial prev
					preBaseName = curBaseName;
					preStreetName = curStreetName;
					preDirIndex = curDirIndex;
					preFunctionalClass = functionalClass;
				}
				
				// for arterial
				if(isArterialClass(preFunctionalClass) && isArterialClass(functionalClass)) {
					// change direction or road happen
					if(!preBaseName.equals(curBaseName) || !Geometry.isSameDirection(curDirIndex, preDirIndex)) {
						// pre street has name and not exit name
						if(!preStreetName.equals(UNKNOWN) && !preExitName) {
							// first route happen
							if(firstRoute) {
								System.out.println("Head " + Geometry.getDirectionStr(preDirIndex) + " on " + preStreetName + " toward " + curStreetName);
								firstRoute = false;
							}
							// sign table first
							RDFSignInfo sign = getTargetSign(linkId, signList);
							if(sign != null) {	// valid sign exist, take ramp on to highway
								RDFSignDestInfo signDest = sign.getSignDest(linkId);
								// get all the route sign text available
								HashSet<String> signTextSet = getSignText(signDest);
								String signText = searchPathSign(i, signTextSet);
								
								if(signText != null) {	// find the target sign
									System.out.println( df.format(distance) + " miles");
									if(link.isRamp())
										System.out.println("Take ramp onto " + signText);
									else 
										System.out.println("Merge onto " + signText);
									distance = 0;
								}
							}
							else {	// check normal condition
								// no turn need, cumulative distance
								if(Geometry.isSameDirection(curDirIndex, preDirIndex) && preBaseName.equals(curBaseName)) {
									
								}
								else if(!preBaseName.equals(curBaseName) && Geometry.isSameDirection(curDirIndex, preDirIndex)) {	// change road
									if(!curStreetName.equals(UNKNOWN) && !exitName) {
										System.out.println( df.format(distance) + " miles");
										System.out.println("Merge onto " + curStreetName);
										distance = 0;
									}
								}
								else if(preBaseName.equals(curBaseName) && !Geometry.isSameDirection(curDirIndex, preDirIndex)) {	// change direction
									if(!curStreetName.equals(UNKNOWN) && !exitName) {
										System.out.println( df.format(distance) + " miles");
										int turn = Geometry.getTurn(preDirIndex, curDirIndex);
										String turnText = getTurnText(turn);
										turnText += "to stay on " + curStreetName;
										System.out.println(turnText);
										distance = 0;
									}
								}
								else {	// change direction and road
									if(!curStreetName.equals(UNKNOWN) && !exitName) {
										System.out.println( df.format(distance) + " miles");
										int turn = Geometry.getTurn(preDirIndex, curDirIndex);
										String turnText = getTurnText(turn);
										turnText += "onto " + curStreetName;
										System.out.println(turnText);
										distance = 0;
									}
								}
							}
						}
					}
				}
				// for highway
				if(isHighwayClass(preFunctionalClass) && isHighwayClass(functionalClass)) {
					// using sign table
					RDFSignInfo sign = getTargetSign(linkId, signList);
					if(sign != null) {	// valid sign exist, take exit onto
						RDFSignDestInfo signDest = sign.getSignDest(linkId);
						HashSet<String> signTextSet = getSignText(signDest);
						// if the current name is contained in the available sign text, no need for routing
						if(!signTextSet.contains(curBaseName)) {
							System.out.println( df.format(distance) + " miles");
							String signText = searchPathSign(i, signTextSet);
							// last route text is different from this one
							if(!signText.equals(lastRouteText)) {
								lastRouteText = signText;
								if(signDest.getExitNumber() != null)
									signText = "Take the exit " + signDest.getExitNumber() + " on to " + signText;
								else
									signText = "Take the exit on to " + signText;
								System.out.println(signText);
								distance = 0;
							}
						}
					}
				}
				// from arterial to highway
				if(isArterialClass(preFunctionalClass) && isHighwayClass(functionalClass)) {
					//System.out.println("from arterial to highway");
				}
				// from highway to arterial
				if(isHighwayClass(preFunctionalClass) && isArterialClass(functionalClass)) {
					//System.out.println("from arterial to highway");
				}
				distance += Geometry.calculateDistance(link.getPointList());
				
				// arrive destination
				if(i == pathNodeList.size() - 1) {
					if(distance > 0) {
						System.out.println("Go straight on " + curStreetName);
						System.out.println( df.format(distance) + " miles");
					}
					System.out.println("Arrive destination");
				}
					
				preNodeId = curNodeId;
				preBaseName = curBaseName;
				preStreetName = curStreetName;
				preDirIndex = curDirIndex;
				preExitName = exitName;
				preFunctionalClass = functionalClass;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("turnByTurn: debug code " + debug);
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
