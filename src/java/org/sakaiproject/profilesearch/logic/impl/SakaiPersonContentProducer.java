/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.profilesearch.logic.impl;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.search.api.EntityContentProducer;
import org.sakaiproject.search.api.SearchIndexBuilder;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.search.model.SearchBuilderItem;
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
	private List<String> addEvents = null;

	// runtime dependency
	private List<String> removeEvents = null;
	
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
	

	private UserDirectoryService userDirectoryService;
	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}

	/**
	 * @param addEvents
	 *        The addEvents to set.
	 */
	public void setAddEvents(List<String> addEvents)
	{
		this.addEvents = addEvents;
	}

	
	public void setRemoveEvents(List<String> removeEvents) {
		this.removeEvents = removeEvents;
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
			for (Iterator<String> i = addEvents.iterator(); i.hasNext();)
			{
				searchService.registerFunction((String) i.next());
			}
			
			for (Iterator<String> i = removeEvents.iterator(); i.hasNext();)
			{
				searchService.registerFunction((String) i.next());
			}
			
			searchIndexBuilder.registerEntityContentProducer(this);
		}
	}

	

	public boolean canRead(String reference) {
		return true;
	}

	public String getContainer(String reference) {
		
		EntityReference ref = new EntityReference(reference);
		String ret = ref.getSpaceReference();
		return ret;
	}

	public String getContent(String reference) {
		log.debug("getting " + reference);
		SakaiPerson sp = getSakaiPersonFromRef(reference);
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

	public Map<String, ?> getCustomProperties(String ref) {
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

	
	private List<String> getSiteContent(String context) {

		log.debug("getting SakaiPersons in " + context);
		List<String> all = new ArrayList<String>();
		if ("!admin".equals(context)) {
			
			//get the members of the site

			//context is a site id
			int first = 1;
			int last = 500;
			int increment = 500;
			
			boolean doAnother = true;
			while (doAnother) {
				log.debug("first: " + first + " last " + last);
				List<User> users = userDirectoryService.getUsers(first, last);
				log.debug("got "  + users.size() + " users");
				for (int i = 0; i < users.size(); i++) {
					User me = (User)users.get(i);
					String userId = me.getId();
					String pref = "/" + toolName + "/type/" + spm.getUserMutableType().getUuid() + "/id/" + userId;
					log.debug("adding " + pref);
					all.add(pref);
				}
				if (users.size() < increment) {
					doAnother = false;
				} else {
					first = last +1;
					last = last + increment;
				}
			}

			return all;

		} else {
			log.debug("wont look for users in " + context);
		}

		return all;
	}

	public Iterator<String> getSiteContentIterator(String context) {
		
		return getSiteContent(context).iterator();
	}

	public String getSiteId(String reference) {
		
		return "~global";
		//return "0c383443-7b3e-4240-a42c-d8046de34473";
				
	}
	
	
	public String getType(String reference) {
		String ret = ""; // ref.getIdFromRefByKey("ref", "type");
		return ret;
	}

	public String getSubType(String reference) {
		return null;
	}

	public String getTitle(String reference) {
		// TODO Auto-generated method stub
		log.debug("getTitle(String " + reference +" )");
		SakaiPerson sp = getSakaiPersonFromRef(reference);
		return sp.getGivenName() + " " + sp.getSurname();
	}

	public String getTool() {
		return toolName;
	}



	public String getUrl(String reference) {
		// /viewProfile?id=t0016405
		SakaiPerson sp = getSakaiPersonFromRef(reference);
		String userId = sp.getUid();
		String eid;
		String url = null;
		try {
			eid = userDirectoryService.getUserEid(userId);
			Map<String, String> parameters = new HashMap<String,String>();
			parameters.put("id", eid);
			//url = developerHelperService.getToolViewURL("sakai.profilewow", "/viewProfile", parameters, "/site/~01302922/");
			url = "/direct/"  + toolName + "/" + userId;
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
		log.debug("isForIndex " + reference);
		SakaiPerson sp = getSakaiPersonFromRef(reference);
		if (sp != null) {
			//check this is not orphaned
			try {
				User user = userDirectoryService.getUser(sp.getAgentUuid());
				
				//we don't add inactive types
				if ("Inactive".equals(user.getType()) || "inactiveStaff".equals(user.getType()) || "inactiveStaff".equals(user.getType())) {
					log.debug("we won't add user of " + user.getType() + " type");
					return false;
				}
			}
			catch (UserNotDefinedException e) {
				log.warn("No user found for: " + sp.getAgentUuid());
				return false;
			}
			log.debug("we index this one index");
			return true;
		} 
		log.debug("no sakai person found for id " + reference);
		return false;
	}

	private SakaiPerson getSakaiPersonFromRef(String reference) {
		final String id = EntityReference.getIdFromRefByKey(reference, "id");
		SakaiPerson sp = this.spm.getSakaiPerson(id, spm.getUserMutableType());
		return sp;
	}

	
	public boolean matches(String reference) {
		EntityReference ref = new EntityReference(reference);
		String prefix = ref.getPrefix();
		log.debug(reference + "tool ref: " + prefix);
		if (toolName.endsWith(prefix)) {
			log.debug("Matches!");
			return true;
		}
		
		return false;
	}

	public Integer getAction(Event event) {
		String evt = event.getEvent();
		if (evt == null) return SearchBuilderItem.ACTION_UNKNOWN;
		for (Iterator<String> i = addEvents.iterator(); i.hasNext();)
		{
			String match = (String) i.next();
			if (evt.equals(match))
			{
				return SearchBuilderItem.ACTION_ADD;
			}
		}
		for (Iterator<String> i = removeEvents.iterator(); i.hasNext();)
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
		return matches(event.getResource());
	}

}
