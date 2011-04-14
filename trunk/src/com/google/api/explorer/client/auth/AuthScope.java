/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.api.explorer.client.auth;

import com.google.api.explorer.client.base.ApiService;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Hard-coded Authentication scopes for various APIs.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
// TODO(jasonhall): Get these from Discovery when it supports it.
public enum AuthScope {

  BUZZ("buzz", "Buzz (read/write)", "https://www.googleapis.com/auth/buzz"),
  BUZZ_READONLY("buzz", "Buzz (read-only)", "https://www.googleapis.com/auth/buzz.readonly"),
  BUZZ_PHOTOS("buzz", "Buzz (photos)", "https://www.googleapis.com/auth/photos"),
  MODERATOR("moderator", "Moderator", "https://www.googleapis.com/auth/moderator"),
  LATITUDE("latitude", "Latitude", "https://www.googleapis.com/auth/latitude"),
  TASKS("tasks", "Tasks (read/write)", "https://www.googleapis.com/auth/tasks"),
  TASKS_READONLY("tasks", "Tasks (read-only)", "https://www.googleapis.com/auth/tasks.readonly"),
  URLSHORTENER("urlshortener", "URL Shortener", "https://www.googleapis.com/auth/urlshortener"),
  ;

  private final String serviceName;
  final String name;
  final String authScopeUrl;

  AuthScope(String serviceName, String name, String authScopeUrl) {
    this.serviceName = serviceName;
    this.name = name;
    this.authScopeUrl = authScopeUrl;
  }

  public static List<AuthScope> forService(ApiService service) {
    List<AuthScope> scopes = Lists.newArrayList();
    String serviceName = service.getName();
    for (AuthScope scope : values()) {
      if (scope.serviceName.equals(serviceName)) {
        scopes.add(scope);
      }
    }
    return scopes;
  }
}
