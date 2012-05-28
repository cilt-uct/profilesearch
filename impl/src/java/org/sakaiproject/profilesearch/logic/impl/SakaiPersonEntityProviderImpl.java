package org.sakaiproject.profilesearch.logic.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RESTful;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RequestAware;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.exception.EntityNotFoundException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

//, ActionsExecutable
public class SakaiPersonEntityProviderImpl extends AbstractEntityProvider implements
		 CoreEntityProvider,
		AutoRegisterEntityProvider, RESTful, RequestAware  {

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
	
	private UserDirectoryService userDirectoryService; 
	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}
	
	//TODO these need to be in components
	private SiteService siteService;
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}
	
	
	private RequestGetter requestGetter;
	public void setRequestGetter(RequestGetter requestGetter) {
		this.requestGetter = requestGetter;
		
	}
	

	
	public final static String ENTITY_PREFIX = "profileClassic";
	
	public String getEntityPrefix() {
		return ENTITY_PREFIX;
	}

	public boolean entityExists(String id) {
		String sakaiPersonId;
		
		sakaiPersonId = id;
		try{
			
			String userId = userDirectoryService.getUserId(sakaiPersonId);
			SakaiPerson sp = sakaiPersonManager.getSakaiPerson(userId, (sakaiPersonManager.getUserMutableType()));
			if (sp != null)
				return true;

		} catch (NumberFormatException e) {
			// invalid number so roll through to the false
		} catch (UserNotDefinedException e) {
			//user not found is expected
			return false;
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
		/*
		if ((!sessionManager.getCurrentSessionUserId().equals(ref.getId())) && ! developerHelperService.isUserAdmin(developerHelperService.getCurrentUserReference())) {
			throw new SecurityException();
		}
		*/	
	      if (ref.getId() == null) {
	          return sakaiPersonManager.getPrototype();
	       }
	       SakaiPerson entity = sakaiPersonManager.getSakaiPerson(ref.getId(), (sakaiPersonManager.getUserMutableType())); 
	       if (entity != null) {
	    	   if (((!sessionManager.getCurrentSessionUserId().equals(ref.getId())) && ! developerHelperService.isUserAdmin(developerHelperService.getCurrentUserReference()))) {
	    		   return getPublicProfile(entity);
	    	   } else {
	    		   return entity;
	    	   }
	       }
	       throw new IllegalArgumentException("Invalid id:" + ref.getId());
	}

	private Object getPublicProfile(SakaiPerson profile) {
		SakaiPerson publicProfile = sakaiPersonManager.getPrototype();
		publicProfile.setAgentUuid(profile.getAgentUuid());
		publicProfile.setGivenName(profile.getGivenName());
		publicProfile.setMail(profile.getMail());
		publicProfile.setSystemPicturePreferred(profile.isSystemPicturePreferred());
		publicProfile.setPictureUrl(profile.getPictureUrl());
		return profile;
	}

	public void deleteEntity(EntityReference ref) {
		// TODO Auto-generated method stub
		
	}

	public List<?> getEntities(EntityReference ref, Search search) {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getHandledOutputFormats() {
		return new String[] {Formats.XML, Formats.JSON};
	}

	public String[] getHandledInputFormats() {
		 return new String[] {Formats.XML, Formats.JSON};
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
	
	
	@EntityCustomAction(action="image",viewKey=EntityView.VIEW_SHOW)
	public Object getProfileImage(OutputStream out, EntityView view, Map<String,Object> params, EntityReference ref) {
		
		//convert input to uuid
		String uuid = ensureUuid(ref.getId());
		if(StringUtils.isBlank(uuid)) {
			throw new EntityNotFoundException("Invalid user.", ref.getId());
		}
		
		SakaiPerson sakaiperson = null;
		boolean wantsThumbnail = "thumb".equals(view.getPathSegment(3)) ? true : false;
		
		//optional siteid
		String siteId = (String)params.get("siteId");
		if(StringUtils.isNotBlank(siteId) && !checkForSite(siteId)){
			throw new EntityNotFoundException("Invalid siteId: " + siteId, ref.getReference());
		}
		
		//get the SP object
		sakaiperson = sakaiPersonManager.getSakaiPerson(uuid, (sakaiPersonManager.getSystemMutableType()));
		
		if(sakaiperson == null) {
			throw new EntityNotFoundException("No profile image for " + ref.getId(), ref.getReference());
		}
		//TODO - we may need to capture the official photo preffered option
		
		String url = sakaiperson.getPictureUrl();
		if(StringUtils.isNotBlank(url)) {
			try {
				requestGetter.getResponse().sendRedirect(url);
			} catch (IOException e) {
				throw new EntityException("Error redirecting to external image for " + ref.getId() + " : " + e.getMessage(), ref.getReference());
			}
		}
		
		return null;
	}

	
	/**
 	* {@inheritDoc}
 	*/
	private String ensureUuid(String userId) {
		
		//check for userId
		try {
			User u = userDirectoryService.getUser(userId);
			if(u != null){
				return userId;
			}
		} catch (UserNotDefinedException e) {
			//do nothing, this is fine, cotninue to next check
		}
		
		//check for eid
		try {
			User u = userDirectoryService.getUserByEid(userId);
			if(u != null){
				return u.getId();
			}
		} catch (UserNotDefinedException e) {
			//do nothing, this is fine, continue
		}
		
		log.error("User: " + userId + " could not be found in any lookup by either id or eid");
		return null;
	}

	
	/**
 	* {@inheritDoc}
 	*/
	private boolean checkForSite(String siteId) {
		return siteService.siteExists(siteId);
	}

	

}
