package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.index.IndexField;

import java.util.List;

public interface IndexFieldService {

	List<IndexField> getIndexFields(String resourceTypeName);

	IndexField getIndexField(String indexFieldName);

	IndexField add(IndexField indexField);

	void delete(String indexField,String resourceTypeName);
}

