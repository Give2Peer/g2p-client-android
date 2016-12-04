package org.give2peer.karma.service;

import org.androidannotations.rest.spring.annotations.Accept;
import org.androidannotations.rest.spring.annotations.Get;
import org.androidannotations.rest.spring.annotations.Rest;
import org.androidannotations.rest.spring.api.MediaType;
import org.androidannotations.rest.spring.api.RestClientErrorHandling;
import org.androidannotations.rest.spring.api.RestClientRootUrl;
import org.give2peer.karma.response.Stats;

import org.springframework.http.converter.json.GsonHttpMessageConverter;


/**
 * This is an effortless REST client thanks to Android Annotations (v3).
 * A documentation and sandbox are available at https://g2p.give2peer.org
 *
 * Note:
 *   You have to call setRootUrl(app.getCurrentServer().getUrl()) at some point before using this,
 *   in a method annotated with @AfterInject seems a good idea.
 *
 * This is a WIP. It still needs :
 *   - proper error handling.
 */
@Rest(
        interceptors = { AuthenticationInterceptor.class },
        converters   = { GsonHttpMessageConverter.class  }
)
@Accept(MediaType.APPLICATION_JSON)
public interface RestClient extends RestClientRootUrl, RestClientErrorHandling
{
    @Get("/stats")
    Stats getStats();
}