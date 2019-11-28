package it.polito.dp2.BIB.sol2;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.net.URI;

import it.polito.dp2.BIB.ArticleReader;
import it.polito.dp2.BIB.BibReader;
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

	private Map<Integer, URI> itemToUri; // map placeID-URI
	// private Map<URI, String> UriToPlace; // map URI-placeID
	// private Map<String, NodeBody> idToNode; // map placeID-NodeBody
	private Set<URI> relations; // all relations in the system

	public CitationFinder(Properties prop) {
		serviceBaseUri = prop.getProperty("it.polito.dp2.BIB.ass2.URL");
		serviceBasePort = prop.getProperty("it.polito.dp2.BIB.ass2.PORT");
		// TODO Auto-generated constructor stub
	}

	@Override
	public BookReader getBook(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ItemReader> getItems(String arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JournalReader getJournal(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<JournalReader> getJournals(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ItemReader> findAllCitingItems(ItemReader item, int maxDepth)
			throws UnknownItemException, ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	protected void loadGraph(Set<ItemReader> itemList) {
		if (itemList != null) {
			// load nodes and relations
			loadNodes(itemList);
			loadRelationships(itemList);
		}
		return;
	}

	protected void loadNodes(Set<ItemReader> itemList) {
		for (ItemReader reader : itemList) { // each place
			Node node = new Node(); // new node
			
			int id = 0;
			
			if (reader instanceof ArticleReader) {

				id = myHash(((ArticleReader) reader).getJournal().getTitle(),
						((ArticleReader) reader).getIssue().getYear());

			} else if (reader instanceof BookReader) {

				id = myHash(reader.getTitle(), ((BookReader) reader).getYear());

			}
			
			node.setId(id);

			NodeBody result;

			result = insertNode(node); // insertion in Neo4j

			this.itemToUri.put(id, URI.create(node.getSelf())); 
		}

	}

	protected NodeBody insertNode(Node n) {

		WebTarget target = this.client.target(UriBuilder.fromUri(serviceBaseUri + ":" + serviceBasePort + "/data/node"));

		Response response = target // base URI
				.request() // define what types of data can be accepted
				.accept(MediaType.APPLICATION_JSON).post(Entity.json(n)); // POST
																			// invocation

		if (response.getStatus() != 201) {
			System.out.println("Error: " + response.getStatus() + " " + response.getStatusInfo());
		}

		NodeBody nb = response.readEntity(NodeBody.class);
		if (nb == null) {
			// throw new UnknownIdException("Exception during node uploading");
		}

		return nb;
	}

	protected void loadRelationships(Set<ItemReader> itemList) {
		for (ItemReader i : itemList) {
			for (ItemReader ci : i.getCitingItems()) { // each next place

				Relationship relationship = new Relationship(); // new relationship
				int id = 0;
				
				if (ci instanceof ArticleReader) {

					id = myHash(((ArticleReader) ci).getJournal().getTitle(),
							((ArticleReader) ci).getIssue().getYear());

				} else if (ci instanceof BookReader) {

					id = myHash(ci.getTitle(), ((BookReader) ci).getYear());

				}
				
				URI uri = this.itemToUri.get(id); // URI of next place
				//int id = this.idToNode.get(p.getId()).metadata.getId().intValue();
				relationship.setTo(uri.toString()); // set to
				relationship.setType("CitedBy"); // connection type
				RelationshipBody result = insertRelationship(relationship, id); // insertion in Neo4j

				// System.out.println("Relationship: " + result.getStart() + " /
				// " + result.getEnd());

				relations.add(URI.create(result.getSelf())); // save the new relation
			}
		}

	}

	protected RelationshipBody insertRelationship(Relationship r, int id) {

		Response res;
		RelationshipBody rb;

		WebTarget target = this.client
				.target(UriBuilder.fromUri(serviceBaseUri + ":" + serviceBasePort + "/data/node/" + String.valueOf(id) + "/relationships"));

		res = target.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).post(Entity.json(r));
		if (res.getStatus() != 201) {
			System.out.println("Error: " + res.getStatus() + " " + res.getStatusInfo());
		}
		rb = res.readEntity(RelationshipBody.class);

		return rb;
	}

	private int myHash(String title, int year) {
		int hash = 7;
		int hash1 = 0;
		for (int i = 0; i < title.length(); i++) {
			hash1 += 32 * hash + title.charAt(i);
		}
		return (hash1 * year);
	}

	private int myIssueHash(int number, int year) {
		int hash = 7;
		int hash1 = 0;
		for (int i = 0; i < number; i++) {
			hash1 += 32 * hash + i;
		}
		return (hash1 * year);
	}

}
