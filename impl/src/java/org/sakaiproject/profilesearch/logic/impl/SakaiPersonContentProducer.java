package org.sakaiproject.profilesearch.logic.impl;

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.search.api.EntityContentProducer;
import org.sakaiproject.search.api.SearchIndexBuilder;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.search.model.SearchBuilderItem;

/**
 * @author dhorwitz
 *
 */
public class SakaiPersonContentProducer implements EntityContentProducer {

	// runtime dependency
	private List addEvents = null;

	// runtime dependency
	private List removeEvents = null;
	
	
	private EntityBroker entityBroker;
	public void setEntityBroker(EntityBroker eb) {
		this.entityBroker = eb;
	}
	
	private SakaiPersonManager spm;
	public void setSakaiPersonManager(SakaiPersonManager spm) {
		this.spm = spm;
	}
	
	private ServerConfigurationService serverConfigurationService;
	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService)
	{
		this.serverConfigurationService = serverConfigurationService;
	}
	/**
	 * @param addEvents
	 *        The addEvents to set.
	 */
	public void setAddEvents(List addEvents)
	{
		this.addEvents = addEvents;
	}

	
	// injected dependency
	private SearchService searchService = null;
	/**
	 * @param searchService the searchService to set
	 */
	public void setSearchService(SearchService searchService)
	{
		this.searchService = searchService;
	}
	
	// injected dependency
	private SearchIndexBuilder searchIndexBuilder = null;

	/**
	 * @param searchIndexBuilder the searchIndexBuilder to set
	 */
	public void setSearchIndexBuilder(SearchIndexBuilder searchIndexBuilder)
	{
		this.searchIndexBuilder = searchIndexBuilder;
	}

	
	
	public void init()
	{

		if ( "true".equals(serverConfigurationService.getString(
				"search.enable", "false")))
		{
			for (Iterator i = addEvents.iterator(); i.hasNext();)
			{
				searchService.registerFunction((String) i.next());
			}
			for (Iterator i = removeEvents.iterator(); i.hasNext();)
			{
				searchService.registerFunction((String) i.next());
			}
			searchIndexBuilder.registerEntityContentProducer(this);
		}
	}

	

	public boolean canRead(String reference) {
		// TODO Auto-generated method stub
		return true;
	}

	public String getContainer(String ref) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getContent(String reference) {
		SakaiPerson sp = (SakaiPerson)entityBroker.fetchEntity(reference);
		StringBuilder sb = new StringBuilder();
		sb.append("firstName: " + sp.getGivenName());
		sb.append("lastName: " + sp.getSurname());
		sb.append("email: " + sp.getMail());
		
		return sb.toString();
	}

	public Reader getContentReader(String reference) {
		return new StringReader(getContent(reference));
	}

	public Map getCustomProperties(String ref) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getCustomRDF(String ref) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getId(String ref) {
		SakaiPerson sp = (SakaiPerson)entityBroker.fetchEntity(ref);
		if (sp != null)
			return 	sp.getUid();
		return null;
	}

	public List getSiteContent(String context) {
		// TODO Auto-generated method stub
		return null;
	}

	public Iterator getSiteContentIterator(String context) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSiteId(String reference) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSubType(String ref) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTitle(String reference) {
		// TODO Auto-generated method stub
		SakaiPerson sp = (SakaiPerson)entityBroker.fetchEntity(reference);
		
		return sp.getGivenName() + " " + sp.getSurname();
	}

	public String getTool() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getType(String ref) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUrl(String reference) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isContentFromReader(String reference) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isForIndex(String reference) {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean matches(String reference) {
		// TODO Auto-generated method stub
		return false;
	}

	public Integer getAction(Event event) {
		String evt = event.getEvent();
		if (evt == null) return SearchBuilderItem.ACTION_UNKNOWN;
		for (Iterator i = addEvents.iterator(); i.hasNext();)
		{
			String match = (String) i.next();
			if (evt.equals(match))
			{
				return SearchBuilderItem.ACTION_ADD;
			}
		}
		for (Iterator i = removeEvents.iterator(); i.hasNext();)
		{
			String match = (String) i.next();
			if (evt.equals(match))
			{
				return SearchBuilderItem.ACTION_DELETE;
			}
		}
		return SearchBuilderItem.ACTION_UNKNOWN;
	}

	public boolean matches(Event event) {
		// TODO Auto-generated method stub
		return false;
	}

}
