package org.sakaiproject.profilesearch.logic.impl;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.api.common.type.TypeManager;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityBroker;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.search.api.EntityContentProducer;
import org.sakaiproject.search.api.SearchIndexBuilder;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.search.model.SearchBuilderItem;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * @author dhorwitz
 *
 */
public class SakaiPersonContentProducer implements EntityContentProducer {

	private static Log log = LogFactory.getLog(SakaiPersonContentProducer.class);
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
	
	private SiteService siteService;
	
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	private AuthzGroupService authzGroupService;
	
	public void setAuthzGroupService(AuthzGroupService authzGroupService) {
		this.authzGroupService = authzGroupService;
	}
	
	private DeveloperHelperService developerHelperService;
	public void setDeveloperHelperService(
			DeveloperHelperService developerHelperService) {
		this.developerHelperService = developerHelperService;
	}

	private UserDirectoryService userDirectoryService;
	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
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
	
	public void init()
	{

		if ( "true".equals(serverConfigurationService.getString(
				"search.enable", "false")))
		{
			for (Iterator i = addEvents.iterator(); i.hasNext();)
			{
				searchService.registerFunction((String) i.next());
			}
			/* there are no remove events for sakaiperson!
			for (Iterator i = removeEvents.iterator(); i.hasNext();)
			{
				searchService.registerFunction((String) i.next());
			}
			*/
			searchIndexBuilder.registerEntityContentProducer(this);
		}
	}

	

	public boolean canRead(String reference) {
		// TODO Auto-generated method stub
		return true;
	}

	public String getContainer(String reference) {
		
		EntityReference ref = new EntityReference(reference);
		String ret = ref.getSpaceReference();
		return ret;
	}

	public String getContent(String reference) {
		log.info("getting " + reference);
		SakaiPerson sp = spm.getSakaiPerson(getId(reference), spm.getUserMutableType());
		StringBuilder sb = new StringBuilder();
		if (sp != null) {
			sb.append("firstName: " + sp.getGivenName());
			sb.append("lastName: " + sp.getSurname());
			sb.append("email: " + sp.getMail());
		}
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

	public String getId(String reference) {
		EntityReference ref = new EntityReference(reference);
		String ret = ref.getId();
		return ret;
	}

	
	public List getSiteContent(String context) {

		log.info("getting SakaiPersons in " + context);
		List<String> all = new ArrayList<String>();
		if ("!admin".equals(context)) {
			
			//get the members of the site

			//context is a site id
			String ref = siteService.siteReference(context);
			List<User> users = userDirectoryService.getUsers(1,500);
			log.info("got "  + users.size() + " members");
			for (int i = 0; i < users.size(); i++) {
				User me = (User)users.get(i);
				String userId = me.getId();
				String pref = "/SakaiPerson/type/" + spm.getUserMutableType().getUuid() + "/" + userId;
				log.info("adding " + pref);
				all.add(pref);
			}

			return all;

		} else {
			log.info("wont look for users in " + context);
		}

		return all;
	}

	public Iterator getSiteContentIterator(String context) {
		
		return getSiteContent(context).iterator();
	}

	public String getSiteId(String reference) {
		// TODO Auto-generated method stub
		return ".auth";
				
	}
	
	
	public String getType(String reference) {
		EntityReference ref = new EntityReference(reference);
		String ret = ""; // ref.getIdFromRefByKey("ref", "type");
		return ret;
	}

	public String getSubType(String reference) {
		return null;
	}

	public String getTitle(String reference) {
		// TODO Auto-generated method stub
		log.info("getTitle(String " + reference +" )");
		SakaiPerson sp = spm.getSakaiPerson(getId(reference), spm.getUserMutableType());
		return sp.getGivenName() + " " + sp.getSurname();
	}

	public String getTool() {
		return toolName;
	}



	public String getUrl(String reference) {
		// /viewProfile?id=t0016405
		EntityReference ref = new EntityReference(reference);
		String userId = ref.getId();
		String eid;
		String url = null;
		try {
			eid = userDirectoryService.getUserEid(userId);
			Map<String, String> parameters = new HashMap<String,String>();
			parameters.put("id", eid);
			url = developerHelperService.getToolViewURL("saka.profilewow", "/viewProfile", parameters, null);
			
		} catch (UserNotDefinedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return url;
	}

	public boolean isContentFromReader(String reference) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isForIndex(String reference) {
		log.info("isForIndex " + reference);
		EntityReference ref = new EntityReference(reference);
		SakaiPerson sp = this.spm.getSakaiPerson(ref.getId(), spm.getSystemMutableType());
		if (sp != null) {
			log.info("is for index");
			return true;
		}
		return false;
	}

	
	public boolean matches(String reference) {
		EntityReference ref = new EntityReference(reference);
		String prefix = ref.getPrefix();
		log.info(reference + "tool ref: " + prefix);
		if (toolName.endsWith(prefix)) {
			log.info("Matches!");
			return true;
		}
		
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
