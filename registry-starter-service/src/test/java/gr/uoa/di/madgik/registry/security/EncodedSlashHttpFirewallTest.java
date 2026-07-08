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

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.firewall.RequestRejectedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncodedSlashHttpFirewallTest {

    @Test
    void relaxesOnlyTheConfiguredPath() {
        EncodedSlashHttpFirewall firewall = new EncodedSlashHttpFirewall(List.of("records"));

        assertDoesNotThrow(() -> firewall.getFirewalledRequest(encodedSlashRequest("/records/idPrefix%2FidSuffix")));
        assertThrows(RequestRejectedException.class,
                () -> firewall.getFirewalledRequest(encodedSlashRequest("/other/idPrefix%2FidSuffix")));
    }

    @Test
    void relaxesEveryConfiguredPathInTheList() {
        EncodedSlashHttpFirewall firewall = new EncodedSlashHttpFirewall(List.of("records", "employee"));

        assertDoesNotThrow(() -> firewall.getFirewalledRequest(encodedSlashRequest("/records/idPrefix%2FidSuffix")));
        assertDoesNotThrow(() -> firewall.getFirewalledRequest(encodedSlashRequest("/employee/idPrefix%2FidSuffix")));
        assertThrows(RequestRejectedException.class,
                () -> firewall.getFirewalledRequest(encodedSlashRequest("/other/idPrefix%2FidSuffix")));
    }

    @Test
    void normalizesPathEntriesRegardlessOfSlashes() {
        EncodedSlashHttpFirewall withoutSlashes = new EncodedSlashHttpFirewall(List.of("employee"));
        EncodedSlashHttpFirewall withSlashes = new EncodedSlashHttpFirewall(List.of("/employee/"));

        assertDoesNotThrow(() -> withoutSlashes.getFirewalledRequest(encodedSlashRequest("/employee/idPrefix%2FidSuffix")));
        assertDoesNotThrow(() -> withSlashes.getFirewalledRequest(encodedSlashRequest("/employee/idPrefix%2FidSuffix")));
    }

    private static MockHttpServletRequest encodedSlashRequest(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setRequestURI(requestUri);
        return request;
    }
}
