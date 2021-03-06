import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

public class FrequentSubgraphMining {

	GraphDatabaseService db;
	File dbpath;
	HashSet<Traverser> connectedComponents = new HashSet();
	HashSet visitedNodes = new HashSet();
	HashSet<TreeNode> candidates = new HashSet();
	HashSet<String> graphCandidates = new HashSet();
	HashSet<String> newgraphCandidates = new HashSet();
	HashSet<Node> nodesUsed = new HashSet();
	ArrayList<HashSet<String>> generatedCandidates = new ArrayList();

	public static void main(String[] args) {
		FrequentSubgraphMining dbInstance = new FrequentSubgraphMining();
		dbInstance.initCandidates();
	//	dbInstance.createCandidates();
	//	dbInstance.getDifferentComponents();
	//	System.out.println("Number of connected components: "+dbInstance.connectedComponents.size());
	//	dbInstance.generateCandidates();
	}
	
	
	public FrequentSubgraphMining(){
		dbpath = new File("D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db");
		db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbpath).setConfig(GraphDatabaseSettings.pagecache_memory, "2G" ).setConfig(GraphDatabaseSettings.string_block_size, "60" ).setConfig(GraphDatabaseSettings.array_block_size, "50" ).newGraphDatabase();
	}
	
	
	/**
	 * Frequent edge is one which will lead to good support while isomorphism count check.
	 * As long as we don't connect two nodes in candidate generation tree, they can be considered as different connected components
	 * Get frequent type of edge from the graph and select one instance of it to start with.
	 * get next frequent type of edge in remaining set of connected components and add it to tree 
	 */
	
	/**
	 * Get different connected components from the graph
	 * @param candidateTree
	 */	
	public void getDifferentComponents(){
		Transaction tx = db.beginTx();		
		ResourceIterator<Node> nodes = db.getAllNodes().iterator();
		while(nodes.hasNext()){
			Node node = nodes.next();
			if(! visitedNodes.contains(node.getId())){
			TraversalDescription traversalPlacesFriendsVisited =
					db.traversalDescription().depthFirst()
					 .relationships(RelationshipType.withName("nearby")) 
					 .relationships(RelationshipType.withName("friend"))
					 .relationships(RelationshipType.withName("visited"))					 
					 .evaluator(Evaluators.toDepth(100)); 
			Traverser traverser = traversalPlacesFriendsVisited.traverse(node);
			HashSet currentNodes = new HashSet();
			boolean addPath = true;
			for ( Node foundNode : traverser.nodes() )
			{
				//Not sure if this check is necessary. Pattern can have many different generation paths but only onee such path should be followed
				if(!visitedNodes.contains(node))
					currentNodes.add(foundNode.getId());
				else{
					addPath = false;
					break;
				}
			}
				if(addPath){
					visitedNodes.addAll(currentNodes);
					connectedComponents.add(traverser);
					if(connectedComponents.size() > 5){
						break;
					}
					System.out.println(traverser.metadata().getNumberOfPathsReturned());
				}
				System.gc();
			}
		}		
		tx.success();
		tx.close();		
	}
	
	public void initCandidates(){
		TreeNode tree = new TreeNode("null");
		Transaction tx = db.beginTx();
		ResourceIterable<Relationship> relationships = db.getAllRelationships();
		for(Relationship relation: relationships){
			Node startNode = relation.getStartNode();
			Node endNode = relation.getEndNode();
			String start = startNode.getLabels().iterator().next().name();
			String end = endNode.getLabels().iterator().next().name();
			graphCandidates.add(start+"-"+end);
			graphCandidates.add(end+"-"+start);
		}
		for(String cand: graphCandidates){
			tree.addChild(cand);
			System.out.println(cand);
		}
		tx.success();
		tx.close();
		generatedCandidates.add(graphCandidates);
	}
	
	
	public void createCandidates(TreeNode treeRoot){
		Transaction tx = db.beginTx();
		ResourceIterable<Node> all = db.getAllNodes();
		HashSet<Node> nodes = new HashSet();
		for(Node node: all){
			nodes.add(node);
		}
		int k = 0;
		TreeNode temp = treeRoot;
		while(k < 10){
		HashSet<String> nextCandidates = new HashSet();
		ArrayList<TreeNode> candi = treeRoot.children;
		for(TreeNode candidate: candi){
			String nodeLabels[] = candidate.type.split("-");
			ResourceIterator<Node> nextNodes = db.findNodes(Label.label(nodeLabels[nodeLabels.length-1]));
			while(nextNodes.hasNext()){
				Node next = nextNodes.next();
				Iterable<Relationship> relations = next.getRelationships();
				for(Relationship relation: relations){
					Node other = relation.getOtherNode(next);
				//	if(!nodesUsed.contains(other)){
					String newCandidate = candidate.type.concat("-"+other.getLabels().iterator().next().name());
					nextCandidates.add(newCandidate);
					System.out.println(newCandidate);
					nodesUsed.add(other);
				//	}
				}				
			}
			for(String childnode: nextCandidates){
				
			}
		}
		generatedCandidates.add(nextCandidates);
		k++;
		}
		tx.success();
		tx.close();
	}
	
	/*while(backNodes.hasNext()){
	Node previous = backNodes.next();
	Iterable<Relationship> relations = previous.getRelationships();
	for(Relationship relation: relations){
		Node other = relation.getOtherNode(previous);
		if(!nodesUsed.contains(other)){
			String newCandidate = reverseCode(candidate).concat("-"+other.getLabels().iterator().next().name());
			nextCandidates.add(newCandidate);
			System.out.println(newCandidate);
			nodesUsed.add(other);
		}
	}
}*/
	
	public String reverseCode(String str){
		String output[] = str.split("-");
		String o = "";
		for(int i = 0; i < output.length; i++){
			o = o +"-"+ output[output.length - i - 1];
		}
		return o;
	}
	
	/**
	 * Generates candidates by using different traverser obtained from data graph
	 * Traverser has multiple paths. So there is a rootNode for a traverser 
	 */
	public void generateCandidates(){		
		Transaction tx = db.beginTx();
		for(Traverser component: connectedComponents){
			TreeNode treeNode = null;
			TreeNode rootNode = null;
			ResourceIterator<Path> paths = component.iterator();
			int ID = 0;
			while(paths.hasNext()){
				Path path = paths.next();
				Iterator<Node> nodes = path.nodes().iterator();
				//for multiple paths from a traverser, start node always remains the same 
				Node startNode = nodes.next();
				if(treeNode == null && rootNode == null){
					treeNode = new TreeNode(startNode.getLabels().iterator().next().name());
					rootNode = treeNode;
				}
				else{
					treeNode = rootNode;
				}
				while(nodes.hasNext()){
					Node node = nodes.next();
					String type = node.getLabels().iterator().next().name();
					boolean nodeFound = false;
					for(TreeNode child: treeNode.children){
						if(child.type.equals(type)){
							treeNode = child;
							nodeFound = true;
							break;
						}
					}
					if(!nodeFound){
						treeNode.children.add(new TreeNode(type));
					}
				}								
			}
			candidates.add(rootNode);
			System.out.println(rootNode.getCannonicalCode());
		}
		tx.close();
	}	
	
}

class TreeNode{
	ArrayList<TreeNode> children = new ArrayList();
	//int ID;
	String type;
	ArrayList<ArrayList> occurrenceList = new ArrayList();
	
	TreeNode(String type){
		this.type = type;
	}
	
	public boolean hasNext(){
		return children.size() != 0;
	}
	
	public void addChild(String newType){
		TreeNode temp = this;
		boolean nodeFound = false;
		for(TreeNode child: temp.children){
			if(child.type.equals(newType)){
				temp = child;
				nodeFound = true;
				break;
			}
		}
		if(!nodeFound){
			children.add(new TreeNode(newType));
		}
	}
	
	public String getCannonicalCode(){
		String output = " "+type;
		for(TreeNode node: children){
			output+=node.getCannonicalCode();
		}
		return output;
	}
}

