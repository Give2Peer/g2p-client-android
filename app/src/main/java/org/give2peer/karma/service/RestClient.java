package org.give2peer.karma.service;

import org.androidannotations.rest.spring.annotations.Accept;
import org.androidannotations.rest.spring.annotations.Get;
import org.androidannotations.rest.spring.annotations.Rest;
import org.androidannotations.rest.spring.api.MediaType;
import org.androidannotations.rest.spring.api.RestClientErrorHandling;
import org.androidannotations.rest.spring.api.RestClientRootUrl;
import org.give2peer.karma.response.PrivateProfileResponse;
import org.give2peer.karma.response.Stats;


/**
 * This is an effortless REST client thanks to Android Annotations (v4).
 * The API documentation and sandbox are available at https://g2p.give2peer.org
 *
 * Note:
 *   You have to call setRootUrl(app.getCurrentServer().getUrl()) at some point before using this,
 *   in a method annotated with @AfterInject seems a good idea.
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

    @Get("/user")
    PrivateProfileResponse getPrivateProfile();
}