package org.give2peer.karma.service;

import android.content.Context;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.give2peer.karma.Application;
import org.springframework.http.HttpAuthentication;
import org.springframework.http.HttpBasicAuthentication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * An authentication interceptor that allows us to authenticate using basic HTTPAuth,
 * as the server expects, with the correct user credentials that the Application provides.
 */
@EBean(scope = EBean.Scope.Singleton)
public class AuthenticationInterceptor implements ClientHttpRequestInterceptor {

    @RootContext
    Context context; // must not be private, whatever the IDE says

    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        Application app = (Application) context.getApplicationContext();

        HttpHeaders headers = request.getHeaders();
        HttpAuthentication auth = new HttpBasicAuthentication(
                app.getUsername(), app.getPassword()
        );
        headers.setAuthorization(auth);

        return execution.execute(request, body);
    }
}
