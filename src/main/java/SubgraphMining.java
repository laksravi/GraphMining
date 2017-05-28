import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class SubgraphMining {
	
	GraphDatabaseService db;
	File dbpath;
	
	
	public static void main(String[] args) {
		SubgraphMining subgraphMiner = new SubgraphMining();
		subgraphMiner.initCandidates();
	}
	
	public SubgraphMining(){
		dbpath = new File("D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db");
		db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbpath).setConfig(GraphDatabaseSettings.pagecache_memory, "2G" ).setConfig(GraphDatabaseSettings.string_block_size, "60" ).setConfig(GraphDatabaseSettings.array_block_size, "50" ).newGraphDatabase();
	}

	public void initCandidates(){
		Transaction tx = db.beginTx();
		ResourceIterable<Relationship> primitiveGraphs = db.getAllRelationships();
		ArrayList<Graph> candidateGraphs = new ArrayList();
		for(Relationship relation: primitiveGraphs){
			
			Graph graph = new Graph(relation);
			printEdgeOrder(graph.edgeOrder);
			candidateGraphs.add(graph);

		}
		extendCandidate(candidateGraphs, 1);
		tx.success();
		tx.close();
	}	
	
	
	/**
	 * Extend candidates until a certain depth
	 * @param graph
	 * @param iteration
	 */
	public void extendCandidate(ArrayList<Graph> graphs, int iteration){
		if(iteration == 4)
			return;			
		for(Graph graph: graphs){
			if(!checkSupport(graph, graphs))
				continue;
			ArrayList<Graph> candidateGraphs = new ArrayList();
			for(GraphNode graphNode: graph.nodeMap.values()){
				//for every graph node in the current graph get all the relations 
				Node node = db.getNodeById(graphNode.nodeId);
				Iterable<Relationship> relations = node.getRelationships(Direction.OUTGOING);
				// for every edge in the current node - try to add end node as a part of  the current graph
				for(Relationship relation: relations){
					//get copy of current graph				
					if(!graph.traversedPaths.contains(relation)){
						Graph extendCandidate = graph.getCopy();
						//extend the graph by adding current edge
						extendCandidate.extendGraph(relation, node);
						if(checkIsomorphism(extendCandidate)){	
							printEdgeOrder(extendCandidate.edgeOrder);
							candidateGraphs.add(extendCandidate);
						}
					}
				}			
			}
			extendCandidate(candidateGraphs, iteration+1);
		}
	}	
	
	
	private boolean checkSupport(Graph graph, ArrayList<Graph> graphs) {
		// TODO Auto-generated method stub
		return false;
	}

	private void printEdgeOrder(ArrayList<GraphNode[]> edgeOrder) {
		for(GraphNode[] pair: edgeOrder){
			System.out.format("%s: %s ", pair[0], pair[1]);
		}
		System.out.println();
	}

	
	public boolean checkIsomorphism(Graph graph){
		ArrayList<GraphNode[]> canonicalCode= graph.getCanonicalCode();
		ArrayList<GraphNode[]> edgeOrder= graph.edgeOrder;
		try {		
			if(canonicalCode.size() != edgeOrder.size())			
				throw new InvalidCanonicalCode(canonicalCode.size() , edgeOrder.size());
		} catch (InvalidCanonicalCode e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
		}
		for(int i = 0; i < canonicalCode.size(); i++){
			GraphNode[] pair1 = canonicalCode.get(i);
			GraphNode[] pair2 = edgeOrder.get(i);
			if(! (pair1[0].equals(pair2[0]) && pair1[1].equals(pair2[1])) )
				return false;
		}
		return true;
	}	
	
}

class Graph{
	HashMap<Long, GraphNode> nodeMap = new HashMap<Long, GraphNode>();
	ArrayList<Relationship> traversedPaths = new ArrayList<Relationship>();
	ArrayList<GraphNode[]> edgeOrder = new ArrayList<GraphNode[]>();
	int ID;
	Integer isomorph;
	
	public Graph(){
		
	}
	
	public Graph(Relationship relation){
		Node startNode = relation.getStartNode();
		Node endNode = relation.getEndNode();
		GraphNode start = new GraphNode(startNode);
		GraphNode end = new GraphNode(endNode); 
		start.extendNode(end);
		nodeMap.put(startNode.getId(), start);
		nodeMap.put(endNode.getId(), end);
		traversedPaths.add(relation);
		GraphNode[] firstPath = new GraphNode[2];
		firstPath[0]=start; firstPath[1]=end;
		edgeOrder.add(firstPath);
		
	}
	
	public ArrayList<GraphNode[]> getEdgeOrder(){
		return edgeOrder;
	}
	
	/**
	 * generate new graph instance from the current instance by adding a new relation
	 * @param relation
	 * @param node
	 */	
	public Graph getExtendedGraph(Relationship relation, Node node){
		Graph newGraph = getCopy();
		newGraph.extendGraph(relation, node);
		return newGraph;
	}
	
