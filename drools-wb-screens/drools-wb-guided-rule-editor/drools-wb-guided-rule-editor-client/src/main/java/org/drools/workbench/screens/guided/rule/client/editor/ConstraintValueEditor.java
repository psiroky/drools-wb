/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.guided.rule.client.editor;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.drools.workbench.models.datamodel.oracle.DataType;
import org.drools.workbench.models.datamodel.oracle.DropDownData;
import org.drools.workbench.models.datamodel.oracle.OperatorsOracle;
import org.drools.workbench.models.datamodel.rule.BaseSingleFieldConstraint;
import org.drools.workbench.models.datamodel.rule.CompositeFieldConstraint;
import org.drools.workbench.models.datamodel.rule.ConnectiveConstraint;
import org.drools.workbench.models.datamodel.rule.ExpressionFormLine;
import org.drools.workbench.models.datamodel.rule.FieldConstraint;
import org.drools.workbench.models.datamodel.rule.RuleModel;
import org.drools.workbench.models.datamodel.rule.SingleFieldConstraint;
import org.drools.workbench.models.datamodel.rule.SingleFieldConstraintEBLeftSide;
import org.drools.workbench.screens.guided.rule.client.editor.events.TemplateVariablesChangedEvent;
import org.drools.workbench.screens.guided.rule.client.editor.util.ConstraintValueEditorHelper;
import org.drools.workbench.screens.guided.rule.client.resources.GuidedRuleEditorResources;
import org.drools.workbench.screens.guided.rule.client.resources.images.GuidedRuleEditorImages508;
import org.drools.workbench.screens.guided.rule.client.widget.EnumDropDown;
import org.drools.workbench.screens.guided.rule.client.widget.ExpressionBuilder;
import org.guvnor.common.services.workingset.client.WorkingSetManager;
import org.guvnor.common.services.workingset.client.factconstraints.customform.CustomFormConfiguration;
import org.jboss.errai.ioc.client.container.IOC;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracle;
import org.kie.workbench.common.widgets.client.datamodel.CEPOracle;
import org.kie.workbench.common.widgets.client.widget.PopupDatePicker;
import org.kie.workbench.common.widgets.client.widget.TextBoxFactory;
import org.uberfire.client.callbacks.Callback;
import org.uberfire.ext.widgets.common.client.common.DropDownValueChanged;
import org.uberfire.ext.widgets.common.client.common.InfoPopup;
import org.uberfire.ext.widgets.common.client.common.SmallLabel;
import org.uberfire.ext.widgets.common.client.common.popups.FormStylePopup;

/**
 * This is an editor for constraint values. How this behaves depends on the
 * constraint value type. When the constraint value has no type, it will allow
 * the user to choose the first time.
 */
