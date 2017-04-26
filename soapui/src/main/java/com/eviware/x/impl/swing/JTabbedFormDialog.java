/*
 * SoapUI, Copyright (C) 2004-2016 SmartBear Software 
 *
 * Licensed under the EUPL, Version 1.1 or - as soon as they will be approved by the European Commission - subsequent 
 * versions of the EUPL (the "Licence"); 
 * You may not use this work except in compliance with the Licence. 
 * You may obtain a copy of the Licence at: 
 * 
 * http://ec.europa.eu/idabc/eupl 
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is 
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the Licence for the specific language governing permissions and limitations 
 * under the Licence. 
 */
package com.eviware.x.impl.swing;

import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.swing.ActionList;
import com.eviware.soapui.support.action.swing.DefaultActionList;
import com.eviware.soapui.support.components.JButtonBar;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.x.form.ValidationMessage;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JTabbedFormDialog extends SwingXFormDialog {

    private JDialog dialog;
    private List<SwingXFormImpl> forms = new ArrayList();
    private JTabbedPane tabs;
    private JButtonBar buttons;

    public JTabbedFormDialog(String name, XForm[] forms, ActionList actions, String description, ImageIcon icon) {
        this(name, forms, actions, description, icon, true);
    }
    
    public JTabbedFormDialog(String name, XForm[] forms, ActionList actions, String description, ImageIcon icon, boolean modal) {
        dialog = new JDialog(UISupport.getMainFrame(), name, modal);
        tabs = new JTabbedPane();
        for (XForm form : forms) {
            SwingXFormImpl swingFormImpl = ((SwingXFormImpl) form);
            this.forms.add(swingFormImpl);

            JPanel panel = swingFormImpl.getPanel();
            panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

            tabs.addTab(form.getName(), panel);
        }

        buttons = UISupport.initDialogActions(actions, dialog);

        if (description != null || icon != null) {
            dialog.getContentPane().add(UISupport.buildDescription(name, description, icon), BorderLayout.NORTH);
        }

        dialog.getContentPane().add(UISupport.createTabPanel(tabs, false), BorderLayout.CENTER);

        buttons.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        Dimension size = dialog.getSize();
        if (size.getHeight() < 300) {
            dialog.setSize(new Dimension((int) size.getWidth(), 300));
        }
    }

    @Override
    public void setSize(int i, int j) {
        dialog.setSize(i, j);
    }

    @Override
    public XForm[] getForms() {
        List<XForm> result = new ArrayList();
        for (XForm form : forms) {
            result.add(form);
        }
        return result.toArray(new XForm[result.size()]);
    }

    @Override
    public void setValues(StringToStringMap values) {
        for (XForm form : forms) {
            form.setValues(values);
        }
    }

    @Override
    public void setOptions(String field, Object[] options) {
        for (XForm form : forms) {
            form.setOptions(field, options);
        }
    }

    @Override
    public XFormField getFormField(String name) {
        for (XForm form : forms) {
            XFormField formField = form.getFormField(name);
            if (formField != null) {
                return formField;
            }
        }

        return null;
    }

    @Override
    public void addAction(Action action) {
        DefaultActionList actions = new DefaultActionList();
        actions.addAction(action);
        buttons.addActions(actions);
    }

    @Override
    public StringToStringMap getValues() {
        StringToStringMap result = new StringToStringMap();

        for (XForm form : forms) {
            result.putAll(form.getValues());
        }

        return result;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            tabs.setSelectedIndex(0);
        }

        UISupport.centerDialog(dialog);

        dialog.setVisible(visible);
    }

    @Override
    public boolean validate() {
        for (int i = 0; i < forms.size(); i++) {
            XFormField[] formFields = forms.get(i).getFormFields();
            for (XFormField formField : formFields) {
                ValidationMessage[] messages = formField.validate();
                if (messages != null && messages.length > 0) {
                    tabs.setSelectedIndex(i);
                    ((AbstractSwingXFormField<?>) messages[0].getFormField()).getComponent().requestFocus();
                    UISupport.showErrorMessage(messages[0].getMessage());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void setFormFieldProperty(String name, Object value) {
        for (XForm form : forms) {
            form.setFormFieldProperty(name, value);
        }
    }

    @Override
    public String getValue(String field) {
        for (XForm form : forms) {
            if (form.getComponent(field) != null) {
                return form.getComponent(field).getValue();
            }
        }

        return null;
    }

    @Override
    public void setValue(String field, String value) {
        for (XForm form : forms) {
            if (form.getComponent(field) != null) {
                form.getComponent(field).setValue(value);
            }
        }
    }

    @Override
    public int getValueIndex(String name) {
        for (SwingXFormImpl form : forms) {
            if (form.getComponent(name) != null) {
                Object[] options = form.getOptions(name);
                if (options == null) {
                    return -1;
                }

                return Arrays.asList(options).indexOf(form.getComponentValue(name));
            }
        }

        return -1;
    }

    @Override
    public boolean show() {
        setReturnValue(XFormDialog.CANCEL_OPTION);
        show(new StringToStringMap());
        return getReturnValue() == XFormDialog.OK_OPTION;
    }

    @Override
    public void setWidth(int i) {
        dialog.setPreferredSize(new Dimension(i, (int) dialog.getPreferredSize().getHeight()));
    }

    @Override
    public void release() {
        dialog.dispose();
        tabs.removeAll();

        dialog = null;
        tabs = null;
    }

    public JComponent getTabs() {
        return tabs;
    }

    public void setResizable(boolean b) {
        dialog.setResizable(b);
    }

}
