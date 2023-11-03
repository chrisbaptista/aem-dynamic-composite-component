package com.example.core.services;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * JCR Util Service
 */
public interface JcrUtilService {

    /**
     * Get Resource Resolver
     *
     * @return ResourceResolver
     * @throws LoginException Login Exception
     */
    ResourceResolver getResourceResolver() throws LoginException;

    /**
     * Get Session
     *
     * @return Session
     * @throws RepositoryException JCR Repository Exception
     */
    Session getSession() throws RepositoryException;

    /**
     * Copy Nodes
     *
     * @param originPath Origin Path
     * @param destinationPath Destination Path
     * @param resourceResolver Resource Resolver
     * @param refreshNodes Refresh Nodes
     */
    void copyNodes(String originPath, String destinationPath, ResourceResolver resourceResolver, boolean refreshNodes);


}
