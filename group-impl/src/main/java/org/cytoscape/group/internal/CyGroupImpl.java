/*
 Copyright (c) 2008, 2010, The Cytoscape Consortium (www.cytoscape.org)

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
package org.cytoscape.group.internal;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.group.events.GroupAboutToBeRemovedEvent;
import org.cytoscape.group.events.GroupAddedToNetworkEvent;
import org.cytoscape.group.events.GroupChangedEvent;
import org.cytoscape.group.events.GroupCollapsedEvent;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;


class CyGroupImpl implements CyGroup {
	final private CyEventHelper cyEventHelper;
	final private CyGroupManager mgr;

	private CyNode groupNode;
	private CySubNetwork groupNet;
	private Set<CyEdge> externalEdges;
	private Set<CyEdge> metaEdges;
	private CyRootNetwork rootNetwork = null;
	private Set<CyNetwork> networkSet = null;
	private Set<CyNetwork> collapseSet = null;
	private boolean batchUpdate = false;
	private boolean nodeProvided = false;  // We'll need this when we destroy ourselves

	CyGroupImpl(final CyEventHelper eventHelper, 
	            final CyGroupManager mgr, CyNetwork network, CyNode node,
	            List<CyNode>nodes, List<CyEdge>edges) {
		this.cyEventHelper = eventHelper;
		this.mgr = mgr;

		this.rootNetwork = ((CySubNetwork)network).getRootNetwork();
		if (node == null)
			this.groupNode = this.rootNetwork.addNode();
		else {
			nodeProvided = true;
			this.groupNode = node;
		}

		this.externalEdges = new HashSet<CyEdge>();
		this.metaEdges = new HashSet<CyEdge>();
		this.networkSet = new HashSet<CyNetwork>();
		this.collapseSet = new HashSet<CyNetwork>();

		networkSet.add(network);

		if (nodes == null)
			nodes = new ArrayList<CyNode>();

		Set<CyNode> nodeMap = new HashSet<CyNode>(nodes);

		if (edges != null) {
			List<CyEdge> intEdges = new ArrayList<CyEdge>();
			// Remove those edges in the list that aren't attached to nodes in
			// the list.  Otherwise, we'll wind up adding nodes to the group
			// that the user didn't request.
			for (CyEdge e: edges) {
				if (nodeMap.contains(e.getSource()) && nodeMap.contains(e.getTarget())) {
					intEdges.add(e);
				} else {
					externalEdges.add(e);
				}
			}
			edges = intEdges;
		} else if (edges == null) {
			// Create the edge lists
			edges = new ArrayList<CyEdge>();

			// Get all of the edges and put them in the right lists
			for (CyNode n: nodes) {
				List<CyEdge> aEdges = network.getAdjacentEdgeList(n, CyEdge.Type.ANY);
				for (CyEdge e: aEdges) {
					if (nodeMap.contains(e.getSource()) && nodeMap.contains(e.getTarget()))
						edges.add(e);
					else {
						// This is an external edge, which means that we need to create
						// a corresponding meta-edge
						externalEdges.add(e);
						CyNode source = e.getSource();
						CyNode target = e.getTarget();
						boolean directed = e.isDirected();
						if (nodeMap.contains(source))
							source = groupNode;
						else
							target = groupNode;
						metaEdges.add(rootNetwork.addEdge(source, target, directed));
					}
				}
			}
		}

		// Create the subnetwork
		groupNet = rootNetwork.addSubNetwork(nodes, edges);
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getGroupNode()
	 */
	@Override
	public CyNode getGroupNode() {
		return groupNode;
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getNodeList()
	 */
	@Override
	public List<CyNode> getNodeList() {
		return groupNet.getNodeList();
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getContainedEdgeList()
	 */
	@Override
	public List<CyEdge> getInteriorEdgeList() {
		return groupNet.getEdgeList();
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getExteriorEdgeList()
	 */
	@Override
	public Set<CyEdge> getExteriorEdgeList() {
		return externalEdges;
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getGroupNetwork()
	 */
	@Override
	public CyNetwork getGroupNetwork() {
		return groupNet;
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addNode()
	 */
	@Override
	public synchronized void addNode(CyNode node) {
		if (!rootNetwork.containsNode(node))
			throwIllegalArgumentException("Can only add a node in the same network tree");
		groupNet.addNode(node);
		cyEventHelper.fireEvent(new GroupChangedEvent(CyGroupImpl.this, node, GroupChangedEvent.ChangeType.NODE_ADDED));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addInternalEdge()
	 */
	@Override
	public synchronized void addInternalEdge(CyEdge edge) {
		if (!rootNetwork.containsEdge(edge))
			throwIllegalArgumentException("Can only add an edge in the same network tree");
		groupNet.addEdge(edge);
		if (!batchUpdate)
			cyEventHelper.fireEvent(new GroupChangedEvent(CyGroupImpl.this, edge, GroupChangedEvent.ChangeType.INTERNAL_EDGE_ADDED));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addExternalEdge()
	 */
	@Override
	public synchronized void addExternalEdge(CyEdge edge) {
		if (!rootNetwork.containsEdge(edge))
			throwIllegalArgumentException("Can only add an edge in the same network tree");
		if (!externalEdges.contains(edge))
			externalEdges.add(edge);
		if (!batchUpdate)
			cyEventHelper.fireEvent(new GroupChangedEvent(CyGroupImpl.this, edge, GroupChangedEvent.ChangeType.EXTERNAL_EDGE_ADDED));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addNodes()
	 */
	@Override
	public synchronized void addNodes(List<CyNode> nodes) {
		Set<CyEdge> edgeSet = new HashSet<CyEdge>();
		batchUpdate = true;
		for (CyNode n: nodes) {
			if (!rootNetwork.containsNode(n))
				throwIllegalArgumentException("Can only add a node in the same network tree");

			addNode(n);
			edgeSet.addAll(rootNetwork.getAdjacentEdgeList(n, CyEdge.Type.ANY));
		}

		for (CyEdge e: edgeSet) {
			if (groupNet.containsNode(e.getSource()) && groupNet.containsNode(e.getTarget())) {
				addInternalEdge(e);
			} else {
				addExternalEdge(e);
			}
		}
		batchUpdate = false;
		cyEventHelper.fireEvent(new GroupChangedEvent(CyGroupImpl.this, nodes, GroupChangedEvent.ChangeType.NODES_ADDED));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#addEdges()
	 */
	@Override
	public synchronized void addEdges(List<CyEdge> edges) {
		for (CyEdge edge: edges) {
			CyNode source = edge.getSource();
			CyNode target = edge.getTarget();
			if(groupNet.containsNode(source) && groupNet.containsNode(target))
				groupNet.addEdge(edge);
			else if (groupNet.containsNode(source) || groupNet.containsNode(target))
				externalEdges.add(edge);
			else
				throwIllegalArgumentException("Attempted to add an edge that has no node in the group");
		}
		cyEventHelper.fireEvent(new GroupChangedEvent(CyGroupImpl.this, edges, GroupChangedEvent.ChangeType.EDGES_ADDED));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#removeNodes()
	 */
	@Override
	public synchronized void removeNodes(List<CyNode> nodes) {
		batchUpdate = true;
		List<CyEdge> netEdges = new ArrayList<CyEdge>();
		for (CyNode node: nodes) {
			List<CyEdge> edges = rootNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
			for (CyEdge edge: edges) {
				if (externalEdges.contains(edge))
					externalEdges.remove(edge);
				else {
					netEdges.add(edge);
				}
			}
		}
		if (netEdges.size() > 0)
			groupNet.removeEdges(netEdges);
		groupNet.removeNodes(nodes);
		batchUpdate = false;
		cyEventHelper.fireEvent(new GroupChangedEvent(CyGroupImpl.this, nodes, GroupChangedEvent.ChangeType.NODES_REMOVED));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#removeEdges()
	 */
	@Override
	public synchronized void removeEdges(List<CyEdge> edges) {
		List<CyEdge> netEdges = new ArrayList<CyEdge>();
		for (CyEdge edge: edges) {
			if (groupNet.containsEdge(edge))
				netEdges.add(edge);
			else if (externalEdges.contains(edge))
				externalEdges.remove(edge);
			else if (metaEdges.contains(edge))
				metaEdges.remove(edge);
		}
		cyEventHelper.fireEvent(new GroupChangedEvent(CyGroupImpl.this, edges, GroupChangedEvent.ChangeType.EDGES_REMOVED));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#getRootNetwork()
	 */
	@Override
	public CyRootNetwork getRootNetwork() {
		return this.rootNetwork;
	}

	/**
 	 * @see org.cytoscape.group.CyGroup#getNetworkSet()
 	 */
	@Override
	public Set<CyNetwork> getNetworkSet() {
		return networkSet;
	}

	/**
 	 * @see org.cytoscape.group.CyGroup#isInNetwork()
 	 */
	@Override
	public boolean isInNetwork(CyNetwork network) {
		return networkSet.contains(network);
	}

	/**
 	 * @see org.cytoscape.group.CyGroup#addGroupToNetwork()
 	 */
	@Override
	public synchronized void addGroupToNetwork(CyNetwork network) {
		// First, we need to make sure this network is in the same
		// root network as the group node
		if (!inSameRoot(network))
			throwIllegalArgumentException("Network not in same root network as group");

		if(!networkSet.contains(network))
			networkSet.add(network);

		// Notify
		cyEventHelper.fireEvent(new GroupAddedToNetworkEvent(CyGroupImpl.this, network));
	}

	/**
 	 * @see org.cytoscape.group.CyGroup#removeGroupFromNetwork()
 	 */
	@Override
	public synchronized void removeGroupFromNetwork(CyNetwork network) {
		// Notify
		cyEventHelper.fireEvent(new GroupAboutToBeRemovedEvent(CyGroupImpl.this, network));

		if(networkSet.contains(network))
			networkSet.remove(network);
	}

	/**
	 * @see org.cytoscape.group.CyGroup#collapse()
	 */
	@Override
	public void collapse(CyNetwork net) {
		if (isCollapsed(net))
			return; // Already collapsed

		if (!networkSet.contains(net))
			return; // We're not in that network

		CySubNetwork subnet = (CySubNetwork) net;

		// Collapse it.
		// Remove all of the nodes from the target network
		subnet.removeNodes(getNodeList());

		subnet.addNode(groupNode);

		// Add the group node and it's edges
		List<CyEdge> groupNodeEdges = rootNetwork.getAdjacentEdgeList(groupNode, CyEdge.Type.ANY);
		for (CyEdge e: groupNodeEdges)
			subnet.addEdge(e);

		Set<CyNode> memberNodes = new HashSet<CyNode>(getNodeList());

		// Add the meta-edges
		for (CyEdge e: getMetaEdgeList()) {
			subnet.addEdge(e);
		}

		collapseSet.add(net);
		cyEventHelper.fireEvent(new GroupCollapsedEvent(CyGroupImpl.this, net, true));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#expand()
	 */
	@Override
	public void expand(CyNetwork net) {
		if (!isCollapsed(net))
			return; // Already expanded

		if (!networkSet.contains(net))
			return; // We're not in that network

		CySubNetwork subnet = (CySubNetwork) net;

		// Expand it.
		// Remove the group node from the target network
		subnet.removeNodes(Collections.singletonList(groupNode));

		// Add all of the member nodes and edges in
		for (CyNode n: getNodeList())
			subnet.addNode(n);

		// Add all of the interior edges in
		for (CyEdge e: getInteriorEdgeList())
			subnet.addEdge(e);

		// Add all of the exterior edges in
		for (CyEdge e: getExteriorEdgeList()) {
			// We need to be careful to only add the edge in
			// if both the edge and the target are available
			// since the target node might have been part of a
			// collapsed group
			if (subnet.containsNode(e.getSource()) && subnet.containsNode(e.getTarget()))
				subnet.addEdge(e);
		}

		collapseSet.remove(net);
		cyEventHelper.fireEvent(new GroupCollapsedEvent(CyGroupImpl.this, net, false));
	}

	/**
	 * @see org.cytoscape.group.CyGroup#isCollapsed()
	 */
	@Override
	public boolean isCollapsed(CyNetwork net) {
		return collapseSet.contains(net);
	}

	/**
 	 * Destroy this group. This will destroy the subnetwork, all metaEdges, and 
 	 * the group node (if we created it).  This is meant to be called from the
 	 * CyGroupManager, only.
 	 */
	public void destroyGroup() {
		// Destroy the subNetwork
		rootNetwork.removeSubNetwork(groupNet);
		groupNet = null;

		// Release all of our external edges
		externalEdges = null;

		// Remove all of our metaEdges from the root network
		rootNetwork.removeEdges(metaEdges);

		// If our group node was not provided, destroy it
		if (!nodeProvided && rootNetwork.containsNode(groupNode)) {
			rootNetwork.removeNodes(Collections.singletonList(groupNode));
		}

		networkSet = null;
		collapseSet = null;
	}

	protected synchronized void addMetaEdge(CyEdge edge) {
		if (!metaEdges.contains(edge))
			metaEdges.add(edge);
	}

	protected synchronized void removeMetaEdge(CyEdge edge) {
		if (!metaEdges.contains(edge))
			metaEdges.remove(edge);
	}

	protected Set<CyEdge> getMetaEdgeList() {
		return metaEdges;
	}

	@Override
	public String toString() {
		return "Group suid: " + groupNode.getSUID() + " node: " + groupNode;
	}

	private boolean inSameRoot(CyNetwork network) {
		CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
		if (!root.equals(rootNetwork))
			return false;
		return true;
	}

	private	void throwIllegalArgumentException(String message) {
		batchUpdate = false;
		throw new IllegalArgumentException(message);
	}
}