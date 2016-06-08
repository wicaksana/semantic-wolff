package nl.utwente.semanticweb.wolffcrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
	
	/**
	 * query en.dbpedia.org to search person
	 * @param resName
	 * @return resource name of the person in dbpedia.org. Otherwise, null.
	 */
	public static String personInDbpediaEn(String resName) {
		String queryString = PREFIXES_EN + 
				"SELECT DISTINCT ?person WHERE {" +
				"SERVICE <http://DBpedia.org/sparql> "+
				"{ " + 
				"?person a dbo:Person ." +
//				"?person foaf:name ?name  ." +
				"?person rdfs:label ?name ." +
				"filter regex( str(?name), \"^" + resName  + "$\", \"i\") " +
				"}" +
				"} LIMIT 5";
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet results = qe.execSelect();
		
		List<String> itemList = new ArrayList<>();
		int count = 0;
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			itemList.add(sol.getResource("person").getLocalName()); //for debugging purposes
			count++;
		}
		if(count == 1) {
			return itemList.get(0);
		} else if (count > 1) {
			System.out.println("-- found more than one resource in dbpedia for: " + resName);
			for(String item : itemList) 
				System.out.print(item + "-\t");
			System.out.println();
		} 
		//otherwise, not found 
		return null;

	}
	
	/**
	 * query de.dbpedia.org to find person
	 * @param resName
	 * @return resource name of the person in de.dbpedia.org. Otherwise, null
	 */
	public static String personInDbpediaDe(String resName) {
		System.out.print("-- looking at de.dbpedia for: " + resName);
		String queryString = PREFIXES_NL + 
				"SELECT DISTINCT ?person WHERE {" +
				"SERVICE <http://de.DBpedia.org/sparql> "+
				"{ " + 
				"?person rdfs:label \""+ resName +"\"@de ." +
				"}" +
				"} LIMIT 5";

		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet results = qe.execSelect();

		List<String> itemList = new ArrayList<>();
		int count = 0;
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			itemList.add(sol.getResource("person").getLocalName()); 
			count++;
		}
		
		if(count == 1) {
			System.out.print(" found.\n");
			return itemList.get(0);
		} 
		System.out.print(" not found.\n");
		
		
		
		return null;
	}

	/**
	 * query nl.dbpedia.org to find person
	 * @param resName
	 * @return resource name of the person in nl.dbpedia.org. Otherwise, null
	 */
	public static String personInDbpediaNl(String resName) {
		System.out.print("-- looking at nl.dbpedia for: " + resName);
		String queryString = PREFIXES_NL + 
				"SELECT DISTINCT ?person WHERE {" +
				"SERVICE <http://nl.DBpedia.org/sparql> "+
				"{ " + 
				"?person rdfs:label \""+ resName +"\"@nl ." +
				"}" +
				"} LIMIT 5";

		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet results = qe.execSelect();

		List<String> itemList = new ArrayList<>();
		int count = 0;
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			itemList.add(sol.getResource("person").getLocalName()); 
			count++;
		}
		
		if(count == 1) {
			System.out.print(" found.\n");
			return itemList.get(0);
		} 
		System.out.print(" not found.\n");
		return null;
	}
	
	/**
	 * 
	 * @param moviename
	 * @return
	 */
	public static String movieInDbpediaEn(String moviename) {
		//sanitize the title: remove "3D"
		moviename = moviename.replace(" 3D","").replace(" (NL)", "");
		System.out.println("-- querying movie: " + moviename);

		//just in case there are more than one resources with the same label
		String mn = moviename.replace(" ", "_");
		Pattern p = Pattern.compile(".*" + mn + "_\\(film\\)$|.*" + mn + "_\\(201(4|5|6)_film\\)$*", Pattern.CASE_INSENSITIVE); 
		
		String queryString = PREFIXES_EN + 
				"SELECT DISTINCT ?movie WHERE {" +
				"SERVICE <http://DBpedia.org/sparql> "+
				"{ " + 
				"?movie a dbo:Film ." +
				"?movie rdfs:label ?title ." +
				"filter regex( str(?title), \"^" + moviename  + "$|^" + moviename + " \\\\(film\\\\)|^" + moviename + " \\\\(201(4|5|6) film\\\\)" + "\", \"i\") " +
				"}" +
				"} LIMIT 5";
		
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet results = qe.execSelect();
		
		List<String> itemList = new ArrayList<>();
		int count = 0;
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			itemList.add(sol.getResource("movie").getURI()); 
			count++;
		}
		
