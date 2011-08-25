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

package com.google.api.explorer.client.history;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiService;
import com.google.common.collect.Maps;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.Map;

/**
 * Tests for the JsonPreffifier
 *
 */
public class JsonPrettifierTest extends TestCase {
  private static final String BUZZ_BASE_PATH = "/buzz/v1/";
  private static final String BUZZ_LINK =
      "https://www.googleapis.com/buzz/v1/activities/123456789/@public?alt=json";
  private static final String EXPLORER_LINK =
      "#_s=buzz&_v=v1&_m=activities.list&alt=json&userId=123456789&scope=@public";
  private static final String LIST_METHOD_NAME = "activities.list";
  private static final String LIST_METHOD_PATH = "activities/{userId}/{scope}";
  private static final String BUZZ_NAME = "buzz";
  private static final String BUZZ_VERSION = "v1";

  private AppState mockAppState;

  @Override
  public void setUp() {
    ApiMethod listActivities = EasyMock.createNiceMock(ApiMethod.class);
    expect(listActivities.getHttpMethod()).andReturn(HttpMethod.GET).anyTimes();
    expect(listActivities.getPath()).andReturn(LIST_METHOD_PATH).anyTimes();
    replay(listActivities);

    Map<String, ApiMethod> allActivities = Maps.newHashMap();
    allActivities.put(LIST_METHOD_NAME, listActivities);

    ApiService buzzService = EasyMock.createNiceMock(ApiService.class);
    expect(buzzService.getBasePath()).andReturn(BUZZ_BASE_PATH).anyTimes();
    expect(buzzService.allMethods()).andReturn(allActivities).anyTimes();
    expect(buzzService.getName()).andReturn(BUZZ_NAME).anyTimes();
    expect(buzzService.getVersion()).andReturn(BUZZ_VERSION).anyTimes();
    replay(buzzService);

    mockAppState = EasyMock.createNiceMock(AppState.class);
    expect(mockAppState.getCurrentService()).andReturn(buzzService).anyTimes();
    replay(mockAppState);

    JsonPrettifier.appState = mockAppState;
  }

  /**
   * Test the identification of explorer links
   */
  public void testExplorerLinks() {
    Map.Entry<String, ApiMethod> method = JsonPrettifier.getMethodForUrl(BUZZ_LINK);
    assertNotNull(method);
    assertEquals(LIST_METHOD_NAME, method.getKey());
    assertEquals(HttpMethod.GET, method.getValue().getHttpMethod());

    String link = JsonPrettifier.createExplorerLink(BUZZ_LINK, method);
    assertEquals(EXPLORER_LINK, link);
  }
}
