/*
  File: HideSelectedTask.java

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
package org.cytoscape.task.internal.hide;


import java.util.List;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.undo.UndoSupport;


public class HideSelectedTask extends AbstractNetworkViewTask {
	private final UndoSupport undoSupport;
	private final CyEventHelper eventHelper;

	public HideSelectedTask(final UndoSupport undoSupport, final CyEventHelper eventHelper,
	                        final CyNetworkView v)
	{
		super(v);
		this.undoSupport = undoSupport;
		this.eventHelper = eventHelper;
	}

	public void run(TaskMonitor e) {
		e.setProgress(0.0);
		final CyNetwork network = view.getModel();
		undoSupport.postEdit(
			new HideEdit(eventHelper, "Hide Selected Nodes & Edges", network, view));

		final List<CyNode> selectedNodes =
			CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		e.setProgress(0.2);
		final List<CyEdge> selectedEdges =
			CyTableUtil.getEdgesInState(network, CyNetwork.SELECTED, true); 
		e.setProgress(0.4);
		HideUtils.setVisibleNodes(selectedNodes, false, view);
		e.setProgress(0.6);
		HideUtils.setVisibleEdges(selectedEdges, false, view);
		e.setProgress(0.8);
		view.updateView();
		e.setProgress(1.0);
	} 
}
