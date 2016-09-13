package org.give2peer.karma.service;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.rest.Accept;
import org.androidannotations.annotations.rest.Get;
import org.androidannotations.annotations.rest.Rest;
import org.androidannotations.api.rest.MediaType;
import org.give2peer.karma.response.Stats;
import org.springframework.http.HttpAuthentication;
import org.springframework.http.HttpBasicAuthentication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;


@Rest(
        rootUrl = "http://g2p.give2peer.org/v1", // todo: make this dynamic
        interceptors = AuthInterceptor.class,
        converters = { GsonHttpMessageConverter.class }
)
@Accept(MediaType.APPLICATION_JSON)
public interface RestClient {
    @Get("/stats")
    Stats getStats();
}