public class ConstraintValueEditor
        extends Composite {

    private final ConstraintValueEditorHelper helper;
    private WorkingSetManager workingSetManager = null;

    private String factType;
    private CompositeFieldConstraint constraintList;
    private String fieldName;
    private String fieldType;

    private final AsyncPackageDataModelOracle oracle;
    private final BaseSingleFieldConstraint constraint;
    private final Panel panel = new SimplePanel();
    private final RuleModel model;
    private final RuleModeller modeller;
    private final EventBus eventBus;

    private DropDownData dropDownData;
    private boolean readOnly;

    private Command onValueChangeCommand;
    private Command onTemplateValueChangeCommand;
    private boolean isDropDownDataEnum;
    private Widget constraintWidget = null;
    private AddConstraintButton addConstraintButton = new AddConstraintButton(
            new ClickHandler() {
                public void onClick( ClickEvent event ) {
                    showTypeChoice( (Widget) event.getSource(),
                                    constraint );
                }
            } );

    public ConstraintValueEditor( BaseSingleFieldConstraint con,
                                  CompositeFieldConstraint constraintList,
                                  RuleModeller modeller,
                                  EventBus eventBus,
                                  boolean readOnly ) {
        this.constraint = con;
        this.constraintList = constraintList;
        this.oracle = modeller.getDataModelOracle();
        this.model = modeller.getModel();

        this.modeller = modeller;
        this.eventBus = eventBus;
        this.readOnly = readOnly;

        setUpConstraint();

        helper = new ConstraintValueEditorHelper( model, oracle, factType, fieldName, constraint, fieldType, dropDownData );

        refreshEditor();
        initWidget( panel );
    }

    private void setUpConstraint() {
        if ( constraint instanceof SingleFieldConstraintEBLeftSide ) {
            setUpSingleFieldConstraintEBLeftSide( (SingleFieldConstraintEBLeftSide) constraint );

        } else if ( constraint instanceof ConnectiveConstraint ) {
            setUpConnectiveConstraint( (ConnectiveConstraint) constraint );

        } else if ( constraint instanceof SingleFieldConstraint ) {
            setUpSingleFieldConstraint( (SingleFieldConstraint) constraint );
        }
    }

    private void setUpSingleFieldConstraint( SingleFieldConstraint sfc ) {
        this.factType = sfc.getFactType();
        this.fieldName = sfc.getFieldName();
        this.fieldType = oracle.getFieldType( factType,
                                              fieldName );
    }

    private void setUpConnectiveConstraint( ConnectiveConstraint cc ) {
        this.factType = cc.getFactType();
        this.fieldName = cc.getFieldName();
        this.fieldType = cc.getFieldType();
    }

    private void setUpSingleFieldConstraintEBLeftSide( SingleFieldConstraintEBLeftSide sfexp ) {
        this.factType = sfexp.getExpressionLeftSide().getPreviousClassType();
        if ( this.factType == null ) {
            this.factType = sfexp.getExpressionLeftSide().getClassType();
        }
        this.fieldName = sfexp.getExpressionLeftSide().getFieldName();
        this.fieldType = sfexp.getExpressionLeftSide().getGenericType();
    }

    public BaseSingleFieldConstraint getConstraint() {
        return constraint;
    }

    public void refreshEditor() {
        panel.clear();
        constraintWidget = null;

        //Expressions' fieldName and hence fieldType can change without creating a new ConstraintValueEditor. 
        //SingleFieldConstraints and their ConnectiveConstraints cannot have the fieldName or fieldType changed 
        //without first deleting and re-creating.
        if ( this.constraint instanceof SingleFieldConstraintEBLeftSide ) {
            setUpSingleFieldConstraintEBLeftSide( (SingleFieldConstraintEBLeftSide) this.constraint );
        }

        //Initialise drop-down data
        getDropDownData();

        //Show an editor for the constraint value type
        if ( constraint.getConstraintValueType() == SingleFieldConstraint.TYPE_UNDEFINED ) {
            addAddConstraintButton();
        } else {
            addConstraintWidget();
        }

        panel.add( constraintWidget );
    }

    private void addConstraintWidget() {
        switch ( constraint.getConstraintValueType() ) {
            case SingleFieldConstraint.TYPE_LITERAL:
            case SingleFieldConstraint.TYPE_ENUM:
                constraintWidget = wrap( literalEditor() );
                break;
            case SingleFieldConstraint.TYPE_RET_VALUE:
                constraintWidget = wrap( returnValueEditor() );
                break;
            case SingleFieldConstraint.TYPE_EXPR_BUILDER_VALUE:
                constraintWidget = wrap( expressionEditor() );
                break;
            case SingleFieldConstraint.TYPE_VARIABLE:
                constraintWidget = wrap( variableEditor() );
                break;
            case BaseSingleFieldConstraint.TYPE_TEMPLATE:
                constraintWidget = wrap( templateKeyEditor() );
                break;
            default:
                break;
        }
    }

    private void addAddConstraintButton() {
        addConstraintButton.setEnabled( !this.readOnly );
        constraintWidget = addConstraintButton;
    }

    public void showError() {
        addConstraintButton.showError();
    }

    public void hideError() {
        addConstraintButton.hideError();
    }

    //Wrap a Constraint Value Editor with an icon to remove the type 
    private Widget wrap( Widget widget ) {
        if ( this.readOnly ) {
            return widget;
        }
        HorizontalPanel wrapper = new HorizontalPanel();
        Image clear = GuidedRuleEditorImages508.INSTANCE.DeleteItemSmall();
        clear.setTitle( GuidedRuleEditorResources.CONSTANTS.RemoveConstraintValueDefinition() );
        clear.addClickHandler( new ClickHandler() {

            public void onClick( ClickEvent event ) {
                //Reset Constraint's value and value type
                if ( Window.confirm( GuidedRuleEditorResources.CONSTANTS.RemoveConstraintValueDefinitionQuestion() ) ) {
                    constraint.setConstraintValueType( BaseSingleFieldConstraint.TYPE_UNDEFINED );
                    constraint.setValue( null );
                    constraint.clearParameters();
                    constraint.setExpressionValue( new ExpressionFormLine() );
                    doTypeChosen();
                }
            }

        } );

        wrapper.add( widget );
        if ( !this.readOnly ) {
            wrapper.add( clear );
            wrapper.setCellVerticalAlignment( clear,
                                              HasVerticalAlignment.ALIGN_MIDDLE );
        }
        return wrapper;
    }

    private String assertValue() {
        if ( constraint.getValue() == null ) {
            return "";
        }
        return constraint.getValue();
    }

    private Widget literalEditor() {

        //Custom screen
        if ( this.constraint instanceof SingleFieldConstraint ) {
            final SingleFieldConstraint con = (SingleFieldConstraint) this.constraint;
            CustomFormConfiguration customFormConfiguration = getWorkingSetManager().getCustomFormConfiguration( modeller.getPath(),
                                                                                                                 factType,
                                                                                                                 fieldName );
            if ( customFormConfiguration != null ) {
                Button btnCustom = new Button( con.getValue(),
                                               new ClickHandler() {

                                                   public void onClick( ClickEvent event ) {
                                                       showTypeChoice( (Widget) event.getSource(),
                                                                       constraint );
                                                   }
                                               } );
                btnCustom.setEnabled( !this.readOnly );
                return btnCustom;
            }
        }

        //Label if read-only
        if ( this.readOnly ) {
            return new SmallLabel( assertValue() );
        }

        //Enumeration (these support multi-select for "in" and "not in", so check before comma separated lists) 
        if ( this.dropDownData != null ) {
            final String operator = constraint.getOperator();
            final boolean multipleSelect = OperatorsOracle.operatorRequiresList( operator );
            EnumDropDown enumDropDown = new EnumDropDown( constraint.getValue(),
                                                          new DropDownValueChanged() {

                                                              public void valueChanged( String newText,
                                                                                        String newValue ) {

                                                                  //Prevent recursion once value change has been applied
                                                                  if ( !newValue.equals( constraint.getValue() ) ) {
                                                                      constraint.setValue(newValue);
                                                                      executeOnValueChangeCommand();
                                                                  }
                                                              }
                                                          },
                                                          dropDownData,
                                                          multipleSelect,
                                                          modeller.getPath() );
            return enumDropDown;
        }

        //Comma separated value list (this will become a dedicated Widget but for now a TextBox suffices)
        String operator = null;
        if ( this.constraint instanceof SingleFieldConstraint ) {
            SingleFieldConstraint sfc = (SingleFieldConstraint) this.constraint;
            operator = sfc.getOperator();
        }
        if ( OperatorsOracle.operatorRequiresList( operator ) ) {
            final TextBox box = TextBoxFactory.getTextBox( DataType.TYPE_STRING );
            box.setStyleName( "constraint-value-Editor" );
            box.addValueChangeHandler( new ValueChangeHandler<String>() {

                public void onValueChange( final ValueChangeEvent<String> event ) {
                    constraint.setValue(event.getValue());
                    executeOnValueChangeCommand();
                }
            } );

            box.setText( assertValue() );
            attachDisplayLengthHandler( box );
            return box;
        }

        //Date picker
        boolean isCEPOperator = CEPOracle.isCEPOperator( ( this.constraint ).getOperator() );
        if ( DataType.TYPE_DATE.equals( this.fieldType ) || ( DataType.TYPE_THIS.equals( this.fieldName ) && isCEPOperator ) ) {
            if ( this.readOnly ) {
                return new SmallLabel( constraint.getValue() );
            }

            final PopupDatePicker dp = new PopupDatePicker( false );

            // Wire up update handler
            dp.addValueChangeHandler( new ValueChangeHandler<Date>() {

                public void onValueChange( final ValueChangeEvent<Date> event ) {
                    constraint.setValue( PopupDatePicker.convertToString( event ) );
                    executeOnValueChangeCommand();
                }

            } );

            dp.setValue( assertValue() );
            return dp;
        }

        //Default editor for all other literals
        final TextBox box = TextBoxFactory.getTextBox( fieldType );
        box.setStyleName( "constraint-value-Editor" );
        box.addValueChangeHandler( new ValueChangeHandler<String>() {

            public void onValueChange( final ValueChangeEvent<String> event ) {
                constraint.setValue(event.getValue());
                executeOnValueChangeCommand();
            }
        } );

        box.setText( assertValue() );
        attachDisplayLengthHandler( box );
        return box;
    }

    private Widget variableEditor() {

        if ( this.readOnly ) {
            return new SmallLabel( this.constraint.getValue() );
        }

        final ListBox box = new ListBox();
        box.addItem( GuidedRuleEditorResources.CONSTANTS.Choose() );

        List<String> bindingsInScope = this.model.getBoundVariablesInScope( this.constraint );

        for ( String var : bindingsInScope ) {
            final String binding = var;
            helper.isApplicableBindingsInScope( var,
                                                new Callback<Boolean>() {
                                                    @Override
                                                    public void callback( final Boolean result ) {
                                                        if ( Boolean.TRUE.equals( result ) ) {
                                                            box.addItem( binding );
                                                            if ( ConstraintValueEditor.this.constraint.getValue() != null && ConstraintValueEditor.this.constraint.getValue().equals( binding ) ) {
                                                                box.setSelectedIndex( box.getItemCount() - 1 );
                                                            }
                                                        }
                                                    }
                                                } );
        }

        box.addChangeHandler( new ChangeHandler() {

            public void onChange( ChangeEvent event ) {
                executeOnValueChangeCommand();
                int selectedIndex = box.getSelectedIndex();
                if ( selectedIndex > 0 ) {
                    constraint.setValue( box.getItemText( selectedIndex ) );
                } else {
                    constraint.setValue( null );
                }
            }
        } );

        return box;
    }

    /**
     * An editor for the retval "formula" (expression).
     */
    private Widget returnValueEditor() {
        TextBox box = new BoundTextBox( constraint );

        if ( this.readOnly ) {
            return new SmallLabel( box.getText() );
        }

        String msg = GuidedRuleEditorResources.CONSTANTS.FormulaEvaluateToAValue();
        Image img = new Image( GuidedRuleEditorResources.INSTANCE.images().functionAssets() );
        img.setTitle( msg );
        box.setTitle( msg );
        box.addValueChangeHandler( new ValueChangeHandler<String>() {

            public void onValueChange( final ValueChangeEvent event ) {
                executeOnValueChangeCommand();
            }
        } );

        Widget ed = widgets( img,
                             box );
        return ed;
    }

    private Widget expressionEditor() {
        ExpressionBuilder builder = null;
        builder = new ExpressionBuilder( this.modeller,
                                         this.eventBus,
                                         this.constraint.getExpressionValue(),
                                         this.readOnly );

        builder.addOnModifiedCommand( new Command() {

            public void execute() {
                executeOnValueChangeCommand();
            }
        } );
        Widget ed = widgets( new HTML( "&nbsp;" ),
                             builder );
        return ed;
    }

    /**
     * An editor for Template Keys
     */
    private Widget templateKeyEditor() {
        if ( this.readOnly ) {
            return new SmallLabel( assertValue() );
        }

        TemplateKeyTextBox box = new TemplateKeyTextBox();
        box.setStyleName( "constraint-value-Editor" );
        box.addValueChangeHandler( new ValueChangeHandler<String>() {

            @Override
            public void onValueChange( final ValueChangeEvent<String> event ) {
                constraint.setValue( event.getValue() );
                executeOnTemplateValueChangeCommand();
            }

        } );
        //FireEvents as the box could assume a default value
        box.setValue( assertValue(),
                      true );
        attachDisplayLengthHandler( box );
        return box;
    }

    //Only display the number of characters that have been entered
    private void attachDisplayLengthHandler( final TextBox box ) {
        int length = box.getText().length();
        box.setVisibleLength( length > 0 ? length : 1 );
        box.addKeyUpHandler( new KeyUpHandler() {

            public void onKeyUp( KeyUpEvent event ) {
                int length = box.getText().length();
                box.setVisibleLength( length > 0 ? length : 1 );
            }
        } );
    }

    /**
     * Show a list of possibilities for the value type.
     */
    private void showTypeChoice( Widget w,
                                 final BaseSingleFieldConstraint con ) {

        CustomFormConfiguration customFormConfiguration = getWorkingSetManager().getCustomFormConfiguration( modeller.getPath(),
                                                                                                             factType,
                                                                                                             fieldName );

        if ( customFormConfiguration != null ) {
            if ( !( con instanceof SingleFieldConstraint ) ) {
                Window.alert( "Unexpected constraint type!" );
                return;
            }
            final CustomFormPopUp customFormPopUp = new CustomFormPopUp( GuidedRuleEditorImages508.INSTANCE.Wizard(),
                                                                         GuidedRuleEditorResources.CONSTANTS.FieldValue(),
                                                                         customFormConfiguration );

            final SingleFieldConstraint sfc = (SingleFieldConstraint) con;

            customFormPopUp.addOkButtonHandler( new ClickHandler() {

                public void onClick( ClickEvent event ) {
                    sfc.setConstraintValueType( SingleFieldConstraint.TYPE_LITERAL );
                    sfc.setId( customFormPopUp.getFormId() );
                    sfc.setValue( customFormPopUp.getFormValue() );
                    doTypeChosen( customFormPopUp );
                }
            } );

            customFormPopUp.show( sfc.getId(),
                                  sfc.getValue() );
            return;
        }

        final FormStylePopup form = new FormStylePopup( GuidedRuleEditorImages508.INSTANCE.Wizard(),
                                                        GuidedRuleEditorResources.CONSTANTS.FieldValue() );

        Button lit = new Button( GuidedRuleEditorResources.CONSTANTS.LiteralValue() );
        lit.addClickHandler( new ClickHandler() {
            public void onClick( ClickEvent event ) {
                con.setConstraintValueType( isDropDownDataEnum && dropDownData != null ? SingleFieldConstraint.TYPE_ENUM : SingleFieldConstraint.TYPE_LITERAL );
                doTypeChosen( form );
            }
        } );

        boolean showLiteralSelector = true;
        boolean showFormulaSelector = !OperatorsOracle.operatorRequiresList( con.getOperator() );
        boolean showVariableSelector = !OperatorsOracle.operatorRequiresList( con.getOperator() );
        boolean showExpressionSelector = !OperatorsOracle.operatorRequiresList( con.getOperator() );

        if ( con instanceof SingleFieldConstraint ) {
            SingleFieldConstraint sfc = (SingleFieldConstraint) con;
            String fieldName = sfc.getFieldName();
            if ( fieldName.equals( DataType.TYPE_THIS ) ) {
                showLiteralSelector = CEPOracle.isCEPOperator( sfc.getOperator() );
                showFormulaSelector = showFormulaSelector && showLiteralSelector;
            }
        } else if ( con instanceof ConnectiveConstraint ) {
            ConnectiveConstraint cc = (ConnectiveConstraint) con;
            String fieldName = cc.getFieldName();
            if ( fieldName.equals( DataType.TYPE_THIS ) ) {
                showLiteralSelector = CEPOracle.isCEPOperator( cc.getOperator() );
                showFormulaSelector = showFormulaSelector && showLiteralSelector;
            }
        }

        //Literal value selector
        if ( showLiteralSelector ) {
            form.addAttribute( GuidedRuleEditorResources.CONSTANTS.LiteralValue() + ":",
                               widgets( lit,
                                        new InfoPopup( GuidedRuleEditorResources.CONSTANTS.LiteralValue(),
                                                       GuidedRuleEditorResources.CONSTANTS.LiteralValTip() ) ) );
        }

        //Template key selector
        if ( modeller.isTemplate() ) {
            String templateKeyLabel = GuidedRuleEditorResources.CONSTANTS.TemplateKey();
            Button templateKeyButton = new Button( templateKeyLabel );
            templateKeyButton.addClickHandler( new ClickHandler() {

                public void onClick( ClickEvent event ) {
                    con.setConstraintValueType( BaseSingleFieldConstraint.TYPE_TEMPLATE );
                    doTypeChosen( form );
                }
            } );

            form.addAttribute( templateKeyLabel + ":",
                               widgets( templateKeyButton,
                                        new InfoPopup( templateKeyLabel,
                                                       GuidedRuleEditorResources.CONSTANTS.LiteralValTip() ) ) );
        }

        //Divider, if we have any advanced options
        if ( showVariableSelector || showFormulaSelector || showExpressionSelector ) {
            form.addRow( new HTML( "<hr/>" ) );
            form.addRow( new SmallLabel( GuidedRuleEditorResources.CONSTANTS.AdvancedOptions() ) );
        }

        //Show variables selector, if there are any variables in scope
        if ( showVariableSelector ) {
            List<String> bindingsInScope = this.model.getBoundVariablesInScope( this.constraint );
            if ( bindingsInScope.size() > 0 || DataType.TYPE_COLLECTION.equals( this.fieldType ) ) {

                final Button bindingButton = new Button( GuidedRuleEditorResources.CONSTANTS.BoundVariable() );

                //This Set is used as a 1flag to know whether the button has been added; due to use of callbacks
                final Set<Button> bindingButtonContainer = new HashSet<Button>();

                for ( String var : bindingsInScope ) {
                    helper.isApplicableBindingsInScope( var,
                                                        new Callback<Boolean>() {
                                                            @Override
                                                            public void callback( final Boolean result ) {
                                                                if ( Boolean.TRUE.equals( result ) ) {
                                                                    if ( !bindingButtonContainer.contains( bindingButton ) ) {
                                                                        bindingButtonContainer.add( bindingButton );
                                                                        bindingButton.addClickHandler( new ClickHandler() {

                                                                            public void onClick( ClickEvent event ) {
                                                                                con.setConstraintValueType( SingleFieldConstraint.TYPE_VARIABLE );
                                                                                doTypeChosen( form );
                                                                            }
                                                                        } );
                                                                        form.addAttribute( GuidedRuleEditorResources.CONSTANTS.AVariable(),
                                                                                           widgets( bindingButton,
                                                                                                    new InfoPopup( GuidedRuleEditorResources.CONSTANTS.ABoundVariable(),
                                                                                                                   GuidedRuleEditorResources.CONSTANTS.BoundVariableTip() ) ) );
                                                                    }
                                                                }
                                                            }
                                                        } );

                }
            }
        }

        //Formula selector
        if ( showFormulaSelector ) {
            Button formula = new Button( GuidedRuleEditorResources.CONSTANTS.NewFormula() );
            formula.addClickHandler( new ClickHandler() {

                public void onClick( ClickEvent event ) {
                    con.setConstraintValueType( SingleFieldConstraint.TYPE_RET_VALUE );
                    doTypeChosen( form );
                }
            } );

            form.addAttribute( GuidedRuleEditorResources.CONSTANTS.AFormula() + ":",
                               widgets( formula,
                                        new InfoPopup( GuidedRuleEditorResources.CONSTANTS.AFormula(),
                                                       GuidedRuleEditorResources.CONSTANTS.FormulaExpressionTip() ) ) );
        }

        //Expression selector
        if ( showExpressionSelector ) {
            Button expression = new Button( GuidedRuleEditorResources.CONSTANTS.ExpressionEditor() );
            expression.addClickHandler( new ClickHandler() {

                public void onClick( ClickEvent event ) {
                    con.setConstraintValueType( SingleFieldConstraint.TYPE_EXPR_BUILDER_VALUE );
                    doTypeChosen( form );
                }
            } );

            form.addAttribute( GuidedRuleEditorResources.CONSTANTS.ExpressionEditor() + ":",
                               widgets( expression,
                                        new InfoPopup( GuidedRuleEditorResources.CONSTANTS.ExpressionEditor(),
                                                       GuidedRuleEditorResources.CONSTANTS.ExpressionEditor() ) ) );
        }

        form.show();
    }

    private void doTypeChosen() {
        executeOnValueChangeCommand();
        executeOnTemplateVariablesChange();
        refreshEditor();
    }

    private void doTypeChosen( final FormStylePopup form ) {
        doTypeChosen();
        form.hide();
    }

    private Panel widgets( IsWidget left,
                           IsWidget right ) {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setVerticalAlignment( HasVerticalAlignment.ALIGN_MIDDLE );
        panel.add( left );
        panel.add( right );
        panel.setWidth( "100%" );
        return panel;
    }

    public void setOnValueChangeCommand( Command onValueChangeCommand ) {
        this.onValueChangeCommand = onValueChangeCommand;
    }

    private void executeOnValueChangeCommand() {
        if ( this.onValueChangeCommand != null ) {
            this.onValueChangeCommand.execute();
        }
    }

    public void setOnTemplateValueChangeCommand( Command onTemplateValueChangeCommand ) {
        this.onTemplateValueChangeCommand = onTemplateValueChangeCommand;
    }

    private void executeOnTemplateValueChangeCommand() {
        if ( this.onTemplateValueChangeCommand != null ) {
            this.onTemplateValueChangeCommand.execute();
        }
    }

    private DropDownData getDropDownData() {
        //Set applicable flags and reference data depending upon type
        if ( DataType.TYPE_BOOLEAN.equals( this.fieldType ) ) {
            this.isDropDownDataEnum = false;
            this.dropDownData = DropDownData.create( new String[]{ "true", "false" } );
        } else {
            this.isDropDownDataEnum = true;

            final Map<String, String> currentValueMap = new HashMap<String, String>();

            if ( constraintList != null && constraintList.getConstraints() != null ) {
                for ( FieldConstraint con : constraintList.getConstraints() ) {
                    if ( con instanceof SingleFieldConstraint ) {
                        SingleFieldConstraint sfc = (SingleFieldConstraint) con;
                        String fieldName = sfc.getFieldName();
                        currentValueMap.put( fieldName,
                                             sfc.getValue() );
                    }
                }
            }

            this.dropDownData = oracle.getEnums( this.factType,
                                                 fieldName,
                                                 currentValueMap );
        }
        return dropDownData;
    }

    //Signal (potential) change in Template variables
    private void executeOnTemplateVariablesChange() {
        TemplateVariablesChangedEvent tvce = new TemplateVariablesChangedEvent( model );
        eventBus.fireEventFromSource( tvce, model );
    }

    private WorkingSetManager getWorkingSetManager() {
        if ( workingSetManager == null ) {
            workingSetManager = IOC.getBeanManager().lookupBean( WorkingSetManager.class ).getInstance();
        }
        return workingSetManager;
    }

}
