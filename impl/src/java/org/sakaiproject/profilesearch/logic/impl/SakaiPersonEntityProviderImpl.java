package org.sakaiproject.profilesearch.logic.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RESTful;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
//import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;

//, ActionsExecutable
public class SakaiPersonEntityProviderImpl extends AbstractEntityProvider implements
		 CoreEntityProvider,
		AutoRegisterEntityProvider, RESTful  {

	private static Log log = LogFactory.getLog(SakaiPersonEntityProviderImpl.class);
	
	private SakaiPersonManager sakaiPersonManager;
	public void setSakaiPersonManager(SakaiPersonManager spm) {
		sakaiPersonManager = spm;
	}

	private SessionManager sessionManager;
	

	public void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	private DeveloperHelperService developerHelperService;
	public void setDeveloperHelperService(
			DeveloperHelperService developerHelperService) {
		this.developerHelperService = developerHelperService;
	}
	
	public final static String ENTITY_PREFIX = "profile";
	
	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public boolean entityExists(String id) {
		String sakaiPersonId;
		
		sakaiPersonId = id;
		try{
			SakaiPerson sp = sakaiPersonManager.getSakaiPerson(sakaiPersonId, (sakaiPersonManager.getUserMutableType()));
			if (sp != null)
				return true;

		} catch (NumberFormatException e) {
			// invalid number so roll through to the false
		}

		log.warn("SakaiPerson: " + id +" does not exist");
		return false;
	}

	public String createEntity(EntityReference ref, Object entity) {
		SakaiPerson sp = (SakaiPerson) entity;
		sakaiPersonManager.save(sp);
		return sp.getUid();
	}

	public Object getSampleEntity() {
		// TODO Auto-generated method stub
		SakaiPerson sp = sakaiPersonManager.getPrototype();
		return sp;
	}

	public void updateEntity(EntityReference ref, Object entity) {
		// TODO Auto-generated method stub
		
	}

	public Object getEntity(EntityReference ref) {
		
		if (sessionManager.getCurrentSessionUserId() == null) {
			throw new SecurityException();
		}
		
		// VULA-146 For now only allow the user's own profile
		if ((!sessionManager.getCurrentSessionUserId().equals(ref.getId())) && ! developerHelperService.isUserAdmin(developerHelperService.getCurrentUserReference())) {
			throw new SecurityException();
		}
				
	      if (ref.getId() == null) {
	          return sakaiPersonManager.getPrototype();
	       }
	       SakaiPerson entity = sakaiPersonManager.getSakaiPerson(ref.getId(), (sakaiPersonManager.getUserMutableType())); 
	       if (entity != null) {
	          return entity;
	       }
	       throw new IllegalArgumentException("Invalid id:" + ref.getId());
	}

	public void deleteEntity(EntityReference ref) {
		// TODO Auto-generated method stub
		
	}

	public List<?> getEntities(EntityReference ref, Search search) {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getHandledOutputFormats() {
		return new String[] {Formats.HTML, Formats.XML, Formats.JSON};
	}

	public String[] getHandledInputFormats() {
		 return new String[] {Formats.HTML, Formats.XML, Formats.JSON};
	}

	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params) {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateEntity(EntityReference ref, Object entity, Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	public List<?> getEntities(EntityReference ref, Search search, Map<String, Object> params) {
		// TODO Auto-generated method stub
		return null;
	}

}
