package org.give2peer.karma.service;

import org.androidannotations.annotations.EBean;

@Deprecated
@EBean(scope = EBean.Scope.Singleton)
public class MyAuthStore {

    /**
     * Figure out how to grab the user's credentials from here ?
     * Or dynamically create such a Bean ?
     * Meanwhile, we're not using this.
     */

    public String getUsername() {
        return "H2G2";
    }

    public String getPassword() {
        return "42";
    }
}
