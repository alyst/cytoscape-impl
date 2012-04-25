package org.cytoscape.app.internal;


import static org.mockito.Mockito.mock;

import java.util.Properties;

import org.cytoscape.app.internal.CyAppAdapterImpl;
import org.cytoscape.application.CyVersion;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.io.datasource.DataSourceManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.io.read.CyPropertyReaderManager;
import org.cytoscape.io.read.CySessionReaderManager;
import org.cytoscape.io.read.CyTableReaderManager;
import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.io.write.CyPropertyWriterManager;
import org.cytoscape.io.write.CySessionWriterManager;
import org.cytoscape.io.write.PresentationWriterManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
//import org.cytoscape.app.CyAppAdapterTest;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.task.TableCellTaskFactory;
import org.cytoscape.task.creation.CloneNetworkTaskFactory;
import org.cytoscape.task.creation.CreateNetworkViewTaskFactory;
import org.cytoscape.task.creation.NewEmptyNetworkViewFactory;
import org.cytoscape.task.creation.NewNetworkSelectedNodesAndEdgesTaskFatory;
import org.cytoscape.task.creation.NewNetworkSelectedNodesOnlyTaskFactory;
import org.cytoscape.task.destruction.DestroyNetworkTaskFactory;
import org.cytoscape.task.destruction.DestroyNetworkViewTaskFactory;
import org.cytoscape.task.edit.ConnectSelectedNodesTaskFactory;
import org.cytoscape.task.export.ExportCurrentTableTaskFactory;
import org.cytoscape.task.export.ExportNetworkImageTaskFactory;
import org.cytoscape.task.export.ExportNetworkViewTaskFactory;
import org.cytoscape.task.export.ExportVizmapTaskFactory;
import org.cytoscape.task.group.GroupNodesTaskFactory;
import org.cytoscape.task.hide.HideSelectedEdgesTaskFactory;
import org.cytoscape.task.hide.HideSelectedNodesTaskFactory;
import org.cytoscape.task.hide.HideSelectedTaskFactory;
import org.cytoscape.task.hide.UnHideAllEdgesTaskFactory;
import org.cytoscape.task.hide.UnHideAllNodesTaskFactory;
import org.cytoscape.task.hide.UnHideAllTaskFactory;
import org.cytoscape.task.layout.ApplyPreferredLayoutTaskFactory;
import org.cytoscape.task.networkobjects.DeleteSelectedNodesAndEdgesTaskFactory;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadNetworkURLTaskFactory;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
import org.cytoscape.task.select.DeselectAllEdgesTaskFactory;
import org.cytoscape.task.select.DeselectAllNodesTaskFactory;
import org.cytoscape.task.select.DeselectAllTaskFactory;
import org.cytoscape.task.select.InvertSelectedEdgesTaskFactory;
import org.cytoscape.task.select.InvertSelectedNodesTaskFactory;
import org.cytoscape.task.select.SelectAdjacentEdgesTaskFactory;
import org.cytoscape.task.select.SelectAllEdgesTaskFactory;
import org.cytoscape.task.select.SelectAllNodesTaskFactory;
import org.cytoscape.task.select.SelectAllTaskFactory;
import org.cytoscape.task.select.SelectConnectedNodesTaskFactory;
import org.cytoscape.task.select.SelectFirstNeighborsNodeViewTaskFactory;
import org.cytoscape.task.select.SelectFirstNeighborsTaskFactory;
import org.cytoscape.task.select.SelectFromFileListTaskFactory;
import org.cytoscape.task.session.NewSessionTaskFactory;
import org.cytoscape.task.session.OpenSessionTaskFactory;
import org.cytoscape.task.session.SaveSessionAsTaskFactory;
import org.cytoscape.task.table.DeleteColumnTaskFactory;
import org.cytoscape.task.table.DeleteTableTaskFactory;
import org.cytoscape.task.table.MapGlobalToLocalTableTaskFactory;
import org.cytoscape.task.table.MapNetworkAttrTaskFactory;
import org.cytoscape.task.table.RenameColumnTaskFactory;
import org.cytoscape.task.title.EditNetworkTitleTaskFactory;
import org.cytoscape.task.vizmap.ApplyVisualStyleTaskFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.swing.PanelTaskManager;
import org.cytoscape.work.swing.SubmenuTaskManager;
import org.cytoscape.work.undo.UndoSupport;
import org.junit.Before;

public class CyAppAdapterImplTest /*extends CyAppAdapterTest*/ {

