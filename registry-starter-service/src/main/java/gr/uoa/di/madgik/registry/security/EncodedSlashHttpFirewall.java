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

package gr.uoa.di.madgik.registry.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.util.Collection;
import java.util.List;

/**
 * Some consumers use "x/y" domain primary keys, which reach a controller's
 * {@code /{prefix}/{id}/...} routes as a URL-encoded slash ("%2F") in {@code {id}}.
 * StrictHttpFirewall rejects both encoded slashes and any decoded "%" by default,
 * so the configured path prefixes need it relaxed. Every other path in the
 * consuming application keeps the default strict firewall.
 */
public class EncodedSlashHttpFirewall implements HttpFirewall {

    private final HttpFirewall strict = new StrictHttpFirewall();
    private final HttpFirewall relaxed = createRelaxedFirewall();
    private final List<String> pathPrefixes;

    public EncodedSlashHttpFirewall(Collection<String> paths) {
        this.pathPrefixes = paths.stream()
                .filter(path -> !path.isBlank())
                .map(EncodedSlashHttpFirewall::normalize)
                .toList();
    }

    @Override
    public FirewalledRequest getFirewalledRequest(HttpServletRequest request) throws RequestRejectedException {
        return (isRelaxedPath(request) ? relaxed : strict).getFirewalledRequest(request);
    }

    @Override
    public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
        return strict.getFirewalledResponse(response);
    }

    private boolean isRelaxedPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        String path = (contextPath != null && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length())
                : uri;
        return pathPrefixes.stream().anyMatch(path::startsWith);
    }

    private static String normalize(String path) {
        String trimmed = path.trim();
        if (!trimmed.startsWith("/")) trimmed = "/" + trimmed;
        if (!trimmed.endsWith("/")) trimmed = trimmed + "/";
        return trimmed;
    }

    private static HttpFirewall createRelaxedFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        return firewall;
    }
}
