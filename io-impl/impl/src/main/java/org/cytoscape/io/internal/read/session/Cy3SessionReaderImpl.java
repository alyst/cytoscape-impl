/*
 Copyright (c) 2006, 2011, The Cytoscape Consortium (www.cytoscape.org)

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications.  In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage.  See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/
package org.cytoscape.io.internal.read.session;


import static org.cytoscape.io.internal.util.session.SessionUtil.BOOKMARKS_FILE;
import static org.cytoscape.io.internal.util.session.SessionUtil.CYSESSION;
import static org.cytoscape.io.internal.util.session.SessionUtil.CYTABLE_METADATA_FILE;
import static org.cytoscape.io.internal.util.session.SessionUtil.CY_PROPS;
import static org.cytoscape.io.internal.util.session.SessionUtil.NETWORKS_FOLDER;
import static org.cytoscape.io.internal.util.session.SessionUtil.NETWORK_VIEWS_FOLDER;
import static org.cytoscape.io.internal.util.session.SessionUtil.PLUGINS_FOLDER;
import static org.cytoscape.io.internal.util.session.SessionUtil.TABLE_EXT;
import static org.cytoscape.io.internal.util.session.SessionUtil.VERSION_EXT;
import static org.cytoscape.io.internal.util.session.SessionUtil.VIZMAP_XML;
import static org.cytoscape.io.internal.util.session.SessionUtil.XGMML_EXT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cytoscape.io.internal.read.datatable.CSVCyReaderFactory;
import org.cytoscape.io.internal.read.session.CyTableMetadataImpl.CyTableMetadataBuilder;
import org.cytoscape.io.internal.read.xgmml.XGMMLNetworkReader;
import org.cytoscape.io.internal.util.session.SessionUtil;
import org.cytoscape.io.internal.util.session.VirtualColumnSerializer;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.io.read.CyPropertyReader;
import org.cytoscape.io.read.CyPropertyReaderManager;
import org.cytoscape.io.read.CyTableReader;
import org.cytoscape.io.read.VizmapReader;
import org.cytoscape.io.read.VizmapReaderManager;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableEntry;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyTableMetadata;
import org.cytoscape.property.bookmark.Bookmarks;
import org.cytoscape.property.session.Cysession;
import org.cytoscape.view.model.CyNetworkView;

/**
 * Session reader implementation that handles the Cytoscape 3 session format.
 */
public class Cy3SessionReaderImpl extends AbstractSessionReader {

	public static final Pattern NETWORK_PATTERN = Pattern.compile(".*/"+NETWORKS_FOLDER+"(([^/]+)[.]xgmml)");
	public static final Pattern NETWORK_NAME_PATTERN = Pattern.compile("(\\d+)(_[^_]+)?");
	public static final Pattern NETWORK_VIEW_PATTERN = Pattern.compile(".*/"+NETWORK_VIEWS_FOLDER+"(([^/]+)[.]xgmml)");
	public static final Pattern NETWORK_VIEW_NAME_PATTERN = Pattern.compile("(\\d+)_(\\d+)(_[^_]+)?"); // netId_viewId_title
	public static final Pattern NETWORK_TABLE_PATTERN = Pattern.compile(".*/(([^/]+)/([^/]+)-([^/]+)-([^/]+)[.]cytable)");
	public static final Pattern GLOBAL_TABLE_PATTERN = Pattern.compile(".*/(global/(\\d+)-([^/]+)[.]cytable)");
	
	private final Map<Long/*network_suid*/, CyNetwork> networkLookup = new HashMap<Long, CyNetwork>();
	private final Map<String/*old_network_id*/, Set<CyTableMetadataBuilder>> networkTableMap = new HashMap<String, Set<CyTableMetadataBuilder>>();

	private final CyNetworkReaderManager networkReaderMgr;
	private final CyPropertyReaderManager propertyReaderMgr;
	private final VizmapReaderManager vizmapReaderMgr;
	private final CSVCyReaderFactory csvCyReaderFactory;
	private final CyNetworkTableManager networkTableManager;

	private Map<String, CyTable> filenameTableMap;
	private Map<CyTableMetadataBuilder, String> builderFilenameMap;

	private List<VirtualColumnSerializer> virtualColumns;
	private boolean networksExtracted;


