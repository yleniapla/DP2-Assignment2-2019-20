package it.polito.dp2.BIB.sol2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.net.URI;

import it.polito.dp2.BIB.ArticleReader;
import it.polito.dp2.BIB.BibReader;
import it.polito.dp2.BIB.BibReaderException;
import it.polito.dp2.BIB.BibReaderFactory;
import it.polito.dp2.BIB.BookReader;
import it.polito.dp2.BIB.ItemReader;
import it.polito.dp2.BIB.JournalReader;
import it.polito.dp2.BIB.ass2.ServiceException;
import it.polito.dp2.BIB.ass2.UnknownItemException;

import it.polito.dp2.BIB.sol2.jaxb.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

public class CitationFinder implements it.polito.dp2.BIB.ass2.CitationFinder {

	private static String serviceBaseUri = "";
	private static String serviceBasePort = "";

	private Client client;
	private BibReader monitor; // interface on the Biblio
	private BibReaderFactory readFactory;

	private Map<Integer, URI> itemToUri; // map itemID-URI
	private Map<Integer, MyNodeBody> idToNode; // map itemID-NodeBody
	private Map<URI, ItemReader> UriToItem; // map URI-Item
	private Set<URI> relations; // all citedby relationships in the system

	public CitationFinder(Properties prop) throws BibReaderException, UnknownItemException {

		// get both system properties
		if (prop.getProperty("it.polito.dp2.BIB.ass2.URL") == null
				|| prop.getProperty("it.polito.dp2.BIB.ass2.PORT") == null) {
			throw new BibReaderException();
		}

		serviceBaseUri = prop.getProperty("it.polito.dp2.BIB.ass2.URL");
		serviceBasePort = prop.getProperty("it.polito.dp2.BIB.ass2.PORT");

		//initialization of all the interfaces and maps
		this.readFactory = BibReaderFactory.newInstance();
		this.monitor = this.readFactory.newBibReader();
		this.client = ClientBuilder.newClient(); 
		
		this.itemToUri = new HashMap<Integer, URI>();
		this.idToNode = new HashMap<Integer, MyNodeBody>();
		this.UriToItem = new HashMap<URI, ItemReader>();
		this.relations = new HashSet<URI>();

		//create the neo4j graph with all the items in the biblio
		loadGraph(this.monitor.getItems(null, 0, 9999));
	}

	@Override
	public BookReader getBook(String arg0) {
		return this.monitor.getBook(arg0);
	}

	@Override
	public Set<ItemReader> getItems(String arg0, int arg1, int arg2) {
		return this.monitor.getItems(arg0, arg1, arg2);
	}

	@Override
	public JournalReader getJournal(String arg0) {
		return this.monitor.getJournal(arg0);
	}

	@Override
	public Set<JournalReader> getJournals(String arg0) {
		return this.monitor.getJournals(arg0);
	}

	@Override
	public Set<ItemReader> findAllCitingItems(ItemReader item, int maxDepth)
			throws UnknownItemException, ServiceException {

		if (this.UriToItem.containsValue(item)) { //if the items whom requires exists

			MyPathReq pathR = new MyPathReq();
			
			//check the value of maxdepth
			if (maxDepth <= 0)
				maxDepth = Integer.MAX_VALUE;

			//fill the request body
			MyPathReq.Relationships r = new MyPathReq.Relationships();

			r.setDirection("out");
			r.setType("CitedBy");

			pathR.setRelationships(r);
			pathR.setMaxDepth(maxDepth);

			int id = 0;

			if (item instanceof ArticleReader) {

				ArticleReader a = (ArticleReader) item;
				id = myHash(a.getJournal().getTitle(), (a.getJournal().getISSN()));

			} else if (item instanceof BookReader) {

				BookReader b = (BookReader) item;
				id = myHash(b.getTitle(), b.getISBN());

			}

			URI uri = this.itemToUri.get(id);
			Response resp;
			
			//request to neo4j
			WebTarget target = this.client.target(UriBuilder.fromUri(uri + "/traverse/node"));

			resp = target.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
					.post(Entity.json(pathR));

			if (resp == null || resp.getStatus() != 200)
				throw new ServiceException();

			//check the result
			MyPath[] citationPath = resp.readEntity(MyPath[].class);

			Set<ItemReader> result = new HashSet<>();
	        for (MyPath node : citationPath) { //for each node in the result

	            Set<ItemReader> matchingItems = this.getItems(node.getData().getTitle(), 0, 9999); //get all the items with that title
	            if (matchingItems.isEmpty()) throw new UnknownItemException();

	            for (ItemReader temp : matchingItems) { // if there is the subtitle checks also it
	                if (node.getData().getSubtitle() != null && !node.getData().getSubtitle().isEmpty()) {
	                    if (node.getData().getTitle().equals(temp.getTitle()) && node.getData().getSubtitle().equals(temp.getSubtitle())) {
	                        result.add(temp);
	                    }
	                } else { //or checks only the title
	                    if (node.getData().getTitle().equals(temp.getTitle())) {
	                        result.add(temp);
	                    }
	                }
	            }
	        }
	        return result;
		} else
			throw new UnknownItemException();

	}

