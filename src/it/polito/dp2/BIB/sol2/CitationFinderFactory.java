package it.polito.dp2.BIB.sol2;

import java.util.Properties;

import it.polito.dp2.BIB.ass2.CitationFinder;
import it.polito.dp2.BIB.ass2.CitationFinderException;

public class CitationFinderFactory extends it.polito.dp2.BIB.ass2.CitationFinderFactory{

	@Override
	public CitationFinder newCitationFinder() throws CitationFinderException {
		
			Properties prop = System.getProperties();
			return new it.polito.dp2.BIB.sol2.CitationFinder(prop);
			
	
	}

}
