/*
The MIT License (MIT)

Copyright (c) Terry Evans Vaughn "VCSCode"

All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.blueseer.inv;

import bsmf.MainFrame;
import com.blueseer.utl.OVData;
import static bsmf.MainFrame.reinitpanels;
import java.awt.Point;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import static bsmf.MainFrame.con;
import static bsmf.MainFrame.db;
import static bsmf.MainFrame.driver;
import static bsmf.MainFrame.pass;
import static bsmf.MainFrame.url;
import static bsmf.MainFrame.user;
import java.awt.Color;
import java.math.RoundingMode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author vaughnte
 */
public class CostRollUpPanel extends javax.swing.JPanel {

    javax.swing.table.DefaultTableModel costmodel = new javax.swing.table.DefaultTableModel(new Object[][]{},
            new String[]{
               "Part", "Op", "WC", "Mch", "Desc", "Dept", "Labor", "Burden", "Material", "Overhead", "Service", "SetupRate($)", "LaborRate($)", "BurdenRate($)", "setupHours", "runHours", "Lotsize", "RollCost", "StdCost"
            });
    javax.swing.table.DefaultTableModel subcostmodel = new javax.swing.table.DefaultTableModel(new Object[][]{},
            new String[]{
               "Part", "Op", "WC", "Mch", "Desc", "Dept", "Labor", "Burden", "Material", "Overhead", "Service", "SetupRate($)", "LaborRate($)", "BurdenRate($)", "setupHours", "runHours", "Lotsize", "RollCost", "StdCost"
            });
    javax.swing.table.DefaultTableModel toplowmodel = new javax.swing.table.DefaultTableModel(new Object[][]{},
            new String[]{
               "Type", "RollLower", "StdLower", "RollUpper", "StdUpper" , "RollTotal", "StdTotal"
            });
    
    String thissite = "";
    
    
    /**
     * Creates new form CostRollUpPanel
     */
    public CostRollUpPanel() {
        initComponents();
    }

    
    public void settoplowmodeltable() {
        ArrayList<Double> costs = new ArrayList<Double>();
        costs = OVData.getItemCostStdElements(tbitem.getText(), "standard", thissite);
       
        calcCost cur = new calcCost();
        DecimalFormat df = new DecimalFormat("#.0000"); 
        ArrayList<Double> costcur = new ArrayList<Double>();
        costcur = cur.getTotalCostElements(tbitem.getText());
        
        double stdmtllow = costs.get(0);
        double stdlbrlow = costs.get(1);
        double stdbdnlow = costs.get(2);
        double stdovhlow = costs.get(3);
        double stdoutlow = costs.get(4);
        double stdmtltop = costs.get(5);
        double stdlbrtop = costs.get(6);
        double stdbdntop = costs.get(7);
        double stdovhtop = costs.get(8);
        double stdouttop = costs.get(9);
        
        double curmtllow = costcur.get(0);
        double curlbrlow = costcur.get(1);
        double curbdnlow = costcur.get(2);
        double curovhlow = costcur.get(3);
        double curoutlow = costcur.get(4);
        double curmtltop = costcur.get(5);
        double curlbrtop = costcur.get(6);
        double curbdntop = costcur.get(7);
        double curovhtop = costcur.get(8);
        double curouttop = costcur.get(9);
        
        double curmtl = curmtllow + curmtltop;
        double curlbr = curlbrlow + curlbrtop;
        double curbdn = curbdnlow + curbdntop;
        double curovh = curovhlow + curovhtop;
        double curout = curoutlow + curouttop;
        
        double stdmtl = stdmtllow + stdmtltop;
        double stdlbr = stdlbrlow + stdlbrtop;
        double stdbdn = stdbdnlow + stdbdntop;
        double stdovh = stdovhlow + stdovhtop;
        double stdout = stdoutlow + stdouttop;
        
        double totcur = curmtl + curlbr + curbdn + curovh + curout;
        double totstd = stdmtl + stdlbr + stdbdn + stdovh + stdout;
        
        toplowmodel.addRow(new Object[] {"Material", curmtllow, df.format(stdmtllow), curmtltop, df.format(stdmtltop), curmtl, stdmtl });
        toplowmodel.addRow(new Object[] {"Labor", curlbrlow, df.format(stdlbrlow), curlbrtop, df.format(stdlbrtop), curlbr, stdlbr });
        toplowmodel.addRow(new Object[] {"Burden", curbdnlow, df.format(stdbdnlow), curbdntop, df.format(stdbdntop), curbdn, stdbdn });
        toplowmodel.addRow(new Object[] {"Overhead", curovhlow, df.format(stdovhlow), curovhtop, df.format(stdovhtop), curovh, stdovh });
        toplowmodel.addRow(new Object[] {"Outside", curoutlow, df.format(stdoutlow), curouttop, df.format(stdouttop), curout, stdout });
        toplowmodel.addRow(new Object[] {"Total", "", "" , "" , "" , totcur, totstd });
        
        
        toplowtable.setModel(toplowmodel);
        
        labelcost.setText(String.valueOf(totcur));
        labelstandard.setText(String.valueOf(totstd));
    }
    