	public Graph getCopy(){
		Graph newGraph = new Graph();		
		
		//Clone the graph to a new graph - create new hash map with pointers to new nodes
		newGraph.nodeMap = new HashMap<Long, GraphNode>();
	   Set<Long> nodeIds= nodeMap.keySet();
	   for(Long nodeId : nodeIds){
		   GraphNode currentIdNode = nodeMap.get(nodeId);
		   GraphNode newNode = new GraphNode(currentIdNode);
		   	newGraph.nodeMap.put(nodeId,  newNode);
	   }
	   
	   //iterate over the edges of the current nodes, create edges in new nodes which point to the new nodes
	   nodeIds= nodeMap.keySet();
	   for(Long nodeId : nodeIds){
		   	GraphNode currentNode = nodeMap.get(nodeId);
		   	GraphNode newNode = newGraph.nodeMap.get(nodeId);
		   	for(GraphNode neighbor :  currentNode.edges ){
		   				Long neighborId = neighbor.nodeId;
		   				GraphNode newNeighborNode = newGraph.nodeMap.get(neighborId);
		   				newNode.extendNode(newNeighborNode);
		   	}	   
	   }	   
	   	newGraph.traversedPaths.addAll(traversedPaths);
		newGraph.edgeOrder.addAll(edgeOrder);
		return newGraph;
	}
	
	/**
	 * Extends existing graph to add new edge from start node to end node
	 * @param relation
	 * @param node
	 */
	public void extendGraph(Relationship relation, Node node){
		Node endNode = relation.getOtherNode(node);
		GraphNode extendNode;	
		GraphNode[]  newPath = new GraphNode[2];
		newPath[0] = nodeMap.get(node.getId());
		if(nodeMap.containsKey(endNode.getId())){
			extendNode = nodeMap.get(endNode.getId());
			nodeMap.get(node.getId()).extendNode(extendNode);
			newPath[1] = nodeMap.get(endNode.getId());
		}
		else{
			GraphNode newNode = new GraphNode(endNode);
			nodeMap.get(node.getId()).extendNode(newNode);
			nodeMap.put(endNode.getId(), newNode);
			newPath[1] = newNode;
		}		
		edgeOrder.add(newPath);
		traversedPaths.add(relation);
	}
	
	/**
	 * Given any graph it returns the canonical code 
	 * @param graph
	 * @return
	 */
	public ArrayList<GraphNode[]> getCanonicalCode(){
		ArrayList<GraphNode> sortedNodes = new ArrayList(this.nodeMap.values());
		Collections.sort(sortedNodes);
		//iterate through nodes and generate the 
		HashSet<GraphNode> visitedNodes = new HashSet<GraphNode>();
		//visitedNodes.add(sortedNodes.get(0));
		return getMinDFSPath(sortedNodes.get(0), visitedNodes);
	}
	
	
	/**
	 * Perform a DFS from the current node - edges selected in the order of 
	 * @param node
	 * @param visitedNodes
	 * @return
	 */
	private ArrayList<GraphNode[]> getMinDFSPath(GraphNode node, HashSet<GraphNode> visitedNodes){
		ArrayList<GraphNode[]> allPaths = new ArrayList<GraphNode[]>();
		ArrayList<GraphNode> edges = node.edges;
		Collections.sort(edges);
		visitedNodes.add(node);
		//visit the edges
		for(GraphNode neighbor: edges){
			GraphNode[] path = new GraphNode[2];
			path[0] = node;
			path[1] = neighbor;
			//if the neighbor has not been visited => visit the neighbor
			if(!visitedNodes.contains(neighbor)){
					allPaths.add(path);
					ArrayList<GraphNode[]> neighborPaths =getMinDFSPath(neighbor, visitedNodes);
					allPaths.addAll(neighborPaths);
			}
		}
		return allPaths;
	}
	
}


/**
 * A prototype representing the nodes in the candidate graph
 * @author Lakshmi Ravi
 * @author Manasi 
 *
 */
class GraphNode implements Comparable<GraphNode>{
	String type;	 
	Long nodeId;
	ArrayList<GraphNode> edges = new ArrayList<GraphNode>();
	
	public GraphNode(Node node){
		this.type = node.getLabels().iterator().next().name();
		this.nodeId = node.getId();
	}
	
	public GraphNode(GraphNode n)
	{
		this.type = n.type;
		this.nodeId = n.nodeId;
	}

	public void extendNode(GraphNode otherNode){
		edges.add(otherNode);
	}
	
	
	
	@Override
	public String toString(){
		return type+"-"+nodeId;
	}
	
	@Override
	public boolean equals(Object otherNode){
		return nodeId.equals(((GraphNode)otherNode).nodeId);
	}
	
	@Override
	public int hashCode(){
		return nodeId.intValue();
	}

	public int compareTo(GraphNode arg0) {
			return (int) (this.nodeId- arg0.nodeId);
	}
}

