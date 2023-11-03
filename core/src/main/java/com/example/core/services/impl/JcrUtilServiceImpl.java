package com.example.core.services.impl;

import com.example.core.services.JcrUtilService;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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

@Component(service = JcrUtilService.class)
public class JcrUtilServiceImpl implements JcrUtilService {

    @Reference
    private SlingRepository slingRepository;

    @Reference
    private ResourceResolverFactory resolverFactory;

    private static final String JCR_SERVICE_USER = "jcr-service-user";

    private static final Logger log = LoggerFactory.getLogger(JcrUtilServiceImpl.class);
    @Override
    public ResourceResolver getResourceResolver() throws LoginException {
        Map< String, Object > authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, JCR_SERVICE_USER);
        return resolverFactory.getServiceResourceResolver(authInfo);
    }

    @Override
    public Session getSession() throws RepositoryException {
        return slingRepository.loginService(JCR_SERVICE_USER, slingRepository.getDefaultWorkspace());
    }

    @Override
    public void copyNodes(String originPath, String destinationPath, ResourceResolver resourceResolver, boolean refreshNodes) {
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
    }


    /**
     * Get Resource Children Name
     *
     * @param resource Resource
     * @return List of Resource Names
     */
    private static List< String > getResourceChildrenName(Resource resource) {
        return StreamSupport.stream(resource.getChildren().spliterator(), false)
            .map(Resource::getName)
            .collect(Collectors.toList());
    }

    /**
     * Clean JCR
     * Clean out all children nodes
     *
     * @param node JCR Node
     */
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
