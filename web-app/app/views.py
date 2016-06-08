from app import app
from flask import render_template
from SPARQLWrapper import SPARQLWrapper, JSON
from bs4 import BeautifulSoup
from urllib import request


WOLFF_OWL = "http://www.wolff.nl/2016/wolff.owl"
NS = WOLFF_OWL + "#"
MOVIE = WOLFF_OWL + "/movie" + "#"
PERSON = WOLFF_OWL + "/person" + "#"
GENRE = WOLFF_OWL + "/genre" + "#"
COMPANY = WOLFF_OWL + "/company" + "#"

PREFIX = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " \
         "PREFIX wolff: <http://www.wolff.nl/2016/wolff.owl#> " \
         "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " \
         "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " \
         "PREFIX wolff: <http://www.wolff.nl/2016/wolff.owl#> " \
         "PREFIX afn: <http://jena.apache.org/ARQ/function#>" \
         "PREFIX dc: <http://purl.org/dc/terms/>" \
         "PREFIX movie: <http://data.linkedmdb.org/resource/movie/>" \
         "PREFIX owl: <http://www.w3.org/2002/07/owl#>" \
         "PREFIX dbo: <http://dbpedia.org/ontology/>" \
         "PREFIX dct: <http://purl.org/dc/terms/>"

sparql = SPARQLWrapper("http://localhost:3030/wolff/query")


@app.route('/')
@app.route('/index')
def index():
    sparql.setQuery(PREFIX + """
    SELECT ?movie ?title ?abstract WHERE {
        ?movie a wolff:Movie .
        ?movie wolff:title ?title .
        ?movie wolff:abstract ?abstract .
    } LIMIT 100
    """)
    sparql.setReturnFormat(JSON)
    movies = sparql.query().convert()

    for i, movie in enumerate(movies['results']['bindings']):
        movies['results']['bindings'][i]['movie']['value'] = movie['movie']['value'].split('#')[1]

    return render_template('index.html', title='Home', list=movies['results']['bindings'])


