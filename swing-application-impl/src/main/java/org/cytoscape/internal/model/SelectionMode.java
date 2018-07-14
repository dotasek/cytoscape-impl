package org.cytoscape.internal.model;

import static org.cytoscape.internal.util.IconUtil.ANNOTATIONS;
import static org.cytoscape.internal.util.IconUtil.EDGES;
import static org.cytoscape.internal.util.IconUtil.NODES;
import static org.cytoscape.internal.util.IconUtil.SELECTION_MODE_ANNOTATIONS;
import static org.cytoscape.internal.util.IconUtil.SELECTION_MODE_EDGES;
import static org.cytoscape.internal.util.IconUtil.SELECTION_MODE_NODES;

/*
 * #%L
 * Cytoscape Swing Application Impl (swing-application-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2018 The Cytoscape Consortium
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

public enum SelectionMode {
	
	NODE_SELECTION(
			"Toggle Node Selection",
			SELECTION_MODE_NODES,
			NODES,
			"Turn off this option if you don't want nodes to be selected when they are clicked or drag-selected.",
			"/images/tooltips/selection-mode-nodes.gif",
			"NETWORK_NODE_SELECTION"
	),
	EDGE_SELECTION(
			"Toggle Edge Selection",
			SELECTION_MODE_EDGES,
			EDGES,
			"Turn off this option if you don't want edges to be selected when they are clicked or drag-selected.",
			"/images/tooltips/selection-mode-edges.gif",
			"NETWORK_EDGE_SELECTION"
	),
	ANNOTATION_SELECTION(
			"Toggle Annotation Selection",
			SELECTION_MODE_ANNOTATIONS,
			ANNOTATIONS,
			"Turn off this option if you don't want annotations to be selected when they are clicked or drag-selected.",
			null,
			"NETWORK_ANNOTATION_SELECTION"
	);
	
	private final String text;
	private final String selectedIconText;
	private final String unselectedIconText;
	private final String toolTipText;
	private final String toolTipImage;
	private final String propertyId;

	private SelectionMode(
			String text,
			String selectedIconText,
			String unselectedIconText,
			String toolTipText,
			String toolTipImage,
			String propertyId
	) {
		this.text = text;
		this.selectedIconText = selectedIconText;
		this.unselectedIconText = unselectedIconText;
		this.toolTipText = toolTipText;
		this.toolTipImage = toolTipImage;
		this.propertyId = propertyId;
	}
	
	public String getText() {
		return text;
	}
	
	public String getSelectedIconText() {
		return selectedIconText;
	}
	
	public String getUnselectedIconText() {
		return unselectedIconText;
	}
	
	public String getPropertyId() {
		return propertyId;
	}
	
	public String getToolTipText() {
		return toolTipText;
	}
	
	public String getToolTipImage() {
		return toolTipImage;
	}
}