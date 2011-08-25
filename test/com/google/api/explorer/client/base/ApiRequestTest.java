/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.explorer.client.base;

import com.google.api.explorer.client.TestUrlEncoder;
import com.google.api.explorer.client.UrlEncoder;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.easymock.EasyMock;

/**
 * Tests validation of parameter values and creation of request path in
 * {@link ApiRequest}s.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ApiRequestTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Config.setApiKey("");
    ApiRequest.urlEncoder = new TestUrlEncoder();
  }

  @Override
  protected void tearDown() {
    ApiRequest.urlEncoder = UrlEncoder.DEFAULT;
  }

  /** When an API key is set, it is added as a parameter value. */
  public void testApiKey() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);

    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    EasyMock.expect(service.method("method")).andReturn(method);

    EasyMock.replay(method, service);

    ApiRequest request = new ApiRequest(service, "method");

    Config.setApiKey("MY_API_KEY");
    request.maybeSetApiKeyParameter();
    assertEquals(Lists.newArrayList("MY_API_KEY"), request.paramValues.get("key"));
    EasyMock.verify(method, service);
  }

  /**
   * Tests expected request properties when given a request path and HTTP
   * method.
   */
  public void testCreateRequestWithPath() {
    ApiRequest request = new ApiRequest("/some/path");

    assertEquals("/some/path", request.getRequestPath());
    assertEquals(HttpMethod.GET, request.httpMethod);
    assertNull(request.method);
    assertNull(request.service);

    // Setting the API key does not make it appear in the query
    Config.setApiKey("MY_API_KEY");
    assertEquals("/some/path", request.getRequestPath());
    // TODO(jasonhall): Test creating a request with query parameters.
  }

  /** Tests proper creation of the Discovery path and error cases. */
  public void testDiscoveryPath() {
    assertEquals("/discovery/v1/apis/service/version/rest",
        ApiServiceFactory.createDiscoveryPath("service", "version"));

    assertIllegalArgument(null, "version", "Service name cannot be null or empty");
    assertIllegalArgument("", "version", "Service name cannot be null or empty");
    assertIllegalArgument("service", null, "Version cannot be null or empty");
    assertIllegalArgument("service", "", "Version cannot be null or empty");
  }

  /**
   * Asserts that an IllegalArgumentException is raised with the expected error
   * message, given the service and version.
   */
  private void assertIllegalArgument(
      String serviceName, String version, String expectedErrorMessage) {
    try {
      ApiServiceFactory.createDiscoveryPath(serviceName, version);
      fail("Illegal argument given, passed precondition. Expected: " + expectedErrorMessage);
    } catch (IllegalArgumentException e) {
      assertEquals(expectedErrorMessage, e.getMessage());
    }
  }

  /**
   * Tests generation of the request path when it is not explicitly set on
   * construction.
   */
  public void testGetRequestPath() {
    ApiMethod method = EasyMock.createControl().createMock(ApiMethod.class);
    EasyMock.expect(method.getHttpMethod()).andReturn(HttpMethod.GET);
    EasyMock.expect(method.getPath()).andReturn("/path/to/{pathParam}").times(4);

    ApiService service = EasyMock.createControl().createMock(ApiService.class);
    EasyMock.expect(service.method("method")).andReturn(method);
    EasyMock.expect(service.getBasePath()).andReturn("/base").times(4);

    EasyMock.replay(method, service);

    ApiRequest request = new ApiRequest(service, "method");

    // Test that the path is generated even when required parameters are not
    // specified.
    assertEquals("/base/path/to/", request.getRequestPath());

    // Setting the path param sets it in the path.
    request.paramValues.put("pathParam", "1234");
    assertEquals("/base/path/to/1234", request.getRequestPath());

    request.paramValues.replaceValues("pathParam", ImmutableList.of("12/34"));
    assertEquals("/base/path/to/12%2F34", request.getRequestPath());

    request.paramValues.put("nonPathParam", "abc/de");
    assertEquals("/base/path/to/12%2F34?nonPathParam=abc%2Fde", request.getRequestPath());

    EasyMock.verify(method, service);
  }
}
