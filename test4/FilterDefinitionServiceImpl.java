package com.projectecho.mc;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.exacttarget.fuelsdk.ETSdkException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectecho.et.ETClientObject;
import com.projectecho.et.ETDataExtensionColumnObject;
import com.projectecho.et.ETFolderObject;
import com.projectecho.et.ETRetrieveDataExtensionObject;
import com.projectecho.et.ETRetrieveFolderObject;
import com.projectecho.et.rest.CBBaseObject;
import com.projectecho.et.rest.FilterDefinitionObject;
import com.projectecho.et.rest.FilterDefinitionObjectList;
import com.projectecho.et.rest.MeasureObject;
import com.projectecho.et.rest.ProfileAttributeObject;
import com.projectecho.et.rest.FilterDefinitionObject.DerivedFromObjectTypeNameEnum;
import com.projectecho.exceptions.CustomException;
import com.projectecho.mvcmodels.SnapshotFilterDefinitionObject;

//@SessionScope
@Service
public class FilterDefinitionServiceImpl extends CommonRestServiceImpl implements FilterDefinitionService {
	
	private final String objectPath = "email";
	private final String defaultApiPath = "filters/filterdefinition";
	
	
	@Autowired
    private FolderSearchService folderService;
	
	@Autowired
    private DataExtensionService deService;
	
	@Autowired
    private ProfileAttributeService profileAttributeService;
	
	@Autowired
    private MeasureObjectService measureService;
	
	public List<FilterDefinitionObject> getByNameLikeInFolder(String accessToken, String endpoint, Integer folderId, String name, List<String> errorCollection) throws Exception {
		
		List<FilterDefinitionObject> ftList = new ArrayList<FilterDefinitionObject>();
		
		FilterDefinitionObjectList list = this.getAllInFolder(accessToken, endpoint, folderId, null, errorCollection);
		if(list == null || list.getItems().isEmpty()) {
			if(errorCollection != null) {
				errorCollection.add("no filter definition found !");
			}
			else {
				throw new CustomException("no filter definition found !");
			}
			return null;
		}
		
		for(FilterDefinitionObject fd : list.getItems()) {
			if(fd.getName().toLowerCase().contains(name)) {
				ftList.add(fd);
			} else {
				int levensteinDistance = StringUtils.getLevenshteinDistance(fd.getName(), name);
				if(levensteinDistance < 5) {
					ftList.add(fd);
				}
			}
			
		}
		
		return ftList;
	}
	
	@Override
	public FilterDefinitionObject getByNameInFolder(String accessToken, String endpoint, Integer folderId, String name, List<String> errorCollection) throws CustomException, Exception {
		FilterDefinitionObjectList list = this.getAllInFolder(accessToken, endpoint, folderId, null, errorCollection);
		if(list == null || list.getItems().isEmpty()) {
			if(errorCollection != null) {
				errorCollection.add("no filter definition found !");
			}
			else {
				throw new CustomException("no filter definition found !");
			}
			return null;
		}
		for(FilterDefinitionObject fd : list.getItems()) {
			if(fd.getName().equalsIgnoreCase(name)) {
				return fd;	
			}
		}
		
		return null;
	}
	
	@Override
	public FilterDefinitionObject getByID(String accessToken, String endpoint, String id, List<String> errorCollection) throws CustomException, Exception {
		String apiPath = this.defaultApiPath+"/"+id;
		ResponseEntity<FilterDefinitionObject> result = super.call(accessToken, endpoint, this.objectPath, apiPath, HttpMethod.GET, org.springframework.http.MediaType.APPLICATION_JSON, null, FilterDefinitionObject.class);
		if(result != null && result.getStatusCode().equals(HttpStatus.OK) ) {
			return result.getBody();
		} else if(result != null) {
			if(errorCollection != null) {
				errorCollection.add(result.getStatusCodeValue()+" "+result.getBody().getMessage());
			} else {
				throw new CustomException(result.getStatusCodeValue()+" "+result.getBody().getMessage());
			}
		}
		return null;
	}
	
