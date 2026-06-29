/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry.controllers;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.HighlightedResult;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ScoredResult;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @BrowseParameters
    @GetMapping(value = "/search/{name}")
    public ResponseEntity<Paging> search(
            @PathVariable("name") String resourceType,
            @RequestParam(defaultValue = "{}") MultiValueMap<String, Object> allRequestParams
//            @RequestParam BrowseParams allRequestParams
    ) throws ServiceException {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType(resourceType);
        return new ResponseEntity<>(searchService.search(filter), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(value = "/search/{name}/semantic")
    public ResponseEntity<Paging<Resource>> semanticSearch(
            @PathVariable("name") String resourceType,
            @RequestParam(defaultValue = "{}") MultiValueMap<String, Object> allRequestParams) throws ServiceException {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType(resourceType);
        return new ResponseEntity<>(searchService.semanticSearch(filter), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(value = "/search/{name}/hybrid")
    public ResponseEntity<Paging<Resource>> hybridSearch(
            @PathVariable("name") String resourceType,
            @RequestParam(defaultValue = "{}") MultiValueMap<String, Object> allRequestParams) throws ServiceException {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType(resourceType);
        return new ResponseEntity<>(searchService.hybridSearch(filter), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(value = "/search/{name}/highlighted")
    public ResponseEntity<Paging<HighlightedResult<Resource>>> searchWithHighlights(
            @PathVariable("name") String resourceType,
            @RequestParam(defaultValue = "{}") MultiValueMap<String, Object> allRequestParams) throws ServiceException {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType(resourceType);
        return new ResponseEntity<>(searchService.searchWithHighlights(filter), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(value = "/search/{name}/hybrid/highlighted")
    public ResponseEntity<Paging<HighlightedResult<Resource>>> hybridSearchWithHighlights(
            @PathVariable("name") String resourceType,
            @RequestParam(defaultValue = "{}") MultiValueMap<String, Object> allRequestParams) throws ServiceException {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType(resourceType);
        return new ResponseEntity<>(searchService.hybridSearchWithHighlights(filter), HttpStatus.OK);
    }

    @GetMapping(value = "/search/cql/{resourceType}")
    public ResponseEntity<Paging> cql(@PathVariable("resourceType") String resourceType,
                                      @RequestParam("query") String query,
                                      @RequestParam(value = "from", required = false, defaultValue = "0") int from,
                                      @RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity,
                                      @RequestParam(value = "sort", required = false, defaultValue = "") String sortBy,
                                      @RequestParam(value = "order", required = false, defaultValue = "ASC") String sortByType) {
        query = URLDecoder.decode(query, Charset.defaultCharset());
        return new ResponseEntity<>(searchService.cqlQuery(query, resourceType, quantity, from, sortBy, sortByType), HttpStatus.OK);
    }

    @BrowseParameters
    @GetMapping(value = "/search/{name}/recommendations")
    public ResponseEntity<List<ScoredResult<Resource>>> recommend(
            @PathVariable("name") String resourceType,
            @RequestParam MultiValueMap<String, Object> allRequestParams) throws ServiceException {
        String field = allRequestParams.containsKey("field")
                ? (String) allRequestParams.remove("field").get(0) : null;
        String value = allRequestParams.containsKey("value")
                ? (String) allRequestParams.remove("value").get(0) : null;
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(searchService.recommend(filter, new SearchService.KeyValue(field, value)));
    }

    @BrowseParameters
    @PostMapping(value = "/search/{name}/recommendations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ScoredResult<Resource>>> recommendByResource(
            @PathVariable("name") String resourceType,
            @RequestBody Resource resource,
            @RequestParam MultiValueMap<String, Object> allRequestParams) throws ServiceException {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType(resourceType);
        return ResponseEntity.ok(searchService.recommend(filter, resource));
    }
}
