package org.cytoscape.view.vizmap.gui.internal.view.editor.valueeditor;

/*
 * #%L
 * Cytoscape VizMap GUI Impl (vizmap-gui-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
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

import java.awt.Component;

import javax.swing.JOptionPane;

public class StringValueEditor extends AbstractValueEditor<String> {

	private static final String MESSAGE = "Enter a new text value:";
	private static final String TITLE = "Text Input";

	public StringValueEditor() {
		super(String.class);
	}

	@Override
	public String showEditor(Component parent, String initialValue) {
		return (String) JOptionPane.showInputDialog(parent, MESSAGE, TITLE, JOptionPane.PLAIN_MESSAGE, null,
				null, initialValue);
	}
}
