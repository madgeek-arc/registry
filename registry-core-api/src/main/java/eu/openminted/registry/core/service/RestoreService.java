package eu.openminted.registry.core.service;

import org.springframework.web.multipart.MultipartFile;

public interface RestoreService {

	void restoreDataFromZip(MultipartFile file);
}
