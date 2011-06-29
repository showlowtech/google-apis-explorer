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

package com.google.api.explorer.client.parameter;

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.Resources;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiMethod.HttpMethod;
import com.google.api.explorer.client.base.ApiParameter;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.editors.Editor;
import com.google.api.explorer.client.editors.EditorFactory;
import com.google.api.explorer.client.parameter.schema.SchemaForm;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * View of the parameter form UI.
 * 
 * @author jasonhall@google.com (Jason Hall)
 */
public class ParameterForm extends Composite implements ParameterFormPresenter.Display {

  // Set of HTTP methods that do not take a request body. If the method being
  // shown is of this type, the body textarea should be hidden.
  public static final Set<HttpMethod> HIDE_BODY_METHODS =
      EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

  private static ParameterFormUiBinder uiBinder = GWT.create(ParameterFormUiBinder.class);

  interface ParameterFormUiBinder extends UiBinder<Widget, ParameterForm> {
  }

  @UiField public Label noParams;
  @UiField public FlexTable table;
  @UiField public Label bodyDisclosure;
  @UiField public PopupPanel popupPanel;
  @UiField public Button close;
  @UiField public InlineLabel requiredDescription;
  @UiField public Button submit;
  @UiField public ImageElement executing;
  @UiField(provided = true) public SchemaForm schemaForm;
  @UiField public RadioButton selectSchemaButton;
  @UiField public RadioButton selectBasicButton;
  @UiField public TextArea requestBody;

  private final AppState appState;
  private final ParameterFormPresenter presenter;
  private final CellFormatter cellFormatter;

  private static final String ADD_REQ_BODY = "Add request body";
  private static final String CHANGE_REQ_BODY = "Change request body";
  
  // Whether or not the basic textbox request body editor should be used.
  private boolean useBasicEditor = false;

  /**
   * Bi-directional mapping between parameter name -> editor responsible for
   * providing that parameter's value.
   */
  private BiMap<String, Editor> nameToEditor = HashBiMap.create();

  public ParameterForm(EventBus eventBus, AppState appState, AuthManager authManager) {
    schemaForm = new SchemaForm(appState);
    initWidget(uiBinder.createAndBindUi(this));

    this.appState = appState;
    cellFormatter = table.getCellFormatter();
    this.presenter = new ParameterFormPresenter(eventBus, appState, authManager, this);

    executing.setSrc(Resources.INSTANCE.miniLoading().getURL());
    UIObject.setVisible(executing, false);

    popupPanel.show();
    popupPanel.hide();

   selectSchemaButton.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        selectSchema();
      }
    });

    selectBasicButton.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        selectBasic();
      }
    });
  }

  protected void initWidget() {
    initWidget(uiBinder.createAndBindUi(this));
  }

  @UiHandler("submit")
  public void submit(ClickEvent event) {
    presenter.submit();
  }

  @UiHandler("bodyDisclosure")
  public void open(ClickEvent event) {
    popupPanel.show();
    popupPanel.center();
  }

  @UiHandler("close")
  public void close(ClickEvent event) {
    bodyDisclosure.setText(getBodyText().isEmpty() ? ADD_REQ_BODY : CHANGE_REQ_BODY);
    popupPanel.hide();
  }

  public void selectSchema() {
    selectSchemaButton.setEnabled(true);
    selectSchemaButton.setValue(true);
    schemaForm.setVisible(true);
    requestBody.setVisible(false);
    useBasicEditor = false;
  }

  public void selectBasic() {
    selectBasicButton.setValue(true);
    schemaForm.setVisible(false);
    requestBody.setVisible(true);
    useBasicEditor = true;
  }

  /** Sets the parameters displayed in the table. */
  @Override
  public void setMethod(ApiMethod method, SortedMap<String, ApiParameter> sortedParams) {
    requiredDescription.setVisible(false);
    boolean hasParameters = !(sortedParams == null || sortedParams.isEmpty());
    noParams.setVisible(!hasParameters);
    bodyDisclosure.setText(ADD_REQ_BODY);
    bodyDisclosure.setVisible(!HIDE_BODY_METHODS.contains(method.getHttpMethod()));

    table.clear(true);
    while (table.getRowCount() > 0) {
      table.removeRow(table.getRowCount() - 1);
    }
    table.setVisible(hasParameters);

    nameToEditor.clear();
    requestBody.setText("");

    if (hasParameters) {
      int row = 0;
      for (Map.Entry<String, ApiParameter> entry : sortedParams.entrySet()) {
        String paramName = entry.getKey();
        ApiParameter param = entry.getValue();
        addEditorRow(paramName, param, row++);
        addDescriptionRow(param, row++);
      }
    }

    // Call selectSchema() to reset enabled/visible state of form.
    selectSchema();
    Schema requestSchema = appState.getCurrentService().requestSchema(method);
    if (requestSchema != null) {
      schemaForm.setSchema(requestSchema);
    } else {
      selectBasic();
      selectSchemaButton.setEnabled(false);
    }
  }

  /**
   * Adds a row to the table containing the parameter name, whether it is
   * required, and an {@link Editor} to provide a value.
   */
  private void addEditorRow(String paramName, ApiParameter param, int row) {
    // First cell in row displays the parameter name and whether the parameter
    // is required.
    boolean required = param.isRequired();
    table.setText(row, 0, (required ? "*" : "") + paramName + " =");
    if (required) {
      requiredDescription.setVisible(true);
    }

    // Second cell in row displays the editor for the parameter value.
    Editor editor = EditorFactory.forParameter(param);
    nameToEditor.put(paramName, editor);
    table.setWidget(row, 1, editor.createAndSetView().asWidget());

    if (paramName.equals("alt")) {
      editor.setValue(ImmutableList.of("json"));
      editor.setEnabled(false);
    }

    cellFormatter.setStyleName(row, 0, Resources.INSTANCE.style().parameterFormNameCell());
    cellFormatter.setStyleName(row, 1, Resources.INSTANCE.style().parameterFormTextBoxCell());
  }

  /** Adds a row to the table containing the description of the parameter. */
  private void addDescriptionRow(ApiParameter param, int row) {
    // Second cell contains the parameter description.
    table.setText(row, 1, ParameterFormPresenter.generateDescriptionString(param));
    cellFormatter.addStyleName(row, 1, Resources.INSTANCE.style().parameterFormDescriptionCell());
  }

  /** Return a {@link Map} of parameter keys to values as specified by the user. */
  @Override
  public Multimap<String, String> getParameterValues() {
    Multimap<String, String> values = ArrayListMultimap.create();
    for (Map.Entry<String, Editor> entry : nameToEditor.entrySet()) {
      Editor editor = entry.getValue();
      editor.displayValidation();
      values.putAll(entry.getKey(), editor.getValue());
    }
    return values;
  }

  @Override
  public void setParameterValues(Multimap<String, String> paramValues) {
    for (String key : paramValues.keySet()) {
      if (nameToEditor.containsKey(key)) {
        nameToEditor.get(key).setValue(Lists.newArrayList(paramValues.get(key)));
      }
    }
  }

  /**
   * Shows or hides the "executing" gif, and enables/disables the "Execute"
   * button.
   */
  @Override
  public void setExecuting(boolean executing) {
    UIObject.setVisible(this.executing, executing);
    submit.setEnabled(!executing);
  }

  @Override
  public String getBodyText() {
    return useBasicEditor ? requestBody.getText() : schemaForm.getStringValue();
  }
}
