package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.index.IndexField;

import java.util.List;

public interface IndexFieldService {
    List<IndexField> getIndexFields(String resourceTypeName);
}