	@Override
	public FilterDefinitionObjectList getAllInFolder(ETClientObject etClientObject, Integer mid, String accessToken, String endpoint, String folderPath, List<String> errorCollection) throws Exception {
		ETFolderObject targetFolder = this.folderService.retrieveTargetETFolderObject(etClientObject, mid, folderPath);
		return this.getAllInFolder(accessToken, endpoint, Integer.parseInt(targetFolder.getId()), null, errorCollection);
	}
	
	@Override
	public FilterDefinitionObjectList getAllInFolder(String accessToken, String endpoint, Integer categoryId, MultiValueMap<String, String> additionalParams, List<String> errorCollection) throws Exception {
		FilterDefinitionObjectList fdList = new FilterDefinitionObjectList();
		fdList.setItems(new ArrayList<FilterDefinitionObject>());
		int page = 1;
		String apiPath = this.defaultApiPath+"/category/"+categoryId;
		while(true) {
			ResponseEntity<FilterDefinitionObjectList> result = super.getList(accessToken, endpoint, this.objectPath, apiPath, page, 200, additionalParams, FilterDefinitionObjectList.class);
			if(result != null && result.getStatusCode().equals(HttpStatus.OK)) {
				if(result.getBody().getCount() < 1) {
					break;
				}
				fdList.getItems().addAll(result.getBody().getItems());
				fdList.setCount(fdList.getCount() + result.getBody().getItems().size());
				if(result.getBody().getCount() <= fdList.getCount() ) {
					break;
				}
				page++;
			} else if (result != null) {
				if(errorCollection != null) {
					errorCollection.add(result.getStatusCodeValue() + " " + result.getBody().getMessage());
				}
				break;
			} else {
				break;
			}
		}
		
		
		return fdList;
	}
	
	
	@Override
	public FilterDefinitionObject create(ETClientObject etClientObject, Integer mid, String accessToken, String endpoint, SnapshotFilterDefinitionObject ssfd, List<String> errorCollection) throws CustomException, Exception {
		if(ssfd.getDerivedFromObjectTypeName().equalsIgnoreCase(DerivedFromObjectTypeNameEnum.DATA_EXTENSION.value())) {
			Integer deMID = mid;
			try {
				ETRetrieveDataExtensionObject deObj = this.deService.retrieveByName(etClientObject, deMID, ssfd.getDerivedFromObjectName());
				if(deObj == null) {
					deObj = this.deService.retrieveByName(etClientObject, null, ssfd.getDerivedFromObjectName());
					deMID = null;
				}
				ssfd.setDerivedFromObjectId(deObj.getId());
				try {
					String xml = ssfd.getFilterDefinitionXml();
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document xmlDoc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
					NodeList filterDefinitionElements = xmlDoc.getElementsByTagName("FilterDefinition");
					for(int idx = 0; idx < filterDefinitionElements.getLength(); idx++) {
						Node filterDefinitionElement = filterDefinitionElements.item(idx);
						if(filterDefinitionElement.getNodeType() == Node.ELEMENT_NODE) {
							Element eElement = (Element) filterDefinitionElement;
							eElement.setAttribute("SourceID", deObj.getId());
						}
					}
					List<ETDataExtensionColumnObject> columns = this.deService.retrieveColumns(etClientObject, deMID, deObj.getKey());
					
					NodeList conditionElements = xmlDoc.getElementsByTagName("Condition");
					for(int idx = 0; idx < conditionElements.getLength(); idx++) {
						Node conditionElement = conditionElements.item(idx);
						if(conditionElement.getNodeType() == Node.ELEMENT_NODE) {
							Element eElement = (Element) conditionElement;
							String conditionID = eElement.getAttribute("ID");
							if( conditionID!= null && !conditionID.isEmpty()) {
								for(ETDataExtensionColumnObject column: columns) {
									if(column != null && column.getName()!=null && column.getName().equalsIgnoreCase(conditionID)) {
										eElement.setAttribute("ID",column.getId());
										break;
									}
								}
							}
						}
						
					}
					TransformerFactory tf = TransformerFactory.newInstance();
				    Transformer trans = tf.newTransformer();
				    trans.setOutputProperty("omit-xml-declaration", "yes");
				    StringWriter sw = new StringWriter();
				    trans.transform(new DOMSource(xmlDoc), new StreamResult(sw));
					ssfd.setFilterDefinitionXml(sw.toString());
				}catch(Exception ee) {
					throw new CustomException("Failed to map filter conditions with Data Extension columns for DE:"+ssfd.getDerivedFromObjectName()+" for Data Filter: "+ssfd.getName());
				}
			}catch(Exception ee) {
				throw new CustomException("Failed to retrieve Data Extension named "+ssfd.getDerivedFromObjectName()+" for Data Filter: "+ssfd.getName());
			}
			
		} else if(ssfd.getDerivedFromObjectTypeName().equalsIgnoreCase(DerivedFromObjectTypeNameEnum.SUBSCRIBER_ATTRIBUTE.value())) {
			try {
				List<ProfileAttributeObject> attrList = this.profileAttributeService.getAll(accessToken, endpoint, null, errorCollection);
				if(attrList == null || attrList.isEmpty()) {
					attrList = new ArrayList<ProfileAttributeObject>();
				}
				String xml = ssfd.getFilterDefinitionXml();
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document xmlDoc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
				NodeList conditionElements = xmlDoc.getElementsByTagName("Condition");
				for(int idx = 0; idx < conditionElements.getLength(); idx++) {
					Node conditionElement = conditionElements.item(idx);
					if(conditionElement.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) conditionElement;
						String conditionID = eElement.getAttribute("ID");
						String sourceTypeVal = eElement.getAttribute("SourceType");
						if( conditionID!= null && !conditionID.isEmpty()) {
							if(sourceTypeVal == null || sourceTypeVal.isEmpty()) {
								for(ProfileAttributeObject attr: attrList) {
									if(attr != null && attr.getName()!=null && attr.getName().equalsIgnoreCase(conditionID)) {
										eElement.setAttribute("ID",attr.getId());
										break;
									}
								}
								if(attrList.isEmpty()) {
									eElement.setAttribute("Name",conditionID);
								}
							} else if(sourceTypeVal.equalsIgnoreCase("measure")) {
								try {
									String objectName = conditionID.substring(conditionID.lastIndexOf("|")+1).trim();
									String folderPath = conditionID.substring(0, conditionID.lastIndexOf("|")).trim();
									ETFolderObject folder = this.folderService.retrieveTargetETFolderObject(etClientObject, mid, folderPath);
									MeasureObject measure = this.measureService.getByNameInFolder(accessToken, endpoint, Integer.parseInt(folder.getId()), objectName, null);
									if(measure != null) {
										eElement.setAttribute("ID",measure.getId());
									} else {
										eElement.setAttribute("Name",objectName);
									}
								}catch(Exception e) {
									
								}
								
							}
						}
					}
					
				}
				TransformerFactory tf = TransformerFactory.newInstance();;
			    Transformer trans = tf.newTransformer();
			    trans.setOutputProperty("omit-xml-declaration", "yes");
			    StringWriter sw = new StringWriter();
			    trans.transform(new DOMSource(xmlDoc), new StreamResult(sw));
			    ssfd.setFilterDefinitionXml(sw.toString());
			}catch(Exception e) {
				if(errorCollection != null) {
					errorCollection.add("Failed to map filter conditions with Profile Attributes OR Measures for Data Filter:"+ssfd.getName());
				}
			}
		}
		try {
			List<ETRetrieveFolderObject> folders = this.folderService.createMissingFoldersInPath(etClientObject, mid, ssfd.getFolderPath(), com.projectecho.utils.ETFolderContentType.FILTER_DEFINITION);
			String folderId = folders.get(folders.size() - 1).getId();
			ssfd.setFolderPath(folderId);
		}catch(Exception ee) {
			throw new CustomException("Failed to create folder path for Data Filter: "+ssfd.getName());
		}
		
