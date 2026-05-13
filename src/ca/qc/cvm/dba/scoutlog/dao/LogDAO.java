package ca.qc.cvm.dba.scoutlog.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.qc.cvm.dba.scoutlog.entity.LogEntry;
import com.sleepycat.je.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Statement;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

public class LogDAO {
	/**
	 * Méthode permettant d'ajouter une entrée
	 * 
	 * Note : Ne changez pas la structure de la méthode ! Elle
	 * permet de faire fonctionner l'ajout d'une entrée du journal.
	 * Il faut donc que la compléter.
	 * 
	 * @param l'objet avec toutes les données de la nouvelle entrée
	 * @return si la sauvegarde a fonctionnée
	 */
	public static boolean addLog(LogEntry log) {
		boolean success = false;
		try{
			Session session = Neo4jConnection.getConnection();
			System.out.println(log.toString());
			switch (log.getStatus()){
				case "Normal":
					//(:Log {date, commandant, status})
					Map<String, Object> params = new HashMap<String, Object>();
					params.put("p1", log.getDate());
					params.put("p2", log.getName());
					params.put("p3", log.getStatus());
					session.run("CREATE (l:Log {date:$p1, commandant:$p2, status:$p3, displayName:$p2})", params);
					success = true;
					break;
				case "Anormal":
					//(:Log:LogAnormal {date, commandant, status, reasons})
					//Map<String, Object> params = new HashMap<String, Object>();
					Map<String, Object> params2 = new HashMap<String, Object>();
					params2.put("p1", log.getDate());
					params2.put("p2", log.getName());
					params2.put("p3", log.getStatus());
					params2.put("p4", log.getReasons());
					session.run("CREATE (l:Log {date:$p1, commandant:$p2, status:$p3, reason:$p4, displayName:$p2})", params2);
					success = true;
					break;
				case "Exploration":



					Map<String, Object> params3 = new HashMap<String, Object>();
					params3.put("p1", log.getDate());
					params3.put("p2", log.getName());
					params3.put("p3", log.getStatus());
					params3.put("p4", log.getNearPlanets()); //Ne pas mettre dans cette planet
					params3.put("p5", log.getReasons());
					params3.put("p6", log.getPlanetName());
					params3.put("p7", log.getGalaxyName()); //Ne pas mettre dans planet
					params3.put("p8", log.isHabitable());

					StatementResult nomPlanetExistante = session.run("MATCH (p:Planete) WHERE p.nom = $p6 RETURN p.nom", params3);

					if (nomPlanetExistante.hasNext()) {
						success = false;
						break;
					}
					else{
						session.run(
								"MERGE (g:Galaxie {nom:$p7}) " +
										"MERGE (p:Planete {nom:$p6}) " +
										"ON CREATE SET p.habitable=$p8, p.displayName=$p6 " +
										"CREATE (l:Log {date:$p1, commandant:$p2, status:$p3, reason:$p5, displayName:$p2}) " +
										"CREATE (l)-[:CONCERNE_PLANETE]->(p) " +
										"CREATE (p)-[:DANS_GALAXIE]->(g)",
								params3
						);
					}

                    if(log.getNearPlanets() != null){
                        for (String voisin : log.getNearPlanets()) {
                            Map<String, Object> routeParams = new  HashMap<>();
                            routeParams.put("p6", log.getPlanetName());
                            routeParams.put("voisin", voisin);
                            session.run("MATCH (p1:Planete {nom:$p6}), (p2:Planete {nom:$voisin}) " +
                                        "MERGE (p1)-[:ROUTE]->(p2)" +
									"MERGE (p2)-[:ROUTE]->(p1)", routeParams);
                        }
                    }

					if (log.getImage() != null) {
						// l'entrée possède une image!
						Database connection = BerkeleyConnection.getConnection();
						try {
							DatabaseEntry theKey = new DatabaseEntry(log.getPlanetName().getBytes("UTF-8"));
							DatabaseEntry theData = new DatabaseEntry(log.getImage());
							connection.put(null,theKey,theData);

						} catch (Exception e) {
							e.printStackTrace();
						}

					}
					success = true;
					break;
				default:
					System.out.println("Aucun statue");
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return success;
	}

	
	/**
	 * Permet de retourner la liste de planètes déjà explorées
	 * 
	 * Note : Ne changer pas la structure de la méthode! Elle
	 * permet de faire fonctionner l'ajout d'une entrée du journal.
	 * Il faut donc que la compléter.
	 * 
	 * @return le nom des planètes déjà explorées
	 */
	public static List<String> getPlanetList() {
		List<String> planets = new ArrayList<String>();

		try {
			Session session = Neo4jConnection.getConnection();

			StatementResult result = session.run("MATCH (p:Planete) RETURN p.nom AS nom");

			while(result.hasNext()) {
				Record record = result.next();
				planets.add(record.get("nom").asString());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return planets;
	}
	
	/**
	 * Retourne l'entrée selon sa position dans le temps.
	 * La dernière entrée est 0,
	 * l'avant dernière est 1,
	 * l'avant avant dernière est 2, etc.
	 * 
	 * Toutes les informations liées à l'entrée doivent être affectées à
	 * l'objet retourné.
	 * 
	 * 
	 * @param position (démarre à 0)
	 * @return
	 */
	public static LogEntry getLogEntryByPosition(int position) {
		LogEntry logEntry = null;
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			List<String> planets = null;
			Session session = Neo4jConnection.getConnection();
			params.put("p1", position);

			StatementResult result = session.run("MATCH (l:Log) " +
					"OPTIONAL MATCH (l:Log)-[:CONCERNE_PLANETE]->(p:Planete) " +
					"OPTIONAL MATCH (p)-[:DANS_GALAXIE]->(g:Galaxie)  " +
					"OPTIONAL MATCH (p)-[:ROUTE]->(voisin:Planete) " +
					"RETURN collect(voisin.nom) AS planeteProche, l.reason AS reason, l.date AS date, l.commandant AS commandant,l.status AS status, p.nom AS nomPlanet, p.habitable AS habitable, g.nom AS nomGalaxie " +
					"ORDER BY l.date DESC SKIP $p1 LIMIT 1", params);

			Record record = result.next();
			logEntry = new LogEntry(record.get("date").asString(), record.get("commandant").asString(),record.get("status").asString());
			if (!record.get("habitable").isNull()) {
				logEntry.setHabitable(record.get("habitable").asBoolean());
			}
			logEntry.setPlanetName(record.get("nomPlanet").asString());
			logEntry.setReasons(record.get("reason").asString());
			logEntry.setGalaxyName(record.get("nomGalaxie").asString());
			logEntry.setNearPlanets(record.get("planeteProche").asList(v -> v.asString()));

			if(record.get("status").asString().equals("Exploration")){
				Database connection = BerkeleyConnection.getConnection();
				try {
					DatabaseEntry theKey = new DatabaseEntry(record.get("nomPlanet").asString().getBytes("UTF-8"));
					DatabaseEntry theData = new DatabaseEntry();

					if (connection.get(null,theKey,theData,null) == OperationStatus.SUCCESS){
						logEntry.setImage(theData.getData());
					}


				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return logEntry;
	}
	
	/**
	 * Permet de supprimer une entrée, selon sa position
	 *  
	 * @param position de l'entrée, identique à getLogEntryByPosition
	 * @return
	 */
	public static boolean deleteLog(int position) {
		boolean success = false;

		try {
			Map<String, Object> params = new HashMap<String, Object>();
			Session session = Neo4jConnection.getConnection();
			params.put("p1", position);

			StatementResult result = session.run("MATCH (l:Log) " +
												"OPTIONAL MATCH (l:Log)-[:CONCERNE_PLANETE]->(p:Planete) " +
												"RETURN p.nom AS nomPlanete, l.status AS status " +
												"ORDER BY l.date DESC SKIP $p1 LIMIT 1", params);
			Record record = result.next();


			session.run("MATCH (l:Log) " +
					"OPTIONAL MATCH (l:Log)-[:CONCERNE_PLANETE]->(p:Planete) " +
					"WITH l " +
					"ORDER BY l.date DESC " +
					"SKIP $p1 " +
					"LIMIT 1 " +
					"DETACH DELETE l" , params);

			if(record.get("status").asString().equals("Exploration")){
				Database connection = BerkeleyConnection.getConnection();
				try {
					DatabaseEntry theKey = new DatabaseEntry(record.get("nomPlanete").asString().getBytes("UTF-8"));
					DatabaseEntry theData = new DatabaseEntry();

					if (connection.get(null,theKey,theData,null) == OperationStatus.SUCCESS){
						connection.delete(null,theKey);
					}


				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			success = true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return success;
	}
	
	/**
	 * Doit retourner le nombre d'entrées dans le journal de bord
	 * 
	 * Note : Ne changer pas la structure de la méthode! Elle
	 * permet de faire fonctionner l'affichage de la liste des entrées
	 * du journal. Il faut donc que la compléter.
	 * 
	 * @return nombre total
	 */
	public static int getNumberOfEntries() {
		int compte = 0;
		try {
			Session session = Neo4jConnection.getConnection();
			StatementResult result = session.run("MATCH (l:Log) RETURN COUNT(l) AS compte");
			compte = Integer.parseInt(result.next().get("compte").toString());

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return compte;
	}
	
	/**
	 * Retourne le nombre de planètes habitables
	 * 
	 * @return nombre total
	 */
	public static int getNumberOfHabitablePlanets() {

		int compte = 0;
		try {
			Session session = Neo4jConnection.getConnection();
			StatementResult result = session.run("MATCH (p:Planete) WHERE p.habitable = true RETURN COUNT(p) AS compte");
			compte = result.next().get("compte").asInt();

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return compte;
	}
	
	/**
	 * Retourne entre 0 et 100 la moyenne d'entrées de type exploration sur le
	 * nombre total d'entrées
	 * 
	 * @return moyenne, entre 0 et 100
	 */
	public static int getExplorationAverage() {

		int nbExploration = 0;

		try {
			Session session = Neo4jConnection.getConnection();
			StatementResult result = session.run("MATCH (l:Log) " +
													"WITH COUNT(l) as total " +
													"MATCH (e:Log {status: 'Exploration'}) " +
													"WITH COUNT(e) AS nbExploration, total " +
													"RETURN (nbExploration * 100 / total) AS PercentageExploration");

			nbExploration = (result.next().get("PercentageExploration")).asInt();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return nbExploration;
	}

	
	/**
	 * Retourne le nombre de photos sauvegardées
	 * 
	 * @return nombre total
	 */
	public static int getPhotoCount() {
		int photoCount = 0;
		Database connection = BerkeleyConnection.getConnection();
		Cursor cursor = connection.openCursor(null, null);
		try {
			DatabaseEntry theKey = new DatabaseEntry();
			DatabaseEntry theData = new DatabaseEntry();

			while (cursor.getNext(theKey,theData,null) == OperationStatus.SUCCESS){
				photoCount++;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cursor.close();
		}


		return photoCount;
	}
	

	/**
	 * Retourne le nom des dernières planètes explorées
	 * 
	 * @param limit nombre à retourner
	 * @return
	 */
	public static List<String> getLastVisitedPlanets(int limit) {
		List<String> planetList = new ArrayList<String>();

		try {

			Map<String, Object> params = new HashMap<String, Object>();
			Session session = Neo4jConnection.getConnection();
			params.put("p1", limit);

			StatementResult result = session.run("MATCH (l:Log) " +
					"MATCH (l)-[:CONCERNE_PLANETE]->(p:Planete) WHERE p IS NOT NULL " +
					"WITH l, p " +
					"RETURN p.nom AS nomPlanete " +
					"ORDER BY l.date DESC " +
					"LIMIT $p1 " , params);

			while(result.hasNext()) {
				Record record = result.next();
				planetList.add(record.get("nomPlanete").asString());
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return planetList;
	}
	
	/**
	 * Permet de trouver la galaxie avec le plus grand nombre de planètes habitables
	 * 
	 * @return le nom de la galaxie
	 */
	public static String getBestGalaxy() {
		String nomGalaxy = "";
		try {
			Session session = Neo4jConnection.getConnection();
			StatementResult result = session.run("MATCH (p:Planete)-[:DANS_GALAXIE]->(g:Galaxie) WHERE p.habitable = true RETURN g.nom AS nom, COUNT(p) AS nombre ORDER BY COUNT(p) DESC LIMIT 1");

			nomGalaxy = (result.next().get("nom")).asString();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return nomGalaxy;
	}
	
	/**
	 * Permet de trouver un chemin pour se rendre d'une planète à une autre
	 * 
	 * @param fromPlanet
	 * @param toPlanet
	 * @return Liste du nom des planètes à parcourir, incluant "fromPlanet" et "toPlanet", ou null si aucun chemin trouvé
	 */
	public static List<String> getTrajectory(String fromPlanet, String toPlanet) {
		List<String> planetes = new ArrayList<>();
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			Session session = Neo4jConnection.getConnection();
			params.put("p1", fromPlanet);
			params.put("p2", toPlanet);
			StatementResult result = session.run("MATCH (p1:Planete {nom: $p1}), (p2:Planete {nom: $p2}) \n" +
					"MATCH p = shortestPath((p1)-[*..]->(p2))\n" +
					"UNWIND nodes(p) AS planete\n" +
					"RETURN planete.nom AS nom",params);

			if(!result.hasNext()) {
				planetes = null;
			}
			while(result.hasNext()) {
				Record record = result.next();
				planetes.add(record.get("nom").asString());
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}


		return planetes;
	}

	/**
	 * La liste des galaxies ayant le plus de planètes explorées (en ordre décroissant)
	 * 
	 * @param limit Nombre à retourner
	 * @return List de nom des galaxies + le nombre de planètes visitées, par exemple : Andromède (7 planètes visitées), ...
	 */	
	public static List<String> getExploredGalaxies(int limit) {
		List<String> galaxyList = new ArrayList<String>();

		try {
			Map<String, Object> params = new HashMap<String, Object>();
			Session session = Neo4jConnection.getConnection();
			params.put("p1",limit);
			StatementResult result = session.run("MATCH (p:Planete)-[:DANS_GALAXIE]->(g:Galaxie)\n" +
					"RETURN g.nom AS nom, COUNT(p) AS nombre \n" +
					"ORDER BY COUNT(p) DESC \n" +
					"LIMIT $p1", params);

			while(result.hasNext()) {
				Record record = result.next();
				galaxyList.add(record.get("nom").asString() + " ( " + record.get("nombre").asInt() + " planètes visitées ), ");
			}


		}
		catch (Exception e) {
			e.printStackTrace();
		}


		return galaxyList;
	}
	
	/**
	 * Suppression de toutes les données
	 */
	public static boolean deleteAll() {
		boolean success = false;
		Transaction txn = null;
		Cursor cursor = null;
		try{
			Session session = Neo4jConnection.getConnection();
			session.run("MATCH (n) DETACH DELETE n");

			Database connection = BerkeleyConnection.getConnection();
			txn = connection.getEnvironment().beginTransaction(null, null);
			cursor = connection.openCursor(txn, null);

			DatabaseEntry key = new DatabaseEntry();
			DatabaseEntry data = new DatabaseEntry();

			while (cursor.getNext(key, data, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				cursor.delete();
			}
			cursor.close();
			txn.commit();
			success = true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}


		return success;
	}
}
