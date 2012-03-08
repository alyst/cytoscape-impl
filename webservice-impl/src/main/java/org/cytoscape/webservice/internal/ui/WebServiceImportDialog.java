package org.cytoscape.webservice.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import org.cytoscape.io.webservice.WebServiceClient;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.util.swing.internal.OpenBrowserImpl;
import org.cytoscape.work.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServiceImportDialog<T> extends JDialog {

	private static final long serialVersionUID = 4454012178961756787L;
	private static final Logger logger = LoggerFactory.getLogger(WebServiceImportDialog.class);

	// Default icon for about dialog
	private static final Icon DEF_ICON = new ImageIcon(
			WebServiceImportDialog.class.getResource("/images/stock_internet-32.png"));

//	private static final Icon NETWORK_IMPORT_ICON = new ImageIcon(
//			WebServiceImportDialog.class.getResource("/images/networkImportIcon.png"));
	private static final String NO_CLIENT = "No Service Client";

	private JButton cancelButton;
	private JComboBox datasourceComboBox;
	private JLabel datasourceLabel;
	private JTabbedPane mainTabbedPane;
	private JPanel propertyPanel;
	private JScrollPane propertyScrollPane;
	private JButton searchButton;
	private JScrollPane searchTermScrollPane;
	private JTextPane queryTextPane;
	private JButton aboutButton;
	private JPanel buttonPanel;
	private JPanel queryPanel;
	private JButton clearButton;
	private JPanel dataQueryPanel;
	private JPanel datasourcePanel;
	private JLabel titleIconLabel;
	private JPanel titlePanel;

	// Registered web service clients
	private Set<WebServiceClient> clients;

	// Client-Dependent GUI panels
	private Map<WebServiceClient, Container> serviceUIPanels = new HashMap<WebServiceClient, Container>();
	private int numClients;

	private final TaskManager<?, ?> taskManager;
	
	private final Class<T> type;
	
	private final OpenBrowser openBrowser;
	
	public WebServiceImportDialog(final Class<T> type, final String title, final TaskManager<?, ?> taskManager, final OpenBrowser openBrowser) {
		super();
		if (taskManager == null)
			throw new NullPointerException("TaskManager is null.");

		this.type = type;
		this.taskManager = taskManager;
		this.openBrowser = openBrowser;

		numClients = 0;
		setModal(false);
		this.clients = new HashSet<WebServiceClient>();

		initGUI();

		datasourceComboBox.addItem(NO_CLIENT);
		setComponentsEnabled(false);
		
		this.setTitle(title);
	}
	
	
	public void addClient(
			final WebServiceClient client, @SuppressWarnings("rawtypes") Map props) {
		
		if(!typeCheck(client))
			return;
		
		if(this.numClients == 0)
			this.datasourceComboBox.removeAllItems();
		
		datasourceComboBox.addItem(client);
		this.clients.add((WebServiceClient) client);
		numClients++;
		setComponentsEnabled(true);
		
		if (client instanceof WebServiceClient) {
			WebServiceClient service = (WebServiceClient) client;
			Container container = service.getQueryBuilderGUI();
			if (container != null) {
				serviceUIPanels.put((WebServiceClient) client, container);
			}
		}
		datasourceComboBox.setSelectedItem(client);
		datasourceComboBoxActionPerformed(null);
		logger.info("New network import client registered: " + client);
	}
	
	
	public void removeClient(
			final WebServiceClient client, @SuppressWarnings("rawtypes") Map props) {
		
		if(!typeCheck(client))
			return;
		
		datasourceComboBox.removeItem(client);
		this.clients.remove(client);
		serviceUIPanels.remove(client);
		numClients--;
		
		if(numClients == 0) {
			this.datasourceComboBox.removeAllItems();
			this.datasourceComboBox.addItem(NO_CLIENT);
			setComponentsEnabled(false);
		}
	}


	private boolean typeCheck(final WebServiceClient client) {
		final Class<?>[] interfaces = client.getClass().getInterfaces();
		boolean found = false;
		for(final Class<?> inf: interfaces) {
			if(inf.equals(type)) {
				found = true;
				break;
			}
		}
		return found;
	}
	
	
	private void initGUI() {

		initComponents();

		// If we have no data sources, show the install panel
		getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(queryPanel, BorderLayout.CENTER);
		this.pack();

		// Initialize GUI panel.
		datasourceComboBoxActionPerformed(null);
	}
	
	private void setComponentsEnabled(boolean enable) {
		datasourceComboBox.setEnabled(enable);
		this.searchButton.setEnabled(enable);
		this.aboutButton.setEnabled(enable);
		this.cancelButton.setEnabled(enable);
	}


	
	private void initComponents() {
		mainTabbedPane = new JTabbedPane();
		searchTermScrollPane = new JScrollPane();
		queryTextPane = new JTextPane();
		propertyPanel = new JPanel();

		queryTextPane.setFont(new java.awt.Font("SansSerif", 0, 12));
		queryTextPane.setText("Please enter search terms...");
		searchTermScrollPane.setViewportView(queryTextPane);

		mainTabbedPane.addTab("Query", searchTermScrollPane);

		GroupLayout propertyPanelLayout = new GroupLayout(propertyPanel);
		propertyPanel.setLayout(propertyPanelLayout);
		propertyPanelLayout.setHorizontalGroup(propertyPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGap(0, 408, Short.MAX_VALUE));
		propertyPanelLayout.setVerticalGroup(propertyPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGap(0, 303, Short.MAX_VALUE));

		propertyScrollPane = new JScrollPane();
		propertyScrollPane.setViewportView(propertyPanel);
		mainTabbedPane.addTab("Search Property", propertyScrollPane);

		titlePanel = new JPanel();
		titleIconLabel = new JLabel();
		datasourcePanel = new JPanel();
		datasourceLabel = new JLabel();
		datasourceComboBox = new JComboBox();
		datasourceComboBox.setRenderer(new ClientComboBoxCellRenderer());
		aboutButton = new JButton();
		buttonPanel = new JPanel();
		searchButton = new JButton();
		cancelButton = new JButton();
		clearButton = new JButton();
		dataQueryPanel = new JPanel();

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		titlePanel.setBackground(new java.awt.Color(0, 0, 0));

//		titleIconLabel.setIcon(NETWORK_IMPORT_ICON);

		GroupLayout titlePanelLayout = new GroupLayout(titlePanel);
		titlePanel.setLayout(titlePanelLayout);
		titlePanelLayout.setHorizontalGroup(titlePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(titleIconLabel, GroupLayout.PREFERRED_SIZE, 461, GroupLayout.PREFERRED_SIZE));
		titlePanelLayout.setVerticalGroup(titlePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(titleIconLabel));

		datasourceLabel.setFont(new java.awt.Font("SansSerif", 0, 12));
		datasourceLabel.setText("Data Source");

		datasourceComboBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				datasourceComboBoxActionPerformed(evt);
			}
		});

		aboutButton.setText("About");
		aboutButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
		aboutButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				aboutButtonActionPerformed(evt);
			}
		});

		GroupLayout datasourcePanelLayout = new GroupLayout(datasourcePanel);
		datasourcePanel.setLayout(datasourcePanelLayout);
		datasourcePanelLayout.setHorizontalGroup(datasourcePanelLayout.createParallelGroup(
				GroupLayout.Alignment.LEADING).addGroup(
				datasourcePanelLayout.createSequentialGroup().addContainerGap().addComponent(datasourceLabel)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(datasourceComboBox, 0, 301, Short.MAX_VALUE)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(aboutButton)
						.addContainerGap()));
		datasourcePanelLayout.setVerticalGroup(datasourcePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(
						datasourcePanelLayout
								.createSequentialGroup()
								.addContainerGap()
								.addGroup(
										datasourcePanelLayout
												.createParallelGroup(GroupLayout.Alignment.BASELINE)
												.addComponent(datasourceLabel)
												.addComponent(aboutButton)
												.addComponent(datasourceComboBox, GroupLayout.PREFERRED_SIZE,
														GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		buttonPanel.setBorder(BorderFactory.createEtchedBorder());

		searchButton.setText("Search");
		searchButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				searchButtonActionPerformed();
			}
		});

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});

		clearButton.setText("Clear");
		clearButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				clearButtonActionPerformed(evt);
			}
		});

		GroupLayout buttonPanelLayout = new GroupLayout(buttonPanel);
		buttonPanel.setLayout(buttonPanelLayout);
		buttonPanelLayout.setHorizontalGroup(buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(
						GroupLayout.Alignment.TRAILING,
						buttonPanelLayout.createSequentialGroup().addContainerGap().addComponent(clearButton)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 225, Short.MAX_VALUE)
								.addComponent(cancelButton).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(searchButton).addContainerGap()));
		buttonPanelLayout.setVerticalGroup(buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(
						GroupLayout.Alignment.TRAILING,
						buttonPanelLayout
								.createSequentialGroup()
								.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addGroup(
										buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
												.addComponent(searchButton).addComponent(cancelButton)
												.addComponent(clearButton)).addContainerGap()));

		GroupLayout dataQueryPanelLayout = new GroupLayout(dataQueryPanel);
		dataQueryPanel.setLayout(dataQueryPanelLayout);
		dataQueryPanelLayout.setHorizontalGroup(dataQueryPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGap(0, 461, Short.MAX_VALUE));
		dataQueryPanelLayout.setVerticalGroup(dataQueryPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGap(0, 247, Short.MAX_VALUE));

		queryPanel = new JPanel();
		GroupLayout layout = new GroupLayout(queryPanel);
		queryPanel.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(titlePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(datasourcePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(buttonPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(dataQueryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
				layout.createSequentialGroup()
						.addComponent(titlePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(datasourcePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(dataQueryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
								Short.MAX_VALUE)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)));

		dataQueryPanel.setLayout(new BorderLayout());
	}

	private void searchButtonActionPerformed() {
		final Object selected = datasourceComboBox.getSelectedItem();
		if (selected == null)
			return;

		WebServiceClient client = null;
		if (selected instanceof WebServiceClient) {
			client = (WebServiceClient) selected;
		} else {
			throw new IllegalStateException("Selected cleint is not a compatible client.");
		}

		// Set query. Just pass the text in the panel.
		client.setQuery(this.queryTextPane.getText());
		taskManager.execute(client);

	}

	/**
	 * Clear query text field.
	 */
	private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {
		// Just set empty string for the field.
		queryTextPane.setText("");
	}

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
		// Do nothing. Just hide this window.
		queryTextPane.setText("");
		dispose();
	}

	private void datasourceComboBoxActionPerformed(ActionEvent evt) {

		Object selected = datasourceComboBox.getSelectedItem();
		if (selected == null) {
			selected = datasourceComboBox.getItemAt(0);
			if (selected == null)
				return;
		}

		queryTextPane.setText("");

		if (selected instanceof WebServiceClient == false)
			return;

		final WebServiceClient client = (WebServiceClient) selected;

		// Update Panel
		dataQueryPanel.removeAll();

		final Container gui = serviceUIPanels.get(client);
		if (gui != null) {
			// This service has custom panel.
			dataQueryPanel.add(gui, BorderLayout.CENTER);
			// Hide button panel.
			buttonPanel.setVisible(false);
		} else {
			// Otherwise, use the default panel.
			dataQueryPanel.add(mainTabbedPane, BorderLayout.CENTER);
			buttonPanel.setVisible(true);
		}

		pack();
		repaint();
	}

	private void aboutButtonActionPerformed(ActionEvent evt) {

		final WebServiceClient wsc = (WebServiceClient) datasourceComboBox.getSelectedItem();

		final String clientName = wsc.getDisplayName();
		final String description = wsc.getDescription();

		Icon icon = null;
		if (icon == null)
			icon = DEF_ICON;
		
		final AboutDialog aboutDialog = new AboutDialog(this, Dialog.ModalityType.APPLICATION_MODAL, openBrowser);
		aboutDialog.showDialog("About " + clientName, icon, description);
	}

	private final class ClientComboBoxCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1494017058040636621L;

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value instanceof WebServiceClient) {
				String displayName = ((WebServiceClient) value).getDisplayName();
				this.setText(displayName);
			}

			return this;

		}
	}

}