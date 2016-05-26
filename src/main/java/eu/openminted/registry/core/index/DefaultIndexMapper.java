package eu.openminted.registry.core.index;

import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.domain.ResourceType;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antleb on 5/20/16.
 */
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

	public List<IndexedField> getValues(String resource, ResourceType resourceType) {
		List<IndexedField> res = new ArrayList<IndexedField>();

		for (IndexField indexField:resourceType.getIndexFields()) {
			try {
				String fieldName = indexField.getName();
				String fieldType = indexField.getType();
				String path = indexField.getPath();

				Object value = getValue(resource, fieldType, path, resourceType.getPayloadType());

				res.add(indexedFieldFactory.getIndexedField(fieldName, value, fieldType));
			} catch (Exception e) {
				logger.error("Errororor", e);
				logger.error(indexedFieldFactory);
			}
		}

		return res;
	}

	private Object getValue(String resource, String fieldType, String path, String payloadType) {
		FieldParser fieldParser;

		if (payloadType.equals("json"))
			fieldParser = jsonFieldParser;
		else if (payloadType.equals("xml"))
			fieldParser = xmlFieldParser;
		else
			fieldParser = null;

		return fieldParser.parse(resource, fieldType, path);
	}
}
