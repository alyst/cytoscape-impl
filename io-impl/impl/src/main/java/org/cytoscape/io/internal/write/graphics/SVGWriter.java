package org.cytoscape.io.internal.write.graphics;

import java.awt.Dimension;
import java.io.OutputStream;

import org.cytoscape.io.write.CyWriter;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SVGWriter extends AbstractTask implements CyWriter {
	
	private static final Logger logger = LoggerFactory.getLogger(PDFWriter.class);

	@Tunable(description="Export text as font")
	public boolean exportTextAsFont = true;

	@ProvidesTitle
	public String getTitle() {
		return "Export Network";
	}
	
	private final Double width;
	private final Double height;
	private final RenderingEngine<?> engine;
	private final OutputStream stream;

	//TODO: enable text font flag
	public SVGWriter(final RenderingEngine<?> engine, final OutputStream stream) {
		if (engine == null)
			throw new NullPointerException("Rendering Engine is null.");
		if (stream == null)
			throw new NullPointerException("Stream is null.");
		
		this.engine = engine;
		this.stream = stream;

		width = engine.getViewModel().getVisualProperty(BasicVisualLexicon.NETWORK_WIDTH);
		height = engine.getViewModel().getVisualProperty(BasicVisualLexicon.NETWORK_HEIGHT);

		logger.debug("SVG Writer created.");
	}


	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setProgress(0.0);
		taskMonitor.setStatusMessage("SVG Rendering Start...");
		logger.debug("SVG Rendering Start.");

		final SVGGraphics2D g = new SVGGraphics2D(stream, new Dimension(width.intValue(), height.intValue()));

		// this sets text as shape
		java.util.Properties p = new java.util.Properties();
		p.setProperty("org.freehep.graphicsio.AbstractVectorGraphicsIO.TEXT_AS_SHAPES",
				Boolean.toString(!exportTextAsFont));
		g.setProperties(p);
		taskMonitor.setProgress(0.2);
		g.startExport();
		engine.printCanvas(g);
		g.endExport();
		
		logger.debug("SVG Rendering DONE.");
		taskMonitor.setStatusMessage("SVG Rendering DONE.");		
		taskMonitor.setProgress(1.0);
	}

}
