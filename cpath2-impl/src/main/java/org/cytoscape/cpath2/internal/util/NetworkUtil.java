package org.cytoscape.cpath2.internal.util;

/*
 * #%L
 * Cytoscape CPath2 Impl (cpath2-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2007 - 2013
 *   Memorial Sloan-Kettering Cancer Center
 *   The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

// imports

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.cytoscape.cpath2.internal.CPath2Factory;
import org.cytoscape.cpath2.internal.task.LoadNetworkFromUrlTask;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

/**
 * This is a network utilities class.
 *
 * @author Benjamin Gross; refactored by rodche
 */
public class NetworkUtil implements Task {

    /**
     * Stores web services url
     */
    private String webServicesURL;

    /**
     * ref to cPathRequst
     */
    private String cPathRequest;

    /**
     * ref to cyNetwork
     */
    private CyNetwork cyNetwork;

    /**
     * ref to cyNetworkTitle
     */
    private String networkTitle;

    /**
     * ref to data source set
     */
    private String dataSources;

    /**
     * boolean indicated if we are merging
     */
    private boolean merging;

	private final CPath2Factory factory;

    /**
     * Neighborhood title parameter.
     */
    private static final String NEIGHBORHOOD_TITLE_ARG = "&neighborhood_title=";

    /**
     * Data Source Arg
     */
    private static final String DATA_SOURCE_ARG = "&data_source=";

    /**
     * Constructor.
     *
     * @param cpathRequest   String
     * @param cyNetwork               CyNetwork
     * @param merging                 boolean
     * @param nodeContextMenuListener NodeContextMenuListener
     */
    public NetworkUtil(String cpathRequest, CyNetwork cyNetwork,
            boolean merging, CPath2Factory factory) {
    	this.factory = factory;
    	
        // init member vars
        parseRequest(cpathRequest);
        this.cyNetwork = cyNetwork;
        this.merging = merging;
    }

    /**
     * Our implementation of run.
     */
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        try {
            URL cpathURL = new URL(cPathRequest);

            // are we merging ?
            if (merging) {
                // start merge network task
            	TaskFactory taskFactory = factory.createMergeNetworkTaskFactory(cpathURL, cyNetwork);
                factory.getTaskManager().execute(taskFactory.createTaskIterator());
                postProcess(cyNetwork, true);
            } else {
                // the biopax graph reader is going to be called
                // it will look for the network view title
                // via system properties, so lets set it now
                if (networkTitle != null && networkTitle.length() > 0) {
                    System.setProperty("biopax.network_view_title", networkTitle);
                }
                TaskIterator iterator = new TaskIterator(new LoadNetworkFromUrlTask(cpathURL, factory));
                factory.getTaskManager().execute(iterator);
                postProcess(factory.getCyApplicationManager().getCurrentNetwork(), false);
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to process/parse the cpath request and
     * set proper member variables.
     *
     * @param cpathRequest String
     */
    private void parseRequest(String cpathRequest) {

        // web services url
        int indexToStartOfPC = cpathRequest.indexOf("/pc");
        if (indexToStartOfPC > 0) {
            this.webServicesURL = cpathRequest.substring(7, indexToStartOfPC + 3);
        }

        // extract title
        this.networkTitle = extractRequestArg(NEIGHBORHOOD_TITLE_ARG,
                cpathRequest);

        // extract data sources
        dataSources = extractRequestArg(DATA_SOURCE_ARG,
                cpathRequest);

        // set request member
        this.cPathRequest = cpathRequest;
    }

    /**
     * Extracts argument from cpath request (url).
     * Method removes argument from cPathRequest arg,
     * and returns it as String.
     *
     * @param arg                   String - the argument to extract
     * @param cpathRequest String
     * @return String
     */
    private String extractRequestArg(String arg, String cpathRequest) {

        // get index of argument
        int indexOfArg = cpathRequest.indexOf(arg);

        // if arg is not in list, bail
        if (indexOfArg == -1) return null;

        int startIndexOfValue = indexOfArg + arg.length();
        int endIndexOfValue = cpathRequest.indexOf("&", startIndexOfValue);
        String value = (endIndexOfValue == -1) ?
                cpathRequest.substring(startIndexOfValue) :
                cpathRequest.substring(startIndexOfValue, endIndexOfValue);

        // remove arg from request
        cpathRequest = (endIndexOfValue == -1) ?
                cpathRequest.substring(0, indexOfArg) :
                cpathRequest.substring(0, indexOfArg) +
                        cpathRequest.substring(endIndexOfValue);

        return value;
    }

    /**
     * Method for any post processing of recently loaded network.
     *
     * @param cyNetwork CyNetwork
     * @param doLayout  boolean
     */
    private void postProcess(final CyNetwork cyNetwork, boolean doLayout) {

        // ref to view used below
    		final Collection<CyNetworkView> views = factory.getCyNetworkViewManager().getNetworkViews(cyNetwork);
    		CyNetworkView view = null;
    		if(views.size() != 0)
    			view = views.iterator().next();

        // if do layout, do it
// TODO: Port this?    
//        if (doLayout) {
//            LayoutUtil layoutUtil = new LayoutUtil();
//            layoutUtil.doLayout(view);
//            view.fitContent();
//        }

        // setup web services url to pc attribute  - used by nodeContextMenuListener
        if (webServicesURL != null) {
        	AttributeUtil.set(cyNetwork, cyNetwork, "biopax.web_services_url", webServicesURL, String.class);
        }

        // setup data sources attribute - used by nodeContextMenuListener - remains encoded
        if (dataSources != null) {
        	AttributeUtil.set(cyNetwork, cyNetwork, "biopax.data_sources", dataSources, String.class);
        }

        // setup the context menu
        //view.addNodeContextMenuListener(nodeContextMenuListener);

        // set focus current
        factory.getCyApplicationManager().setCurrentNetworkView(view);
	}

    
	@Override
	public void cancel() {
	}
}
