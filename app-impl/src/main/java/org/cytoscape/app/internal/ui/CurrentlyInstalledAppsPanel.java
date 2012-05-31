package org.cytoscape.app.internal.ui;

import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import org.cytoscape.app.internal.event.AppsChangedEvent;
import org.cytoscape.app.internal.event.AppsChangedListener;
import org.cytoscape.app.internal.exception.AppInstallException;
import org.cytoscape.app.internal.exception.AppUninstallException;
import org.cytoscape.app.internal.manager.App;
import org.cytoscape.app.internal.manager.App.AppStatus;
import org.cytoscape.app.internal.manager.AppManager;

/**
 * This class represents the panel in the App Manager dialog's tab used for checking for currently installed apps.
 * Its UI setup code is generated by the Netbeans 7 GUI builder.
 */
public class CurrentlyInstalledAppsPanel extends javax.swing.JPanel {

	/** Long serial version identifier required by the Serializable class */
	private static final long serialVersionUID = 7096775814942183176L;
	
    private javax.swing.JScrollPane appsAvailableScrollPane;
    private javax.swing.JTable appsAvailableTable;
    private javax.swing.JLabel appsInstalledLabel;
    private javax.swing.JLabel descriptionLabel;
    private javax.swing.JScrollPane descriptionScrollPane;
    private javax.swing.JTextArea descriptionTextArea;
    private javax.swing.JButton disableSelectedButton;
    private javax.swing.JButton enableSelectedButton;;
	
    private AppManager appManager;
    private AppsChangedListener appListener;
    
    public CurrentlyInstalledAppsPanel(AppManager appManager) {
    	this.appManager = appManager;
    	
    	initComponents();
        
        setupAppListener();
        setupDescriptionListener();
        populateTable();
    }

