package com.DBArena.services.gateway.web;

import com.DBArena.services.gateway.config.GatewayProperties;
import com.DBArena.services.gateway.config.GatewayRouteResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Forwards every {@code /api/v1/**} request to whichever backend service
 * {@link GatewayRouteResolver} matches, unchanged except for hop-by-hop
 * headers. Deliberately does not use {@code @RequestBody}/{@code @ResponseBody}
 * conversion - request/response bytes are copied as-is via the servlet
 * API so an arbitrary content type (including one no converter here
 * knows about) still proxies correctly, and multi-value headers such as
 * a repeated {@code Set-Cookie} survive the trip intact.
 */
@RestController
public class ReverseProxyController {

    private final GatewayRouteResolver routeResolver;
    private final RestClient restClient;

    public ReverseProxyController(GatewayRouteResolver routeResolver, RestClient restClient) {
        this.routeResolver = routeResolver;
        this.restClient = restClient;
    }

    @RequestMapping(value = "/api/v1/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI();
        GatewayProperties.RouteRule route = routeResolver.resolve(path)
                .orElseThrow(() -> new NoRouteFoundException(path));

        URI targetUri = buildTargetUri(route.uri(), path, request.getQueryString());
        byte[] requestBody = request.getInputStream().readAllBytes();

        try {
            RestClient.RequestBodySpec requestSpec = restClient
                    .method(HttpMethod.valueOf(request.getMethod()))
                    .uri(targetUri)
                    .headers(headers -> copyHeaders(request, headers));

            ResponseEntity<byte[]> upstream = requestBody.length > 0
                    ? requestSpec.body(requestBody).retrieve().toEntity(byte[].class)
                    : requestSpec.retrieve().toEntity(byte[].class);

            writeResponse(response, upstream.getStatusCode().value(), upstream.getHeaders(), upstream.getBody());
        } catch (RestClientResponseException e) {
            HttpHeaders upstreamHeaders = e.getResponseHeaders();
            writeResponse(response, e.getStatusCode().value(),
                    upstreamHeaders == null ? new HttpHeaders() : upstreamHeaders,
                    e.getResponseBodyAsByteArray());
        } catch (ResourceAccessException e) {
            throw new UpstreamUnavailableException(route.prefix(), e);
        }
    }

    private static URI buildTargetUri(String baseUri, String path, String queryString) {
        String uri = baseUri + path;
        if (queryString != null && !queryString.isBlank()) {
            uri = uri + "?" + queryString;
        }
        return URI.create(uri);
    }

    private static void copyHeaders(HttpServletRequest request, HttpHeaders target) {
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return;
        }
        for (String name : Collections.list(names)) {
            if (HopByHopHeaders.isHopByHop(name)) {
                continue;
            }
            for (String value : Collections.list(request.getHeaders(name))) {
                target.add(name, value);
            }
        }
    }

    private static void writeResponse(HttpServletResponse response, int status, HttpHeaders headers, byte[] body)
            throws IOException {
        response.setStatus(status);
        headers.forEach((name, values) -> {
            if (HopByHopHeaders.isHopByHop(name)) {
                return;
            }
            for (String value : values) {
                response.addHeader(name, value);
            }
        });
        if (body != null && body.length > 0) {
            response.getOutputStream().write(body);
        }
    }
}
