/*
 File: SaveSessionTaskFactory.java

 Copyright (c) 2006, 2010, The Cytoscape Consortium (www.cytoscape.org)

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
package org.cytoscape.task.internal.session;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.io.util.RecentlyOpenedTracker;
import org.cytoscape.io.write.CySessionWriterManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class SaveSessionTaskFactoryImpl extends AbstractTaskFactory {

	private final CySessionManager sessionMgr;
	private final CySessionWriterManager writerMgr;
	private final RecentlyOpenedTracker tracker;
	private final CyEventHelper cyEventHelper;

	public SaveSessionTaskFactoryImpl(CySessionWriterManager writerMgr, CySessionManager sessionMgr,
			final RecentlyOpenedTracker tracker, final CyEventHelper cyEventHelper) {
		this.sessionMgr = sessionMgr;
		this.writerMgr = writerMgr;
		this.tracker = tracker;
		this.cyEventHelper = cyEventHelper;
	}

	public TaskIterator createTaskIterator() {
		final CySession session = sessionMgr.getCurrentSession();
		if (session == null)
			throw new NullPointerException("Could not find current session.");
		
		// Check session file name is set or not.
		final String sessionFileName = sessionMgr.getCurrentSessionFileName();		
		
		// If there is no file name, use Save As task.  Otherwise, overwrite the current session.
		if (sessionFileName == null)
			return new TaskIterator(new SaveSessionAsTask(writerMgr, sessionMgr, tracker, cyEventHelper));
		else
			return new TaskIterator(new SaveSessionTask(writerMgr, session, sessionFileName, tracker));
	}
}