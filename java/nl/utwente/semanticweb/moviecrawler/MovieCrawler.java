package nl.utwente.semanticweb.moviecrawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.rdf.model.ResourceFactory;


public class MovieCrawler {
	private final static String WOLFF = "http://www.wolff.nl";
	private final static String WOLFF_OWL = WOLFF + "/2016/wolff.owl";
	private final static String NS = WOLFF_OWL + "#";
	private final static String MOVIE = WOLFF_OWL + "/movie" + "#";
	private final static String PERSON = WOLFF_OWL + "/person" + "#";
	private final static String GENRE = WOLFF_OWL + "/genre" + "#";
	private final static String COMPANY = WOLFF_OWL + "/company" + "#";
	
	private final static String REF_ONT = "wolff.owl";
	private final static String OUTPUT = "wolff.rdf";
	
	private final static String movieText = "movielist.txt";
	private static HashSet<String> movielist = new HashSet<String>();
	

	private static OntModel m;
	private static Pattern pattern = Pattern.compile("\\(.*\\)");
		 
	public static void main(String[] args) throws FileNotFoundException, HttpException {

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
		
		System.out.println("reading text file.. ");
		readMovieFile();

		for(String movie : movielist) {
			getMovieAtDbpedia(movie);
		}
		
		m.write(new FileOutputStream(OUTPUT), "RDF/XML");
		System.out.println("--Finish--");
	}
	
	
	private static void getMovieAtDbpedia(String movieTitle) throws HttpException {
		String mResult; //example: http://dbpedia.org/resource/The_Longest_Ride_(film)
		if((mResult = SparqlQuery.movieInDbpediaEn(movieTitle)) != null) {
			Resource movie = m.createResource(MOVIE + movieTitle.replace(" ", "_"));
			Resource temp = ResourceFactory.createResource(mResult);
			
			movie.addProperty(RDF.type, m.getProperty(NS + "Movie"))
				 .addProperty(m.getProperty(NS + "title"), movieTitle)
				 .addProperty(OWL.sameAs, temp);
			
			System.out.println("Movie: " + mResult);
//			movie.addProperty(OWL.sameAs, mResult);
			
			//add movie details to the RDF model
			//details available: runtime, distributor, studio, budget, country, abstract
			HashMap<String, String> mDetails = SparqlQuery.getMovieInfo(mResult);
			
			//get runtime
			if(mDetails.containsKey("runtime")) {
				movie.addProperty(m.getProperty(NS + "duration"), mDetails.get("runtime"), XSDDatatype.XSDdouble);
				System.out.println("\tDuration: " + mDetails.get("runtime"));
			}
			
			//get release date
			if(mDetails.containsKey("releaseDate")) {
				String releaseDate = mDetails.get("releaseDate");
				if(releaseDate.contains(","))
					releaseDate = releaseDate.split(",")[0];
				if(releaseDate.trim().matches("^[0-9]{4}$"))
					releaseDate = releaseDate.trim() + "-01-01";
				movie.addProperty(m.getProperty(NS + "releaseDate"), releaseDate + "T00:00:00", XSDDatatype.XSDdateTime);
				System.out.println("\tRelease date: " + releaseDate);
			}
			
			//get budget
			if(mDetails.containsKey("budget")) {
				movie.addProperty(m.getProperty(NS + "budget"), mDetails.get("budget"), XSDDatatype.XSDdouble);
				System.out.println("\tBudget: " + mDetails.get("budget"));
				
			}
			//get country
			if(mDetails.containsKey("country")) {
				if(mDetails.get("country").contains("*")) { //multiple countries
					String[] countries = mDetails.get("country").split("\\*");
					for (String country : countries) {
						if(!country.isEmpty()) {
							movie.addProperty(m.getProperty(NS + "country"), country.trim());
							System.out.println("\tCountry: " + country.trim());
						}
					}
				} else {
					movie.addProperty(m.getProperty(NS + "country"), mDetails.get("country"));
					System.out.println("\tCountry: " + mDetails.get("country"));
				}
			}
			//get distributor
			if(mDetails.containsKey("distributor")) {
				System.out.println("\tDistributor: " + mDetails.get("distributor"));
				temp = ResourceFactory.createResource(mDetails.get("distributor"));
				
				Resource distributor = m.createResource(COMPANY + mDetails.get("distributor").replace("http://dbpedia.org/resource/", ""));
				distributor.addProperty(RDF.type, m.getProperty(NS + "Company"))
						   .addProperty(m.getProperty(NS + "name"), mDetails.get("distributor").replace("http://dbpedia.org/resource/", "").replace("_", " "))
						   .addProperty(OWL.sameAs, temp);
				movie.addProperty(m.getProperty(NS + "isDistributedBy"), distributor);	
			}
			
			//get studio
			if(mDetails.containsKey("studio")) {
				System.out.println("\tStudio: " + mDetails.get("studio"));
				temp = ResourceFactory.createResource(mDetails.get("studio"));
				
				Resource studio = m.createResource(COMPANY + mDetails.get("studio").replace("http://dbpedia.org/resource/", ""));
				studio.addProperty(RDF.type, m.getProperty(NS + "Company"))
						   .addProperty(m.getProperty(NS + "name"), mDetails.get("studio").replace("http://dbpedia.org/resource/", "").replace("_", " "))
						   .addProperty(OWL.sameAs, temp);
				movie.addProperty(m.getProperty(NS + "isProducedBy"), studio);	
			}
			
			//get abstract
			if(mDetails.containsKey("abstract")) {
				String abs = mDetails.get("abstract");
				movie.addProperty(m.getProperty(NS + "abstract"), abs);
				System.out.println("\tAbstract: " + abs);
				
				//get genre
				if(abs.toLowerCase().contains("action")) 
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Action"));
				if(abs.toLowerCase().contains("adventure")) 
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Adventure"));
				if(abs.toLowerCase().contains("animation") || abs.toLowerCase().contains("animated")) 
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Animation"));
				if(abs.toLowerCase().contains("comedy"))
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Comedy"));
				if(abs.toLowerCase().contains("documentary"))
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Documentary"));
				if(abs.toLowerCase().contains("drama"))
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Drama"));
				if(abs.toLowerCase().contains("science fiction") || abs.toLowerCase().contains("fantasy") || abs.toLowerCase().contains("sci-fi") || abs.toLowerCase().contains("science-fiction"))
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Fantasy"));
				if(abs.toLowerCase().contains("horror"))
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Horror"));
				if(abs.toLowerCase().contains("romantic") || abs.toLowerCase().contains("romance")) 
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Romance"));
				if(abs.toLowerCase().contains("thriller"))
					movie.addProperty(m.getProperty(NS + "hasGenre"), m.getResource(NS + "Thriller"));
				
				//get 3D movie
				if(abs.toLowerCase().contains("3d"))
					movie.addProperty(m.getProperty(NS + "hasPresentation"), m.getResource(NS + "3D"));
			}
			
			List<Set<String>> people = SparqlQuery.getPerson(mResult);
			
			Set<String> directors = people.get(0);
			Set<String> actors = people.get(1);
			Set<String> producers = people.get(2);
			Set<String> composers = people.get(3);
			Set<String> editors = people.get(4);
			Set<String> writers = people.get(5);
			Set<String> cinematographers = people.get(6);
			
			//create director
			if(!directors.isEmpty()) {
				for(String director: directors) {
					//get personal data
					HashMap<String,String> data = SparqlQuery.getPersonalInfo(director);
					temp = ResourceFactory.createResource(director);
					
					System.out.println("\tDirector: " + director);
					String name = pattern.matcher(director.replace("http://dbpedia.org/resource/", "").replace("_"," ")).replaceAll("").trim();
					Resource person = m.createResource(PERSON + director.replace("http://dbpedia.org/resource/", ""));
					person.addProperty(RDF.type, m.getProperty(NS + "Person"))
						  .addProperty(m.getProperty(NS + "name"), name)
						  .addProperty(OWL.sameAs, temp);
					//add birth date
					if(data.containsKey("birthdate")) {
						String birthdate = data.get("birthdate");
						if(birthdate.trim().matches("^[0-9]{4}$"))
							birthdate = birthdate.trim() + "-01-01";
						person.addProperty(m.getProperty(NS + "birthDate"), birthdate + "T00:00:00", XSDDatatype.XSDdateTime);
						System.out.println("\t\tBirthdate: " + birthdate);
					}
					//add nationality
					if(data.containsKey("nationality")) {
						String[] nationalities = data.get("nationality").replace(" ","").replace("\"","").split(",");
						for (String nationality : nationalities) {
							person.addProperty(m.getProperty(NS + "nationality"), nationality);
							System.out.println("\t\tNationality: " + nationality);
						}
					}
					movie.addProperty(m.getProperty(NS + "hasDirector"), person);
				}
			}
			
			//create actors
			if(!actors.isEmpty()) {
				for(String actor: actors) {
					//get personal data
					HashMap<String,String> data = SparqlQuery.getPersonalInfo(actor);
					temp = ResourceFactory.createResource(actor);
					
					System.out.println("\tActor: " + actor);
					String name = pattern.matcher(actor.replace("http://dbpedia.org/resource/", "").replace("_"," ")).replaceAll("").trim();
					Resource person = m.createResource(PERSON + actor.replace("http://dbpedia.org/resource/", ""));
					person.addProperty(RDF.type, m.getProperty(NS + "Person"))
						  .addProperty(m.getProperty(NS + "name"), name)
						  .addProperty(OWL.sameAs, temp);
					//add birth date
					if(data.containsKey("birthdate")) {
						String birthdate = data.get("birthdate");
						if(birthdate.trim().matches("^[0-9]{4}$"))
							birthdate = birthdate.trim() + "-01-01";
						person.addProperty(m.getProperty(NS + "birthDate"), birthdate + "T00:00:00", XSDDatatype.XSDdateTime);
						System.out.println("\t\tBirthdate: " + birthdate);
					}
					//add nationality
					if(data.containsKey("nationality")) {
						String[] nationalities = data.get("nationality").replace(" ","").replace("\"","").split(",");
						for (String nationality : nationalities) {
							person.addProperty(m.getProperty(NS + "nationality"), nationality);
							System.out.println("\t\tNationality: " + nationality);
						}
					}

					movie.addProperty(m.getProperty(NS + "hasActor"), person);
				}
			}
			
			//create producers
			if(!producers.isEmpty()) {
				for(String producer: producers) {
					//get personal data
					HashMap<String,String> data = SparqlQuery.getPersonalInfo(producer);
					temp = ResourceFactory.createResource(producer);
					
					System.out.println("\tProducer: " + producer);
					String name = pattern.matcher(producer.replace("http://dbpedia.org/resource/", "").replace("_"," ")).replaceAll("").trim();
					Resource person = m.createResource(PERSON + producer.replace("http://dbpedia.org/resource/", ""));
					person.addProperty(RDF.type, m.getProperty(NS + "Person"))
						  .addProperty(m.getProperty(NS + "name"), name)
						  .addProperty(OWL.sameAs, temp);
					//add birth date
					if(data.containsKey("birthdate")) {
						String birthdate = data.get("birthdate");
						if(birthdate.trim().matches("^[0-9]{4}$"))
							birthdate = birthdate.trim() + "-01-01";
						person.addProperty(m.getProperty(NS + "birthDate"), birthdate + "T00:00:00", XSDDatatype.XSDdateTime);
						System.out.println("\t\tBirthdate: " + birthdate);
					}
					//add nationality
					if(data.containsKey("nationality")) {
						String[] nationalities = data.get("nationality").replace(" ","").replace("\"","").split(",");
						for (String nationality : nationalities) {
							person.addProperty(m.getProperty(NS + "nationality"), nationality);
							System.out.println("\t\tNationality: " + nationality);
						}
					}

					movie.addProperty(m.getProperty(NS + "hasProducer"), person);
				}
			}
			
			//create composers
			if(!composers.isEmpty()) {
				for(String composer: composers) {
					//get personal data
					HashMap<String,String> data = SparqlQuery.getPersonalInfo(composer);
					temp = ResourceFactory.createResource(composer);
					
					System.out.println("\tComposer: " + composer);
					String name = pattern.matcher(composer.replace("http://dbpedia.org/resource/", "").replace("_"," ")).replaceAll("").trim();

					Resource person = m.createResource(PERSON + composer.replace("http://dbpedia.org/resource/", ""));
					person.addProperty(RDF.type, m.getProperty(NS + "Person"))
						  .addProperty(m.getProperty(NS + "name"), name)
						  .addProperty(OWL.sameAs, temp);
					//add birth date
					if(data.containsKey("birthdate")) {
						String birthdate = data.get("birthdate");
						if(birthdate.trim().matches("^[0-9]{4}$"))
							birthdate = birthdate.trim() + "-01-01";
						person.addProperty(m.getProperty(NS + "birthDate"), birthdate + "T00:00:00", XSDDatatype.XSDdateTime);
						System.out.println("\t\tBirthdate: " + birthdate);
					}
					//add nationality
					if(data.containsKey("nationality")) {
						String[] nationalities = data.get("nationality").replace(" ","").replace("\"","").split(",");
						for (String nationality : nationalities) {
							person.addProperty(m.getProperty(NS + "nationality"), nationality);
							System.out.println("\t\tNationality: " + nationality);
						}
					}

					movie.addProperty(m.getProperty(NS + "hasMusicComposer"), person);
				}
			}
			
			//create editors
			if(!editors.isEmpty()) {
				for(String editor: editors) {
					//get personal data
					HashMap<String,String> data = SparqlQuery.getPersonalInfo(editor);
					temp = ResourceFactory.createResource(editor);
					
					System.out.println("\tEditor: " + editor);
					String name = pattern.matcher(editor.replace("http://dbpedia.org/resource/", "").replace("_"," ")).replaceAll("").trim();
					Resource person = m.createResource(PERSON + editor.replace("http://dbpedia.org/resource/", ""));
					person.addProperty(RDF.type, m.getProperty(NS + "Person"))
						  .addProperty(m.getProperty(NS + "name"), name)
						  .addProperty(OWL.sameAs, temp);
					//add birth date
					if(data.containsKey("birthdate")) {
						String birthdate = data.get("birthdate");
						if(birthdate.trim().matches("^[0-9]{4}$"))
							birthdate = birthdate.trim() + "-01-01";
						person.addProperty(m.getProperty(NS + "birthDate"), birthdate + "T00:00:00", XSDDatatype.XSDdateTime);
						System.out.println("\t\tBirthdate: " + birthdate);
					}
					//add nationality
					if(data.containsKey("nationality")) {
						String[] nationalities = data.get("nationality").replace(" ","").replace("\"","").split(",");
						for (String nationality : nationalities) {
							person.addProperty(m.getProperty(NS + "nationality"), nationality);
							System.out.println("\t\tNationality: " + nationality);
						}
					}
					movie.addProperty(m.getProperty(NS + "hasEditor"), person);
				}
			}
			
			//create writers
			if(!writers.isEmpty()) {
				for(String writer: writers) {
					//get personal data
					HashMap<String,String> data = SparqlQuery.getPersonalInfo(writer);
					temp = ResourceFactory.createResource(writer);
					
					System.out.println("\tWriter: " + writer);
					String name = pattern.matcher(writer.replace("http://dbpedia.org/resource/", "").replace("_"," ")).replaceAll("").trim();
					Resource person = m.createResource(PERSON + writer.replace("http://dbpedia.org/resource/", ""));
					person.addProperty(RDF.type, m.getProperty(NS + "Person"))
						  .addProperty(m.getProperty(NS + "name"), name)
						  .addProperty(OWL.sameAs, temp);
					//add birth date
					if(data.containsKey("birthdate")) {
						String birthdate = data.get("birthdate");
						if(birthdate.trim().matches("^[0-9]{4}$"))
							birthdate = birthdate.trim() + "-01-01";
						person.addProperty(m.getProperty(NS + "birthDate"), birthdate + "T00:00:00", XSDDatatype.XSDdateTime);
						System.out.println("\t\tBirthdate: " + birthdate);
					}
					//add nationality
					if(data.containsKey("nationality")) {
						String[] nationalities = data.get("nationality").replace(" ","").replace("\"","").split(",");
						for (String nationality : nationalities) {
							person.addProperty(m.getProperty(NS + "nationality"), nationality);
							System.out.println("\t\tNationality: " + nationality);
						}
					}

					movie.addProperty(m.getProperty(NS + "hasWriter"), person);
				}
			}

			//create cinematographers
			if(!cinematographers.isEmpty()) {
				for(String cinematographer: cinematographers) {
					//get personal data
					HashMap<String,String> data = SparqlQuery.getPersonalInfo(cinematographer);
					temp = ResourceFactory.createResource(cinematographer);
					
					System.out.println("\tCinematographer: " + cinematographer);
					String name = pattern.matcher(cinematographer.replace("http://dbpedia.org/resource/", "").replace("_"," ")).replaceAll("").trim();
					Resource person = m.createResource(PERSON + cinematographer.replace("http://dbpedia.org/resource/", ""));
					person.addProperty(RDF.type, m.getProperty(NS + "Person"))
						  .addProperty(m.getProperty(NS + "name"), name)
						  .addProperty(OWL.sameAs, temp);
					//add birth date
					if(data.containsKey("birthdate")) {
						String birthdate = data.get("birthdate");
						if(birthdate.trim().matches("^[0-9]{4}$"))
							birthdate = birthdate.trim() + "-01-01";
						person.addProperty(m.getProperty(NS + "birthDate"), birthdate + "T00:00:00", XSDDatatype.XSDdateTime);
						System.out.println("\t\tBirthdate: " + birthdate);
					}
					//add nationality
					if(data.containsKey("nationality")) {
						String[] nationalities = data.get("nationality").replace(" ","").replace("\"","").split(",");
						for (String nationality : nationalities) {
							person.addProperty(m.getProperty(NS + "nationality"), nationality);
							System.out.println("\t\tNationality: " + nationality);
						}
					}

					movie.addProperty(m.getProperty(NS + "hasCinematographer"), person);
				}
			}

		} 
	}

	private static void readMovieFile() {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(movieText));
			try {
				String line = br.readLine();
				while(line != null) {
					movielist.add(line);
					line = br.readLine();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}		
	}

}
