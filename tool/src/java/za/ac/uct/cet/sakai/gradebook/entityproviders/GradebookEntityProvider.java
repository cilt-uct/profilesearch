package za.ac.uct.cet.sakai.gradebook.entityproviders;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RESTful;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

import za.ac.uct.cet.sakai.gradebook.entityproviders.logic.AbstractExternalLogic;

public class GradebookEntityProvider extends AbstractEntityProvider implements CoreEntityProvider, RESTful  {

	private final static Log log = LogFactory.getLog(GradebookEntityProvider.class);
	public AbstractExternalLogic logic;
	public void setLogic(AbstractExternalLogic logic) {
		this.logic = logic;
	}




	public boolean entityExists(String id) {
		log.info("entityExists(" + id +")");
		try {
			logic.getCourseGradebook(id, null);
			return true;
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (SecurityException se) {
			se.printStackTrace();
		}
		return false;
		
	}

	private static final String PREFIX = "gradebookuct";
	public String getEntityPrefix() {
		return PREFIX;
	}
	

	

	public String createEntity(EntityReference ref, Object entity,
			Map<String, Object> params) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getSampleEntity() {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateEntity(EntityReference ref, Object entity,
			Map<String, Object> params) {
		// TODO Auto-generated method stub
		
	}

	public Object getEntity(EntityReference ref) {
		return logic.getCourseGradebook(ref.getId(), null);
	}

	public void deleteEntity(EntityReference ref, Map<String, Object> params) {
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

	
	

	
}
