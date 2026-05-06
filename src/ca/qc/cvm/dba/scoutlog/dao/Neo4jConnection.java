package ca.qc.cvm.dba.scoutlog.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

public class Neo4jConnection {
	private static Driver graphDB;
	private static Session session;

	/**
	 * Méthode permettant d'obtenir une connexion
	 * @return une connexion
	 */
	public static Session getConnection() {		
		if (graphDB == null) {
			graphDB = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "AAAaaa111") );
			session = graphDB.session();
		}
		
		return session;
	}
}
