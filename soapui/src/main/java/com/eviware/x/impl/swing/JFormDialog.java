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

import com.eviware.soapui.support.HelpActionMarker;
import com.eviware.soapui.support.StringUtils;
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
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.util.concurrent.CountDownLatch;

public class JFormDialog extends SwingXFormDialog {

    private JDialog dialog;
    private SwingXFormImpl form;
    private JButtonBar buttons;
    private boolean resized;
    private ActionList actions;
    private JPanel panel;

    public JFormDialog(String name, SwingXFormImpl form, ActionList actions, String description, ImageIcon icon) {
        this(name, form, actions, description, icon, true);
    }
    
    public JFormDialog(String name, SwingXFormImpl form, ActionList actions, String description, ImageIcon icon, boolean modal) {
        dialog = new JDialog(UISupport.getMainFrame(), name, modal);
        dialog.setName(name);
        this.actions = actions;
        buttons = UISupport.initDialogActions(actions, dialog);
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        JPanel panel = new JPanel(new BorderLayout());
        this.form = (SwingXFormImpl) form;
        panel.add((this.form.getPanel()), BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (description != null || icon != null) {
            dialog.getContentPane().add(UISupport.buildDescription(name, description, icon), BorderLayout.NORTH);
        }

        dialog.getContentPane().add(panel, BorderLayout.CENTER);

        buttons
                .setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.WHITE)), BorderFactory.createEmptyBorder(3, 5,
                        3, 5)));

        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        this.panel = panel;
    }

    @Override
    public void setValues(StringToStringMap values) {
        form.setValues(values);
    }

    public JDialog getDialog() {
        return dialog;
    }

    @Override
    public void setSize(int i, int j) {
        dialog.setSize(i, j);
        resized = true;
    }

    @Override
    public ActionList getActionsList() {
        return actions;
    }

    @Override
    public XForm[] getForms() {
        return new XForm[]{form};
    }

    @Override
    public StringToStringMap getValues() {
        StringToStringMap result = new StringToStringMap();
        result.putAll(form.getValues());

        return result;
    }

    @Override
    public void setOptions(String field, Object[] options) {
        form.setOptions(field, options);
    }

    @Override
    public void setVisible(boolean visible) {
        if (!resized && visible) {
            dialog.pack();
            if (dialog.getHeight() < 210) {
                dialog.setSize(new Dimension(dialog.getWidth(), 210));
            }

            if (dialog.getWidth() < 320) {
                dialog.setSize(new Dimension(320, dialog.getHeight()));
            }
        }

        if (visible) {
            UISupport.centerDialog(dialog);
        }
        dialog.setVisible(visible);

        if (startSignal != null) {
            startSignal.countDown();
        }
    }

    @Override
    public void addAction(Action action) {
        DefaultActionList actions = new DefaultActionList();
        actions.addAction(action);
        buttons.addActions(actions);
    }

    @Override
    public boolean validate() {
        XFormField[] formFields = form.getFormFields();
        for (XFormField formField : formFields) {
            ValidationMessage[] messages = formField.validate();
            if (messages != null && messages.length > 0) {
                ((AbstractSwingXFormField<?>) messages[0].getFormField()).getComponent().requestFocus();
                UISupport.showErrorMessage(messages[0].getMessage());
                return false;
            }
        }

        return true;
    }

    @Override
    public void setFormFieldProperty(String name, Object value) {
        form.setFormFieldProperty(name, value);
    }

    @Override
    public String getValue(String field) {
        return form.getComponentValue(field);
    }

    @Override
    public void setValue(String field, String value) {
        form.setComponentValue(field, value);
    }

    @Override
    public int getValueIndex(String name) {
        Object[] options = form.getOptions(name);
        if (options == null) {
            return -1;
        }

        return StringUtils.toStringList(options).indexOf(form.getComponentValue(name));
    }

    private CountDownLatch startSignal;

    @Override
    public boolean show() {
        setReturnValue(XFormDialog.CANCEL_OPTION);
        show(new StringToStringMap());
        if (dialog.getModalityType() == ModalityType.MODELESS) {
            startSignal = new CountDownLatch(1);
            try {
                startSignal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            startSignal = null;
        }

        return getReturnValue() == XFormDialog.OK_OPTION;
    }

    @Override
    public XFormField getFormField(String name) {
        return form.getFormField(name);
    }

    @Override
    public void setWidth(int i) {
        dialog.setPreferredSize(new Dimension(i, (int) dialog.getPreferredSize().getHeight()));
    }

    @Override
    public void release() {
        dialog.dispose();
    }

    /*
     * Is there any other way to do this?
     */
    public void setHelpUrl(String helpUrl) {
        for (int cnt = 0; cnt < actions.getActionCount(); cnt++) {
            if (actions.getActionAt(cnt) instanceof HelpActionMarker) {
                ((SwingXFormDialogBuilder.HelpAction) actions.getActionAt(cnt)).setUrl(helpUrl);
                break;
            }
        }
    }

    public JPanel getPanel() {
        return panel;
    }

}
