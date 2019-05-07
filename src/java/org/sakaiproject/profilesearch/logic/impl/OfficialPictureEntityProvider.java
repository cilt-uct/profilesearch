package org.sakaiproject.profilesearch.logic.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

import lombok.extern.slf4j.Slf4j;

import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;

@Slf4j
public class OfficialPictureEntityProvider extends AbstractEntityProvider implements CoreEntityProvider,
		AutoRegisterEntityProvider {

	
	private SakaiPersonManager sakaiPersonManager;
	public void setSakaiPersonManager(SakaiPersonManager spm) {
		sakaiPersonManager = spm;
	}
	
	public boolean entityExists(String id) {
		log.info("got ref: " + id);
		SakaiPerson sp = sakaiPersonManager.getSakaiPerson(id, (sakaiPersonManager.getSystemMutableType()));
		if (sp != null && sp.getJpegPhoto() != null)
			return true;
		
		return false;
	}

	public String getEntityPrefix() {
		return "official_picture";
	}

}
