package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.index.IndexedField;

import java.util.List;

public interface IndexedFieldService {
    List<IndexedField> getIndexedFields(String resourceId);
}

