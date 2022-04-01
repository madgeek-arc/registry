package eu.openminted.registry.core.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.service.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DefaultIndexMapper implements IndexMapper {

	private static Logger logger = LogManager.getLogger(DefaultIndexMapper.class);

	private List<IndexField> indexFields;

	private IndexedFieldFactory indexedFieldFactory;
	private XMLFieldParser xmlFieldParser;
	private JSONFieldParser jsonFieldParser;

	@Autowired
	public DefaultIndexMapper(IndexedFieldFactory indexedFieldFactory, XMLFieldParser xmlFieldParser,
							  JSONFieldParser jsonFieldParser) {
		this.indexedFieldFactory = indexedFieldFactory;
		this.xmlFieldParser = xmlFieldParser;
		this.jsonFieldParser = jsonFieldParser;
	}

	public List<IndexedField> getValues(String payload, ResourceType resourceType) throws ServiceException{
		List<IndexedField> res = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		for (IndexField indexField:resourceType.getIndexFields()) {
			try {
				String fieldName = indexField.getName();
				String fieldType = indexField.getType();
				String path = indexField.getPath();
				String value = indexField.getDefaultValue();
				Set<Object> values;
				logger.debug("Indexing field " + fieldName + " (" + fieldType + ") with path " + path + " and DEFAULT VALUE:" + value);

				//if there is no xpath add default value
				if(path == null) {
					values = new HashSet<>();
					if(value == null) {
						throw new ServiceException("Indexfield:"  + fieldName +" with no xpath must supply a default value");
					}

					values.add(mapper.convertValue(value,Class.forName(indexField.getType())));
					res.add(indexedFieldFactory.getIndexedField(fieldName,values,fieldType));
				}else {
					//there is an xpath
					values = getValue(payload, fieldType, path, resourceType.getPayloadType(), indexField.isMultivalued());
					if (values != null && !values.isEmpty()) {
						res.add(indexedFieldFactory.getIndexedField(fieldName, values, fieldType));
					}else{
						values = new HashSet<>();
						if(value != null) {
							values.add(mapper.convertValue(value,Class.forName(indexField.getType())));
						}
						
						res.add(indexedFieldFactory.getIndexedField(fieldName,values,fieldType));
					}

				}
			} catch (Exception e) {
				throw new ServiceException(e.getMessage());
			}
		}
		
		return res;
	}

	private Set<Object> getValue(String payload, String fieldType, String path, String payloadType, boolean isMultiValued) {
		FieldParser fieldParser;

		if (payloadType.equals("json"))
			fieldParser = jsonFieldParser;
		else if (payloadType.equals("xml"))
			fieldParser = xmlFieldParser;
		else
			fieldParser = null;

		return fieldParser.parse(payload, fieldType, path,isMultiValued);
	}

	public void setIndexFields(List<IndexField> indexFields) {
		this.indexFields = indexFields;
	}

	@Override
	public List<IndexField> getIndexFields() {
		return indexFields;
	}
}
