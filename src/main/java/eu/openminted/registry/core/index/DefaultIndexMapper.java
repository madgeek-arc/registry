package eu.openminted.registry.core.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.ResourceService;

public class DefaultIndexMapper implements IndexMapper {

	private static Logger logger = Logger.getLogger(DefaultIndexMapper.class);

	private List<IndexField> indexFields;

	// TODO spring me!
	private IndexedFieldFactory indexedFieldFactory = new IndexedFieldFactory();

	// TODO: springisize this part!
	private XMLFieldParser xmlFieldParser = new XMLFieldParser();
	private JSONFieldParser jsonFieldParser = new JSONFieldParser();

	public DefaultIndexMapper(List<IndexField> indexFields) {
		this.indexFields = indexFields;
	}

	public List<IndexField> getIndexFields() {
		return indexFields;
	}

	public List<IndexedField> getValues(String payload, ResourceType resourceType) {
		List<IndexedField> res = new ArrayList<IndexedField>();

		for (IndexField indexField:resourceType.getIndexFields()) {
			try {
				String fieldName = indexField.getName();
				String fieldType = indexField.getType();
				String path = indexField.getPath();
				
				logger.debug("Indexing field " + fieldName + " (" + fieldType + ") with path " + path);
				
				Set<Object> value = getValue(payload, fieldType, path, resourceType.getPayloadType(),indexField.isMultivalued());
				if(!value.isEmpty()) {
					res.add(indexedFieldFactory.getIndexedField(fieldName, value, fieldType));
				}
			} catch (Exception e) {
				logger.error("Errororor", e);
//				logger.error(indexedFieldFactory);
			}
		}
		
		return res;
	}

	public Set<Object> getValue(String payload, String fieldType, String path, String payloadType, boolean isMultiValued) {
		FieldParser fieldParser;

		if (payloadType.equals("json"))
			fieldParser = jsonFieldParser;
		else if (payloadType.equals("xml"))
			fieldParser = xmlFieldParser;
		else
			fieldParser = null;

		return fieldParser.parse(payload, fieldType, path,isMultiValued);
	}
}
