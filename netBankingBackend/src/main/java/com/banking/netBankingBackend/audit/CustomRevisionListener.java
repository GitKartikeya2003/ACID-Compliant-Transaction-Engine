package com.banking.netBankingBackend.audit;

import com.banking.netBankingBackend.entity.CustomRevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class CustomRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity revision = (CustomRevisionEntity) revisionEntity;

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes();

            String host = attributes.getRequest().getRemoteAddr();
            String user = attributes.getRequest().getUserPrincipal() != null
                    ? attributes.getRequest().getUserPrincipal().getName()
                    : "anonymous";

            revision.setRemoteHost(host);
            revision.setRemoteUser(user);

        } catch (IllegalStateException e) {
            // Happens when change is triggered outside an HTTP request (e.g. scheduled jobs)
            revision.setRemoteHost("system");
            revision.setRemoteUser("system");
        }
    }

    //Every time Envers creates a new revision, it calls newRevision() automatically. This method:
    //
    //->Grabs the current HTTP request
    //->Pulls the IP address and the logged-in username
    //->Stamps both onto the revision row
}