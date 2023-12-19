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
					dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
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

					ObjectMapper mapper = new ObjectMapper();
					mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
					mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
					JsonNode node1 = mapper.readTree(s);
					ArrayNode a =  (ArrayNode) node1;
					when(astSnyk.getOrgs(anyString())).thenReturn(a);
					when(gitSomaManager.getFileContentAsString(anyString(),anyString(),anyString())).thenThrow(new RuntimeException());
					when(gitSomaManager.writeFile(anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new RuntimeException());
					ObjectMapper mapper2 = new ObjectMapper();
					mapper2.setSerializationInclusion(JsonInclude.Include.NON_NULL);
					mapper2.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
					boolean b = snykCiIntegrationManager.onBoardUser();
					
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
										String x = "Sample";
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
			
		}
		return result.getBody();
	}
}
