package it.polito.dp2.BIB.sol2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
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
	private Map<URI, Integer> UriToId; // map URI-itemID
	private Map<Integer, MyNodeBody> idToNode; // map itemID-NodeBody
	private Map<URI, ItemReader> UriToItem; // map URI-Item
	private Set<URI> relations; // all relations in the system

	public CitationFinder(Properties prop) throws BibReaderException, UnknownItemException {
		
		if(prop.getProperty("it.polito.dp2.BIB.ass2.URL")==null || prop.getProperty("it.polito.dp2.BIB.ass2.PORT")==null){
			throw new BibReaderException();
		}
		
		serviceBaseUri = prop.getProperty("it.polito.dp2.BIB.ass2.URL");
		serviceBasePort = prop.getProperty("it.polito.dp2.BIB.ass2.PORT");
		
		this.readFactory = BibReaderFactory.newInstance();
		this.monitor = this.readFactory.newBibReader();
		this.client = ClientBuilder.newClient(); 		// create the Client object
		
		this.itemToUri = new HashMap<Integer, URI>();
		this.UriToId = new HashMap<URI, Integer>();
		this.idToNode = new HashMap<Integer, MyNodeBody>();
		this.UriToItem = new HashMap<URI, ItemReader>();
		this.relations = new HashSet<URI>();
		
		System.out.println("PARTE IL CARICAMENTO");
		loadGraph(this.monitor.getItems(null, 0, 9999));
	}

	@Override
	public BookReader getBook(String arg0) {
		//return UriToItem.values().stream().filter(BookReader.class::isInstance).map(BookReader.class::cast)
				//.filter(b -> b.getISBN() == arg0).findFirst().get();
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
		
		if(this.UriToItem.containsValue(item))
		{
			// System.out.println("SOURCE: " + item.getTitle() + "LENGTH: " + maxDepth);
			
			MyPathReq pathR = new MyPathReq();
			if(maxDepth<=0)
				maxDepth = Integer.MAX_VALUE;
			
			MyPathReq.Relationships r = new MyPathReq.Relationships();
			
			r.setDirection("out");
			r.setType("CitedBy");

			pathR.setRelationships(r);
			pathR.setMaxDepth(maxDepth);
			
			int id = 0;

			if (item instanceof ArticleReader) {
				
				ArticleReader a = (ArticleReader) item;

				id = myHash(a.getJournal().getTitle(),
						(a.getJournal().getISSN()));

			} else if (item instanceof BookReader) {

				BookReader b = (BookReader) item;
				
				id = myHash(b.getTitle(), b.getISBN());

			}
			
			URI uri = this.itemToUri.get(id);
			
			System.out.println("ID TRAVERSE: " + uri.toString());
			
			Response resp;
			/*WebTarget target = this.client.target(UriBuilder.fromUri(serviceBaseUri+"/data/node/" + 
					id+ "/traverse/node"));*/
			
			WebTarget target = this.client.target(UriBuilder.fromUri(uri + "/traverse/node"));
						
			resp = target	
					.request(MediaType.APPLICATION_JSON)  
					.accept(MediaType.APPLICATION_JSON)
					.post(Entity.json(pathR));

			if(resp == null || resp.getStatus()!=200)
				throw new ServiceException();
	
			for(Map.Entry<URI, ItemReader> es : this.UriToItem.entrySet()){
				System.out.println("URI: " + es.getKey() + " TITOLO: " + es.getValue().getTitle() + " SOTTOTIT: " + es.getValue().getSubtitle());
			}
			
			MyPath[] citationPath = resp.readEntity(MyPath[].class);
			
			System.out.println("LUNGHEZZA RISP: " + citationPath.length);
			
			if(citationPath == null)
			{
				throw new ServiceException();
			}
			
			Set<ItemReader> set = new HashSet<ItemReader>();
			List<ItemReader> list = new ArrayList<ItemReader>();
			
			for(int i=0; i<citationPath.length; i++)
			{
				String s = citationPath[i].getSelf();
				System.out.println("EL " + i + " : " + s);
				
//				if(this.UriToItem.get(s).getTitle()!=null)
//					System.out.println("TITOLO: " + this.UriToItem.get(s).getTitle());
//				if(this.UriToItem.get(s).getSubtitle()!=null)
//					System.out.println("SUB: " + this.UriToItem.get(s).getSubtitle());
				
					list.add(this.UriToItem.get(s));
				/*if (set.add(this.UriToItem.get(s)))	
					System.out.println("CIT: " + i);
				else
					System.out.println("NON HA AGGIUNTO: " + set.size());
				*/
				
			}
			System.out.println("LUNGHEZZA LISTA: " + list.size());
//			Set<ItemReader> hSet = new TreeSet<ItemReader>(list); 
//			System.out.println("LUNGHEZZA SET: " + hSet.size());
			
			return list.stream().collect(Collectors.toSet());
		}
		else
			throw new UnknownItemException();
		
	}

	protected void loadGraph(Set<ItemReader> itemList) throws UnknownItemException {
		if (itemList != null) {
			// load nodes and relations
			loadNodes(itemList);
			loadRelationships(itemList);
		}
		
//		for(Map.Entry<URI, ItemReader> es : this.UriToItem.entrySet()){
//			System.out.println("URI: " + es.getKey() + " TITOLO: " + es.getValue().getTitle() + " SOTTOTIT: " + es.getValue().getSubtitle());
//		}
		
		return;
	}

	protected void loadNodes(Set<ItemReader> itemList) throws UnknownItemException {
		System.out.println("CARICO I NODI");
		for (ItemReader reader : itemList) { // each place
			MyNode node = new MyNode(); // new node

			int id = 0;
			
			if (reader instanceof ArticleReader) {
				
				ArticleReader a = (ArticleReader) reader;

				id = myHash(a.getJournal().getTitle(),
						(a.getJournal().getISSN()));
				
				node.setCode(a.getJournal().getISSN());

			} else if (reader instanceof BookReader) {

				BookReader b = (BookReader) reader;
				
				id = myHash(b.getTitle(), b.getISBN());
				
				node.setCode(b.getISBN());

			} else {
				throw new UnknownItemException();
			}

			node.setId(id);
			node.setTitle(reader.getTitle());
			node.setSubtitle(reader.getSubtitle());

			MyNodeBody result;

			result = insertNode(node); // insertion in Neo4j

			// System.out.println("NODO: " + result.getSelf());
			
			this.itemToUri.put(id, URI.create(result.getSelf()));
			this.UriToId.put(URI.create(result.getSelf()), id); // add it to the
																// maps
			this.idToNode.put(id, result);
			this.UriToItem.put(URI.create(result.getSelf()), reader);
		}

	}

	protected MyNodeBody insertNode(MyNode n) {

		// System.out.println(serviceBaseUri + "/data/node");
		
		WebTarget target = this.client
				.target(UriBuilder.fromUri(serviceBaseUri +/* ":" + serviceBasePort +*/ "/data/node"));

		Response response = target // base URI
				.request() // define what types of data can be accepted
				.accept(MediaType.APPLICATION_JSON).post(Entity.json(n)); // POST
																			// invocation

		if (response.getStatus() != 201) {
			System.out.println("Error: " + response.getStatus() + " " + response.getStatusInfo());
		}

		MyNodeBody nb = response.readEntity(MyNodeBody.class);
		if (nb == null) {
			// throw new UnknownIdException("Exception during node uploading");
		}

		return nb;
	}

	protected void loadRelationships(Set<ItemReader> itemList) {
		System.out.println("CARICO LE RELAZIONI");
		for (ItemReader i : itemList) {
			for (ItemReader ci : i.getCitingItems()) { // each next place

				MyRelationship relationship = new MyRelationship(); // new
																	// relationship
				int id = 0;
				int idf = 0;

				if (ci instanceof ArticleReader) {
					
					ArticleReader a = (ArticleReader) ci;

					id = myHash(a.getJournal().getTitle(),
							(a.getJournal().getISSN()));

				} else if (ci instanceof BookReader) {

					BookReader b = (BookReader) ci;
					
					id = myHash(b.getTitle(), b.getISBN());

				}

				if (i instanceof ArticleReader) {
					
					ArticleReader a = (ArticleReader) i;

					idf = myHash(a.getJournal().getTitle(),
							(a.getJournal().getISSN()));

				} else if (i instanceof BookReader) {

					BookReader b = (BookReader) i;
					
					idf = myHash(b.getTitle(), b.getISBN());

				}
				
				String fromid = this.idToNode.get(idf).getSelf();

				URI uri = this.itemToUri.get(id); // URI of next place
				relationship.setTo(uri.toString()); // set to
				relationship.setType("CitedBy"); // connection type
				MyRelationshipBody result = insertRelationship(relationship, fromid); // insertion
																					// in
																					// Neo4j

				// System.out.println("Relationship: " + result.getStart() + " /
				// " + result.getEnd());

				relations.add(URI.create(result.getSelf())); // save the new
																// relation
			}
		}

	}

	protected MyRelationshipBody insertRelationship(MyRelationship r, String id) {

		Response res;
		MyRelationshipBody rb;
		
		// System.out.println("URI RELAZIONI:  " + id + "/relationships");

		WebTarget target = this.client.target(UriBuilder.fromUri(
				/*serviceBaseUri + ":" + serviceBasePort + "/data/node/" + String.valueOf(id) */ id + "/relationships"));

		res = target.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(Entity.json(r));
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
