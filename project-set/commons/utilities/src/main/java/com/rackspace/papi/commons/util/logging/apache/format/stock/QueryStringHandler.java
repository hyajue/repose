package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QueryStringHandler implements FormatterLogic {

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        return request.getQueryString() == null ? "" : request.getQueryString();
    }
}
