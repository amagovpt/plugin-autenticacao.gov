/*
 * The MIT License
 *
 * Copyright 2014, 2015, 2016 Rui Martinho (rmartinho@gmail.com), António Braz (antoniocbraz@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.poreid.dialogs.selectcard;

import java.awt.Dialog;
import org.poreid.dialogs.DialogEventListener;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.DefaultListModel;
import org.poreid.common.Util;
import org.poreid.config.POReIDConfig;

/**
 *
 * @author POReID
 * @param <T> Classe que implemente a interface POReIDSmartCard
 */
public class SelectCardDialog<T> extends javax.swing.JDialog {
    private final DefaultListModel<T> model = new DefaultListModel<>();
    private final DialogEventListener<T> listener;
    private final ResourceBundle bundle;
    
    /**
     * Creates new form SelectCardDialog     
     * @param list Lista de classes que implementam a interface POReIDSmartCard
     * @param locale Linguagem utilizada
     * @param listener Objecto a notificar
     */
    public SelectCardDialog(final List<T> list, Locale locale, DialogEventListener<T> listener) {
        super((Dialog)null, true);
        
        setIconImages(Util.getIconImages());
        
        this.listener = listener; 
        for (T t : list) {
            model.addElement(t);
        }
        bundle = POReIDConfig.getBundle(SelectCardDialog.class.getSimpleName(),locale);
        initComponents();
        
        this.setTitle(bundle.getString("dialog.title"));
        this.getAccessibleContext().setAccessibleDescription(bundle.getString("dialog.description"));   
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form
     * Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        availableCards = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        selectBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setBackground(new java.awt.Color(228, 228, 228));
        setIconImage(null);
        setModal(true);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(228, 228, 228));

        availableCards.setModel(model);
        availableCards.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        availableCards.setToolTipText(bundle.getString("list.tooltip"));
        availableCards.getAccessibleContext().setAccessibleDescription(bundle.getString("list.description"));
        availableCards.setCellRenderer(new SelectCardListCellRenderer());
        availableCards.setVisibleRowCount(5);
        availableCards.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                availableCardsValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(availableCards);

        jPanel1.setBackground(new java.awt.Color(228, 228, 228));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 50, 2));

        selectBtn.setFont(selectBtn.getFont().deriveFont(selectBtn.getFont().getSize()-1f));
        selectBtn.setText(bundle.getString("select.button"));
        selectBtn.getAccessibleContext().setAccessibleDescription(bundle.getString("select.button.description"));
        selectBtn.setAutoscrolls(true);
        selectBtn.setEnabled(false);
        selectBtn.setMaximumSize(new java.awt.Dimension(136, 30));
        selectBtn.setMinimumSize(new java.awt.Dimension(136, 30));
        selectBtn.setPreferredSize(new java.awt.Dimension(136, 30));
        selectBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectBtnActionPerformed(evt);
            }
        });
        jPanel1.add(selectBtn);

        cancelBtn.setFont(cancelBtn.getFont().deriveFont(cancelBtn.getFont().getSize()-1f));
        cancelBtn.setText(bundle.getString("cancel.button"));
        cancelBtn.getAccessibleContext().setAccessibleDescription(bundle.getString("cancel.button.description"));
        cancelBtn.setMaximumSize(new java.awt.Dimension(136, 30));
        cancelBtn.setMinimumSize(new java.awt.Dimension(136, 30));
        cancelBtn.setPreferredSize(new java.awt.Dimension(136, 30));
        cancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBtnActionPerformed(evt);
            }
        });
        jPanel1.add(cancelBtn);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 413, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 413, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    
    private void availableCardsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_availableCardsValueChanged
        if (!evt.getValueIsAdjusting() && null != availableCards.getSelectedValue()) {
            selectBtn.setEnabled(true);
        } else {
            selectBtn.setEnabled(false);
        }
    }//GEN-LAST:event_availableCardsValueChanged

    @SuppressWarnings({"unchecked", "varargs"})
    private void selectBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectBtnActionPerformed
        listener.onContinue(availableCards.getSelectedValue());
        dispose();
    }//GEN-LAST:event_selectBtnActionPerformed

    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBtnActionPerformed
        listener.onCancel();
        setVisible(false);
        dispose();
    }//GEN-LAST:event_cancelBtnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        listener.onDiagloclosed();
        setVisible(false);
        dispose();
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<T> availableCards;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton selectBtn;
    // End of variables declaration//GEN-END:variables
}