	@Before
	public void setUp() {
		CyAppAdapter adapter = new CyAppAdapterImpl( 
			mock(CyApplicationManager.class),
			mock(CyEventHelper.class),
			mock(CyLayoutAlgorithmManager.class),
			mock(CyNetworkFactory.class),
			mock(CyNetworkManager.class),
			mock(CyNetworkViewFactory.class),
			mock(CyNetworkViewManager.class),
			mock(CyNetworkReaderManager.class),
			mock(CyNetworkViewWriterManager.class),
			(CyProperty<Properties>)mock(CyProperty.class),
			mock(CyPropertyReaderManager.class),
			mock(CyPropertyWriterManager.class),
			mock(CyRootNetworkManager.class),
			mock(CyServiceRegistrar.class),
			mock(CySessionManager.class),
			mock(CySessionReaderManager.class),
			mock(CySessionWriterManager.class),
			mock(CySwingApplication.class),
			mock(CyTableFactory.class),
			mock(CyTableManager.class),
			mock(CyTableReaderManager.class),
			mock(CyVersion.class),
//			mock(CyTableWriterManager.class),
			mock(DialogTaskManager.class),
			mock(PanelTaskManager.class),
			mock(SubmenuTaskManager.class),
			mock(PresentationWriterManager.class),
			mock(RenderingEngineManager.class),
			mock(TaskManager.class),
			mock(UndoSupport.class),
			mock(VisualMappingFunctionFactory.class),
			mock(VisualMappingFunctionFactory.class),
			mock(VisualMappingFunctionFactory.class),
			mock(VisualMappingManager.class),
			mock(VisualStyleFactory.class),
			mock(DataSourceManager.class),
			
			// core-task services
			mock(LoadVizmapFileTaskFactory.class),
			mock(LoadNetworkFileTaskFactory.class),
			mock(LoadNetworkURLTaskFactory.class),
			mock(DeleteSelectedNodesAndEdgesTaskFactory.class),
			mock(SelectAllTaskFactory.class),
			mock(SelectAllEdgesTaskFactory.class),
			mock(SelectAllNodesTaskFactory.class),
			mock(SelectAdjacentEdgesTaskFactory.class),
			mock(SelectConnectedNodesTaskFactory.class),
			mock(SelectFirstNeighborsTaskFactory.class),
			mock(SelectFirstNeighborsTaskFactory.class),
			mock(SelectFirstNeighborsTaskFactory.class),
			mock(DeselectAllTaskFactory.class),
			mock(DeselectAllEdgesTaskFactory.class),
			mock(DeselectAllNodesTaskFactory.class),
			mock(InvertSelectedEdgesTaskFactory.class),
			mock(InvertSelectedNodesTaskFactory.class),
			mock(SelectFromFileListTaskFactory.class),
			mock(SelectFirstNeighborsNodeViewTaskFactory.class),
			mock(HideSelectedTaskFactory.class),
			mock(HideSelectedNodesTaskFactory.class),
			mock(HideSelectedEdgesTaskFactory.class),
			mock(UnHideAllTaskFactory.class),
			mock(UnHideAllNodesTaskFactory.class),
			mock(UnHideAllEdgesTaskFactory.class),
			mock(NewEmptyNetworkViewFactory.class),
			mock(CloneNetworkTaskFactory.class),
			mock(NewNetworkSelectedNodesAndEdgesTaskFatory.class),
			mock(NewNetworkSelectedNodesOnlyTaskFactory.class),
			mock(DestroyNetworkTaskFactory.class),
			mock(DestroyNetworkViewTaskFactory.class),
			mock(NetworkViewTaskFactory.class),
			mock(NetworkViewTaskFactory.class),
			mock(NetworkViewTaskFactory.class),
			mock(NetworkViewTaskFactory.class),
			mock(NewSessionTaskFactory.class),
			mock(OpenSessionTaskFactory.class),
			mock(TaskFactory.class),
			mock(SaveSessionAsTaskFactory.class),
			mock(TaskFactory.class),
			mock(EditNetworkTitleTaskFactory.class),
			mock(CreateNetworkViewTaskFactory.class),
			mock(ExportNetworkImageTaskFactory.class),
			mock(ExportNetworkViewTaskFactory.class),
			mock(ExportCurrentTableTaskFactory.class),
			mock(ApplyPreferredLayoutTaskFactory.class),
			mock(DeleteColumnTaskFactory.class),
			mock(RenameColumnTaskFactory.class),
			mock(TableCellTaskFactory.class),
			mock(DeleteTableTaskFactory.class),
			mock(ExportVizmapTaskFactory.class),
			mock(ConnectSelectedNodesTaskFactory.class),
			mock(MapGlobalToLocalTableTaskFactory.class),
			mock(ApplyVisualStyleTaskFactory.class),
			mock(MapNetworkAttrTaskFactory.class),
			mock(GroupNodesTaskFactory.class),
			mock(NetworkViewTaskFactory.class),
			mock(NodeViewTaskFactory.class),
			mock(NodeViewTaskFactory.class),
			mock(NodeViewTaskFactory.class)			
		    );
	}	
}
