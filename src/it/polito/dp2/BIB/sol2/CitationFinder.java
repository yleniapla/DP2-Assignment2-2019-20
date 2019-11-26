package it.polito.dp2.BIB.sol2;

import java.util.Properties;
import java.util.Set;

import it.polito.dp2.BIB.BibReader;
import it.polito.dp2.BIB.BibReaderFactory;
import it.polito.dp2.BIB.BookReader;
import it.polito.dp2.BIB.ItemReader;
import it.polito.dp2.BIB.JournalReader;
import it.polito.dp2.BIB.ass2.ServiceException;
import it.polito.dp2.BIB.ass2.UnknownItemException;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

public class CitationFinder implements it.polito.dp2.BIB.ass2.CitationFinder {
	
	private static String serviceBaseUri = "";
	private static String serviceBasePort = "";
	
	// private Client client;				
	private boolean	status;				//system state
	private BibReader 			monitor;	//interface on the Biblio
	private BibReaderFactory	readFactory;

	private Map<String,URI>		placeToUri;	//map placeID-URI
	private Map<URI,String>		UriToPlace;	//map URI-placeID
	private Map<String,NodeBody>	idToNode;		//map placeID-NodeBody
	private Set<URI>			relations;	//all relations in the system

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

}
