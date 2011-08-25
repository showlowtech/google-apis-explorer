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

package com.google.api.explorer.client.embedded;

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.Resources;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiParameter;
import com.google.api.explorer.client.base.Schema;
import com.google.api.explorer.client.editors.Editor;
import com.google.api.explorer.client.editors.EditorFactory;
import com.google.api.explorer.client.parameter.ParameterForm;
import com.google.api.explorer.client.parameter.ParameterFormPresenter;
import com.google.api.explorer.client.parameter.schema.FieldsEditor;
import com.google.common.collect.ImmutableList;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.Map;
import java.util.SortedMap;

import javax.annotation.Nullable;

/**
 * View of the parameter form UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class EmbeddedParameterForm extends ParameterForm implements ParameterFormPresenter.Display {

  private static EmbeddedParameterFormUiBinder embeddedUiBinder =
      GWT.create(EmbeddedParameterFormUiBinder.class);

  @UiTemplate("EmbeddedParameterForm.ui.xml")
  interface EmbeddedParameterFormUiBinder extends UiBinder<Widget, EmbeddedParameterForm> {
  }

  public EmbeddedParameterForm(EventBus eventBus, AppState appState, AuthManager authManager) {
    super(eventBus, appState, authManager);
  }

  @Override
  protected void initWidget() {
    initWidget(embeddedUiBinder.createAndBindUi(this));
  }

  /** Sets the parameters displayed in the table. */
  @Override
  public void setMethod(ApiMethod method, SortedMap<String, ApiParameter> sortedParams) {
    // Reset the state of the form.
    setVisible(true);
    requiredDescription.setVisible(false);
    bodyDisclosure.setText(ADD_REQ_BODY);
    nameToEditor.clear();

    // Reset the table's contents, clear it out.
    table.clear(true);
    while (table.getRowCount() > 0) {
      table.removeRow(table.getRowCount() - 1);
    }

    boolean hasParameters = !(sortedParams == null || sortedParams.isEmpty());
    table.setVisible(hasParameters);

    // Add an editor row for each parameter in the method.
    int row = 0;
    if (hasParameters) {
      for (Map.Entry<String, ApiParameter> entry : sortedParams.entrySet()) {
        String paramName = entry.getKey();
        ApiParameter param = entry.getValue();
        addEditorRow(paramName, param, row++);
      }
    }

    // Add a row for the fields parameter.
    Schema responseSchema = appState.getCurrentService().responseSchema(method);
    addFieldsRow(responseSchema, row++);
    addRequestBodyRow(row++);
    addExecuteRow(row);

    // Reset the schema editor to having the Guided View selected
    tabPanel.selectTab(0 /* Guided View */);
    requestBody.setText("");
    Schema requestSchema = appState.getCurrentService().requestSchema(method);
    tabPanel.getTabBar().setTabEnabled(0 /* Guided View */, requestSchema != null);
    if (requestSchema != null) {
      schemaForm.setSchema(requestSchema);
    } else {
      tabPanel.selectTab(1 /* Basic View */);
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
    table.setText(row, 0, paramName);
    if (required) {
      requiredDescription.setVisible(true);
      cellFormatter.addStyleName(row, 0, EmbeddedResources.INSTANCE.style().requiredParameter());
    }

    // Second cell in row displays the editor for the parameter value.
    Editor editor = EditorFactory.forParameter(param);
    nameToEditor.put(paramName, editor);
    table.setWidget(row, 1, editor.createAndSetView().asWidget());

    // Third cell in row displays the description.
    table.setText(row, 2, ParameterFormPresenter.generateDescriptionString(param));

    if (paramName.equals("alt")) {
      editor.setValue(ImmutableList.of("json"));
      editor.setEnabled(false);
    }

    cellFormatter.addStyleName(row, 0,
        EmbeddedResources.INSTANCE.style().parameterFormNameCell());
    cellFormatter.addStyleName(row, 2,
        EmbeddedResources.INSTANCE.style().parameterFormDescriptionCell());
  }

  /**
   * Adds a row to the table to edit the partial fields mask.
   * 
   * @param responseSchema Definition of the response object being described.
   * @param row Row index to begin adding rows to the parameter form table.
   */
  private void addFieldsRow(@Nullable Schema responseSchema, int row) {
    fieldsPlaceholder.clear();

    table.setText(row, 0, "fields");

    // Reset the fields textbox's value to empty and add it to the table (with
    // appropriate styling)
    fieldsTextBox.setText("");
    table.setWidget(row, 1, fieldsTextBox);
    
    // Start adding the next cell which will have the description of this param,
    // and potentially a link to open the fields editor.
    HTMLPanel panel = new HTMLPanel("");

    appState.getCurrentService().getParameters().get("fields").getDescription();
    panel.add(new InlineLabel(getFieldsDescription()));

    // If a response schema is provided, add a link to the fields editor and
    // tell the fields editor about this method's response schema.
    if (responseSchema != null) {
      Label openFieldsEditor = new Label("Open fields editor");
      openFieldsEditor.addStyleName(Resources.INSTANCE.style().clickable());
      openFieldsEditor.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          fieldsPopupPanel.show();
          fieldsPopupPanel.center();
        }
      });
      panel.add(openFieldsEditor);

      fieldsEditor = new FieldsEditor(appState, /* This is the root, no field name req'd */"");
      fieldsEditor.setProperties(responseSchema.getProperties());
      fieldsPlaceholder.add(fieldsEditor);
    }

    // Add the description (and maybe fields editor link) to the table.
    table.setWidget(row, 2, panel);

    cellFormatter.addStyleName(row, 0, EmbeddedResources.INSTANCE.style().parameterFormNameCell());
    cellFormatter.addStyleName(row, 2,
        EmbeddedResources.INSTANCE.style().parameterFormDescriptionCell());
  }
  
  private void addRequestBodyRow(int row) {
    table.setText(row, 0, "Request body");
    table.setWidget(row, 1, this.bodyDisclosure);
    table.setText(row, 2, "");
    
    cellFormatter.addStyleName(row, 0, EmbeddedResources.INSTANCE.style().parameterFormNameCell());
  }
  
  private void addExecuteRow(int row) {
    table.setWidget(row, 0, requiredDescription);
    table.setWidget(row, 1, this.submit);
    table.setText(row, 2, "");
    
    cellFormatter.addStyleName(row, 0, EmbeddedResources.INSTANCE.style().requiredParameter());
    cellFormatter.addStyleName(row, 0, EmbeddedResources.INSTANCE.style().parameterFormNameCell());
  }
  
  /**
   * Enables/disables the "Execute" button.
   */
  @Override
  public void setExecuting(boolean executing) {
    submit.setEnabled(!executing);
  }
}
