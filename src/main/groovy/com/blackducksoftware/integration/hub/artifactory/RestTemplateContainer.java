/**
 * hub-artifactory
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.artifactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

import com.blackducksoftware.integration.exception.EncryptionException;

@Component
class RestTemplateContainer extends RestTemplate {
    @Autowired
    private ConfigurationProperties configurationProperties;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() throws EncryptionException {
        restTemplate = new RestTemplate();

        final String artifactoryUsername = configurationProperties.getArtifactoryUsername();
        final String artifactoryApiKey = configurationProperties.getArtifactoryApiKey();
        if (StringUtils.isNotBlank(artifactoryUsername) && StringUtils.isNotBlank(artifactoryApiKey)) {
            final BasicAuthorizationInterceptor basicAuthorizationInterceptor = new BasicAuthorizationInterceptor(artifactoryUsername, artifactoryApiKey);
            restTemplate.getInterceptors().add(basicAuthorizationInterceptor);
        }
    }

    @Override
    public int hashCode() {
        return restTemplate.hashCode();
    }

    @Override
    public void setInterceptors(final List<ClientHttpRequestInterceptor> interceptors) {
        restTemplate.setInterceptors(interceptors);
    }

    @Override
    public List<ClientHttpRequestInterceptor> getInterceptors() {
        return restTemplate.getInterceptors();
    }

    @Override
    public void setRequestFactory(final ClientHttpRequestFactory requestFactory) {
        restTemplate.setRequestFactory(requestFactory);
    }

    @Override
    public ClientHttpRequestFactory getRequestFactory() {
        return restTemplate.getRequestFactory();
    }

    @Override
    public boolean equals(final Object obj) {
        return restTemplate.equals(obj);
    }

    @Override
    public String toString() {
        return restTemplate.toString();
    }

    @Override
    public void setMessageConverters(final List<HttpMessageConverter<?>> messageConverters) {
        restTemplate.setMessageConverters(messageConverters);
    }

    @Override
    public List<HttpMessageConverter<?>> getMessageConverters() {
        return restTemplate.getMessageConverters();
    }

    @Override
    public void setErrorHandler(final ResponseErrorHandler errorHandler) {
        restTemplate.setErrorHandler(errorHandler);
    }

    @Override
    public ResponseErrorHandler getErrorHandler() {
        return restTemplate.getErrorHandler();
    }

    @Override
    public void setDefaultUriVariables(final Map<String, ?> defaultUriVariables) {
        restTemplate.setDefaultUriVariables(defaultUriVariables);
    }

    @Override
    public void setUriTemplateHandler(final UriTemplateHandler handler) {
        restTemplate.setUriTemplateHandler(handler);
    }

    @Override
    public UriTemplateHandler getUriTemplateHandler() {
        return restTemplate.getUriTemplateHandler();
    }

    @Override
    public <T> T getForObject(final String url, final Class<T> responseType, final Object... uriVariables) throws RestClientException {
        return restTemplate.getForObject(url, responseType, uriVariables);
    }

    @Override
    public <T> T getForObject(final String url, final Class<T> responseType, final Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.getForObject(url, responseType, uriVariables);
    }

    @Override
    public <T> T getForObject(final URI url, final Class<T> responseType) throws RestClientException {
        return restTemplate.getForObject(url, responseType);
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(final String url, final Class<T> responseType, final Object... uriVariables) throws RestClientException {
        return restTemplate.getForEntity(url, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(final String url, final Class<T> responseType, final Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.getForEntity(url, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> getForEntity(final URI url, final Class<T> responseType) throws RestClientException {
        return restTemplate.getForEntity(url, responseType);
    }

    @Override
    public HttpHeaders headForHeaders(final String url, final Object... uriVariables) throws RestClientException {
        return restTemplate.headForHeaders(url, uriVariables);
    }

    @Override
    public HttpHeaders headForHeaders(final String url, final Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.headForHeaders(url, uriVariables);
    }

    @Override
    public HttpHeaders headForHeaders(final URI url) throws RestClientException {
        return restTemplate.headForHeaders(url);
    }

    @Override
    public URI postForLocation(final String url, final Object request, final Object... uriVariables) throws RestClientException {
        return restTemplate.postForLocation(url, request, uriVariables);
    }

    @Override
    public URI postForLocation(final String url, final Object request, final Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.postForLocation(url, request, uriVariables);
    }

    @Override
    public URI postForLocation(final URI url, final Object request) throws RestClientException {
        return restTemplate.postForLocation(url, request);
    }

    @Override
    public <T> T postForObject(final String url, final Object request, final Class<T> responseType, final Object... uriVariables) throws RestClientException {
        return restTemplate.postForObject(url, request, responseType, uriVariables);
    }

    @Override
    public <T> T postForObject(final String url, final Object request, final Class<T> responseType, final Map<String, ?> uriVariables)
            throws RestClientException {
        return restTemplate.postForObject(url, request, responseType, uriVariables);
    }

    @Override
    public <T> T postForObject(final URI url, final Object request, final Class<T> responseType) throws RestClientException {
        return restTemplate.postForObject(url, request, responseType);
    }

    @Override
    public <T> ResponseEntity<T> postForEntity(final String url, final Object request, final Class<T> responseType, final Object... uriVariables)
            throws RestClientException {
        return restTemplate.postForEntity(url, request, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> postForEntity(final String url, final Object request, final Class<T> responseType, final Map<String, ?> uriVariables)
            throws RestClientException {
        return restTemplate.postForEntity(url, request, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> postForEntity(final URI url, final Object request, final Class<T> responseType) throws RestClientException {
        return restTemplate.postForEntity(url, request, responseType);
    }

    @Override
    public void put(final String url, final Object request, final Object... uriVariables) throws RestClientException {
        restTemplate.put(url, request, uriVariables);
    }

    @Override
    public void put(final String url, final Object request, final Map<String, ?> uriVariables) throws RestClientException {
        restTemplate.put(url, request, uriVariables);
    }

    @Override
    public void put(final URI url, final Object request) throws RestClientException {
        restTemplate.put(url, request);
    }

    @Override
    public <T> T patchForObject(final String url, final Object request, final Class<T> responseType, final Object... uriVariables) throws RestClientException {
        return restTemplate.patchForObject(url, request, responseType, uriVariables);
    }

    @Override
    public <T> T patchForObject(final String url, final Object request, final Class<T> responseType, final Map<String, ?> uriVariables)
            throws RestClientException {
        return restTemplate.patchForObject(url, request, responseType, uriVariables);
    }

    @Override
    public <T> T patchForObject(final URI url, final Object request, final Class<T> responseType) throws RestClientException {
        return restTemplate.patchForObject(url, request, responseType);
    }

    @Override
    public void delete(final String url, final Object... uriVariables) throws RestClientException {
        restTemplate.delete(url, uriVariables);
    }

    @Override
    public void delete(final String url, final Map<String, ?> uriVariables) throws RestClientException {
        restTemplate.delete(url, uriVariables);
    }

    @Override
    public void delete(final URI url) throws RestClientException {
        restTemplate.delete(url);
    }

    @Override
    public Set<HttpMethod> optionsForAllow(final String url, final Object... uriVariables) throws RestClientException {
        return restTemplate.optionsForAllow(url, uriVariables);
    }

    @Override
    public Set<HttpMethod> optionsForAllow(final String url, final Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.optionsForAllow(url, uriVariables);
    }

    @Override
    public Set<HttpMethod> optionsForAllow(final URI url) throws RestClientException {
        return restTemplate.optionsForAllow(url);
    }

    @Override
    public <T> ResponseEntity<T> exchange(final String url, final HttpMethod method, final HttpEntity<?> requestEntity, final Class<T> responseType,
            final Object... uriVariables)
                    throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> exchange(final String url, final HttpMethod method, final HttpEntity<?> requestEntity, final Class<T> responseType,
            final Map<String, ?> uriVariables)
                    throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> exchange(final URI url, final HttpMethod method, final HttpEntity<?> requestEntity, final Class<T> responseType)
            throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType);
    }

    @Override
    public <T> ResponseEntity<T> exchange(final String url, final HttpMethod method, final HttpEntity<?> requestEntity,
            final ParameterizedTypeReference<T> responseType,
            final Object... uriVariables) throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> exchange(final String url, final HttpMethod method, final HttpEntity<?> requestEntity,
            final ParameterizedTypeReference<T> responseType,
            final Map<String, ?> uriVariables) throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }

    @Override
    public <T> ResponseEntity<T> exchange(final URI url, final HttpMethod method, final HttpEntity<?> requestEntity,
            final ParameterizedTypeReference<T> responseType)
                    throws RestClientException {
        return restTemplate.exchange(url, method, requestEntity, responseType);
    }

    @Override
    public <T> ResponseEntity<T> exchange(final RequestEntity<?> requestEntity, final Class<T> responseType) throws RestClientException {
        return restTemplate.exchange(requestEntity, responseType);
    }

    @Override
    public <T> ResponseEntity<T> exchange(final RequestEntity<?> requestEntity, final ParameterizedTypeReference<T> responseType) throws RestClientException {
        return restTemplate.exchange(requestEntity, responseType);
    }

    @Override
    public <T> T execute(final String url, final HttpMethod method, final RequestCallback requestCallback, final ResponseExtractor<T> responseExtractor,
            final Object... uriVariables)
                    throws RestClientException {
        return restTemplate.execute(url, method, requestCallback, responseExtractor, uriVariables);
    }

    @Override
    public <T> T execute(final String url, final HttpMethod method, final RequestCallback requestCallback, final ResponseExtractor<T> responseExtractor,
            final Map<String, ?> uriVariables)
                    throws RestClientException {
        return restTemplate.execute(url, method, requestCallback, responseExtractor, uriVariables);
    }

    @Override
    public <T> T execute(final URI url, final HttpMethod method, final RequestCallback requestCallback, final ResponseExtractor<T> responseExtractor)
            throws RestClientException {
        return restTemplate.execute(url, method, requestCallback, responseExtractor);
    }
}