	protected void loadGraph(Set<ItemReader> itemList) throws UnknownItemException {
		if (itemList != null) {
			// load all nodes and relations
			loadNodes(itemList);
			loadRelationships(itemList);
		}
		return;
	}

	protected void loadNodes(Set<ItemReader> itemList) throws UnknownItemException {
		for (ItemReader reader : itemList) { // for each place
			MyNode node = new MyNode(); // create a new node

			int id = 0;

			//get the id of the item based on the type
			if (reader instanceof ArticleReader) {

				ArticleReader a = (ArticleReader) reader;
				id = myHash(a.getJournal().getTitle(), (a.getJournal().getISSN()));
				
			} else if (reader instanceof BookReader) {

				BookReader b = (BookReader) reader;
				id = myHash(b.getTitle(), b.getISBN());

			} else {
				throw new UnknownItemException();
			}

			//set node propertied
			node.setId(id);
			node.setTitle(reader.getTitle());
			node.setSubtitle(reader.getSubtitle());

			MyNodeBody result;
			
			// insertion in Neo4j
			result = insertNode(node); 
			
			//fill the maps
			this.itemToUri.put(id, URI.create(result.getSelf()));
			this.idToNode.put(id, result);
			this.UriToItem.put(URI.create(result.getSelf()), reader);
		}

	}

	protected MyNodeBody insertNode(MyNode n) throws UnknownItemException {

		//service port seems to be always included in the base uri
		WebTarget target = this.client.target(UriBuilder
				.fromUri(serviceBaseUri + /* ":" + serviceBasePort + */ "/data/node"));

		//http request
		Response response = target 
				.request()
				.accept(MediaType.APPLICATION_JSON).post(Entity.json(n));

		response.bufferEntity();

		if (response.getStatus() != 201) {
			System.out.println("Error: " + response.getStatus() + " " + response.getStatusInfo());
		}

		MyNodeBody nb = response.readEntity(MyNodeBody.class);
		if (nb == null) {
			throw new UnknownItemException();
		}

		return nb;
	}

	protected void loadRelationships(Set<ItemReader> itemList) {
		for (ItemReader i : itemList) {
			for (ItemReader ci : i.getCitingItems()) { // for each citation

				MyRelationship relationship = new MyRelationship(); // create a new relationship
				int id = 0;
				int idf = 0;

				//who cite
				if (ci instanceof ArticleReader) {

					ArticleReader a = (ArticleReader) ci;

					id = myHash(a.getJournal().getTitle(), (a.getJournal().getISSN()));

				} else if (ci instanceof BookReader) {

					BookReader b = (BookReader) ci;

					id = myHash(b.getTitle(), b.getISBN());

				}

				//who is cited
				if (i instanceof ArticleReader) {

					ArticleReader a = (ArticleReader) i;

					idf = myHash(a.getJournal().getTitle(), (a.getJournal().getISSN()));

				} else if (i instanceof BookReader) {

					BookReader b = (BookReader) i;

					idf = myHash(b.getTitle(), b.getISBN());

				}

				String fromid = this.idToNode.get(idf).getSelf();

				URI uri = this.itemToUri.get(id); // URI of who cite
				relationship.setTo(uri.toString()); 
				relationship.setType("CitedBy"); // connection type
				MyRelationshipBody result = insertRelationship(relationship, fromid); // insertion in Neo4j

				relations.add(URI.create(result.getSelf())); // save the new relation
			}
		}

	}

	protected MyRelationshipBody insertRelationship(MyRelationship r, String id) {

		Response res;
		MyRelationshipBody rb;

		WebTarget target = this.client.target(
				UriBuilder.fromUri(/*
									 * serviceBaseUri + ":" + serviceBasePort +
									 * "/data/node/" + String.valueOf(id)
									 */ id + "/relationships"));

		res = target.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(Entity.json(r));

		res.bufferEntity();

		if (res.getStatus() != 201) {
			System.out.println("Error: " + res.getStatus() + " " + res.getStatusInfo());
		}
		rb = res.readEntity(MyRelationshipBody.class);

		return rb;
	}

	private int myHash(String title, String code) {
		int hash = 7;
		int hash1 = 0;
		for (int i = 0; i < title.length(); i++) {
			hash1 += 32 * hash + title.charAt(i);
		}
		return (hash1 * code.hashCode());
	}

}