		FilterDefinitionObject fd = new FilterDefinitionObject();		
		fd.clone(ssfd);
		fd.setId(null);
		
		ResponseEntity<FilterDefinitionObject> result  = null;
		try {
			result = super.post(accessToken, endpoint, this.objectPath, this.defaultApiPath, fd, FilterDefinitionObject.class);
		} catch(org.springframework.web.client.HttpClientErrorException httpException) {
			try {
				String x = httpException.getResponseBodyAsString();
				com.projectecho.et.rest.RestBaseObject errorContent = new ObjectMapper().readValue(x, com.projectecho.et.rest.RestBaseObject.class);
				String reasons = errorContent.getMessage();
				if(errorContent.getValidationErrors() != null && !errorContent.getValidationErrors().isEmpty()) {
					for(CBBaseObject validationError : errorContent.getValidationErrors()) {
						reasons = reasons+", "+validationError.getMessage();
					}
				}
				throw new CustomException("Failed to create Data Filter with name :" + ssfd.getName() +". Reasons: "+reasons);
			}catch(Exception xx) {
				throw new CustomException("Failed to create Data Filter with name :" + ssfd.getName() +". Reason: Response Error.");
			}
		}
		if(result == null || !result.getStatusCode().equals(HttpStatus.CREATED)) {
			String reason = "UNKNOWN";
			if(result != null && result.getBody() != null && result.getBody().getMessage() != null) {
				reason = result.getBody().getMessage();
			}
			if(errorCollection !=null) {
				errorCollection.add("Failed to create Data Filter with name :" + ssfd.getName() +". Reason: "+reason);
			}
			throw new CustomException("Failed to create Data Filter with name :" + ssfd.getName() +". Reason: "+reason);
		}
		
		
		return result.getBody();
	}
	
	@Override
	public SnapshotFilterDefinitionObject convertToSnapshotObject(ETClientObject etClientObject, Integer mid, String restAccessToken, String endpoint, FilterDefinitionObject fd, Map<Integer, String> folderPathCollection, String echoFolderSuffix, List<String> errorCollection) {
		SnapshotFilterDefinitionObject ssfd = new SnapshotFilterDefinitionObject();
		ssfd.setObjectType(com.projectecho.utils.ETAPIObjectType.FILTER_DEFINITION);
		ssfd.setCustomerKey(fd.getKey());
		ssfd.setObjectID(fd.getId());
		try {
			ssfd.setFolderPath(this.getFolderPathFromId(etClientObject, mid, Integer.parseInt(fd.getCategoryId()), folderPathCollection, echoFolderSuffix));
		} catch (ETSdkException e1) {
			ssfd.setFolderPath(com.projectecho.utils.ETFolderTopParents.FILTER_DEFINITION + ">" + echoFolderSuffix);
		}
		ssfd.setName(fd.getName());
		ssfd.setDescription(fd.getDescription());
		
		if(fd.getDerivedFromObjectTypeName().equalsIgnoreCase(FilterDefinitionObject.DerivedFromObjectTypeNameEnum.DATA_EXTENSION.value())) {
			ssfd.setDerivedFromObjectTypeName(fd.getDerivedFromObjectTypeName());
			ssfd.setDerivedFromObjectName(fd.getDerivedFromObjectName());
			Integer deMid = mid;
			try {
				ETRetrieveDataExtensionObject de = this.deService.retrieveByObjectID(etClientObject, deMid, fd.getDerivedFromObjectId());
				if(de == null) {
					de = this.deService.retrieveByObjectID(etClientObject, null, fd.getDerivedFromObjectId());
					deMid = null;
				}
				List<ETDataExtensionColumnObject> columns = null;
				try {
					columns = this.deService.retrieveColumns(etClientObject, deMid, de.getKey());
				}catch(Exception deE) {
					
				}
				
				String xml = fd.getFilterDefinitionXml();;
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document xmlDoc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
				NodeList conditionElements = xmlDoc.getElementsByTagName("Condition");
				for(int idx = 0; idx < conditionElements.getLength(); idx++) {
					Node conditionElement = conditionElements.item(idx);
					if(conditionElement.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) conditionElement;
						String conditionID = eElement.getAttribute("ID");
						if( conditionID!= null && !conditionID.isEmpty()) {
							for(ETDataExtensionColumnObject column: columns) {
								if(column != null && column.getId()!=null && column.getId().equalsIgnoreCase(conditionID)) {
									eElement.setAttribute("ID",column.getName());
									break;
								}
							}
						}
					}
					
				}
				TransformerFactory tf = TransformerFactory.newInstance();
			    Transformer trans = tf.newTransformer();
			    trans.setOutputProperty("omit-xml-declaration", "yes");
			    StringWriter sw = new StringWriter();
			    trans.transform(new DOMSource(xmlDoc), new StreamResult(sw));
			      
				ssfd.setFilterDefinitionXml(sw.toString());
			}catch(Exception e) {
				if(errorCollection != null) {
					errorCollection.add("Failed to map filter conditions with Data Extension columns for DE: "+ssfd.getDerivedFromObjectTypeName()+" for Data Filter:"+ssfd.getName());
				}
			}
			
		} else if(fd.getDerivedFromObjectTypeName().equalsIgnoreCase(FilterDefinitionObject.DerivedFromObjectTypeNameEnum.SUBSCRIBER_ATTRIBUTE.value())) {
			ssfd.setDerivedFromObjectTypeName(fd.getDerivedFromObjectTypeName());
			try {
				List<ProfileAttributeObject> attrList = this.profileAttributeService.getAll(restAccessToken, endpoint, null, errorCollection);
				if(attrList == null || attrList.isEmpty()) {
					attrList = new ArrayList<ProfileAttributeObject>();;
				}
				String xml = fd.getFilterDefinitionXml();
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document xmlDoc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
				NodeList conditionElements = xmlDoc.getElementsByTagName("Condition");
				for(int idx = 0; idx < conditionElements.getLength(); idx++) {
					Node conditionElement = conditionElements.item(idx);
					if(conditionElement.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) conditionElement;
						String conditionID = eElement.getAttribute("ID");
						String sourceTypeVal = eElement.getAttribute("SourceType");
						if( conditionID!= null && !conditionID.isEmpty()) {
							if(sourceTypeVal == null || sourceTypeVal.isEmpty()) {
								for(ProfileAttributeObject attr: attrList) {
									if(attr != null && attr.getId()!=null && attr.getId().equalsIgnoreCase(conditionID)) {
										eElement.setAttribute("ID",attr.getName());
										break;
									}
								}
							} else if(sourceTypeVal.equalsIgnoreCase("measure")) {
								MeasureObject measure = this.measureService.getByID(restAccessToken, endpoint, conditionID, null);
								if(measure != null) {
									String mName = measure.getName();
									try {
										String folderPathName = this.getFolderPathFromId(etClientObject, mid, Integer.parseInt(measure.getCategoryId()), folderPathCollection, null);
										if(folderPathName != null && folderPathName.isEmpty()) {
											mName = folderPathName +">"+measure.getName();
										} else {
											mName = com.projectecho.utils.ETFolderTopParents.MEASURE +">"+ measure.getName();
										}
									}catch(Exception e) {
										mName = com.projectecho.utils.ETFolderTopParents.MEASURE +">"+ measure.getName();
									}
									mName = mName.replaceAll(">", "|");
									eElement.setAttribute("ID",mName);
								}
							}
						}
					}
					
				}
				TransformerFactory tf = TransformerFactory.newInstance();;
			    Transformer trans = tf.newTransformer();
			    trans.setOutputProperty("omit-xml-declaration", "yes");
			    StringWriter sw = new StringWriter();
			    trans.transform(new DOMSource(xmlDoc), new StreamResult(sw));
			    ssfd.setFilterDefinitionXml(sw.toString());
			}catch(Exception e) {
				if(errorCollection != null) {
					errorCollection.add("Failed to map filter conditions with Profile Attributes OR Measures for Data Filter:"+ssfd.getName());
				}
			}
			
		}
		
		return ssfd;
	}
	
	
	private String getFolderPathFromId(ETClientObject etClientObject, Integer mid, Integer categoryID, Map<Integer, String> folderPathCollection, String echoFolderSuffix) throws ETSdkException {
		String folderPath = "";
		if(folderPathCollection!= null && folderPathCollection.containsKey(categoryID)) {
			folderPath = folderPathCollection.get(categoryID);
		} else if(folderPathCollection!= null && categoryID != null && categoryID.intValue() > 0) {
			if(echoFolderSuffix == null) {
				folderPath = this.folderService.retrieveFolderPath(etClientObject, mid, categoryID, true);
			} else {
				folderPath = this.folderService.retrieveFolderPath(etClientObject, mid, categoryID, echoFolderSuffix);
			}
			folderPathCollection.put(categoryID, folderPath);
		}
		return folderPath;
	}
	
}
