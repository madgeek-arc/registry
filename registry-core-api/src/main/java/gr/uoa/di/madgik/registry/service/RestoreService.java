package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.domain.BatchResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface RestoreService {

    Map<String, BatchResult> restoreDataFromZip(MultipartFile file);
}