    private void initComponents() {

        appsAvailableScrollPane = new javax.swing.JScrollPane();
        appsAvailableTable = new javax.swing.JTable();
        appsInstalledLabel = new javax.swing.JLabel();
        enableSelectedButton = new javax.swing.JButton();
        disableSelectedButton = new javax.swing.JButton();
        descriptionLabel = new javax.swing.JLabel();
        descriptionScrollPane = new javax.swing.JScrollPane();
        descriptionTextArea = new javax.swing.JTextArea();

        appsAvailableTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "App", "Name", "Author", "Version", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        appsAvailableScrollPane.setViewportView(appsAvailableTable);
        appsAvailableTable.getColumnModel().getColumn(1).setPreferredWidth(195);
        appsAvailableTable.removeColumn(appsAvailableTable.getColumn("App"));
        
        appsInstalledLabel.setText("0 Apps installed.");

        enableSelectedButton.setText("Reinstall");
        enableSelectedButton.setEnabled(false);
        enableSelectedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableSelectedButtonActionPerformed(evt);
            }
        });

        disableSelectedButton.setText("Uninstall");
        disableSelectedButton.setEnabled(false);
        disableSelectedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disableSelectedButtonActionPerformed(evt);
            }
        });

        descriptionLabel.setText("App Information:");

        descriptionTextArea.setEditable(false);
        descriptionTextArea.setFocusable(false);
        descriptionScrollPane.setViewportView(descriptionTextArea);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(appsAvailableScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 634, Short.MAX_VALUE)
                    .add(descriptionScrollPane)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(descriptionLabel)
                            .add(layout.createSequentialGroup()
                                .add(disableSelectedButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(enableSelectedButton))
                            .add(appsInstalledLabel))
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(appsInstalledLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(appsAvailableScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(descriptionLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(descriptionScrollPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 106, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(enableSelectedButton)
                    .add(disableSelectedButton))
                .addContainerGap())
        );
    }
        
        
    private void enableSelectedButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	// Obtain App objects corresponding to currently selected table entries
        Set<App> selectedApps = getSelectedApps();
        
        for (App app : selectedApps) {
        	// Only install apps that are not already installed
        	if (app.getStatus() != AppStatus.INSTALLED) {
        		try {
					appManager.installApp(app);
				} catch (AppInstallException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
        
        enableSelectedButton.setEnabled(false);
        disableSelectedButton.setEnabled(true);
    }

    private void disableSelectedButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	// Obtain App objects corresponding to currently selected table entries
    	Set<App> selectedApps = getSelectedApps();
        
        for (App app : selectedApps) {
        	// Only uninstall apps that are installed
        	if (app.getStatus() == AppStatus.INSTALLED) {
        		try {
					appManager.uninstallApp(app);
				} catch (AppUninstallException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
        
        disableSelectedButton.setEnabled(false);
        enableSelectedButton.setEnabled(true);
    }

    private void showTypeComboxBoxActionPerformed(java.awt.event.ActionEvent evt) {
    }
    
    /**
     * Obtain the set of {@link App} objects corresponding to currently selected entries in the table of apps
     * @return A set of {@link App} objects corresponding to selected apps in the table
     */
    private Set<App> getSelectedApps() {
        Set<App> selectedApps = new HashSet<App>();
    	int[] selectedRows = appsAvailableTable.getSelectedRows();
    	
        for (int index = 0; index < selectedRows.length; index++) {
        	
        	App app = (App) appsAvailableTable.getModel().getValueAt(
        			appsAvailableTable.convertRowIndexToModel(selectedRows[index]), 0);
        	
        	selectedApps.add(app);
        }
    	
    	return selectedApps;
    }
    
    /**
     * Registers a listener to the {@link AppManager} to listen for app change events in order to rebuild the table
     */
    private void setupAppListener() {
    	appListener = new AppsChangedListener() {

			@Override
			public void appsChanged(AppsChangedEvent event) {
				
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						Set<App> selectedApps = getSelectedApps();
						
						// Clear table
						DefaultTableModel tableModel = (DefaultTableModel) appsAvailableTable.getModel();
						for (int rowIndex = tableModel.getRowCount() - 1; rowIndex >= 0; rowIndex--) {
							tableModel.removeRow(rowIndex);
						}
						
						// Re-populate table
						populateTable();
						
						// Update labels
						updateLabels();

						// Re-select previously selected apps
						for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
							if (selectedApps.contains(tableModel.getValueAt(rowIndex, 0))) {
								appsAvailableTable.addRowSelectionInterval(rowIndex, rowIndex);
							}
						}
					}
				});
				
			}
    	};
    	
    	appManager.addAppListener(appListener);
    }
    
    /**
     * Populate the table of apps by obtaining the list of currently available apps from the AppManager object.
     */
    private void populateTable() {
    	DefaultTableModel tableModel = (DefaultTableModel) appsAvailableTable.getModel();
		
    	for (App app : appManager.getApps()) {	
    		tableModel.addRow(new Object[]{
					app,
					app.getAppFile() != null ? app.getAppName() : app.getAppName() + " (File moved)",
					app.getAuthors(),
					app.getVersion(),
					app.getStatus()
			});
    	}
    	
    	updateLabels();
    }
    
    /**
     * Update the labels that display the number of currently installed and available apps.
     */
    private void updateLabels() {
    	int installedCount = 0;
    	
    	for (App app : appManager.getApps()) {
    		// Simple apps require a restart to be completely uninstalled; count them as installed
    		// until the restart
    		if (app.getStatus() != AppStatus.UNINSTALLED) {
    			installedCount++;
    		}
    	}
    	
    	appsInstalledLabel.setText(installedCount + " Apps installed.");
    }
    
    /**
     * Setup and register a listener to the table to listen for selection changed events in order to update the
     * app description box
     */
    private void setupDescriptionListener() {
    	appsAvailableTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateDescriptionBox();
			}
		});
    }
    
    private void updateDescriptionBox() {
    	Set<App> selectedApps = getSelectedApps();
    	int numSelected = selectedApps.size();
    	
    	// If no apps are selected, clear the description box
    	if (numSelected == 0) {
    		descriptionTextArea.setText("");
    		
    		// Disable buttons
    		enableSelectedButton.setEnabled(false);
    		disableSelectedButton.setEnabled(false);
    		
    	// If a single app is selected, show its app description
    	} else if (numSelected == 1){
    		App selectedApp = selectedApps.iterator().next();
    		
    		String text = "App description not found.";
    		descriptionTextArea.setText(text);
    		
    		// Enable/disable the appropriate button
    		if (selectedApp.getStatus() == AppStatus.INSTALLED) {
    			enableSelectedButton.setEnabled(false);
    			disableSelectedButton.setEnabled(true);
    		} else {
    			enableSelectedButton.setEnabled(true);
    			disableSelectedButton.setEnabled(false);
    		}
    	} else {
    		descriptionTextArea.setText(numSelected + " apps selected.");
    		
    		// Enable/disable the appropriate buttons
    		boolean allInstalled = true;
    		boolean allUninstalled = true;
    		
    		for (App selectedApp : selectedApps) {
    			if (selectedApp.getStatus() == AppStatus.INSTALLED) {
    				allUninstalled = false;
    			}
    			
    			if (selectedApp.getStatus() != AppStatus.INSTALLED) {
    				allInstalled = false;
    			}
    		}
    		
    		if (allInstalled) {
    			enableSelectedButton.setEnabled(false);
    			disableSelectedButton.setEnabled(true);
    		} else if (allUninstalled) {
    			enableSelectedButton.setEnabled(true);
    			disableSelectedButton.setEnabled(false);
    		} else {
    			// If some of the selected apps are installed and some are uninstalled,
    			// enable both buttons
    			enableSelectedButton.setEnabled(true);
    			disableSelectedButton.setEnabled(true);
    		}
    	}
    }
}