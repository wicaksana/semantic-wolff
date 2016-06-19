## Enriching Wolff Bioscopen Website Using Linked Data from DBpedia

### Overview

We developed a proof-of-concept of an improved version of [Wolff Bioscopen website](http://www.wolff.nl/) which uses Linked Data from [DBpedia](http://wiki.dbpedia.org/) to provide non-trivial dynamic information which cannot be found in search engine easily. To achieve this, we developed a movie ontology and enriched original Wolff movie data with additional data from DBpedia by using the self-developed ontology as the reference.

### System Description

![alt text][systemDescription]

Steps:

1. To structure the datasets, the ontology is firstly defined and used as the common vocabularies.
2. Wolff ontology is developed using the existing data in Wolff website to identify properties and classes needed. Since data provided from the website is limited, additional data (or, additional vocabularies) related to movie production and to person. Protege is used to create this ontology.
3. Movie data from Wolff website is retrieved and transformed into RDF datasets using Java application. Web scraping is done using JSoup. Additional data is retrieved from DBpedia based on the top-20 most popular movies from each year between 2010-2015 as listed in IMDB.
4. All data collected in step 3 is transformed into RDF data using [Apache Jena](https://jena.apache.org/), by using the ontology developed in step 2 as the reference.
5. The newly-created RDF data are transferred to [Fuseki Server](https://jena.apache.org/documentation/serving_data/), to serve the RDF data over HTTP (acting as a SPARQL endpoint). The RDF data is stored persistently in TDB database.
6. User accesses the RDF data via a web application ([Flask](http://flask.pocoo.org/)).
7. For each user request, Flask performs SPARQL query against Fuseki Server to get the data
8. The data is delivered to user over HTTP 

### Features

1. 'With whom does a subject frequently works together with, and in what movies?'
For example, we know that Leonardo diCaprio (the subject) has worked together with director Martin Scorcese in many movies in the past. But to find the similar knowledge over arbitrary person in movie industry is not an easy question. We cannot immediately find the answer through quick search in Google, especially if the subject is not really well-known. 

The following SPARQL query addresses this question:

```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX dbo: <http://dbpedia.org/ontology/>

SELECT DISTINCT
?name
(GROUP_CONCAT(DISTINCT ?movietitle; SEPARATOR = "; ") AS ?movielist)
(COUNT(DISTINCT ?movietitle) AS ?movietotal)
WHERE {
wolff-p:Quentin_Tarantino owl:sameAs ?dbplink .
SERVICE <http://dbpedia.org/sparql> {
?movie a dbo:Film .
?movie ?p ?dbplink .
?movie rdfs:label ?movietitle .
?movie ?p2 ?actor .
?actor a dbo:Person .
?actor rdfs:label ?name .
FILTER (langMatches(lang(?movietitle),"en"))
FILTER (langMatches(lang(?name),"en"))
FILTER (?actor != ?dbplink)
}
}

GROUP BY ?name
ORDER BY DESC(?movietotal)
LIMIT 15
```

2. 'Which Oscar winners whom the subject worked together in the past, and in what movies?'

 This is more or less similar to the previous query, and is difficult to find the answer by quick googling for arbitrary person. Here, we define Oscar winners as actors whom ever won either best actor, best actress, best supporting actor, or best supporting actress of Academy Awards

```
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX dbo: <http://dbpedia.org/ontology/>
PREFIX dct: <http://purl.org/dc/terms/>

SELECT DISTINCT ?strippedname ?strippedtitle WHERE {
wolff-p:Quentin_Tarantino owl:sameAs ?p_link .
SERVICE <http://dbpedia.org/sparql> {
?movie a dbo:Film .
?movie ?pre ?p_link .
?movie dbo:starring ?actor .
{ ?actor dct:subject <http://dbpedia.org/resource/Category:Best_Actor_Academy_Award_winners>
.}
UNION {
?actor dct:subject <http://dbpedia.org/resource/Category:Best_Actress_Academy_Award_winners> . }
UNION {
?actor dct:subject <http://dbpedia.org/resource/Category:Best_Supporting_Actor_Academy_Award_winners> }
UNION {
?actor dct:subject <http://dbpedia.org/resource/Category:Best_Supporting_Actress_Academy_Award_winners> }
?actor rdfs:label ?name .
?movie rdfs:label ?title .
FILTER(?p_link != ?actor)
FILTER (langMatches(lang(?name),"en"))
FILTER (langMatches(lang(?title),"en"))
BIND(str(?name) AS ?strippedname)
BIND(str(?title) AS ?strippedtitle)
}
}
```
### The Rest of the Details..

Please read the [report](report.pdf)

### Screenshots

![main page][mainPage]

The main page displays overview of movies in the database. If any of it is clicked, it will display the movie information (the director, the actors, the producers, distributer, studio, etc.).

![example of a movie information page][movieInfo]

If any of the person listed on the page above is clicked, it will display another page that provide the result of two SPARQL queries listed above.

This is an example of the list of people whom Quentin Tarantino frequently collaborates with, and in what movies.

![people frequently work with Quentin Tarantino][peopleFrequently]
 
And the following is the example of the list of Oscar winners with whom Quentin Tarantino ever worked together.

![Oscar winner working with Quentin Tarantino][oscarWinner]


### Files 

* [wolff.owl](wolff.owl) Wolff movie ontology
* [wolff.rdf](wolff.rdf) RDF datasets
* [WolffCrawler.java](java/nl/utwente/semanticweb/wolffcrawler/WolffCrawler.java) Java program to scrape movie data from Wolff website, enrich it using DBpedia data, and create the RDF datasets
* [MovieCrawler.java](java/nl/utwente/moviecrawler/moviecrawler/MovieCrawler.java) Java program to get additional movie datasets from DBpedia, based on the list of movies (`movielist.txt`) taken from IMDB
* [web-app/](web-app/) Flask web application. 

[systemDescription]: img/sysdescr.png
[mainPage]: img/main-page.png
[movieInfo]: img/movie-info.png
[peopleFrequently]: img/people-frequently.png
[oscarWinner]: img/oscar-winner.png
