package nl.utwente.semanticweb.wolffcrawler;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class WolffCrawler {
	
	private final static String WOLFF = "http://www.wolff.nl";
	private final static String WOLFF_OWL = WOLFF + "/2016/wolff.owl";
	private final static String NS = WOLFF_OWL + "#";
	private final static String MOVIE = WOLFF_OWL + "/movie" + "#";
	private final static String PERSON = WOLFF_OWL + "/person" + "#";
	private final static String GENRE = WOLFF_OWL + "/genre" + "#";
	private final static String COMPANY = WOLFF_OWL + "/company" + "#";
	
	private final static String DBR = "http://dbpedia.org/resource/";
	private final static String DBR_NL = "http://nl.dbpedia.org/resource/";
	private final static String DBR_DE = "http://de.dbpedia.org/resource/";
	
	private final static String REF_ONT = "wolff.owl";
	private final static String OUTPUT = "wolff.rdf";
	
	private final OntModel m;
	
	private final static String[] DUTCH_MONTH = {"januari", "februari", "maart", "april", "mei", "juni", 
												 "juli", "augustus", "september", "oktober", "november", "december"};
	
	private final static Map<String, String> LANGUAGE;
	static {
		LANGUAGE = new HashMap<String, String>();
		LANGUAGE.put("engels", "English");
		LANGUAGE.put("nederlands", "Dutch");
		LANGUAGE.put("turks", "Turkish");
		LANGUAGE.put("duits", "German");
	}
	
	//to translate Dutch genre to English
	private static Map<String, String> GENRE_MAP;
	static {
		GENRE_MAP = new HashMap<String, String>();
		GENRE_MAP.put("western", "Western");
		GENRE_MAP.put("drama", "Drama");
		GENRE_MAP.put("animatie", "Animation");
		GENRE_MAP.put("thriller", "Thriller");
		GENRE_MAP.put("romantiek", "Romance");
		GENRE_MAP.put("avontuur", "Adventure");
		GENRE_MAP.put("fantasy", "Fantasy");
		GENRE_MAP.put("comedy", "Comedy");
		GENRE_MAP.put("familie", "Family");
		GENRE_MAP.put("actie", "Action");
		
	}
	
	Resource temp;
	
	public WolffCrawler() throws NoSuchAlgorithmException {
		//load ontology file
		System.out.println("*) load ontology file....");
		m = ModelFactory.createOntologyModel();
		try {
			m.read(FileManager.get().open(REF_ONT), "", "RDF/XML");
		} catch(NullPointerException e) {
			System.out.print("Something error with reading the ontology file: ");
			e.printStackTrace();
		}
	
		m.setNsPrefix("wolff", NS);
		m.setNsPrefix("wolff-m", MOVIE);
		m.setNsPrefix("wolff-p", PERSON);
		m.setNsPrefix("wolff-g", GENRE);
		m.setNsPrefix("wolff-c", COMPANY);
		
		
	}
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
		
		WolffCrawler crawler = new WolffCrawler();
		crawler.parseIndex();
		crawler.save();
		
		System.out.println("---finish---");
	}
	
	/**
	 * save the model into a file with RDF/XML format
	 * @throws FileNotFoundException
	 */
	private void save() throws FileNotFoundException {
		m.write(new FileOutputStream(OUTPUT), "RDF/XML");
	}

	/**
	 * parse movie index to get the movie list
	 * @throws IOException
	 */
	private void parseIndex() throws IOException {
		Document doc = Jsoup.connect(WOLFF + "/bioscopen/cinestar/").get();
		Elements movies = doc.select(".table_agenda td[width=245] a");
		
		Set<String> urls = new HashSet<String>();
		for(Element movie : movies) {
			urls.add(movie.attr("href"));
		}
		
		for(String url: urls) {
			parseMovie(url);
		}
	}

	/**
	 * parse each movie, and create the corresponding resources
	 * @param url
	 */
	private void parseMovie(String url) {
		Boolean is3D = false;
		Document doc;
		try {
			doc = Jsoup.connect(WOLFF + url).get();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}		

		String movietitle = doc.select("#wrapper_left h2").first().text().trim();
		System.out.println("Movie: " + movietitle);
		
		// check if it is 3D movie
		if(url.contains("-3d")) {
			is3D = true;
		}

		//create resource movie with the type 'Movie' and data property 'title'
		Resource movie = m.createResource(MOVIE + url.replace("/film/", ""));
		movie.addProperty(RDF.type, m.getProperty(NS + "Movie"))
			 .addProperty(m.getProperty(NS + "title"), movietitle);

		//if it is 3D movie..
		if(is3D)
			movie.addProperty(m.getProperty(NS + "hasPresentation"), m.getResource(NS + "3D"));

		// does it have corresponding dbpedia resource?
		String mResult;
		if((mResult = SparqlQuery.movieInDbpediaEn(movietitle)) != null) {
			temp = ResourceFactory.createResource(mResult);
			movie.addProperty(OWL.sameAs, temp);
		} //else if((mResult = SparqlQuery.movieInDbpediaNl(movietitle)) != null) {
//			temp = ResourceFactory.createResource(mResult);
//			movie.addProperty(OWL.sameAs, temp);
//		}else if((mResult = SparqlQuery.movieInDbpediaDe(movietitle)) != null) {
//			temp = ResourceFactory.createResource(mResult);
//			movie.addProperty(OWL.sameAs, temp);
//		}
//		
		//parse sidebar information
		Elements sidebar = doc.select(".sidebar_container").get(1).select(".table_view tr td");
		
		for (Element element:sidebar) {
			switch (element.select("strong").text()) {
			//get all actors
			case "Acteurs":
				String[] actors = element.text().substring(8).split(", "); //Remove "Acteurs" from string
				for(String actor : actors) {
					Resource person = m.createResource(PERSON + actor.replace(" ", "_"));
					person.addProperty(RDF.type, m.getProperty(NS + "Person"))
						  .addProperty(m.getProperty(NS + "name"), actor.trim());
					movie.addProperty(m.getProperty(NS + "hasActor"), person);
					
					//check if the actor has dbpedia page. Describe as sameAs if true
					String qResult;
					if((qResult = SparqlQuery.personInDbpediaEn(actor)) != null) { // in dbpedia.org
						temp = ResourceFactory.createResource(qResult);
						person.addProperty(OWL.sameAs, temp);
					} //else if((qResult = SparqlQuery.personInDbpediaNl(actor)) != null) { // in nl.dbpedia.org
//						temp = ResourceFactory.createResource(qResult);
//						person.addProperty(OWL.sameAs, temp);
//					} else if((qResult = SparqlQuery.personInDbpediaDe(actor)) != null) { // in de.dbpedia.org
//						temp = ResourceFactory.createResource(qResult);
//						person.addProperty(OWL.sameAs, temp);
//					}
				}
				break;
			
			//get the director
			case "Regie": //director
				String nameString = element.text().substring(6).toString().trim(); //Remove "Regie" from string
				Resource person = m.createResource(PERSON + nameString.replace(" ", "_"));
				person.addProperty(m.getProperty(NS + "name"), nameString);
				movie.addProperty(m.getProperty(NS + "hasDirector"), person);
				
				//check if the director has dbpedia page. Describe as sameAs if true 
				String qResult;
				if((qResult = SparqlQuery.personInDbpediaEn(nameString)) != null) { // in dbpedia.org
					person.addProperty(OWL.sameAs, DBR + qResult);
				}// else if((qResult = SparqlQuery.personInDbpediaNl(nameString)) != null) { // in nl.dbpedia.org
//					person.addProperty(OWL.sameAs, DBR_NL + qResult);
//				} else if((qResult = SparqlQuery.personInDbpediaDe(nameString)) != null) { // in de.dbpedia.org
//					person.addProperty(OWL.sameAs, DBR_DE + qResult);
//				}
				
				// a little bit cheating for JJ Abrams
				if(nameString.equals("Jeffrey (J.J.) Abrams")) {
					person.addProperty(OWL.sameAs, DBR + "J._J._Abrams");
				}
				break;

			//get the duration
			case "Speelduur":
				movie.addProperty(m.getProperty(NS + "duration"), last(element).toString().trim().split(" ")[0], XSDDatatype.XSDint);
				break;

			//get the genre
			case "Genre":
				String[] genres = last(element).toString().toLowerCase().split(", ");
				for (String genreName:genres) {
					if(GENRE_MAP.containsKey(genreName))
						genreName = GENRE_MAP.get(genreName);
					else { //unknown genre; report it and create new resource to acommodate
						System.out.println("*) another genre found: " + genreName);
						m.createResource(GENRE + genreName)
						 .addProperty(RDF.type, m.getProperty(NS + "Genre"));
					}						
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(GENRE + genreName));
				}					
				break;
			
			//get the language presentation
			case "Taal":
				String lang = last(element).toString().trim().toLowerCase();
				if(LANGUAGE.containsKey(lang)) {
					lang = LANGUAGE.get(lang);
				} else {
					System.out.println("another language found: " + lang);
				}

				movie.addProperty(m.getProperty(NS + "language"), lang);	
				break;
			
			//get the release date
			case "In de bioscoop sinds":
				String[] releasedate = last(element).toString().trim().split(" ");
				String day = releasedate[0];
				String month = String.valueOf((Arrays.asList(DUTCH_MONTH).indexOf(releasedate[1].toLowerCase()) + 1));
				String year = releasedate[2];
				String formatteddate = year + "-" + month + "-" + day + "T00:00:00";
				movie.addProperty(m.getProperty(NS + "releaseDate"), formatteddate, XSDDatatype.XSDdateTime);			
				break;
			
			//get the local distributor
			case "Distributeur":
				Node distributorLink = (Node) last(element);
				Resource distributorResource;
				if (distributorLink instanceof Element) {
					distributorResource = m.createResource(COMPANY + ((Element) distributorLink).text().replace(" ", "_"));
					distributorResource.addProperty(m.getProperty(NS + "companyURL"), distributorLink.attr("href"));
					distributorResource.addProperty(m.getProperty(NS + "companyName"), ((Element) distributorLink).text());
					movie.addProperty(m.getProperty(NS + "isDistributedBy"), distributorResource);
				} else {
					distributorResource = m.createResource(COMPANY + distributorLink.toString().replace(" ", "_"));
					distributorResource.addProperty(m.getProperty(NS + "companyName"), distributorLink.toString());
				}
					
				movie.addProperty(m.getProperty(NS + "isDistributedBy"), distributorResource);
				break;
			}
		}
		
	}


	/**
	 * Get last child node of element
	 * @param element
	 * @return
	 */
	private Object last(Element element) {
		List<Node> nodes = element.childNodes();
		return nodes.get(nodes.size()-1);
	}
}
