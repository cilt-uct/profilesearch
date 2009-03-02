package org.sakaiproject.profilesearch.logic.impl;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.blogwow.constants.BlogConstants;
import org.sakaiproject.blogwow.logic.BlogLogic;
import org.sakaiproject.blogwow.logic.EntryLogic;
import org.sakaiproject.blogwow.model.BlogWowBlog;
import org.sakaiproject.blogwow.model.BlogWowEntry;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.search.api.EntityContentProducer;
import org.sakaiproject.search.api.SearchIndexBuilder;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.search.model.SearchBuilderItem;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.FormattedText;

public class BlogwowEntryEntityContentProducer implements EntityContentProducer {
	private static Log log = LogFactory.getLog(BlogwowEntryEntityContentProducer.class);
	
	private BlogLogic blogLogic;
	public void setBlogLogic(BlogLogic blogLogic) {
		this.blogLogic = blogLogic;
	}


	private EntryLogic entryLogic;
	public void setEntryLogic(EntryLogic entryLogic) {
		this.entryLogic = entryLogic;
	}

	// runtime dependency
	private List addEvents = null;

	// runtime dependency
	private List removeEvents = null;
	
	/**
	 * @param addEvents
	 *        The addEvents to set.
	 */
	public void setAddEvents(List addEvents)
	{
		this.addEvents = addEvents;
	}

	
	public void setRemoveEvents(List removeEvents) {
		this.removeEvents = removeEvents;
	}
	
	// runtime dependency
	private String toolName = null;
	/**
	 * @param toolName
	 *        The toolName to set.
	 */
	public void setToolName(String toolName)
	{
		this.toolName = toolName;
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
	
	private ServerConfigurationService serverConfigurationService;
	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService)
	{
		this.serverConfigurationService = serverConfigurationService;
	}
	
	
	private EntityBroker entityBroker;
	public void setEntityBroker(EntityBroker eb) {
		this.entityBroker = eb;
	}
	
	
	private SessionManager sessionManager;
	public void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
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
	
	/*
	 * Entityty content methods
	 * 
	 */
	
	public boolean canRead(String reference) {
		// for now return true as blogs are public
		return true;
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

	public String getContainer(String ref) {
		BlogWowBlog blog = getBlogByRef(ref);
		if (blog != null) {
			return blog.getLocation();
		}
		return null;
	}
	
	
	private BlogWowEntry getEntryByref(String ref) {
	
		String id = EntityReference.getIdFromRef(ref);
		Session session = sessionManager.getCurrentSession();
		session.setUserId("admin");
		session.setUserEid("admin");
		BlogWowEntry ret =  entryLogic.getEntryById(id, null);
		session.setUserId(null);
		session.setUserEid(null);
		return ret;
	}

	private BlogWowBlog getBlogByRef(String ref) {
		String id = EntityReference.getIdFromRef(ref);
		Session session = sessionManager.getCurrentSession();
		session.setUserId("admin");
		session.setUserEid("admin");
		BlogWowBlog blog = blogLogic.getBlogById(id);
		session.setUserId(null);
		session.setUserEid(null);
		return blog;
	}
	public String getContent(String reference) {
		log.debug("getContent(" + reference);
		BlogWowEntry entry = getEntryByref(reference);
		StringBuilder sb = new StringBuilder();
		sb.append(" Title: " + entry.getTitle());
		sb.append(" Body: " + FormattedText.convertFormattedTextToPlaintext(entry.getText()));
		log.debug("adding text: " + sb.toString());
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
		return EntityReference.getIdFromRef(ref);
	}

	/**
	 * deprecated
	 */
	public List getSiteContent(String context) {
				
		return null;
	}

	public Iterator getSiteContentIterator(String context) {
		log.debug("getSiteContentIterator(" + context);
		List<BlogWowBlog> blogs = blogLogic.getAllVisibleBlogs("/site/" + context, null, true, 0, 0);
		List<String> ids = new ArrayList<String>();
		log.debug("got a list of " + blogs.size() + " blogs");
		for (int i = 0; i < blogs.size(); i++) {
			BlogWowBlog blog = blogs.get(i);
			String id = blog.getId();
			log.debug("adding to id list: " + id);
			ids.add(id);
		}
		log.debug("finished with id list ");
		String[] idArray = ids.toArray(new String[ids.size()]);
		List<String> ret = new ArrayList<String>();
		log.debug("getting entries for all blogs");
		List<BlogWowEntry> entries = entryLogic.getAllVisibleEntries(idArray, "admin", null, true, 0, 0);
		for (int i =0; i < entries.size(); i++) {
			BlogWowEntry ent = entries.get(i);
			String ref = "/blog-entry/" + ent.getId();
			log.debug("adding " + ref);
			ret.add(ref);
			
		}
		log.debug("found " + ret.size() + " entries");
		
		return ret.iterator();
	}

	public String getSiteId(String reference) {
		BlogWowEntry entry = getEntryByref(reference);
		if (entry == null)
			return null;
		
		String context = entry.getBlog().getLocation();
		return EntityReference.getIdFromRef(context);
	}

	public String getSubType(String ref) {
		return "blog-entry";
	}

	public String getTitle(String reference) {
		BlogWowEntry entry = getEntryByref(reference);
		if (entry == null)
			return null;
		return entry.getTitle();
	}

	public String getTool() {
		return "blog";
	}

	public String getType(String ref) {
		return "Blog-type";
	}

	public String getUrl(String reference) {
		BlogWowEntry entry = getEntryByref(reference);
		if (entry == null)
			return null;
		return "/direct/blog-entry/" + entry.getId();
	}

	public boolean isContentFromReader(String reference) {
		return false;
	}

	public boolean isForIndex(String reference) {
		BlogWowEntry entry = getEntryByref(reference);
		if (entry == null)
			return false;
		if (BlogConstants.PRIVACY_PRIVATE.equals(entry.getPrivacySetting()))
			return false;
				
		return true;
	}


	public boolean matches(String reference) {
		String prefix = EntityReference.getPrefix(reference);
		log.debug("checkin if " + prefix + " matches");
		if ("blog-entry".equals(prefix))
			return true;
		
		return false;
	}

	public boolean matches(Event event) {
		// TODO Auto-generated method stub
		return matches(event.getResource());
	}

}
