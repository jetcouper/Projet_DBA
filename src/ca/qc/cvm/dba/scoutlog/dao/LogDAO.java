package ca.qc.cvm.dba.scoutlog.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.qc.cvm.dba.scoutlog.entity.LogEntry;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
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
	 * Note : Ne changer pas la structure de la méthode! Elle
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

			// Les planètes et galaxies sont des nœuds séparés
//			(:Planete {nom, habitable})
//			(:Galaxie {nom})
//
//			// Relations
//			(logExploration)-[:CONCERNE_PLANETE]->(planete)
//			(logExploration)-[:PROCHE_DE]->(autrePlanete)      // nearPlanets
//			(planete)-[:DANS_GALAXIE]->(galaxie)
//			(planete)-[:ROUTE]->(autrePlanete) //Distance

//			Node concept1a = getConceptNode(log.);
//			Node concept2a = getConceptNode(concept2);
//
//			if (concept1a == null) {
//				concept1a = createNode(concept1);
//			}
//			if (concept2a == null) {
//				concept2a = createNode(concept2);
//			}
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
					//(:Log:LogExploration {date, commandant, status, reasons, habitable})
//					Node concept1a = getConceptNode(concept1);
//					Node concept2a = getConceptNode(concept2);
//
//					if (concept1a == null) {
//						concept1a = createNode(concept1);
//					}
//					if (concept2a == null) {
//						concept2a = createNode(concept2);
//					}
//
//					Relationship r = getRelationship(concept1a,concept2a,link);
//
//					if (r == null) {
//						Map<String, Object> params = new HashMap<String, Object>();
//						params = new HashMap<String, Object>();
//						params.put("p1", concept1a.get("nom"));
//						params.put("p2", concept2a.get("nom"));
//						params.put("p3", link);
//						session.run("MATCH (a:Concept),(b:Concept) WHERE a.nom = $p1 AND b.nom = $p2 CREATE (a)-[r:LIEN { desc : $p3 } ]->(b)", params);
//						alreadyKnow = false;
//					}


					Map<String, Object> params3 = new HashMap<String, Object>();
					params3.put("p1", log.getDate());
					params3.put("p2", log.getName());
					params3.put("p3", log.getStatus());
					params3.put("p4", log.getNearPlanets()); //Ne pas mettre dans cette planet
					params3.put("p5", log.getReasons());
					params3.put("p6", log.getPlanetName());
					params3.put("p7", log.getGalaxyName()); //Ne pas mettre dans planet
					params3.put("p8", log.isHabitable());
					session.run("CREATE (l:Log {date:$p1, commandant:$p2, status:$p3 ,reason:$p5, planetName:$p6, isHabitable:$p8, displayName:$p6})", params3);

					if (!checkIfExist(log.getPlanetName())){
						session.run("CREATE (l:Log {planetName:$p6})",params3);
					}
					else{

					}

					if (log.getImage() != null) {
						// l'entrée possède une image!
						Database connection = BerkeleyConnection.getConnection();
						String cle = log.getPlanetName();
						byte[] data = log.getImage();

						try {
							DatabaseEntry theKey = new DatabaseEntry(cle.getBytes("UTF-8"));
							DatabaseEntry theData = new DatabaseEntry(data);
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

	private static Node createNode(String concept) {
		Node conceptRetour = null;

		try {
			Session session = Neo4jConnection.getConnection();
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("p1", concept);
			session.run("CREATE (l:Log {nom: $p1})", params);

			StatementResult result = session.run("MATCH (n:Concept) WHERE n.nom = $p1 RETURN n",
					params);

			while(result.hasNext()) {
				Record record = result.next();
				conceptRetour = record.get("n").asNode();
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return conceptRetour;
	}
	private static boolean checkIfExist(String planet) {
		Node node = null;
		boolean isExist = false;

		try {
			Session session = Neo4jConnection.getConnection();

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("p1", planet);

			StatementResult result = session.run("MATCH (l:Log) WHERE l.planetName = $p1 RETURN l",
					params);

			while(result.hasNext()) {
				Record record = result.next();
				node = record.get("l").asNode();
				System.out.println(node.get("planetName").asString());
				isExist = true;
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}
		return isExist;
	}
	public static Relationship getRelationship(Node concept1, Node concept2, String relation) {
		Relationship r = null;
		try {
			Session session = Neo4jConnection.getConnection();
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("p1", concept1.get("planetName"));
			params.put("p2", concept2.get("planetName"));
			params.put("p3", relation);

			StatementResult result = session.run("MATCH (n1)-[l:CONCERNE_PLANETE]->(n2) WHERE n1.nom=$p1 and n2.nom=$p2 and l.desc=$p3 RETURN DISTINCT l",
					params);

			while(result.hasNext()) {
				Record record = result.next();
				r = record.get("l").asRelationship();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}


		return r;
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

		//Session session = Neo4jConnection.getConnection();

		//StatementResult result = session.run("MATCH (l:Log) RETURN ");

		// Exemple...
		planets.add("Terre");
		planets.add("Solaria");
		planets.add("Dune");
		
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
		return null;
	}
	
	/**
	 * Permet de supprimer une entrée, selon sa position
	 *  
	 * @param position de l'entrée, identique à getLogEntryByPosition
	 * @return
	 */
	public static boolean deleteLog(int position) {
		boolean success = false;
		
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
		return 0;
	}
	
	/**
	 * Retourne le nombre de planètes habitables
	 * 
	 * @return nombre total
	 */
	public static int getNumberOfHabitablePlanets() {
		return 0;
	}
	
	/**
	 * Retourne entre 0 et 100 la moyenne d'entrées de type exploration sur le
	 * nombre total d'entrées
	 * 
	 * @return moyenne, entre 0 et 100
	 */
	public static int getExplorationAverage() {
		return 0;
	}

	
	/**
	 * Retourne le nombre de photos sauvegardées
	 * 
	 * @return nombre total
	 */
	public static int getPhotoCount() {
		return 0;
	}
	

	/**
	 * Retourne le nom des dernières planètes explorées
	 * 
	 * @param limit nombre à retourner
	 * @return
	 */
	public static List<String> getLastVisitedPlanets(int limit) {
		List<String> planetList = new ArrayList<String>();
				
		return planetList;
	}
	
	/**
	 * Permet de trouver la galaxie avec le plus grand nombre de planètes habitables
	 * 
	 * @return le nom de la galaxie
	 */
	public static String getBestGalaxy() {
		return "";
	}
	
	/**
	 * Permet de trouver un chemin pour se rendre d'une planète à une autre
	 * 
	 * @param fromPlanet
	 * @param toPlanet
	 * @return Liste du nom des planètes à parcourir, incluant "fromPlanet" et "toPlanet", ou null si aucun chemin trouvé
	 */
	public static List<String> getTrajectory(String fromPlanet, String toPlanet) {
		
		return null;
	}

	/**
	 * La liste des galaxies ayant le plus de planètes explorées (en ordre décroissant)
	 * 
	 * @param limit Nombre à retourner
	 * @return List de nom des galaxies + le nombre de planètes visitées, par exemple : Andromède (7 planètes visitées), ...
	 */	
	public static List<String> getExploredGalaxies(int limit) {
		List<String> galaxyList = new ArrayList<String>();

		return galaxyList;
	}
	
	/**
	 * Suppression de toutes les données
	 */
	public static boolean deleteAll() {
		boolean success = false;
		
		return success;
	}
}
