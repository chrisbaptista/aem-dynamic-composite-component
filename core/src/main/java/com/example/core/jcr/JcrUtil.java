package com.example.core.jcr;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JcrUtil {

    private static final String EDITABLE_COMPONENTS_SYSTEM_USER = "editable-components-service-user";

    private static final Logger log = LoggerFactory.getLogger(JcrUtil.class);

    /**
     * This creates a ResourceResolver with the rights of the given user (by default that is 'anonymous').
     * <strong>It is the caller's responsibility to close the ResourceResolver </strong>
     * @param resolverFactory the factory to get a ResourceResolver from
     * @return the ResourceResolver; never null
     */
    public static ResourceResolver getResourceResolver(ResourceResolverFactory resolverFactory) throws LoginException {
        if (resolverFactory == null) throw new IllegalArgumentException("resolverFactory == null");
        Map < String, Object > authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, EDITABLE_COMPONENTS_SYSTEM_USER);
        return resolverFactory.getServiceResourceResolver(authInfo);
    }

    /**
     * This creates a JCR Session with the rights of the given user (by default that is 'anonymous').
     * <strong>It is the caller's responsibility to logout of the Session </strong>
     * @param slingRepository the Repository to log into
     * @return the Session; never null
     */
    public static Session getSession(SlingRepository slingRepository) throws RepositoryException {
        return slingRepository.loginService(EDITABLE_COMPONENTS_SYSTEM_USER, slingRepository.getDefaultWorkspace());
    }

    /**
     * Given origin path and destination path, copies the JCR node without deleting the origin
     * @param originPath
     * @param destinationPath
     */
    public static void copyNodes(String originPath, String destinationPath, ResourceResolverFactory resolverFactory, boolean refreshNodes) {
        try {
            ResourceResolver resourceResolver = getResourceResolver(resolverFactory);
            Resource originResource = resourceResolver.getResource(originPath);
            Resource destinationResource = resourceResolver.getResource(destinationPath);

            if (originResource != null && destinationResource != null) {
                originResource.getChildren().forEach(currentResource -> {
                    Node srcNode = currentResource.adaptTo(Node.class);
                    Node destNode = destinationResource.adaptTo(Node.class);
                    try {
                        boolean isNotSameOrHasNoChildren = true;

                        if (destinationResource.hasChildren()) {
                            isNotSameOrHasNoChildren = getResourceChildrenName(destinationResource)
                                    .stream()
                                    .anyMatch(name -> !StringUtils.equalsIgnoreCase(name, currentResource.getName()));
                        }

                        if (isNotSameOrHasNoChildren || refreshNodes) {
                            // clear JCR before copying atomic components view
                            cleanJcr(destNode);
                            com.day.cq.commons.jcr.JcrUtil.copy(srcNode, destNode, null, true);
                        }
                    } catch (RepositoryException e) {
                        log.error("Error copying" + originPath + " -> Error: " + e.toString());
                    }
                });
            }
            resourceResolver.commit();
        } catch (PersistenceException | LoginException e) {
            log.error("Error copying" + originPath + " -> Error: " + e.toString());
        }
    }

    private static List < String > getResourceChildrenName(Resource resource) {
        return StreamSupport.stream(resource.getChildren().spliterator(), false)
                .map(Resource::getName)
                .collect(Collectors.toList());
    }

    private static void cleanJcr(Node node) {
        if (node != null) {
            try {
                NodeIterator nodes = node.getNodes();
                @SuppressWarnings("unchecked") Iterable < Node > nodeIterable = () -> nodes;
                nodeIterable.forEach(currentNode -> {
                    try {
                        currentNode.remove();
                    } catch (RepositoryException e) {
                        log.error("Error removing Nodes" + e.getMessage());
                    }
                });

            } catch (RepositoryException e) {
                log.error("Error checking Nodes" + e.getMessage());
            }
        }
    }
}