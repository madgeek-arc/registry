package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.index.IndexedField;

import java.util.List;

public interface IndexedFieldService {
    List<IndexedField> getIndexedFields(String resourceId);
}