	/**
	 */
	public Cy3SessionReaderImpl(final InputStream sourceInputStream,
							    final CyNetworkReaderManager networkReaderMgr,
							    final CyPropertyReaderManager propertyReaderMgr,
							    final VizmapReaderManager vizmapReaderMgr,
							    final CSVCyReaderFactory csvCyReaderFactory,
							    final CyTableManager tableManager,
							    final CyTableFactory tablefacory,
							    final CyNetworkTableManager networkTableManager) {
		super(sourceInputStream, tableManager, tablefacory);

		if (networkReaderMgr == null) throw new NullPointerException("network reader manager is null!");
		this.networkReaderMgr = networkReaderMgr;
		
		if (propertyReaderMgr == null) throw new NullPointerException("property reader manager is null!");
		this.propertyReaderMgr = propertyReaderMgr;

		if (vizmapReaderMgr == null) throw new NullPointerException("vizmap reader manager is null!");
		this.vizmapReaderMgr = vizmapReaderMgr;
		
		if (csvCyReaderFactory == null) throw new NullPointerException("table reader manager is null!");
		this.csvCyReaderFactory = csvCyReaderFactory;

		if (networkTableManager == null) throw new NullPointerException("network table manager is null!");
		this.networkTableManager = networkTableManager;

		filenameTableMap = new HashMap<String, CyTable>();
		builderFilenameMap = new HashMap<CyTableMetadataBuilder, String>();
	}
	
	@Override
	protected void handleEntry(InputStream is, String entryName) throws Exception {
		if (!networksExtracted) {
			// First pass..
			if (entryName.contains("/" + PLUGINS_FOLDER)) {
				extractPluginEntry(is, entryName);
			} else if (entryName.endsWith(CYSESSION)) {
				extractSessionState(is, entryName);
			} else if (entryName.endsWith(VIZMAP_XML)) {
				extractVizmap(is, entryName);
			} else if (entryName.endsWith(CY_PROPS)) {
				extractCytoscapeProps(is, entryName);
			} else if (entryName.endsWith(XGMML_EXT)) {
				// Ignore network view files for now...
				Matcher matcher = NETWORK_PATTERN.matcher(entryName);
				
				if (matcher.matches()) {
					extractNetworks(is, entryName);
				}
			} else if (entryName.endsWith(BOOKMARKS_FILE)) {
				extractBookmarks(is, entryName);
			} else if (entryName.endsWith(TABLE_EXT)) {
				extractTable(is, entryName);
			} else if (entryName.endsWith(CYTABLE_METADATA_FILE)) {
				extractCyTableMetadata(is, entryName);
			} else if (!entryName.endsWith(VERSION_EXT)) {
				logger.warn("Unknown entry found in session zip file!\n" + entryName);
			}
		} else {
			// Second pass..
			if (!entryName.contains("/" + PLUGINS_FOLDER) && entryName.endsWith(XGMML_EXT)) {
				// Now the network views can be extracted!
				Matcher matcher = NETWORK_VIEW_PATTERN.matcher(entryName);
				
				if (matcher.matches()) {
					extractNetworkView(is, entryName);
				}
			}
		}
	}

	@Override
	protected void complete() throws Exception {
		tm.setProgress(0.4);
		tm.setTitle("Set network tables");
		tm.setStatusMessage("Setting network tables...");
		mergeNetworkTables();
		
		tm.setProgress(0.5);
		tm.setTitle("Restore virtual columns");
		tm.setStatusMessage("Restoring virtual columns...");
		restoreVirtualColumns();
		
		tm.setProgress(0.6);
		tm.setTitle("Extract network views");
		tm.setStatusMessage("Extracting network views...");
		// Read the session file again, this time to extract the network views
		networksExtracted = true;
		readSessionFile();
		
		super.complete();
	}

