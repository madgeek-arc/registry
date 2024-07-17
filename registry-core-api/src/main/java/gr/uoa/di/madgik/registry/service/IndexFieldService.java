package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.index.IndexField;

import java.util.List;

public interface IndexFieldService {
    List<IndexField> getIndexFields(String resourceTypeName);
}

