package org.give2peer.karma.service;

import org.androidannotations.rest.spring.annotations.Accept;
import org.androidannotations.rest.spring.annotations.Body;
import org.androidannotations.rest.spring.annotations.Field;
import org.androidannotations.rest.spring.annotations.Get;
import org.androidannotations.rest.spring.annotations.Part;
import org.androidannotations.rest.spring.annotations.Path;
import org.androidannotations.rest.spring.annotations.Post;
import org.androidannotations.rest.spring.annotations.Rest;
import org.androidannotations.rest.spring.api.MediaType;
import org.androidannotations.rest.spring.api.RestClientErrorHandling;
import org.androidannotations.rest.spring.api.RestClientRootUrl;
import org.give2peer.karma.response.CreateItemResponse;
import org.give2peer.karma.response.FindItemsResponse;
import org.give2peer.karma.response.PictureItemBeforehandResponse;
import org.give2peer.karma.response.PrivateProfileResponse;
import org.give2peer.karma.response.Stats;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.converter.FormHttpMessageConverter;


/**
 * This is an effortless REST client thanks to Android Annotations (v4).
 * The API documentation and sandbox are available at https://g2p.give2peer.org
 *
 * Note:
 *   You have to call setRootUrl(app.getCurrentServer().getUrl()) at some point before using this,
 *   in a method annotated with @AfterInject seems a good idea.
 *   You need to do that because we want the server to be configured dynamically.
 * Warn:
 *   The order of the converters MATTER ! FormHttpMessageConverter will fail if it is not first !
 */
@Rest(
        interceptors = { AuthenticationInterceptor.class },
        converters   = { FormHttpMessageConverter.class, GsonHttpMessageConverter.class }
)
@Accept(MediaType.APPLICATION_JSON)
public interface RestClient extends RestClientRootUrl, RestClientErrorHandling
{
    @Get("/stats")
    Stats getStats();

    @Get("/user")
    PrivateProfileResponse getPrivateProfile();

    @Get("/items/around/{latitude}/{longitude}?skip={skip}&maxDistance={maxDistance}")
    FindItemsResponse findItemsAround(
            @Path String latitude, @Path String longitude,
            @Path String skip,     @Path String maxDistance
    );

    @Post("/item")
    CreateItemResponse createItem(
            @Field String location,
            @Field String title,
            @Field String description,
            @Field String type,
            @Field("pictures[]") String picture  // such a HAX, won't work with tags :(
    );

    @Post("/item/picture")
    PictureItemBeforehandResponse pictureItemBeforehand(@Part FileSystemResource picture);

}