@app.route('/film/<uri>')
def get_film(uri='none'):
    uri = MOVIE + uri.replace(" ", "_")
    sparql.setQuery( PREFIX +
                     "SELECT ?predicate ?o WHERE { " +
                     "<" + uri +"> ?p ?o . " +
                     "BIND (afn:localname(?p) AS ?predicate)" +
                     "}"
                    )

    sparql.setReturnFormat(JSON)
    qResults = sparql.query().convert()

    movieData = {'directors': [],
                 'actors': [],
                 'producers': [],
                 'cgraphers': [],
                 'writers': [],
                 'composers': [],
                 'editors': [],
                 'distributors': [],
                 'studio': [],
                 'genre': [],
                 'abstract': [],
                 'title': [],
                 'budget': [],
                 'country': [],
                 'duration': [],
                 }

    for i, result in enumerate(qResults['results']['bindings']):
        if result['predicate']['value'] == 'hasDirector':
            movieData['directors'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'hasProducer':
            movieData['producers'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'hasActor':
            movieData['actors'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'hasCinematographer':
            movieData['cgraphers'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'hasMusicComposer':
            movieData['composers'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'hasEditor':
            movieData['editors'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'hasWriter':
            movieData['writers'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'isDistributedBy':
            movieData['distributors'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'isProducedBy':
            movieData['studio'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'hasGenre':
            movieData['genre'].append(result['o']['value'].split('#')[1].replace("_"," "))
        if result['predicate']['value'] == 'abstract':
            movieData['abstract'].append(result['o']['value'])
        if result['predicate']['value'] == 'title':
            movieData['title'].append(result['o']['value'])
        if result['predicate']['value'] == 'budget':
            movieData['budget'].append(result['o']['value'])
        if result['predicate']['value'] == 'country':
            movieData['country'].append(result['o']['value'])
        if result['predicate']['value'] == 'duration':
            movieData['duration'].append(result['o']['value'])

    img_url = get_image(uri)

    return render_template('film.html', img=img_url, list=movieData)


@app.route('/person/<string:name>')
def get_person(name):
    roles = {'hasActor':'actor',
            'hasDirector': 'director',
            'hasCinematographer': 'cinematographer',
            'hasEditor': 'editor',
            'hasMusicComposer': 'composer',
            'hasWriter': 'writer',
            'hasProducer': 'producer'}

    uri = PERSON + name.replace(" ", "_")
    sparql.setQuery( PREFIX +
                     "SELECT DISTINCT * WHERE { " +
                     "<" + uri +"> wolff:name ?name . " +
                     "OPTIONAL { <" + uri + "> wolff:birthDate ?birthDate . } " +
                     "OPTIONAL { <" + uri + "> wolff:nationality ?nationality . } " +
                     "?movie ?role <" + uri +"> ." +
                     "}"
                    )

    sparql.setReturnFormat(JSON)
    qResults = sparql.query().convert()

    personData = {'name': [],
                  'birthdate': [],
                  'movie': {}} # k, v where k==moviename, v==role in list format

    for k, v in enumerate(qResults['results']['bindings']):
        if v['name']['value'] not in personData['name']:
            personData['name'].append(v['name']['value'])
        try:
            if v['birthDate']['value'].replace("T00:00:00", "") not in personData['birthdate']:
                personData['birthdate'].append(v['birthDate']['value'].replace("T00:00:00", ""))
        except KeyError:
            pass

        if v['movie']['value'].split('#')[1].replace("_"," ") not in personData['movie'].keys():
            personData['movie'][v['movie']['value'].split('#')[1].replace("_"," ")] = []
            personData['movie'][v['movie']['value'].split('#')[1].replace("_"," ")].append(v['role']['value'].split('#')[1].replace("_"," "))
        else:
            personData['movie'][v['movie']['value'].split('#')[1].replace("_"," ")].append(v['role']['value'].split('#')[1].replace("_"," "))

    for movie in personData['movie']:
        for i, role in enumerate(personData['movie'][movie]):
            if role in roles:
                personData['movie'][movie][i] = roles[role]

    url_img = get_image(uri)
    collaboration = get_collaboration(uri)
    oscar_winners = get_oscar_winners(uri)

    return render_template('person.html', oscar_winners=oscar_winners, collaboration=collaboration, img=url_img, list=personData)


@app.route('/genre/<string:genre>')
def get_genre(genre):
    uri = "<" + NS + genre + ">"

    sparql.setQuery( PREFIX +
                     "SELECT ?movie WHERE { " +
                        "?movie wolff:hasGenre " + uri + " ." +
                     "}"
                    )

    sparql.setReturnFormat(JSON)
    qResults = sparql.query().convert()

    movieData = []
    for movie in qResults['results']['bindings']:
        movieData.append(movie['movie']['value'].split('#')[1].replace("_", " "))

    return render_template('genre.html', genre=genre, list=movieData)


@app.route('/country/<string:country>')
def get_country(country):

    query = "SELECT ?movie WHERE {{ ?movie wolff:country \"{}\" . }}".format(country)
    # sparql.setQuery( PREFIX + "SELECT ?movie WHERE { ?movie wolff:country + \""" + country + "\" . }" )
    sparql.setQuery( PREFIX + query)

    sparql.setReturnFormat(JSON)
    qResults = sparql.query().convert()

    movieData = []
    for movie in qResults['results']['bindings']:
        movieData.append(movie['movie']['value'].split('#')[1].replace("_", " "))

    return render_template('country.html', country=country, list=movieData)


@app.route('/company/<string:company>')
def get_company(company):
    uri = "<" + COMPANY + company.replace(" ", "_") + ">"

    sparql.setQuery( PREFIX +
                     "SELECT ?movie WHERE { " +
                        "{ ?movie wolff:isDistributedBy " + uri + " . } " +
                        "UNION " +
                        "{ ?movie wolff:isProducedBy " + uri + " . } " +
                     "}"
                    )

    sparql.setReturnFormat(JSON)
    qResults = sparql.query().convert()

    movieData = []
    for movie in qResults['results']['bindings']:
        movieData.append(movie['movie']['value'].split('#')[1].replace("_", " "))

    return render_template('company.html', company=company, list=movieData)


@app.route('/trivia')
def trivia():
    return

def get_image(uri):
    """
    Get image from wikipedia for the corresponding film
    :param uri: movie dbpedia URI
    :return: img_url: URL of movie poster
    """
    wiki_url = "https://en.wikipedia.org/wiki/" + uri.split("#")[1]
    img_url = ""

    try:
        webpage = request.urlopen(wiki_url).read()
        soup = BeautifulSoup(webpage, "html.parser")

        infobox = soup.find("table", class_="infobox")
        img_url = "http:" + infobox.find("img")['src']
    except (AttributeError, TypeError, UnicodeEncodeError) as e:
        img_url = "https://upload.wikimedia.org/wikipedia/en/e/ee/Unknown-person.gif"
        return img_url

    return img_url


def get_collaboration(uri):
    """
    Get top-5 list of people who frequently collaborate in projects with the corresponding person
    :param uri:
    :return: collaboration: list of collaboration (person name, frequency of collaboration, list of collaborated movies)
    """
    sparql.setQuery( PREFIX +
                         "SELECT DISTINCT ?name (GROUP_CONCAT(DISTINCT ?movietitle; SEPARATOR = \"|\") AS ?movielist)  (COUNT(DISTINCT ?movietitle) AS ?movietotal) WHERE {" +
                            "<" + uri + "> owl:sameAs ?dbplink ." +
                            "SERVICE <http://dbpedia.org/sparql> {" +
                                "?movie a dbo:Film ." +
                                "?movie ?p ?dbplink ." +
                                "?movie rdfs:label ?movietitle ." +
                                "?movie ?p2 ?actor ." +
                                "?actor a dbo:Person ." +
                                "?actor rdfs:label ?name ." +
                                "FILTER (langMatches(lang(?movietitle),\"en\"))" +
                                "FILTER (langMatches(lang(?name),\"en\"))" +
                                "FILTER (?actor != ?dbplink)" +
                            "}" +
                         "} GROUP BY ?name " +
                         "ORDER BY DESC(?movietotal) " +
                         "LIMIT 15"
                        )

    sparql.setReturnFormat(JSON)
    qResults = sparql.query().convert()

    collaboration = []
    for i, qResult in enumerate(qResults['results']['bindings']):
        collaboration.append({})
        collaboration[i]['name'] = qResult['name']['value']
        collaboration[i]['movietotal'] = qResult['movietotal']['value']
        collaboration[i]['movielist'] = [movie for movie in qResult['movielist']['value'].split('|')]

    return collaboration


def get_oscar_winners(uri):
    """
    get the list of oscar winners whom the subject ever worked with
    :param uri:
    :return: list of oscar winners together with the movie where the subject ever worked with
    """
    sparql.setQuery( PREFIX +
                     "SELECT DISTINCT ?strippedname ?strippedtitle WHERE { " +
                        "<" + uri + "> owl:sameAs ?p_link ." +
                        "SERVICE <http://dbpedia.org/sparql> { " +
                            "?movie a dbo:Film . " +
	                        "?movie ?pre ?p_link . " +
                            "?movie dbo:starring ?actor . " +
                            " { ?actor dct:subject <http://dbpedia.org/resource/Category:Best_Actor_Academy_Award_winners> . }" +
                            " UNION {?actor dct:subject <http://dbpedia.org/resource/Category:Best_Actress_Academy_Award_winners> . }" +
                            " UNION {?actor dct:subject <http://dbpedia.org/resource/Category:Best_Supporting_Actor_Academy_Award_winners> }" +
                            " UNION {?actor dct:subject <http://dbpedia.org/resource/Category:Best_Supporting_Actress_Academy_Award_winners> }"
                            " ?actor rdfs:label ?name . " +
                            " ?movie rdfs:label ?title . " +
                            " FILTER(?p_link != ?actor) " +
                            " FILTER (langMatches(lang(?name),\"en\")) " +
                            " FILTER (langMatches(lang(?title),\"en\")) " +
                            " BIND(str(?name) AS ?strippedname) " +
                            " BIND(str(?title) AS ?strippedtitle) " +
                        "}" +
                    "}"

                    )

    sparql.setReturnFormat(JSON)
    qResults = sparql.query().convert()

    oscar_winners = []
    for i, qResult in enumerate(qResults['results']['bindings']):
            oscar_winners.append({})
            oscar_winners[i]['name'] = qResult['strippedname']['value']
            oscar_winners[i]['movie'] = qResult['strippedtitle']['value']

    return oscar_winners