    public void setcostmodeltable() {
         ArrayList<String> costs = new ArrayList<String>();
        costs = OVData.rollCost(tbitem.getText());
        double total = 0.0;
        double totmtl = 0.0;
        double totlbr = 0.0;
        double totbdn = 0.0;
        double totovh = 0.0;
        double totout = 0.0;
        String stdtotalcost = "";
        DecimalFormat df = new DecimalFormat("#.00000"); 
        df.setRoundingMode(RoundingMode.HALF_UP);
          for (String cost : costs) {
              String[] elements = cost.split(",", -1);
              total = total + Double.valueOf(elements[17]);
              totmtl = totmtl + Double.valueOf(elements[6]);
              totlbr = totlbr + Double.valueOf(elements[7]);
              totbdn = totbdn + Double.valueOf(elements[8]);
              totovh = totovh + Double.valueOf(elements[9]);
              totout = totout + Double.valueOf(elements[10]);
              stdtotalcost = elements[19];
                  costmodel.addRow(new Object[]{
                      elements[0],
                      elements[1],
                      elements[2],
                      elements[3],
                      elements[4],
                      elements[5],
                      df.format(Double.valueOf(elements[6].toString())),
                      df.format(Double.valueOf(elements[7].toString())),
                      df.format(Double.valueOf(elements[8].toString())),
                      df.format(Double.valueOf(elements[9].toString())),
                      df.format(Double.valueOf(elements[10].toString())),
                      elements[11],
                      elements[12],
                      elements[13],
                      elements[14],
                      elements[15],
                      elements[16],
                      elements[17],
                      elements[18],
                      elements[19]
                      });
          } 
          
          costmodel.addRow(new Object[]{
                      "total",
                     "",
                      "",
                      "",
                      "",
                      "",
                      df.format(totmtl),
                      df.format(totlbr),
                      df.format(totbdn),
                      df.format(totovh),
                      df.format(totout),
                      "",
                      "",
                      "",
                      "",
                      "",
                      "",
                      "",
                      "",
                      ""
                      });
                costtable.setModel(costmodel);
                
    }
    
     public void setsubcostmodeltable(String part) {
         
         subcostmodel.setNumRows(0);
         ArrayList<String> costs = new ArrayList<String>();
        costs = OVData.rollCost(part);
        Double total = 0.0;
        String stdtotalcost = "";
          for (String cost : costs) {
              String[] elements = cost.split(",", -1);
              total = total + Double.valueOf(elements[17]);
              stdtotalcost = elements[19];
                  subcostmodel.addRow(new Object[]{
                      elements[0],
                      elements[1],
                      elements[2],
                      elements[3],
                      elements[4],
                      elements[5],
                      elements[6],
                      elements[7],
                      elements[8],
                      elements[9],
                      elements[10],
                      elements[11],
                      elements[12],
                      elements[13],
                      elements[14],
                      elements[15],
                      elements[16],
                      elements[17],
                      elements[18],
                      elements[19]
                      });
          } 
                subcosttable.setModel(subcostmodel);
    }
    
