package nl.utwente.semanticweb.moviecrawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetImpl;

public class SparqlQuery {

	final static String OWL 		= "PREFIX owl: <http://www.w3.org/2002/07/owl#> ";
	final static String XSD 		= "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";
	final static String RDFS 		= "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>";
	final static String RDF 		= "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";
	final static String FOAF 		= "PREFIX foaf: <http://xmlns.com/foaf/0.1/> ";
	final static String DC 			= "PREFIX dc: <http://purl.org/dc/terms/> "; //modify this to follow 'dc' in linkedmdb
	final static String DEFAULT 	= "PREFIX : <http://dbpedia.org/resource/> ";
	final static String DBPEDIA2 	= "PREFIX dbpedia2: <http://dbpedia.org/property/> ";
	final static String DBPEDIA 	= "PREFIX dbpedia: <http://dbpedia.org/> ";
	final static String SKOS 		= "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> ";
	final static String DBO 		= "PREFIX dbo: <http://dbpedia.org/ontology/>";
	final static String DBPEDIA_NL	= "PREFIX dbpedia-nl: <http://nl.dbpedia.org/resource/> ";
	final static String PROP_NL		= "PREFIX prop-nl: <http://nl.dbpedia.org/property/> ";
	final static String DBPEDIA_DE	= "PREFIX dbpedia-de: <http://de.dbpedia.org/resource/> ";
	final static String PROP_DE		= "PREFIX prop-de: <http://de.dbpedia.org/property/> ";
	final static String MOVIE		= "PREFIX movie: <http://data.linkedmdb.org/resource/movie/> ";
	
	final static String PREFIXES_EN = OWL + XSD + RDFS + RDF + FOAF + DC + DEFAULT + MOVIE +
								   DBPEDIA + DBPEDIA2 + SKOS + DBO;
	final static String PREFIXES_NL = PREFIXES_EN + DBPEDIA_NL + PROP_NL;
	final static String PREFIXES_DE = PREFIXES_EN + DBPEDIA_DE + PROP_DE;
	
	
	public static String movieInDbpediaEn(String moviename) {
		//sanitize the title: remove "3D"
//		moviename = moviename.replace(" 3D","").replace(" (NL)", "");
//		System.out.println("-- querying movie: " + moviename);

		//just in case there are more than one resources with the same label
		String mn = moviename.replace(" ", "_");
//		Pattern p = Pattern.compile(".*" + mn + "_\\(film\\)$|.*" + mn + "_\\(19(8|9)[0-9]_film\\)$|.*" + mn + "_\\(201(4|5|6)_film\\)$*", Pattern.CASE_INSENSITIVE); //ends with either "(film)" or "(????_film)"
		Pattern p = Pattern.compile(".*" + mn + "_\\(film\\)$|.*" + mn + "_\\(201(4|5|6)_film\\)$*", Pattern.CASE_INSENSITIVE); //ends with either "(film)" or "(????_film)"
		
		String queryString = PREFIXES_EN + 
				"SELECT DISTINCT ?movie WHERE {" +
				"SERVICE <http://DBpedia.org/sparql> "+
				"{ " + 
				"?movie a dbo:Film ." +
				"?movie rdfs:label ?title ." +
//				"filter regex( str(?title), \"^" + moviename  + "$|^" + moviename + " \\\\(film\\\\)|^" + moviename + " \\\\(199[0-9] film\\\\)|^" + moviename + " \\\\(20(0|1)[0-9] film\\\\)" + "\", \"i\") " +
				"filter regex( str(?title), \"^" + moviename  + "$|^" + moviename + " \\\\(film\\\\)|^" + moviename + " \\\\(201(4|5|6) film\\\\)" + "\", \"i\") " +
				"}" +
				"} LIMIT 5";
//		
		
		/**
		 * TEMPORARY!!!!
		 */
//		String queryString = PREFIXES_EN + 
//				"SELECT DISTINCT ?movie WHERE {" +
//				"SERVICE <http://DBpedia.org/sparql> "+
//				"{ " + 
//				"?movie a dbo:Film ." +
//				"?movie rdfs:label ?label ." +
//				"?movie dbo:cinematography <http://dbpedia.org/resource/Dick_Pope_(cinematographer)> ." +
//				"filter regex( str(?label), \"Legend\", \"i\")" +
//				"FILTER (langMatches(lang(?label),\"en\"))" +
//				"}" +
//				"} ";
//
		/**
		 * End temporary
		 */
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet results = qe.execSelect();

		//querying movie is a little bit tricky. Dbpedia has inconsistent labeling for movies. For example, old movies/franchises/movies which using popular words as title
		//uses suffix "(film)". Others simply don't use it. 
		
		List<String> itemList = new ArrayList<>();
		int count = 0;
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			itemList.add(sol.getResource("movie").getURI()); 
			count++;
		}
		
