package org.give2peer.karma.service;

import org.androidannotations.annotations.EBean;

@EBean(scope = EBean.Scope.Singleton)
public class MyAuthStore {



    public String getUsername() {
        return "H2G2";
    }

    public String getPassword() {
        return "42";
    }
}
