import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class LoadData {
	
	File dbpath;
	static HashMap<String, Long> offset = new HashMap();
	BatchInserter insert;
	
	static{
		offset.put("user", new Long(80000000));
		offset.put("venue", new Long(90000000));
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoadData loadData = new LoadData();
		loadData.loadDataToNeo4j();
	}
	
	public LoadData(){
		dbpath = new File("D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db");
	}
	
	public void loadDataToNeo4j(){
		try {
			Map<String, String> config = new HashMap();
			config.put("cache_type","weak");
			config.put("use_memory_mapped_buffers", "true");
		    config.put( "neostore.nodestore.db.mapped_memory", "3G" );
		    config.put( "neostore.relationshipstore.db.mapped_memory", "2G" );
		    config.put( "neostore.propertystore.db.mapped_memory", "500M" );
		    config.put( "neostore.propertystore.db.strings.mapped_memory", "500M" );
		    config.put( "neostore.propertystore.db.arrays.mapped_memory", "500M" );
		    config.put( "neostore.propertystore.db.index.keys.mapped_memory", "5M");	
			config.put( "dbms.jvm.additional", "XX:+UseG1GC");
			insert = BatchInserters.inserter(dbpath);
			readUserData();
			readVenueData();
			createRatingRelationship();
			createFriendRelationship();
			insert.shutdown();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void readUserData(){
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource("users.csv").getFile());
			BufferedReader fileReader = new BufferedReader(new FileReader(file));
			String line;
			fileReader.readLine();
			while((line = fileReader.readLine()) != null){
				String[] values = line.split(",");
				//
				
				if(values.length == 3){
					HashMap<String, Object> valuesMap = new HashMap();					
					Long locationId = getLocationId(values[1], values[2], valuesMap);
					if(!insert.nodeExists(locationId)){
						insert.createNode(locationId, valuesMap, Label.label("location"));
					}
					insert.createNode(Long.parseLong(values[0])+offset.get("user"), null, Label.label("user"));
					insert.createRelationship(Long.parseLong(values[0])+offset.get("user"), locationId, RelationshipType.withName("nearby"), null);
				}
			}
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Long getLocationId(String lat, String lon, HashMap valuesMap ){
		Double longitude = Double.parseDouble(lon) + 180;
		DecimalFormat df = new DecimalFormat("#.#");
		longitude = Double.parseDouble(df.format(longitude));
	//	System.out.println(longitude);
		Double latitude = Double.parseDouble(lat) + 90;
		latitude = Double.parseDouble(df.format(latitude));
//		System.out.println(latitude);
		Long locationId = (long) (longitude * 100000 + latitude * 10);
		valuesMap.put("latitude", latitude);
		valuesMap.put("longitude", longitude);
	//	System.out.println(locationId);
		return locationId;
	}
	
	public void readVenueData(){
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource("venues.csv").getFile());
			BufferedReader fileReader = new BufferedReader(new FileReader(file));
			String line;
			fileReader.readLine();
			while((line = fileReader.readLine()) != null){
				String[] values = line.split(",");
				if(values.length == 3){
					HashMap<String, Object> valuesMap = new HashMap();					
					Long locationId = getLocationId(values[1], values[2], valuesMap);
					if(!insert.nodeExists(locationId)){
						insert.createNode(locationId, valuesMap, Label.label("location"));
					}					
					insert.createNode(Long.parseLong(values[0])+offset.get("venue"), null, Label.label("venue"));
					insert.createRelationship(Long.parseLong(values[0])+offset.get("venue"), locationId, RelationshipType.withName("nearby"), null);
				}
			}
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void createRatingRelationship(){
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource("ratings.csv").getFile());
			BufferedReader fileReader = new BufferedReader(new FileReader(file));
			String line;
			fileReader.readLine();
			String[] values = fileReader.readLine().split(",");
			while(true){				
				Long id1 = Long.parseLong(values[0]);
				Long id2 = Long.parseLong(values[1]);
				ArrayList<String> ratings = new ArrayList(); 
				ratings.add(values[2]);
				while((line = fileReader.readLine()) != null){
					values = line.split(",");
					if(values[0].equals(id1+"") && values[1].equals(id2+"")){
						ratings.add(values[2]);
					}
					else{
						break;
					}
				}
				HashMap<String, Object> valuesMap = new HashMap();
				String[] ratingsArray = new String[ratings.size()];
				ratingsArray = ratings.toArray(ratingsArray);
				valuesMap.put("ratings", ratingsArray);
				int sum=0, count=0;
				for(String rating: ratings){
					count++;
					sum+=Integer.parseInt(rating);
				}
				valuesMap.put("count", count);
				valuesMap.put("avg_rating", (float)sum/(float)count);
				if(insert.nodeExists(id1+offset.get("user")) && insert.nodeExists(id2+offset.get("venue"))){
					insert.createRelationship(id1+offset.get("user"), id2+offset.get("venue"), RelationshipType.withName("visited"), valuesMap);
				}
				if(line == null){
					break;
				}
			}	
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void createFriendRelationship(){
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource("socialgraph.csv").getFile());
			BufferedReader fileReader = new BufferedReader(new FileReader(file));
			String line;
			fileReader.readLine();
			while((line = fileReader.readLine()) != null){
				String[] values = line.split(",");
				Long id1 = Long.parseLong(values[0]) + offset.get("user");
				Long id2 = Long.parseLong(values[1]) + offset.get("user");
				if(insert.nodeExists(id1) && insert.nodeExists(id2)){
					insert.createRelationship(id1, id2, RelationshipType.withName("friend"), null);
				}
				//Ignoring alternate lines since edges are bidirectional
				fileReader.readLine();
			}			
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
