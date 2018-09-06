package org.cytoscape.ding.impl.cyannotator.create;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JDialog;

import org.cytoscape.ding.impl.DGraphView;
import org.cytoscape.ding.impl.cyannotator.annotations.ShapeAnnotationImpl;
import org.cytoscape.ding.impl.cyannotator.dialogs.ShapeAnnotationDialog;
import org.cytoscape.ding.internal.util.IconUtil;
import org.cytoscape.ding.internal.util.ViewUtil;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.TextIcon;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
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

public class ShapeAnnotationFactory extends AbstractDingAnnotationFactory<ShapeAnnotation> {

	public static final String NAME = "Shape";

	private Icon icon;
	
	public ShapeAnnotationFactory(final CyServiceRegistrar serviceRegistrar) {
		super(ShapeAnnotation.class, serviceRegistrar);
	}
	
	@Override
	public JDialog createAnnotationDialog(DGraphView view, Point2D location) {
		return new ShapeAnnotationDialog(view, location, ViewUtil.getActiveWindow(view));
	}

	@Override
	public ShapeAnnotation createAnnotation(Class<? extends ShapeAnnotation> type, CyNetworkView view,
			Map<String, String> argMap) {
		if (!(view instanceof DGraphView) || !this.type.equals(type))
			return null;

		return new ShapeAnnotationImpl((DGraphView) view, argMap);
	}
	
	@Override
	public String getId() {
		return NAMESPACE + "Shape";
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Icon getIcon() {
		if (icon == null) {
			// Lazily initialize the icon here, because the LAF might not have been set yet
			// and we need to get the correct colors
			Font font = serviceRegistrar.getService(IconManager.class).getIconFont(IconUtil.CY_FONT_NAME, 16f);
			icon = new TextIcon(
					new String[] { IconUtil.ICON_ANNOTATION_SHAPE_1, IconUtil.ICON_ANNOTATION_SHAPE_2 },
					font,
					new Color[] { Color.BLACK, Color.WHITE },
					ICON_SIZE, ICON_SIZE,
					1
			);
		}
		
		return icon;
	}
}