	private void extractCyTableMetadata(InputStream tmpIs, String entryName) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(tmpIs, "UTF-8"));
		virtualColumns = new ArrayList<VirtualColumnSerializer>();
		
		try {
			String line = reader.readLine();
			while (line != null) {
				virtualColumns.add(new VirtualColumnSerializer(line));
				line = reader.readLine();
			}
		} finally {
			reader.close();
		}
	}

	private void extractTable(InputStream stream, String entryName) throws Exception {
		csvCyReaderFactory.setInputStream(stream, entryName);
		CyTableReader reader = (CyTableReader) csvCyReaderFactory.createTaskIterator().next();
		reader.run(taskMonitor);

		// Assume one table per entry
		CyTable table = reader.getCyTables()[0];
		Matcher matcher = NETWORK_TABLE_PATTERN.matcher(entryName);
		
		if (matcher.matches()) {
			String networkName = SessionUtil.unescape(matcher.group(2));
			String oldNetId = getOldNetworkId(networkName);
			String namespace = SessionUtil.unescape(matcher.group(3));
			Class<?> type = Class.forName(SessionUtil.unescape(matcher.group(4)));
			String title = SessionUtil.unescape(matcher.group(5));
			table.setTitle(title);
			CyTableMetadataBuilder builder = new CyTableMetadataBuilder().setCyTable(table).setNamespace(namespace)
					.setType(type);
			Set<CyTableMetadataBuilder> builders = networkTableMap.get(oldNetId);
			
			if (builders == null) {
				builders = new HashSet<CyTableMetadataBuilder>();
				networkTableMap.put(oldNetId, builders);
			}
			
			builders.add(builder);
			
			String filename = matcher.group(1);
			filenameTableMap.put(filename, table);
			builderFilenameMap.put(builder, filename);
			
			return;
		}

		matcher = GLOBAL_TABLE_PATTERN.matcher(entryName);
		
		if (matcher.matches()) {
			String title = SessionUtil.unescape(matcher.group(3));
			table.setTitle(title);
			CyTableMetadataBuilder builder = new CyTableMetadataBuilder().setCyTable(table).setNetwork(null);
			tableMetadata.add(builder.build());
			
			String filename = matcher.group(1);
			filenameTableMap.put(filename, table);
			builderFilenameMap.put(builder, filename);
		}
	}

	private void extractNetworks(InputStream is, String entryName) throws Exception {
		CyNetworkReader reader = networkReaderMgr.getReader(is, entryName);
		reader.run(taskMonitor);
		CyNetwork[] netArray = reader.getCyNetworks();
		
		for (CyNetwork net : netArray) {
			networkLookup.put(net.getSUID(), net);
			networks.add(net);
		}
	}
	
	private void extractNetworkView(InputStream is, String entryName) throws Exception {
		// Get the token which identifies the network
		Matcher matcher = NETWORK_VIEW_PATTERN.matcher(entryName);
		String oldNetId = null;
		
		if (matcher.matches()) {
			String netViewToken = matcher.group(2);
			matcher = NETWORK_VIEW_NAME_PATTERN.matcher(netViewToken);
			
			if (matcher.matches())
				oldNetId = matcher.group(1);
		}
		
		if (oldNetId != null && !oldNetId.isEmpty()) {
			final Long netId = getNewId(oldNetId);
			final CyNetwork network = networkLookup.get(netId);
			
			if (network != null) {
				CyNetworkReader reader = networkReaderMgr.getReader(is, entryName);
				
				if (reader instanceof XGMMLNetworkReader) {
					((XGMMLNetworkReader) reader).setViewFormat(true); // TODO: let the Reader factory decide based on XGMML content
					reader.run(taskMonitor);
					
					final CyNetworkView view = reader.buildCyNetworkView(network);
					networkViews.add(view);
				} else {
					logger.error("CyNetworkReader should be an instance of XGMMLNetworkReader! "
							+ "Cannot extract network view: " + entryName);
				}
			}
		} else {
			logger.error("The network view entry is invalid: " + entryName);
		}
		
		// TODO: populate visual style map
	}

	private void extractPluginEntry(InputStream is, String entryName) {
		String[] items = entryName.split("/");

		if (items.length < 3) {
			// It's a directory name, not a file name
			return;
		}

		String pluginName = items[2];
		String fileName = items[items.length - 1];

		String tmpDir = System.getProperty("java.io.tmpdir");
		File theFile = new File(tmpDir, fileName);

		try {
			// Write input stream into tmp file
			BufferedWriter out = null;
			BufferedReader in = null;

			in = new BufferedReader(new InputStreamReader(is));
			out = new BufferedWriter(new FileWriter(theFile));

			// Write to tmp file
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				out.write(inputLine);
				out.newLine();
			}

			in.close();
			out.close();
		} catch (IOException e) {
			logger.error("Error: read from zip: " + entryName, e);
			return;
		}

		// Put the file into pluginFileListMap
		if (!pluginFileListMap.containsKey(pluginName)) pluginFileListMap.put(pluginName, new ArrayList<File>());

		List<File> fileList = pluginFileListMap.get(pluginName);
		fileList.add(theFile);
	}

	private void extractVizmap(InputStream is, String entryName) throws Exception {
		VizmapReader reader = vizmapReaderMgr.getReader(is, entryName);
		reader.run(taskMonitor);
		visualStyles.addAll(reader.getVisualStyles());
	}

	private void extractCytoscapeProps(InputStream is, String entryName) throws Exception {
		CyPropertyReader reader = propertyReaderMgr.getReader(is, entryName);
		reader.run(taskMonitor);
		cytoscapeProps = (Properties) reader.getProperty();
	}

	private void extractBookmarks(InputStream is, String entryName) throws Exception {
		CyPropertyReader reader = propertyReaderMgr.getReader(is, entryName);
		reader.run(taskMonitor);
		bookmarks = (Bookmarks) reader.getProperty();
	}

	private void extractSessionState(InputStream is, String entryName) throws Exception {
		CyPropertyReader reader = propertyReaderMgr.getReader(is, entryName);
		reader.run(taskMonitor);
		cysession = (Cysession) reader.getProperty();
	}
	
	private void restoreVirtualColumns() {
		if (virtualColumns == null) {
			return;
		}
		
		for (VirtualColumnSerializer columnData : virtualColumns) {
			CyTable targetTable = filenameTableMap.get(columnData.getTargetTable());
			CyTable sourceTable = filenameTableMap.get(columnData.getSourceTable());
			targetTable.addVirtualColumn(columnData.getName(),
										 columnData.getSourceColumn(),
										 sourceTable,
										 columnData.getTargetJoinKey(),
										 columnData.isImmutable());
		}
	}

	private void mergeNetworkTables() throws UnsupportedEncodingException {
		for (Entry<Long, CyNetwork> entry : networkLookup.entrySet()) {
			CyNetwork network = entry.getValue();
			String oldId = getOldId(network.getSUID());
			Set<CyTableMetadataBuilder> builders = networkTableMap.get(oldId);

			if (builders == null)
				continue;

			for (CyTableMetadataBuilder builder : builders) {
				builder.setNetwork(network);
				mergeNetworkTable(network, builder);
				CyTableMetadata metadata = builder.build();
				tableMetadata.add(metadata);
				
				// Update filename<->table maps
				String filename = builderFilenameMap.get(builder);
				filenameTableMap.put(filename, metadata.getCyTable());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void mergeNetworkTable(CyNetwork network, CyTableMetadataBuilder builder) {
		Class<? extends CyTableEntry> type = (Class<? extends CyTableEntry>) builder.getType();
		String namespace = builder.getNamespace();

		if ("VIEW".equals(namespace)) {
			return; // TODO: disabled due to timing conflicts with Ding.
		}

		Map<String, CyTable> tableMap = networkTableManager.getTables(network, type);
		CyTable targetTable = tableMap.get(namespace);
		CyTable sourceTable = builder.getCyTable();
		Map<String, Long> mappings = getSUIDMap();
		mergeTables(sourceTable, targetTable, mappings);
		builder.setCyTable(targetTable);
	}
	
	private void mergeTables(CyTable source, CyTable target, Map<String, Long> mappings) {
		CyColumn sourceKey = source.getPrimaryKey();
		CyColumn targetKey = target.getPrimaryKey();
		String keyName = sourceKey.getName();

		// Make sure keys match
		if (keyName.equals(targetKey.getName())) {
			for (CyRow sourceRow : source.getAllRows()) {
				Long key = sourceRow.get(keyName, Long.class);

				if (mappings != null) {
					key = mappings.get(""+key);
				}

				CyRow targetRow = target.getRow(key);
				mergeRow(keyName, sourceRow, targetRow);
			}
		}
	}

	private Map<String, Long> getSUIDMap() {
		CyTable mappingTable = tableManager.getTable(SessionUtil.getIdMappingTableSUID());
		
		if (mappingTable == null) {
			return Collections.emptyMap();
		}

		Map<String, Long> mappings = new HashMap<String, Long>();

		for (CyRow row : mappingTable.getAllRows()) {
			String oldSUID = row.get(SessionUtil.ORIGINAL_ID_COLUMN, String.class);
			Long newSUID = row.get(SessionUtil.ENTRY_SUID_COLUMN, Long.class);
			mappings.put(oldSUID, newSUID);
		}

		return mappings;
	}

	private void mergeRow(String keyName, CyRow sourceRow, CyRow targetRow) {
		for (CyColumn column : sourceRow.getTable().getColumns()) {
			String columnName = column.getName();

			if (columnName.equals(keyName)) {
				continue;
			}

			Class<?> type = column.getType();
			boolean immutable = column.isImmutable();
			CyTable targetTable = targetRow.getTable();

			if (type.equals(List.class)) {
				Class<?> elementType = column.getListElementType();
				List<?> list = sourceRow.getList(columnName, elementType);

				if (targetTable.getColumn(columnName) == null)
					targetTable.createListColumn(columnName, elementType, immutable);

				targetRow.set(columnName, list);
			} else {
				Object value = sourceRow.get(columnName, type);

				if (targetTable.getColumn(columnName) == null)
					targetTable.createColumn(columnName, type, immutable);

				targetRow.set(columnName, value);
			}
		}
	}
	
	/**
	 * @param networkToken
	 * @return
	 */
	private static String getOldNetworkId(final String networkToken) {
		String id = null;
		Matcher matcher = NETWORK_NAME_PATTERN.matcher(networkToken);
		
		if (matcher.matches()) {
			id = matcher.group(1);
		}
		
		return id;
		
	}
}