package gr.uoa.di.madgik.registry.client;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

@Service("searchService")
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Value("${registry.base}")
    private String registryHost;


    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType, int quantity, int from, String sortByField, String sortOrder) {
        RestTemplate restTemplate = new RestTemplate();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(registryHost + "/search/cql/" + resourceType + "/" + query + "/")
                .queryParam("from", from)
                .queryParam("quantity", quantity)
                .queryParam("sortBy", sortByField)
                .queryParam("sortByType", sortOrder);

        ResponseEntity<Paging> response = restTemplate.getForEntity(builder.toUriString(), Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return new Paging<>();
        }
    }

    @Override
    public Paging<Resource> cqlQuery(String query, String resourceType) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Paging> response = restTemplate.getForEntity(registryHost + "/search/cql/" + resourceType + "/" + query + "/", Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return new Paging<>();
        }
    }

    @Override
    public Paging<Resource> search(FacetFilter filter) throws ServiceException {
        RestTemplate restTemplate = new RestTemplate();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(registryHost + "/search/" + filter.getResourceType() + "/*/")
                .queryParam("keyword", filter.getKeyword())
                .queryParam("from", filter.getFrom())
                .queryParam("quantity", filter.getQuantity())
                .queryParam("browseBy", filter.getBrowseBy());

        ResponseEntity<Paging> response = restTemplate.getForEntity(builder.toUriString(), Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return new Paging<>();
        }
    }

    @Override
    public Paging<Resource> searchKeyword(String resourceType, String keyword) throws ServiceException {
        RestTemplate restTemplate = new RestTemplate();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(registryHost + "/search/" + resourceType + "/*/")
                .queryParam("keyword", keyword);

        ResponseEntity<Paging> response = restTemplate.getForEntity(builder.toUriString(), Paging.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return new Paging<>();
        }
    }

    @Override
    public Resource searchFields(String resourceType, KeyValue... fields) throws ServiceException {
        String query = "";
        for (KeyValue keyValue : fields)
            query = query.concat(keyValue.getField() + "=" + keyValue.getValue() + " AND ");
        List<Resource> resources = cqlQuery(query, resourceType).getResults();
        if (resources.size() != 0)
            return resources.get(0);
        else
            return null;
    }

    @Override
    public Map<String, List<Resource>> searchByCategory(FacetFilter filter, String category) {
        return null;
    }
}