//		System.out.println("count: " + count);
//		for(String item : itemList)
//				System.out.println("item: " + item);
		
		if(count == 1) {
			System.out.print(" found.\n");
			return itemList.get(0);
		} if(count > 1) {
			System.out.print("-- crap, got multiple results for movie:\"" + moviename + "\".... ");
			//see if it ends with "(film)" or "(201x film)"
			for(String item : itemList) {
				if(p.matcher(item).find()) {
					System.out.println(" Found (" + item +")");
					return item;
				}
			}
		}
		System.out.print(" not found.\n");		
		return null;
	}
	
	/**
	 * 
	 * @param movietitle
	 * @return
	 */
	public static String movieInDbpediaNl(String movietitle) {
		//sanitize the title: remove "3D"
		movietitle = movietitle.replace(" 3D","").replace(" (NL)", "");
		System.out.println("-- querying movie: " + movietitle);

		//just in case there are more than one resources with the same label
		String mn = movietitle.replace(" ", "_");
		Pattern p = Pattern.compile(".*" + mn + "_\\(film\\)$|.*" + mn + "_\\(201\\d_film\\)$", Pattern.CASE_INSENSITIVE); //ends with either "(film)" or "(201*_film)"
		
		String queryString = PREFIXES_NL + 
				"SELECT DISTINCT ?movie WHERE {" +
				"SERVICE <http://nl.DBpedia.org/sparql> "+
				"{ " + 
				"?movie a dbo:Film ." +
				"?movie prop-nl:titel ?title ." +
				"filter regex( str(?title), \"^" + movietitle  + "\", \"i\") " +
				"}" +
				"} LIMIT 5";
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet results = qe.execSelect();

		//querying movie is a little bit tricky. Dbpedia has inconsistent labeling for movies. For example, old movies/franchises/movies which using popular words as title
		//uses suffix "(film)". Others simply don't use it. 
		
		List<String> itemList = new ArrayList<>();
		int count = 0;
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			Resource movie = sol.getResource("movie");
			itemList.add(sol.getResource("movie").getURI()); 
			count++;
		}
		
//		System.out.println("count: " + count);
//		for(String item : itemList)
//				System.out.println("item: " + item);
		
		if(count == 1) {
			System.out.print(" found.\n");
			return itemList.get(0);
		} if(count > 1) {
			System.out.println("-- crap, got multiple results for movie.");
			//see if it ends with "(film)" or "(201x film)"
			for(String item : itemList) {
				if(p.matcher(item).find())
					return item;
			}
		}
		System.out.println(" movie not found: " + movietitle);		
		return null;
	}

	/**
	 * 
	 * @param movietitle
	 * @return
	 */
	public static String movieInDbpediaDe(String movietitle) {
		//sanitize the title: remove "3D"
		movietitle = movietitle.replace(" 3D","").replace(" (NL)", "");
		System.out.println("-- querying movie: " + movietitle);

		//just in case there are more than one resources with the same label
		String mn = movietitle.replace(" ", "_");
		Pattern p = Pattern.compile(".*" + mn + "_\\(film\\)$|.*" + mn + "_\\(201\\d_film\\)$", Pattern.CASE_INSENSITIVE); //ends with either "(film)" or "(201*_film)"
		
		String queryString = PREFIXES_DE + 
				"SELECT DISTINCT ?movie WHERE {" +
				"SERVICE <http://de.DBpedia.org/sparql> "+
				"{ " + 
				"?movie a dbo:Film ." +
				"?movie rdfs:label ?title ." +
				"filter regex( str(?title), \"^" + movietitle  + "\", \"i\") " +
				"}" +
				"} LIMIT 5";
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, new DatasetImpl(ModelFactory.createDefaultModel()));
		ResultSet results = qe.execSelect();

		//querying movie is a little bit tricky. Dbpedia has inconsistent labeling for movies. For example, old movies/franchises/movies which using popular words as title
		//uses suffix "(film)". Others simply don't use it. 
		
		List<String> itemList = new ArrayList<>();
		int count = 0;
		while(results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			Resource movie = sol.getResource("movie");
			itemList.add(sol.getResource("movie").getURI()); 
			count++;
		}
		
//		System.out.println("count: " + count);
//		for(String item : itemList)
//				System.out.println("item: " + item);
		
		if(count == 1) {
			System.out.print(" found.\n");
			return itemList.get(0);
		} if(count > 1) {
			System.out.println("-- crap, got multiple results for movie.");
			//see if it ends with "(film)" or "(201x film)"
			for(String item : itemList) {
				if(p.matcher(item).find())
					return item;
			}
		}
		System.out.println(" movie not found: " + movietitle);		
		return null;
	}
}