    public void getCompInfo(String parent, String component) {
         try {
            Class.forName(bsmf.MainFrame.driver).newInstance();
            bsmf.MainFrame.con = DriverManager.getConnection(bsmf.MainFrame.url + bsmf.MainFrame.db, bsmf.MainFrame.user, bsmf.MainFrame.pass);
            try {
                Statement st = bsmf.MainFrame.con.createStatement();
                ResultSet res = null;

                int i = 0;

            //    javax.swing.table.DefaultTableModel mymodel = new javax.swing.table.DefaultTableModel(new Object[][]{},
          //          new String[]{"Site", "Loc", "Qty", "Date"});
           //     mymodel.setRowCount(0);
                
                            
                // ReportPanel.TableReport.getColumn("CallID").setCellRenderer(new ButtonRenderer());
                //          ReportPanel.TableReport.getColumn("CallID").setCellEditor(
                    //       new ButtonEditor(new JCheckBox()));

               res = st.executeQuery("SELECT ps_child, it_desc, ps_op, ps_qty_per, itc_total  " +
                        " FROM  pbm_mstr inner join item_mstr on it_item = ps_child  " +
                       " left outer join item_cost on itc_item = it_item and itc_set = 'standard' " +
                       " where ps_parent = " + "'" + parent.toString() + "'" + 
                       " AND ps_child = " + "'" + component.toString() + "'" +
                        " ;");

                while (res.next()) {
                    i++;
                    //mymodel.addRow(new Object[]{
                     //           res.getString("in_site")
                     //  });
                     tbdesc.setText(res.getString("it_desc"));
                     tbqtyper.setText(res.getString("ps_qty_per"));
                     tbstdcost.setText(res.getString("itc_total"));
                     tbcomp.setText(res.getString("ps_child"));
                     tbop.setText(res.getString("ps_op"));
                }
                
            } catch (SQLException s) {
                MainFrame.bslog(s);
                bsmf.MainFrame.show("unable to select pbm_mstr and/or item_mstr");
            }
            bsmf.MainFrame.con.close();
        } catch (Exception e) {
            MainFrame.bslog(e);
        }
    }
    
     public void bind_tree(String parentpart) {
      //  jTree1.setModel(null);
       
      //  DefaultMutableTreeNode mynode = get_nodes(parentpart);
        DefaultMutableTreeNode mynode = OVData.get_nodes_without_op(parentpart);
      // DefaultMutableTreeNode mynode = OVData.get_op_nodes_experimental(parentpart);
       
        DefaultTreeModel model = (DefaultTreeModel)jTree1.getModel();
        
        model.setRoot(mynode);
        jTree1.setVisible(true);
        
    }
     
     public void establishParent(String item) {
          boolean validItem =  OVData.isValidItem(item);
          String desc = OVData.getItemDesc(item);
          String type = OVData.getItemCode(item);
             if (validItem) {
              tbitem.setEditable(false);
              tbitem.setForeground(Color.blue);
                   settoplowmodeltable();
                   setcostmodeltable();
                   bind_tree(item);
             lblitem.setText("type: " + type + " / " + desc);
             enableAll();
             } else {
               tbitem.setEditable(true);
               tbitem.setForeground(Color.black); 
               lblitem.setText("invalid item");
               disableAll();
             }
    }
    
     
    public void disableAll() {
        btroll.setEnabled(false);
        btclear.setEnabled(false);
        
    } 
    
    public void enableAll() {
         btroll.setEnabled(true);
        btclear.setEnabled(true);
    }
     
    public void clearAll() {
         DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
       jTree1.setModel(new DefaultTreeModel(root));
       tbitem.setText("");
       tbitem.setEditable(true);
       tbitem.setForeground(Color.black); 
        costmodel.setNumRows(0);
        subcostmodel.setNumRows(0);
        toplowmodel.setNumRows(0);
        labelcost.setText("0");
         labelstandard.setText("0");
        tbdesc.setText("");
         tbqtyper.setText("");
         tbstdcost.setText("");
         tbcomp.setText("");
         thissite = OVData.getDefaultSite();
    }
     
