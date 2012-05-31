package com.rackspace.papi.service.proxy.jersey;

import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;
import com.rackspace.papi.http.proxy.common.HttpResponseCodeProcessor;
import com.rackspace.papi.service.proxy.RequestProxyService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("requestProxyService")
public class RequestProxyServiceImpl implements RequestProxyService {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProxyServiceImpl.class);
    private Client client;
    private Integer connectionTimeout = new Integer(0);
    private Integer readTimeout = new Integer(0);
    //private final Map<String, TargetHostInfo> hosts = new HashMap<String, TargetHostInfo>();

    public RequestProxyServiceImpl() {
    }

    @Override
    public synchronized void setTimeouts(Integer connectionTimeout, Integer readTimeout) {
        LOG.info("Connection and/or read timeouts changed to: " + connectionTimeout + "/" + readTimeout);
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;

        // Invalidate client
        client = null;
    }

    private static class TargetHostInfo {

        private final URI proxiedHostUri;
        private final URL proxiedHostUrl;

        public TargetHostInfo(String targetHost) {
            URI targetUri = null;

            try {
                targetUri = new URI(targetHost);
            } catch (URISyntaxException e) {
                LOG.error("Invalid target host url: " + targetHost, e);
            }

            proxiedHostUri = targetUri;
            proxiedHostUrl = asUri(proxiedHostUri);

        }

        private URL asUri(URI host) {
            try {
                return new URL(host.getScheme(), host.getHost(), host.getPort(), "");
            } catch (MalformedURLException ex) {
                LOG.error("Invalid host url: " + host, ex);
            }
            return null;
        }

        public URI getProxiedHostUri() {
            return proxiedHostUri;
        }

        public URL getProxiedHostUrl() {
            return proxiedHostUrl;
        }
    }

    private synchronized Client getClient() {
        if (client == null) {
            DefaultClientConfig cc = new JerseyPropertiesConfigurator(connectionTimeout, readTimeout).configure();
            client = Client.create(cc);

            if (LOG.isInfoEnabled()) {
                LOG.info("Enabling info logging of jersey client requests");
                client.addFilter(new LoggingFilter());
            } else {
                LOG.warn("**** Jersey client request logging not enabled *****");
            }

        }

        return client;
    }

    @Override
    public int proxyRequest(String targetHost, HttpServletRequest request, HttpServletResponse response) throws IOException {
        TargetHostInfo host = new TargetHostInfo(targetHost);
        final String target = host.getProxiedHostUrl().toExternalForm() + request.getRequestURI();

        JerseyRequestProcessor processor = new JerseyRequestProcessor(request, host.getProxiedHostUri());
        WebResource resource = getClient().resource(target);
        WebResource.Builder builder = processor.process(resource);
        try {
            return executeProxyRequest(host, builder, request, response);
        } catch (HttpException ex) {
            LOG.error("Error processing request", ex);
        }

        return -1;
    }

    private String extractHostPath(HttpServletRequest request) {
        final StringBuilder myHostName = new StringBuilder(request.getServerName());

        if (request.getServerPort() != 80) {
            myHostName.append(":").append(request.getServerPort());
        }

        return myHostName.append(request.getContextPath()).toString();
    }

    private int executeProxyRequest(TargetHostInfo host, WebResource.Builder builder, HttpServletRequest sourceRequest, HttpServletResponse sourceResponse) throws IOException, HttpException {

        ClientResponse response = builder.method(sourceRequest.getMethod(), ClientResponse.class);

        HttpResponseCodeProcessor responseCode = new HttpResponseCodeProcessor(response.getStatus());
        JerseyResponseProcessor responseProcessor = new JerseyResponseProcessor(response, sourceResponse);

        if (responseCode.isRedirect()) {
            responseProcessor.sendTranslatedRedirect(host.getProxiedHostUrl().toExternalForm(), extractHostPath(sourceRequest));
        } else {
            responseProcessor.process();
        }

        return responseCode.getCode();
    }

    @Override
    public ServiceClientResponse get(String uri, Map<String, String> headers, String... queryParameters) {
        return new ServiceClient(getClient()).get(uri, headers, queryParameters);
    }

    @Override
    public ServiceClientResponse post(String uri, JAXBElement body, MediaType contentType) {
        return new ServiceClient(getClient()).post(uri, body, contentType);
    }

    @Override
    public ServiceClientResponse post(String uri, byte[] body, MediaType contentType) {
        return new ServiceClient(getClient()).post(uri, body, contentType);
    }

    @Override
    public ServiceClientResponse delete(String uri, Map<String, String> headers, String... queryParameters) {
        return new ServiceClient(getClient()).delete(uri, headers, queryParameters);
    }

    @Override
    public ServiceClientResponse put(String uri, JAXBElement body, MediaType contentType) {
        return new ServiceClient(getClient()).put(uri, body, contentType);
    }

    @Override
   public ServiceClientResponse put(String uri, byte[] body, MediaType contentType) {
        return new ServiceClient(getClient()).put(uri, body, contentType);
   }
}