package eu.openminted.registry.core.service;

import eu.openminted.registry.core.domain.BatchResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface RestoreService {

    Map<String, BatchResult> restoreDataFromZip(MultipartFile file);
}
