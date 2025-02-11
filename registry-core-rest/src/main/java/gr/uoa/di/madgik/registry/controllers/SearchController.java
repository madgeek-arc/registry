/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.Charset;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @BrowseParameters
    @RequestMapping(value = "/search/{name}", method = RequestMethod.GET)
    public ResponseEntity<Paging> search(
            @PathVariable("name") String resourceType,
            @RequestParam(defaultValue = "{}") MultiValueMap<String, Object> allRequestParams
//            @RequestParam BrowseParams allRequestParams
    ) throws ServiceException {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType(resourceType);
        return new ResponseEntity<>(searchService.search(filter), HttpStatus.OK);
    }

    @RequestMapping(value = "/search/cql/{resourceType}", method = RequestMethod.GET)
    public ResponseEntity<Paging> cql(@PathVariable("resourceType") String resourceType,
                                      @RequestParam("query") String query,
                                      @RequestParam(value = "from", required = false, defaultValue = "0") int from,
                                      @RequestParam(value = "quantity", required = false, defaultValue = "10") int quantity,
                                      @RequestParam(value = "sort", required = false, defaultValue = "") String sortBy,
                                      @RequestParam(value = "order", required = false, defaultValue = "ASC") String sortByType) {
        query = URLDecoder.decode(query, Charset.defaultCharset());
        return new ResponseEntity<>(searchService.cqlQuery(query, resourceType, quantity, from, sortBy, sortByType), HttpStatus.OK);
    }
}
