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

package com.google.api.explorer.client;

import com.google.api.explorer.client.auth.AuthView;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ApiService.AuthInformation;
import com.google.api.explorer.client.base.ApiService.AuthScope;
import com.google.api.explorer.client.base.ApiServiceFactory;
import com.google.api.explorer.client.base.Config;
import com.google.api.explorer.client.base.DefaultAsyncCallback;
import com.google.api.explorer.client.event.MethodSelectedEvent;
import com.google.api.explorer.client.event.ServiceDefinitionsLoadedEvent;
import com.google.api.explorer.client.event.ServiceLoadedEvent;
import com.google.api.explorer.client.event.ServiceSelectedEvent;
import com.google.api.explorer.client.event.VersionSelectedEvent;
import com.google.api.explorer.client.history.HistoryPanel;
import com.google.api.explorer.client.method.MethodSelector;
import com.google.api.explorer.client.parameter.ParameterForm;
import com.google.api.explorer.client.service.ServiceSelector;
import com.google.api.explorer.client.version.VersionSelector;
import com.google.api.gwt.oauth2.client.Auth;
import com.google.api.gwt.oauth2.client.AuthRequest;
import com.google.common.collect.Iterables;
import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.Map;

/**
 * View of the whole app.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class FullView extends Composite
    implements
    ServiceLoadedEvent.Handler,
    ServiceSelectedEvent.Handler,
    MethodSelectedEvent.Handler,
    ServiceDefinitionsLoadedEvent.Handler,
    VersionSelectedEvent.Handler {

  // Height of the method selector pane when not hidden. When it is hidden its
  // height will be set to 0. This matches the heigh specified in the .ui.xml
  // template.
  private static final int SELECTOR_PANEL_SIZE = 306;

  // OAuth2 scope to use when making a request for private APIs.
  // TODO(jasonhall): When we know what scope URL to put here, just hard-code it
  // and don't jump through the hoop of looking it up in Discovery itself. This
  // is just a short-term solution so that the code can start working as soon as
  // possible.
  private static String discoveryAuthScope = null;

  private static FullViewUiBinder uiBinder = GWT.create(FullViewUiBinder.class);

  interface FullViewUiBinder extends UiBinder<Widget, FullView> {
  }

  @UiField DockLayoutPanel dockLayoutPanel;
  @UiField Image logo;
  @UiField Image tt;
  @UiField HTMLPanel selectorPanel;
  @UiField(provided = true) AuthView authView;
  @UiField TableCellElement serviceColumn;
  @UiField(provided = true) ServiceSelector serviceSelector;
  @UiField TableCellElement versionColumn;
  @UiField(provided = true) VersionSelector versionSelector;
  @UiField TableCellElement methodColumn;
  @UiField(provided = true) MethodSelector methodSelector;
  @UiField(provided = true) ParameterForm parameterForm;
  @UiField Label showHide;
  @UiField HTMLPanel footer;

  // Whether or not the method selection panel is currently hidden.
  private boolean hidden = false;

  // Duration (in ms) of animation to show/hide the method selection panel.
  private static final int ANIMATION_DURATION = 200;

  /** {@link Animation} used to show the method selection panel. */
  private final Animation showMethodPanelAnimation = new Animation() {
    @Override
    protected void onUpdate(double progress) {
      dockLayoutPanel.setWidgetSize(selectorPanel, SELECTOR_PANEL_SIZE * progress);
      hidden = false;
    }
  };

  /** {@link Animation} used to hide the method selection panel. */
  private final Animation hideMethodPanelAnimation = new Animation() {
    @Override
    protected void onUpdate(double progress) {
      dockLayoutPanel.setWidgetSize(
          selectorPanel, SELECTOR_PANEL_SIZE - SELECTOR_PANEL_SIZE * progress);
      hidden = true;
    }
  };

  private final EventBus eventBus;

  public FullView(EventBus eventBus, AppState appState, AuthManager authManager) {
    Scheduler scheduler = Scheduler.get();
    this.authView = new AuthView(eventBus, authManager);
    this.serviceSelector = new ServiceSelector(eventBus);
    this.versionSelector = new VersionSelector(eventBus, scheduler);
    this.methodSelector = new MethodSelector(eventBus, appState, scheduler);
    this.parameterForm = new ParameterForm(eventBus, appState, authManager);

    initWidget(uiBinder.createAndBindUi(this));

    this.eventBus = eventBus;

    dockLayoutPanel.add(new HistoryPanel(eventBus, appState));

    eventBus.addHandler(ServiceLoadedEvent.TYPE, this);
    eventBus.addHandler(ServiceSelectedEvent.TYPE, this);
    eventBus.addHandler(MethodSelectedEvent.TYPE, this);
    eventBus.addHandler(ServiceDefinitionsLoadedEvent.TYPE, this);
    eventBus.addHandler(VersionSelectedEvent.TYPE, this);

    // If Private APIs UI is enabled, make a request to find out the
    // Discovery auth scope to use, and set the private API icon visible.
    if (Config.isPrivateApiEnabled()) {
      final String oauth2Key = "oauth2";
      ApiServiceFactory.INSTANCE.create("discovery", "v1", new AsyncCallback<ApiService>() {
        @Override
        public void onSuccess(ApiService result) {
          Map<String, AuthInformation> auth = result.getAuth();
          if (auth != null && auth.containsKey(oauth2Key)) {
            Map<String, AuthScope> scopes = auth.get(oauth2Key).getScopes();
            if (!scopes.isEmpty()) {
              // TODO(jasonhall): This value is a constant. When we know it,
              // just hard-code it.
              discoveryAuthScope = Iterables.getOnlyElement(scopes.keySet());
              tt.setVisible(true);
            }
          }
        }

        @Override
        public void onFailure(Throwable caught) {
          // Ignore failures in fetching Discovery here, we'll just keep the
          // lock icon hidden.
        }
      });
    }
  }

  /** Go back to the "home" state of the app when the logo is clicked. */
  @UiHandler("logo")
  void clickLogo(ClickEvent event) {
    Window.Location.assign("");
  }

  @UiHandler("showHide")
  void showHide(ClickEvent event) {
    if (hidden) {
      showMethodPanelAnimation.run(ANIMATION_DURATION);
    } else {
      hideMethodPanelAnimation.run(ANIMATION_DURATION);
    }
    hidden = !hidden;
  }

  @SuppressWarnings("deprecation")
  @UiHandler("tt")
  void authWithDiscovery(ClickEvent event) {
    if (discoveryAuthScope == null) {
      return;
    }

    // When the private APIs lock icon is clicked, display the OAuth 2.0 popup
    // prompt with the discovery auth scope.
    AuthRequest req =
        new AuthRequest(Config.AUTH_URL, Config.CLIENT_ID).withScopes(discoveryAuthScope);

    Auth.get().login(req, new Callback<String, Throwable>() {
      @Override
      public void onSuccess(String token) {
        // When the user grants access to auth'd Discovery, make a request
        // using the new token to get the full list of APIs, then display them
        // in the service selector (by firing a service definitions loaded
        // event).
        Config.setDiscoveryAuthToken(token);
        ApiRequest request = new ApiRequest(Config.DIRECTORY_REQUEST_PATH);
        request.send(new DefaultAsyncCallback<ApiResponse>() {
          @Override
          public void onSuccess(ApiResponse response) {
            ApiDirectory directory = ApiDirectory.Helper.fromString(response.body);
            eventBus.fireEvent(new ServiceDefinitionsLoadedEvent(directory.getItems()));
            tt.setVisible(false);
          }
        });
      }

      @Override
      public void onFailure(Throwable caught) {
        // Ignore failures in granting access, the user can just click the icon
        // again to start over.
      }
    });
  }

  @Override
  public void onVersionSelected(VersionSelectedEvent event) {
    versionSelector.setVisible(true);
    methodSelector.setVisible(true);
    parameterForm.setVisible(false);
  }

  @Override
  public void onServiceDefinitionsLoaded(ServiceDefinitionsLoadedEvent event) {
    serviceSelector.setVisible(true);
    versionSelector.setVisible(false);
    methodSelector.setVisible(false);
    parameterForm.setVisible(false);
  }

  @Override
  public void onServiceSelected(ServiceSelectedEvent event) {
    serviceSelector.setVisible(true);
    versionSelector.setVisible(true);
    methodSelector.setVisible(false);
    parameterForm.setVisible(false);
  }

  @Override
  public void onServiceLoaded(ServiceLoadedEvent event) {
    serviceSelector.setVisible(true);
    versionSelector.setVisible(true);
    methodSelector.setVisible(true);
    parameterForm.setVisible(false);
  }

  @Override
  public void onMethodSelected(MethodSelectedEvent event) {
    serviceSelector.setVisible(true);
    versionSelector.setVisible(true);
    methodSelector.setVisible(true);
    parameterForm.setVisible(true);
  }
}