		if(count == 1) {
			System.out.println(" Found (" + itemList.get(0) +")");
			return itemList.get(0);
		} if(count > 1) {
			System.out.print("-- crap, got multiple results for movie:\"" + moviename + "\".... ");
			//see if it ends with "(film)" or "(201x film)"
			for(String item : itemList) {
				if(p.matcher(item).find())
					System.out.println(" Found (" + item +")");
					return item;
			}
		}		
		System.out.println("not found.");
		return null;
	}

	
	/**
	 * get all people from a movie (directors, actors, producers, composers, editors)
	 * @param mRes
	 * @return
	 */
	public static List<Set<String>> getPerson(String mRes) {
		HashSet<String> directors = new HashSet<>();
		HashSet<String> actors = new HashSet<>();
		HashSet<String> producers = new HashSet<>();
		HashSet<String> composers = new HashSet<>();
		HashSet<String> editors = new HashSet<>();
		HashSet<String> writers = new HashSet<>();
		HashSet<String> cinematographers = new HashSet<>();
		
		List<Set<String>> results = new ArrayList<>(); 
				
		String queryString = PREFIXES_EN + 
			  	 "SELECT DISTINCT ?actor ?director ?producer ?composer ?editor ?writer ?cinematographer WHERE {"+
		  			"SERVICE <http://DBpedia.org/sparql> "+
		  			"{ " + 
			  		"OPTIONAL { <" + mRes + "> dbo:starring ?actor . }" +
			  		"OPTIONAL { <" + mRes + "> dbo:director ?director . }" +
			  		"OPTIONAL { <" + mRes + "> dbo:producer ?producer . }" +
			  		"OPTIONAL { <" + mRes + "> dbo:musicComposer ?composer . }" +
			  		"OPTIONAL { <" + mRes + "> dbo:editing ?editor . }" +
			  		"OPTIONAL { <" + mRes + "> dbo:writer ?writer . }" +
			  		"OPTIONAL { <" + mRes + "> dbo:cinematography ?cinematographer . }" +
		  			" } " +
			  		"}";
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet rs = qe.execSelect();

		while(rs.hasNext()) {
			QuerySolution sol = rs.nextSolution();
			if(sol.contains("director"))
				directors.add(sol.getResource("director").getURI());
			if(sol.contains("actor"))
				actors.add(sol.getResource("actor").getURI());
			if(sol.contains("producer"))
				producers.add(sol.getResource("producer").getURI());
			if(sol.contains("composer"))
				composers.add(sol.getResource("composer").getURI());
			if(sol.contains("writer"))
				writers.add(sol.getResource("writer").getURI());
			if(sol.contains("cinematographer")) {
				try {
					cinematographers.add(sol.getResource("cinematographer").getURI());
				} catch(ClassCastException e) {
					System.out.println("!! [Error]: cannot cast Literal to Resource: " + e);
				}
			}
		}
		
		results.add(directors);
		results.add(actors);
		results.add(producers);
		results.add(composers);
		results.add(editors);
		results.add(writers);
		results.add(cinematographers);
		
		return results;
	}
	
	
	/**
	 * 
	 * @param mRes
	 * @return
	 */
	public static HashMap<String, String> getMovieInfo(String mRes) {
		HashMap<String, String> results = new HashMap<>();
		
		String queryString = PREFIXES_EN + 
			  	 "SELECT DISTINCT ?runtime ?distributor ?studio ?budget ?country ?abstract WHERE {"+
		  			"SERVICE <http://DBpedia.org/sparql> "+
		  			"{ " + 
			  		"OPTIONAL { <" + mRes + "> dbo:Work\\/runtime ?rt . " + 
			  			"BIND (str(?rt) AS ?runtime)" +
			  		"}" +
			  		"OPTIONAL { <" + mRes + "> dbo:distributor ?distributor . }" +
			  		"OPTIONAL { <" + mRes + "> dbpedia2:studio ?studio . }" +
			  		"OPTIONAL { <" + mRes + "> dbo:budget ?bud . " + 
			  			"BIND (str(?bud) AS ?budget)" +
			  		"}" +
			  		"OPTIONAL { <" + mRes + "> dbpedia2:country ?c .  " + 
			  			"FILTER(lang(?c) = \"en\")" +
			  			"BIND (str(?c) AS ?country)" +
			  		"}" +
			  		"OPTIONAL { <" + mRes + "> dbo:abstract ?abs .  " +
			  			"FILTER(lang(?abs) = \"en\")" +
			  			"BIND (str(?abs) AS ?abstract)" +
			  		"}" +
		  			" } " +
			  		"}";

		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet rs = qe.execSelect();

		while(rs.hasNext()) {
			QuerySolution sol = rs.nextSolution();
			if(sol.contains("runtime"))
				results.put("runtime", sol.getLiteral("runtime").toString());
			if(sol.contains("distributor"))
				results.put("distributor", sol.getResource("distributor").getURI());
			if(sol.contains("studio")) {
				try {
					results.put("studio", sol.getResource("studio").getURI());
				} catch(Exception e) {
					System.out.println("!! [Error]: studio: " + e);
				}
				
			}
			if(sol.contains("budget"))
				results.put("budget", sol.getLiteral("budget").toString());
			if(sol.contains("country"))
				results.put("country", sol.getLiteral("country").toString());
			if(sol.contains("abstract"))
				results.put("abstract", sol.getLiteral("abstract").toString());
		}

		//get release date from linkedmdb
//		String queryString2 = PREFIXES_EN + 
//				"SELECT ?date WHERE {" +
//					"SERVICE <http://data.linkedmdb.org/sparql> {" +
//						"?movie owl:sameAs <" + mRes + "> ." +
//						"?movie movie:initial_release_date ?date ." +
//					"}" +
//				"}";
//		Query query2 = QueryFactory.create(queryString2);
//		QueryExecution qe2 = QueryExecutionFactory.create(query2, new DatasetImpl(ModelFactory.createDefaultModel()));
//		ResultSet rs2 = qe2.execSelect();
//		
//		while(rs2.hasNext()) {
//			QuerySolution sol = rs2.nextSolution();
//			if(sol.contains("date"))
//				results.put("releaseDate", sol.getLiteral("date").toString());				
//		}

		return results;
	}
	
	
	/**
	 * 
	 * @param pRes
	 * @return
	 */
	public static HashMap<String, String> getPersonalInfo(String pRes) {
		HashMap<String, String> results = new HashMap<>();
		String queryString = PREFIXES_EN + 
			  	 "SELECT DISTINCT ?birthdate ?nationality WHERE {"+
		  			"SERVICE <http://DBpedia.org/sparql> "+
		  			"{ " + 
			  		"OPTIONAL { <" + pRes + "> dbo:birthDate ?bd . " + 
			  			"BIND(str(?bd) AS ?birthdate)" +
			  		"}" +
			  		"OPTIONAL { <" + pRes + "> dbpedia2:nationality ?nat . "+
			  			"FILTER(lang(?nat) = \"en\")" +
			  			"BIND(str(?nat) AS ?nationality)" +
			  		"}" +
		  			"} " +
			  		"}";

		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet rs = qe.execSelect();

		while(rs.hasNext()) {
			QuerySolution sol = rs.nextSolution();
			if(sol.contains("birthdate"))
				results.put("birthdate", sol.getLiteral("birthdate").toString());
			if(sol.contains("nationality"))
				results.put("nationality", sol.getLiteral("nationality").toString());
		}

		return results;
	}
}