    public void initvars(String[] arg) {
       
        clearAll();
        disableAll();
      
         
         
         if (arg != null && arg.length > 0) {
           tbitem.setText(arg[0]);
           establishParent(arg[0]);
          
       }
         
         
       
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        tbitem = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        labelcost = new javax.swing.JLabel();
        labelstandard = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        tbop = new javax.swing.JTextField();
        tbqtyper = new javax.swing.JTextField();
        tbcomp = new javax.swing.JTextField();
        tbstdcost = new javax.swing.JTextField();
        tbdesc = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        subcosttable = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        costtable = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        toplowtable = new javax.swing.JTable();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        btroll = new javax.swing.JButton();
        btbrowse = new javax.swing.JButton();
        lblitem = new javax.swing.JLabel();
        btclear = new javax.swing.JButton();

        setBackground(new java.awt.Color(0, 102, 204));

        jLabel1.setText("Part");

        tbitem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tbitemActionPerformed(evt);
            }
        });

        jLabel3.setText("Roll Cost:");

        jLabel5.setText("Std Cost:");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Component Attributes"));

        jLabel2.setText("Component");

        jLabel4.setText("Description");

        jLabel6.setText("Operation");

        jLabel7.setText("Standard Cost");

        jLabel8.setText("Qty Per");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(tbdesc)
                    .addComponent(tbqtyper, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tbop, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tbstdcost, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tbcomp, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(42, 42, 42))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbcomp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbdesc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbqtyper, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tbstdcost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Sub Assembly Routing Cost"));

        subcosttable.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        subcosttable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane4.setViewportView(subcosttable);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 1052, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Parent Routing Cost Structure"));

        costtable.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        costtable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(costtable);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 152, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Parent BOM Cost Structure"));

        toplowtable.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        toplowtable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Type", "CurLowLevel", "StdLowLevel", "CurTopLevel", "StdTopLevel"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(toplowtable);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 370, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Parent Tree Structure"));

        jTree1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("JTree");
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree1ValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(jTree1);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(128, 128, 128))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 331, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        btroll.setText("Roll");
        btroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btrollActionPerformed(evt);
            }
        });

        btbrowse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/lookup.png"))); // NOI18N
        btbrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btbrowseActionPerformed(evt);
            }
        });

        btclear.setText("clear");
        btclear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btclearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tbitem, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btbrowse, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btclear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblitem, javax.swing.GroupLayout.PREFERRED_SIZE, 391, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btroll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelstandard, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelcost, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(tbitem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(labelstandard, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(labelcost, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btroll)
                    .addComponent(jLabel5)
                    .addComponent(btbrowse)
                    .addComponent(lblitem, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btclear))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        add(jPanel3);
    }// </editor-fold>//GEN-END:initComponents

    private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree1ValueChanged
        String comp = "";
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)jTree1.getLastSelectedPathComponent(); 
        DefaultMutableTreeNode thisnode = new DefaultMutableTreeNode();
        DefaultMutableTreeNode grandnode = new DefaultMutableTreeNode();
        if (selectedNode != null) {
            comp = selectedNode.getUserObject().toString();
            
            if (! selectedNode.isRoot()) {
                // lets see if op is in between parent and child...i.e. we may be looking for grandparent
                thisnode = (DefaultMutableTreeNode) selectedNode.getParent();
               // if (thisnode != null) {
              //  grandnode = (DefaultMutableTreeNode) thisnode.getParent();
              //  }
                if (thisnode != null) {
                   // bsmf.MainFrame.show(grandnode.toString() + "/" + comp);
                    getCompInfo(thisnode.toString(), comp);
                }
            }
            if (! selectedNode.isRoot() && ! selectedNode.isLeaf()) {
            setsubcostmodeltable(comp);
            }
            if ( selectedNode.isRoot() || selectedNode.isLeaf()) {
            subcostmodel.setNumRows(0);
            }
        }
        
        
        
    }//GEN-LAST:event_jTree1ValueChanged

    private void btrollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btrollActionPerformed
       try {

            Class.forName(driver).newInstance();
            con = DriverManager.getConnection(url + db, user, pass);
            try {
                Statement st = con.createStatement();
                ResultSet res = null;
                boolean proceed = true;
                
                int i = 0;
                String perms = "";
                double itrcost = 0.00;
                String routing = OVData.getItemRouting(tbitem.getText());
                ArrayList<String> ops = OVData.getOperationsByPart(tbitem.getText());
                // lets do item_cost first 
                res = st.executeQuery("SELECT itc_item FROM  item_cost where itc_item = " + "'" + tbitem.getText() + "'" + ";");
                    while (res.next()) {
                        i++;
                    }

                    if (i > 0) {
                        st.executeUpdate("update item_cost set "
                                + "itc_mtl_low = " + "'" + toplowtable.getValueAt(0, 1) + "'" + ","
                                + "itc_mtl_top = " + "'" + toplowtable.getValueAt(0, 3) + "'" + ","
                                + "itc_lbr_low = " + "'" + toplowtable.getValueAt(1, 1) + "'" + ","
                                + "itc_lbr_top = " + "'" + toplowtable.getValueAt(1, 3) + "'" + ","
                                + "itc_bdn_low = " + "'" + toplowtable.getValueAt(2, 1) + "'" + ","
                                + "itc_bdn_top = " + "'" + toplowtable.getValueAt(2, 3) + "'" + ","
                                + "itc_ovh_low = " + "'" + toplowtable.getValueAt(3, 1) + "'" + ","
                                + "itc_ovh_top = " + "'" + toplowtable.getValueAt(3, 3) + "'" + ","
                                + "itc_out_low = " + "'" + toplowtable.getValueAt(4, 1) + "'" + ","
                                + "itc_out_top = " + "'" + toplowtable.getValueAt(4, 3) + "'" + ","
                                + "itc_total = " + "'" + toplowtable.getValueAt(5, 5) + "'" 
                                + " where itc_item = " + "'" + tbitem.getText() + "'"
                                + " AND itc_set = 'standard' "
                                + " AND itc_site = " + "'" + thissite + "'"
                                + ";");
                       
                    } else {
                        bsmf.MainFrame.show("No item_cost record found");
                        proceed = false;
                    }

                    
                    // ok now lets do itemr_cost ...routing based costing
                   if (proceed) { 
                    i = -1;
                    for (String op : ops) {
                    // bsmf.MainFrame.show(op);
                    i++;
                    // delete original itemr_cost records for this item, op, routing, standard
                    st.executeUpdate(" delete FROM  itemr_cost where itr_item = " + "'" + tbitem.getText() + "'" 
                                         +  " AND itr_op = " + "'" + op + "'"
                                         + " AND itr_set = 'standard' "
                                         + " AND itr_site = " + "'" + thissite + "'"
                                         + " AND itr_routing = " + "'" + routing + "'" + ";");
                        
                         
                            itrcost = Double.valueOf(costtable.getValueAt(i,8).toString()) + 
                                   Double.valueOf(costtable.getValueAt(i,6).toString()) +
                                 Double.valueOf(costtable.getValueAt(i,7).toString()) +
                                 Double.valueOf(costtable.getValueAt(i,9).toString()) +
                                 Double.valueOf(costtable.getValueAt(i,10).toString()) ;
                                
                         
                            st.executeUpdate("insert into itemr_cost (itr_item, itr_site, itr_set, itr_routing, itr_op, " +
                                 " itr_total, itr_mtl_top, itr_lbr_top, itr_bdn_top, itr_ovh_top, itr_out_top, " +
                                 " itr_mtl_low, itr_lbr_low, itr_bdn_low, itr_ovh_low, itr_out_low ) values ( "
                                + "'" + tbitem.getText() + "'" + ","
                                + "'" + thissite + "'" + ","
                                + "'" + "standard" + "'" + ","
                                + "'" + routing + "'" + ","
                                + "'" + op + "'" + ","
                                + "'" + itrcost + "'" + "," 
                                + "'" + costtable.getValueAt(i, 8) + "'" + ","   
                                + "'" + costtable.getValueAt(i, 6) + "'" + ","
                                + "'" + costtable.getValueAt(i, 7) + "'" + ","
                                + "'" + costtable.getValueAt(i, 9) + "'" + ","
                                + "'" + costtable.getValueAt(i, 10) + "'" + ","
                                + "'" + "0" + "'" + ","
                                + "'" + "0" + "'" + ","
                                + "'" + "0" + "'" + ","
                                + "'" + "0" + "'" + ","
                                + "'" + "0" + "'" + " ) ;");
                       
                        
                  
                    } // for ops
                    bsmf.MainFrame.show("Cost Roll Complete");
                    initvars(new String[]{tbitem.getText()});
                   }
            } // if proceed
            catch (SQLException s) {
                bsmf.MainFrame.show("problem rolling cost at sql level");
                MainFrame.bslog(s);
            }
            con.close();
        } catch (Exception e) {
            MainFrame.bslog(e);
        }  
    }//GEN-LAST:event_btrollActionPerformed

    private void btbrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btbrowseActionPerformed
        reinitpanels("BrowseUtil", true, new String[]{"costmaint","it_item"});
    }//GEN-LAST:event_btbrowseActionPerformed

    private void tbitemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tbitemActionPerformed
       establishParent(tbitem.getText());
    }//GEN-LAST:event_tbitemActionPerformed

    private void btclearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btclearActionPerformed
        initvars(null);
    }//GEN-LAST:event_btclearActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btbrowse;
    private javax.swing.JButton btclear;
    private javax.swing.JButton btroll;
    private javax.swing.JTable costtable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTree jTree1;
    private javax.swing.JLabel labelcost;
    private javax.swing.JLabel labelstandard;
    private javax.swing.JLabel lblitem;
    private javax.swing.JTable subcosttable;
    private javax.swing.JTextField tbcomp;
    private javax.swing.JTextField tbdesc;
    private javax.swing.JTextField tbitem;
    private javax.swing.JTextField tbop;
    private javax.swing.JTextField tbqtyper;
    private javax.swing.JTextField tbstdcost;
    private javax.swing.JTable toplowtable;
    // End of variables declaration//GEN-END:variables
}
