package gr.uoa.di.madgik.registry.controllers;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @RequestMapping(value = "/search/{name}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<Paging> search(
            @PathVariable("name") String name,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "from", required = false, defaultValue = "0") int from,
            @RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity,
            @RequestParam(value = "browseBy", required = false, defaultValue = "") String[] browseBy,
            @RequestParam Map<String, Object> allRequestParams
    ) throws ServiceException {
        allRequestParams.remove("keyword");
        allRequestParams.remove("from");
        allRequestParams.remove("quantity");
        allRequestParams.remove("order");
        allRequestParams.remove("orderField");
        FacetFilter filter = new FacetFilter(keyword, name, from, quantity, allRequestParams, Arrays.asList(browseBy), null);
        return new ResponseEntity<>(searchService.search(filter), HttpStatus.OK);
    }

    @RequestMapping(value = "/search/cql/{resourceType}/{query}/", method = RequestMethod.GET)
    public ResponseEntity<Paging> cql(@PathVariable("query") String query,
                                      @PathVariable("resourceType") String resourceType,
                                      @RequestParam(value = "from", required = false, defaultValue = "0") int from,
                                      @RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity,
                                      @RequestParam(value = "sortBy", required = false, defaultValue = "") String sortBy,
                                      @RequestParam(value = "sortByType", required = false, defaultValue = "ASC") String sortByType) {
        if (sortByType.equals("DESC") || sortByType.equals("ASC"))
            return new ResponseEntity<>(searchService.cqlQuery(query, resourceType, quantity, from, sortBy, sortByType), HttpStatus.OK);
        else
            throw new ServiceException("Unsupported order by type");
    }
}
