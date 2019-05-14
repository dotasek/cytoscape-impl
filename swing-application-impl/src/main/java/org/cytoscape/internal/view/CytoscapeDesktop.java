package org.cytoscape.internal.view;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static org.cytoscape.application.swing.CytoPanelState.DOCK;
import static org.cytoscape.application.swing.CytoPanelState.FLOAT;
import static org.cytoscape.application.swing.CytoPanelState.HIDE;
import static org.cytoscape.internal.view.CytoPanelNameInternal.BOTTOM;
import static org.cytoscape.internal.view.CytoPanelNameInternal.EAST;
import static org.cytoscape.internal.view.CytoPanelNameInternal.SOUTH;
import static org.cytoscape.internal.view.CytoPanelNameInternal.SOUTH_WEST;
import static org.cytoscape.internal.view.CytoPanelNameInternal.WEST;
import static org.cytoscape.internal.view.util.ViewUtil.invokeOnEDT;
import static org.cytoscape.internal.view.util.ViewUtil.invokeOnEDTAndWait;
import static org.cytoscape.internal.view.util.ViewUtil.isScreenMenuBar;
import static org.cytoscape.util.swing.LookAndFeelUtil.getSmallFontSize;
import static org.cytoscape.util.swing.LookAndFeelUtil.isNimbusLAF;
import static org.cytoscape.util.swing.LookAndFeelUtil.isWinLAF;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicButtonUI;

import org.cytoscape.app.event.AppsFinishedStartingEvent;
import org.cytoscape.app.event.AppsFinishedStartingListener;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyShutdown;
import org.cytoscape.application.events.CyStartEvent;
import org.cytoscape.application.events.CyStartListener;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.application.swing.ToolBarComponent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.application.swing.events.CytoPanelStateChangedEvent;
import org.cytoscape.application.swing.events.CytoPanelStateChangedListener;
import org.cytoscape.internal.command.CommandToolPanel;
import org.cytoscape.internal.util.CoalesceTimer;
import org.cytoscape.internal.view.util.ToggleableButtonGroup;
import org.cytoscape.internal.view.util.VerticalButtonUI;
import org.cytoscape.internal.view.util.ViewUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.events.TableAddedEvent;
import org.cytoscape.model.events.TableAddedListener;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.session.events.SessionSavedEvent;
import org.cytoscape.session.events.SessionSavedListener;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.util.swing.TextIcon;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.swing.StatusBarPanelFactory;

/*
 * #%L
 * Cytoscape Swing Application Impl (swing-application-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2019 The Cytoscape Consortium
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

/**
 * The CytoscapeDesktop is the central Window for working with Cytoscape.<br><br>
 * Layout:<br><br>
 * <b>1. MASTER Pane:</b>
 * <pre>
 *  ___________________________
 * |         TOP Pane          |
 * |___________________________|
 * |        Automation         |
 * |___________________________|
 * </pre>
 * <b>2. TOP Pane:</b>
 * <pre>
 *  ___________________________
 * | LEFT Pane |  RIGHT Pane   |
 * |___________|_______________|
 * </pre>
 * <b>3. LEFT Pane:</b>
 * <pre>
 *  ___________
 * |    NW     |
 * |___________|
 * |    SW     |
 * |___________|
 * </pre>
 * <b>4. RIGHT Pane:</b>
 * <pre>
 *  ________________
 * | TOP-RIGHT Pane |
 * |________________|
 * |       S        |
 * |________________|
 * </pre>
 * <b>5. TOP-RIGHT Pane:</b>
 * <pre>
 *  ________________
 * |   C    |   E   |
 * |________|_______|
 * </pre>
 */
@SuppressWarnings("serial")
public class CytoscapeDesktop extends JFrame
		implements CySwingApplication, CyStartListener, AppsFinishedStartingListener, SessionLoadedListener,
		SessionSavedListener, SetCurrentNetworkListener, SetCurrentNetworkViewListener, TableAddedListener,
		CytoPanelStateChangedListener, CytoPanelComponentSelectedListener {

	private static final String TITLE_PREFIX_STRING = "Session: ";
	private static final String NEW_SESSION_NAME = "New Session";

	static final Dimension DEF_DESKTOP_SIZE = new Dimension(1300, 850);
	private static final int DEF_DIVIDER_LOATION = 540;

	private static final String SMALL_ICON = "/images/logo.png";
	
	/**
	 * The CyMenus object provides access to the all of the menus and toolbars
	 * that will be needed.
	 */
	protected CytoscapeMenus cyMenus;

	/**
	 * The NetworkViewManager can support three types of interfaces.
	 * Tabbed/InternalFrame/ExternalFrame
	 */
	protected NetworkViewMediator netViewMediator;

	private SideBar westSideBar;
	private SideBar eastSideBar;
	private SideBar southSideBar;
	private ComponentPopup popup;
	/** This button group is used with buttons from all CytoPanels in state HIDE. */
	private final ToggleableButtonGroup minimizedButtonGroup;
	private boolean isAdjusting;
	
	private BiModalJSplitPane masterPane;
	private BiModalJSplitPane topPane;
	private BiModalJSplitPane leftPane;
	private BiModalJSplitPane rightPane;
	private BiModalJSplitPane topRightPane;
	
	private CytoPanelImpl northWestPanel;
	private CytoPanelImpl southWestPanel;
	private CytoPanelImpl eastPanel;
	private CytoPanelImpl southPanel;
	private CytoPanelImpl automationPanel;

	// Status Bar TODO: Move this to log-swing to avoid cyclic dependency.
	private JPanel mainPanel;
	private JPanel centerPanel;
	private JPanel bottomPanel;
	private JToolBar statusToolBar;
	private StarterPanel starterPanel;
	private StatusBarPanelFactory taskStatusPanelFactory;
	private StatusBarPanelFactory jobStatusPanelFactory;
	
	private final GlassPaneMouseListener glassPaneMouseListener = new GlassPaneMouseListener();
	
	/** User preferred sizes for each cytopanel popup, to be set when the user manually resizes a popup. */
	private final Map<CytoPanelNameInternal, Dimension> popupPreferredSizes = new HashMap<>();
	
	private final CoalesceTimer resizeEventTimer = new CoalesceTimer(200, 1);
	
	// These Control Panel components must respect this order
	private List<String> controlComponentsOrder = Arrays.asList(
			"org.cytoscape.Network",
			"org.cytoscape.Style",
			"org.cytoscape.Filter",
			"org.cytoscape.Annotation"
	);
	
	/** Holds frames that contain floating CytoPanels */
	private final  Map<CytoPanel, JFrame> floatingFrames = new HashMap<>();
	private boolean ignoreFloatingFrameCloseEvents;
	
	private final CyServiceRegistrar serviceRegistrar;

	public CytoscapeDesktop(
			final CytoscapeMenus cyMenus,
			final NetworkViewMediator netViewMediator,
			final CyServiceRegistrar serviceRegistrar
	) {
		super(TITLE_PREFIX_STRING + NEW_SESSION_NAME);

		this.cyMenus = cyMenus;
		this.netViewMediator = netViewMediator;
		this.serviceRegistrar = serviceRegistrar;
		
		final DialogTaskManager taskManager = serviceRegistrar.getService(DialogTaskManager.class);
		taskManager.setExecutionContext(this);

		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource(SMALL_ICON)));

		if (MacFullScreenEnabler.supportsNativeFullScreenMode())
			MacFullScreenEnabler.setEnabled(this, true);

		// Don't automatically close window. Let shutdown.exit(returnVal) handle this
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		setJMenuBar(cyMenus.getJMenuBar());
		
		((JComponent) getGlassPane()).setLayout(null);
		
		minimizedButtonGroup = new ToggleableButtonGroup(true) {
			@Override
			public void setSelected(ButtonModel model, boolean selected) {
				if (isAdjusting || selected == isSelected(model))
					return;
				
				isAdjusting = true;
				onSidebarButtonSelected((SidebarToggleButton) getButton(model), selected);
				super.setSelected(model, selected);
				isAdjusting = false;
			}
		};
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				// This is necessary because the same menu bar can be used by other frames
				final JMenuBar menuBar = cyMenus.getJMenuBar();
				final Window window = SwingUtilities.getWindowAncestor(menuBar);
				
				if (!CytoscapeDesktop.this.equals(window)) {
					if (window instanceof JFrame && !isScreenMenuBar()) {
						// Do this first, or the user could see the menu disappearing from the out-of-focus windows
						final JMenuBar dummyMenuBar = cyMenus.createDummyMenuBar();
						((JFrame) window).setJMenuBar(dummyMenuBar);
						dummyMenuBar.updateUI();
						window.repaint();
					}
					
					if (isScreenMenuBar())
						cyMenus.setMenuBarVisible(true);
					
					setJMenuBar(menuBar);
					menuBar.updateUI();
				}
				
				taskManager.setExecutionContext(CytoscapeDesktop.this);
			}
			@Override
			public void windowClosing(WindowEvent we) {
				final CyShutdown cyShutdown = serviceRegistrar.getService(CyShutdown.class);
				cyShutdown.exit(0);
			}
		});

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent evt) {
				// We need to do this later in the cycle to make sure everything is loaded
				if (jobStatusPanelFactory == null || taskStatusPanelFactory == null) {
					jobStatusPanelFactory = serviceRegistrar.getService(StatusBarPanelFactory.class, "(type=JobStatus)");
					taskStatusPanelFactory = serviceRegistrar.getService(StatusBarPanelFactory.class, "(type=TaskStatus)");
					statusToolBar = setupStatusPanel(jobStatusPanelFactory, taskStatusPanelFactory);
				}
			}
			@Override
			public void componentResized(ComponentEvent evt) {
				if (isVisible())
					resizeEventTimer.coalesce(() -> {
						invokeOnEDT(() -> {
							// Update the sidebars
							for (CytoPanelImpl cp : getAllCytoPanels()) {
								SideBar.TrimStack ts = getTrimStackOf(cp);
								
								if (ts != null && !cp.isRemoved())
									ts.update(); // TODO Probably too expensive to update everything here!!!
							}
						});
					});
			}
		});

		setContentPane(getMainPanel());
		
		// Prepare to show the desktop...
		for (CytoPanelImpl cp : getAllCytoPanels()) {
			if (cp.getState() == HIDE)
				minimizeCytoPanel(cp);
			else
				addToCytoPanelButtonGroup(cp);
			
			addListeners(cp);
		}
		
		pack();
		setSize(DEF_DESKTOP_SIZE);
		
		// Move it to the center
		setLocationRelativeTo(null);

		// ...but don't actually show it!!!!
		// Once the system has fully started the JFrame will be set to 
		// visible by the StartupMostlyFinished class, found elsewhere.
	}

	private List<CytoPanelImpl> getAllCytoPanels() {
		return Arrays.asList(getNorthWestPanel(), getSouthWestPanel(), getEastPanel(), getSouthPanel(),
				getAutomationPanel());
	}
	
	private void addListeners(CytoPanelImpl cp) {
		cp.getFloatButton().addActionListener(evt -> {
			if (cp.getState() == FLOAT || cp.getState() == HIDE)
				cp.setState(DOCK);
			else
				cp.setState(FLOAT);
		});
		cp.getMinimizeButton().addActionListener(evt -> {
			if (cp.getState() != HIDE) {
				// Set the HIDE state, as expected
				cp.setState(HIDE);
			} else {
				// Force-hide it, because it already has the HIDE state, which means the event would not be fired
				disposeComponentPopup();
				
				isAdjusting = true;
				minimizedButtonGroup.clearSelection();
				isAdjusting = false;
			}
		});
		cp.getTitlePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2)
					toggleMaximizeCytoPanel(cp);
			}
		});
	}

	private JToolBar setupStatusPanel(StatusBarPanelFactory jobStatusPanelFactory,
			StatusBarPanelFactory taskStatusPanelFactory) {
		final JPanel taskStatusPanel = taskStatusPanelFactory.createTaskStatusPanel();
		final JPanel jobStatusPanel = jobStatusPanelFactory.createTaskStatusPanel();
		final MemStatusPanel memStatusPanel = new MemStatusPanel(serviceRegistrar);
		
		final JToolBar statusToolBar = new JToolBar();
		statusToolBar.setFloatable(false);
		statusToolBar.setBorder(BorderFactory.createEmptyBorder());
		
		if (isNimbusLAF()) {
			jobStatusPanel.setOpaque(false);
			taskStatusPanel.setOpaque(false);
			statusToolBar.setOpaque(false);
			memStatusPanel.setOpaque(false);
		}

		final JPanel statusPanel = new JPanel();
		statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")));
		
		final GroupLayout layout = new GroupLayout(statusPanel);
		statusPanel.setLayout(layout);
		layout.setAutoCreateContainerGaps(false);
		layout.setAutoCreateGaps(false);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(jobStatusPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(taskStatusPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addComponent(statusToolBar, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addComponent(memStatusPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
				.addContainerGap()
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGap(isWinLAF() ? 5 : 0)
				.addGroup(layout.createParallelGroup(Alignment.CENTER, true)
						.addComponent(jobStatusPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(taskStatusPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(statusToolBar, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
						.addComponent(memStatusPanel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
				)
				.addGap(isWinLAF() ? 5 : 0)
		);
		
		getBottomPanel().add(statusPanel, BorderLayout.SOUTH);

		return statusToolBar;
	}

	public void addAction(CyAction action, Map<?, ?> props) {
		cyMenus.addAction(action, props);
	}

	@Override
	public void addAction(CyAction action) {
		cyMenus.addAction(action, new HashMap<>());
	}

	public void removeAction(CyAction action, Map<?, ?> props) {
		cyMenus.removeAction(action);
	}

	@Override
	public void removeAction(CyAction action) {
		cyMenus.removeAction(action);
	}

	@Override
	public JMenu getJMenu(String name) {
		return cyMenus.getJMenu(name);
	}

	@Override
	public JMenuBar getJMenuBar() {
		return cyMenus.getJMenuBar();
	}

	@Override
	public JToolBar getJToolBar() {
		return cyMenus.getJToolBar();
	}

	@Override
	public JFrame getJFrame() {
		return this;
	}

	@Override
	public CytoPanel getCytoPanel(final CytoPanelName compassDirection) {
		return getCytoPanelInternal(CytoPanelNameInternal.valueOf(compassDirection));
	}
	
	public CytoPanel getCytoPanel(final CytoPanelNameInternal compassDirection) {
		return getCytoPanelInternal(compassDirection);
	}

	private CytoPanelImpl getCytoPanelInternal(final CytoPanelNameInternal compassDirection) {
		switch (compassDirection) {
			case BOTTOM:
				return getAutomationPanel();
			case SOUTH:
				return getSouthPanel();
			case EAST:
				return getEastPanel();
			case WEST:
				return getNorthWestPanel();
			case SOUTH_WEST:
				return getSouthWestPanel();
		}

		throw new IllegalArgumentException(
				"Illegal Argument:  " + compassDirection + ".  Must be one of:  {SOUTH,EAST,WEST,SOUTH_WEST}.");
	}

	public void addCytoPanelComponent(CytoPanelComponent cpc, Map<?, ?> props) {
		invokeOnEDTAndWait(() -> {
			final CytoPanelImpl cytoPanel;
			
			if (cpc instanceof CommandToolPanel)
				cytoPanel = getCytoPanelInternal(BOTTOM);
			else
				cytoPanel = getCytoPanelInternal(CytoPanelNameInternal.valueOf(cpc.getCytoPanelName()));

			int index = getInsertIndex(cpc, cytoPanel);
			cytoPanel.insert(cpc, index);
			getTrimStackOf(cytoPanel).update();
		});
	}

	public void removeCytoPanelComponent(CytoPanelComponent cpc, Map<?, ?> props) {
		invokeOnEDTAndWait(() -> {
			final CytoPanelImpl cytoPanel;
			
			if (cpc instanceof CommandToolPanel)
				cytoPanel = getCytoPanelInternal(BOTTOM);
			else
				cytoPanel = getCytoPanelInternal(CytoPanelNameInternal.valueOf(cpc.getCytoPanelName()));
			
			cytoPanel.remove(cpc);
			getTrimStackOf(cytoPanel).update();
			getSideBarOf(cytoPanel).update();
			
			if (cytoPanel.getCytoPanelComponentCount() == 0)
				cytoPanel.setState(HIDE);
		});
	}

	@Override
	public JToolBar getStatusToolBar() {
		return statusToolBar;
	}

	public void addToolBarComponent(ToolBarComponent tp, Map<?, ?> props) {
		invokeOnEDTAndWait(() -> {
			((CytoscapeToolBar) cyMenus.getJToolBar()).addToolBarComponent(tp);
		});
	}

	public void removeToolBarComponent(ToolBarComponent tp, Map<?, ?> props) {
		invokeOnEDTAndWait(() -> {
			((CytoscapeToolBar) cyMenus.getJToolBar()).removeToolBarComponent(tp);
		});
	}
	
	@Override
	public void handleEvent(CyStartEvent e) {
		invokeOnEDT(() -> {
			setVisible(true);
			toFront();
		});
	}
	
	@Override
	public void handleEvent(AppsFinishedStartingEvent e) {
		invokeOnEDT(() -> {
			// Only show Starter Panel the first time if the initial session is empty
			// (for instance, Cystoscape can start up with a session file specified through the terminal)
			final CyNetworkManager netManager = serviceRegistrar.getService(CyNetworkManager.class);
			final CyTableManager tableManager = serviceRegistrar.getService(CyTableManager.class);
			
			if (netManager.getNetworkSet().isEmpty() && tableManager.getAllTables(false).isEmpty())
				showStarterPanel();
		});
	}
	
	@Override
	public void handleEvent(SessionLoadedEvent e) {
		// Update window title
		String sessionName = e.getLoadedFileName();
		
		if (sessionName == null)
			sessionName = NEW_SESSION_NAME;
		
		final String title = TITLE_PREFIX_STRING + sessionName;
		
		invokeOnEDT(() -> {
			setTitle(title);
			hideStarterPanel();
		});
	}
	
	@Override
	public void handleEvent(SessionSavedEvent e) {
		// Update window title
		final String sessionName = e.getSavedFileName();
		invokeOnEDT(() -> setTitle(TITLE_PREFIX_STRING + sessionName));
	}
	
	@Override
	public void handleEvent(SetCurrentNetworkEvent e) {
		invokeOnEDT(() -> {
			if (e.getNetwork() != null && !isCommandDocGenNetwork(e.getNetwork()))
				hideStarterPanel();
		});
	}
	
	@Override
	public void handleEvent(SetCurrentNetworkViewEvent e) {
		invokeOnEDT(() -> {
			if (e.getNetworkView() != null && !isCommandDocGenNetwork(e.getNetworkView().getModel()))
				hideStarterPanel();
		});
	}
	
	private boolean isCommandDocGenNetwork(CyNetwork network) {
		if (network != null) {
			String name = network.getRow(network).get(CyNetwork.NAME, String.class);
			//Note: DO NOT CHANGE THE NETWORK NAME FROM "cy:command_documentation_generation", it is necessary for a workaround.
			return name != null && name.equals("cy:command_documentation_generation");
		}
		
		return false;
	}
	
	@Override
	public void handleEvent(TableAddedEvent e) {
		CyTable table = e.getTable();
		
		// It does not make sense to hide the Starter Panel when the table is private, because the user will not see it.
		// Also apps can create private tables during initialization, which would hide the Starter Panel by accident.
		if (table != null && table.isPublic()) {
			CyNetwork network = serviceRegistrar.getService(CyApplicationManager.class).getCurrentNetwork();
			
			if (network != null && !isCommandDocGenNetwork(network))
				invokeOnEDT(() -> hideStarterPanel());
		}
	}
	
	@Override
	public void handleEvent(CytoPanelStateChangedEvent e) {
		if (e.getCytoPanel() instanceof CytoPanelImpl == false)
			return;
		
		CytoPanelImpl cytoPanel = (CytoPanelImpl) e.getCytoPanel();
		CytoPanelState state = e.getNewState();

		switch (state) {
			case HIDE:
				minimizeCytoPanel(cytoPanel);
				break;
			case FLOAT:
				floatCytoPanel(cytoPanel);
				break;
			case DOCK:
				dockCytoPanel(cytoPanel);
				break;
		}
	}
	
	@Override
	public void handleEvent(CytoPanelComponentSelectedEvent evt) {
		CytoPanel cp = evt.getCytoPanel();
		
		if (cp instanceof CytoPanelImpl && evt.getSelectedIndex() >= 0) {
			SideBar.TrimStack ts = getTrimStackOf((CytoPanelImpl) cp);
			
			if (ts != null) {
				JToggleButton btn = ts.getButton(evt.getSelectedIndex());
				
				if (btn != null && !btn.isSelected()) {
					if (minimizedButtonGroup.contains(btn))
						minimizedButtonGroup.setSelected(btn, true);
					else
						ts.getButtonGroup().setSelected(btn, true);
				}
			}
		}
	}
	
	public void showStarterPanel() {
		// TODO check removed and updated preferred height
		getCenterPanel().add(getStarterPanel(), StarterPanel.NAME);
		getStarterPanel().update();
		((CardLayout) getCenterPanel().getLayout()).show(getCenterPanel(), StarterPanel.NAME);
	}
	
	public void hideStarterPanel() {
		if (isStarterPanelVisible()) {
			getCenterPanel().remove(getStarterPanel());
			((CardLayout) getCenterPanel().getLayout()).show(getCenterPanel(), NetworkViewMainPanel.NAME);
		}
	}
	
	public boolean isStarterPanelVisible() {
		return getStarterPanel().isVisible();
	}
	
	public boolean isShowSideBarLabels() {
		return Boolean.parseBoolean(ViewUtil.getViewProperty("sideBar.showLabels", "true", serviceRegistrar));
	}
	
	public void setShowSideBarLabels(boolean show) {
		ViewUtil.setViewProperty("sideBar.showLabels", "" + show, serviceRegistrar);
			
		for (CytoPanelImpl cp : getAllCytoPanels()) {
			SideBar.TrimStack ts = getTrimStackOf(cp);
			
			if (ts != null)
				ts.update();
		}
	}
	
	/**
	 * Only invoke this method when the CytoPanel is removed by a user action,
	 * because this also updates a CyProperty.
	 */
	public void removeCytoPanel(CytoPanelImpl cytoPanel) {
		// We have to set it to HIDE first, for backwards compatibility--apps expect
		// invisible cytopanels to have the HIDE state, no matter whether it's
		// minimized or removed from the desktop
		cytoPanel.setState(HIDE);
		cytoPanel.setRemoved(true);
		getTrimStackOf(cytoPanel).update();
		getSideBarOf(cytoPanel).update();
		
		if (popup != null && isUnpinned(cytoPanel)) {
			popup.getContentPane().removeAll();
			popup.setVisible(false);
		}
	}

	public void showCytoPanel(CytoPanelImpl cytoPanel) {
		if (cytoPanel.getCytoPanelComponentCount() == 0)
			return;
		
		if (cytoPanel.isRemoved()) {
			cytoPanel.setRemoved(false);
			cytoPanel.setState(DOCK);
		}
		
		getTrimStackOf(cytoPanel).update();
		getSideBarOf(cytoPanel).update();
	}
	
	private void floatCytoPanel(CytoPanelImpl cytoPanel) {
		if (popup != null && isUnpinned(cytoPanel)) {
			disposeComponentPopup();
			addToCytoPanelButtonGroup(cytoPanel);
		}
		
		showCytoPanel(cytoPanel);

		if (!isFloating(cytoPanel)) {
			BiModalJSplitPane splitPane = getSplitPaneOf(cytoPanel);
			
			if (splitPane != null)
				splitPane.removeCytoPanel(cytoPanel);
			
			// New window to place this CytoPanel
			JFrame frame = new JFrame(cytoPanel.getTitle(), getGraphicsConfiguration());
			floatingFrames.put(cytoPanel, frame);
			
			// Add listener to handle when window is closed
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					if (!ignoreFloatingFrameCloseEvents)
						cytoPanel.setState(DOCK);
				}
			});

			if (cytoPanel.getThisComponent().getSize() != null) {
				frame.getContentPane().setPreferredSize(cytoPanel.getThisComponent().getSize());
				frame.getContentPane().setSize(cytoPanel.getThisComponent().getSize());
			}
			
			//  Add CytoPanel to the New External Window
			frame.getContentPane().add(cytoPanel.getThisComponent(), BorderLayout.CENTER);
			frame.pack();
			
			// Show it
			setLocationOfFloatingFrame(frame, cytoPanel);
			frame.setVisible(true);

			if (splitPane != null)
				splitPane.update();
		}
	}

	private void dockCytoPanel(CytoPanelImpl cytoPanel) {
		if (popup != null && isUnpinned(cytoPanel))
			disposeComponentPopup();
		
		// Make sure the button selection is correct
		addToCytoPanelButtonGroup(cytoPanel); // Make sure it belongs to the correct button group
		
		SideBar.TrimStack ts = getTrimStackOf(cytoPanel);
		JToggleButton selBtn = ts != null ? ts.getButton(cytoPanel.getSelectedIndex()) : null;
		
		if (selBtn != null)
			selBtn.setSelected(true);
		
		BiModalJSplitPane splitPane = getSplitPaneOf(cytoPanel);
		
		if (isFloating(cytoPanel))
			disposeFloatingCytoPanel(cytoPanel);

		showCytoPanel(cytoPanel);
		
		if (splitPane != null)
			splitPane.addCytoPanel(cytoPanel);

		if (splitPane != null)
			splitPane.update();
	}
	
	private void toggleMaximizeCytoPanel(CytoPanelImpl cytoPanel) {
		// For now, it can be maximized only when unpinned
		if (popup != null && isUnpinned(cytoPanel)) {
			cytoPanel.setMaximized(!cytoPanel.isMaximized());
			updateComponentPopupBounds();
		}
	}

	private void minimizeCytoPanel(CytoPanelImpl cytoPanel) {
		if (isFloating(cytoPanel)) {
			disposeFloatingCytoPanel(cytoPanel);
		} else if (popup != null && isUnpinned(cytoPanel)) {
			popup.getContentPane().removeAll();
			popup.setVisible(false);
			minimizedButtonGroup.clearSelection();
		}
		
		// Adjust button selection
		SideBar.TrimStack ts = getTrimStackOf(cytoPanel);
		ts.getButtonGroup().clearSelection();
		
		addToMinimizedButtonGroup(cytoPanel);
		
		BiModalJSplitPane splitPane = getSplitPaneOf(cytoPanel);
		
		if (splitPane != null)
			splitPane.update();
	}
	
	private void disposeFloatingCytoPanel(CytoPanelImpl cytoPanel) {
		JFrame frame = floatingFrames.remove(cytoPanel);
		
		if (frame != null) {
			frame.remove(cytoPanel.getThisComponent());
			ignoreFloatingFrameCloseEvents = true;
			
			try {
				frame.dispose();
			} finally {
				ignoreFloatingFrameCloseEvents = false;
			}
		}
	}

	private boolean isFloating(CytoPanelImpl cytoPanel) {
		JFrame frame = floatingFrames.get(cytoPanel);
		
		return frame != null && frame == SwingUtilities.getWindowAncestor(cytoPanel.getThisComponent());
	}
	
	private boolean isUnpinned(CytoPanelImpl cytoPanel) {
		return popup != null && popup.getCytoPanel().equals(cytoPanel);
	}
	
	private void addToMinimizedButtonGroup(CytoPanelImpl cytoPanel) {
		List<SidebarToggleButton> buttons = getSidebarButtons(cytoPanel);
		SideBar.TrimStack ts = getTrimStackOf(cytoPanel);
		
		if (ts != null)
			ts.getButtonGroup().remove(buttons);
		
		minimizedButtonGroup.add(buttons);
	}
	
	private void addToCytoPanelButtonGroup(CytoPanelImpl cytoPanel) {
		List<SidebarToggleButton> buttons = getSidebarButtons(cytoPanel);
		minimizedButtonGroup.remove(buttons);
		
		SideBar.TrimStack ts = getTrimStackOf(cytoPanel);
		
		if (ts != null)
			ts.getButtonGroup().add(buttons);
	}
	
	private List<SidebarToggleButton> getSidebarButtons(CytoPanelImpl cytoPanel) {
		SideBar.TrimStack ts = getTrimStackOf(cytoPanel);
		
		return ts != null ? ts.getAllButtons() : Collections.emptyList();
	}
	
	private void setLocationOfFloatingFrame(JFrame frame, CytoPanelImpl cytoPanel) {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension screenDimension = tk.getScreenSize();

		// Get Absolute Location and Bounds, relative to Screen
		Rectangle bounds = getBounds();
		bounds.setLocation(getLocationOnScreen());

		Point p = CytoPanelUtil.getLocationOfExternalWindow(screenDimension, bounds, frame.getSize(),
				cytoPanel.getCytoPanelNameInternal(), false);
		frame.setLocation(p);
		frame.setVisible(true);
	}
	
	private int getInsertIndex(CytoPanelComponent cpc, CytoPanelImpl cytoPanel) {
		int index = -1;
		int total = cytoPanel.getCytoPanelComponentCount();
		
		if (cytoPanel.getCytoPanelName() == CytoPanelName.WEST && cpc instanceof CytoPanelComponent2) {
			String id = ((CytoPanelComponent2) cpc).getIdentifier();
			index = controlComponentsOrder.indexOf(id);
			
			// If any of the next components have been inserted already, add the new one before that one
			if (index >= 0 && controlComponentsOrder.size() > index + 1) {
				for (int i = index + 1; i < controlComponentsOrder.size(); i++) {
					String nextId = controlComponentsOrder.get(i);
					int nextIndex = cytoPanel.indexOfComponent(nextId);
					
					if (nextIndex < 0)
						continue;
					
					if (index >= nextIndex) {
						index = nextIndex;
						break;
					}
				}
			}
		}
		
		if (index < 0 || index > total)
			index = total;
		
		return index;
	}
	
	private void onSidebarButtonSelected(SidebarToggleButton btn, boolean selected) {
		CytoPanelImpl cytoPanel = btn.getCytoPanel();
		
		if (selected) {
			cytoPanel.setSelectedIndex(btn.getIndex());
			
			if (cytoPanel.getState() == HIDE) {
				showComponentPopup(cytoPanel, btn.getIndex());
			} else if (cytoPanel.getState() == FLOAT) {
				JFrame frame = floatingFrames.get(cytoPanel);
				
				if (frame != null)
					frame.toFront();
			}
		} else if (cytoPanel.getState() == HIDE) {
			disposeComponentPopup();
		}
	}
	
	private BiModalJSplitPane getSplitPaneOf(CytoPanelImpl cytoPanel) {
		switch (cytoPanel.getCytoPanelNameInternal()) {
			case BOTTOM:
				return getMasterPane();
			case SOUTH:
				return getRightPane();
			case EAST:
				return getTopRightPane();
			case WEST:
				return getLeftPane();
			case SOUTH_WEST:
				return getLeftPane();
			default:
				return null;
		}
	}
	
	private SideBar getSideBarOf(CytoPanelImpl cytoPanel) {
		switch (cytoPanel.getCytoPanelNameInternal()) {
			case SOUTH:
			case BOTTOM:
				return getSouthSideBar();
			case WEST:
			case SOUTH_WEST:
				return getWestSideBar();
			case EAST:
			default:
				return getEastSideBar();
		}
	}
	
	private SideBar.TrimStack getTrimStackOf(CytoPanelImpl cytoPanel) {
		SideBar bar = getSideBarOf(cytoPanel);
		
		return bar != null ? bar.getStack(cytoPanel) : null;
	}
	
	private JPanel getMainPanel() {
		if (mainPanel == null) {
			mainPanel = new JPanel();
			mainPanel.setLayout(new BorderLayout());
			mainPanel.add(cyMenus.getJToolBar(), BorderLayout.NORTH);
			mainPanel.add(getMasterPane(), BorderLayout.CENTER);
			mainPanel.add(getWestSideBar(), BorderLayout.WEST);
			mainPanel.add(getEastSideBar(), BorderLayout.EAST);
			mainPanel.add(getBottomPanel(), BorderLayout.SOUTH);
		}
		
		return mainPanel;
	}
	
	private SideBar getWestSideBar() {
		if (westSideBar == null) {
			westSideBar = new SideBar(SwingConstants.WEST, getNorthWestPanel(), getSouthWestPanel());
		}
		
		return westSideBar;
	}
	
	private SideBar getEastSideBar() {
		if (eastSideBar == null) {
			eastSideBar = new SideBar(SwingConstants.EAST, getEastPanel());
		}
		
		return eastSideBar;
	}
	
	private SideBar getSouthSideBar() {
		if (southSideBar == null) {
			southSideBar = new SideBar(SwingConstants.SOUTH, getAutomationPanel(), getSouthPanel());
		}
		
		return southSideBar;
	}
	
	private BiModalJSplitPane getMasterPane() {
		if (masterPane == null) {
			masterPane = new BiModalJSplitPane(
					JSplitPane.VERTICAL_SPLIT,
					getTopPane(),
					getAutomationPanel().getThisComponent()
			);
			masterPane.setDividerLocation(600);
		}
		return masterPane;
	}

	private BiModalJSplitPane getTopPane() {
		if (topPane == null) {
			topPane = new BiModalJSplitPane(
					JSplitPane.HORIZONTAL_SPLIT,
					getLeftPane(),
					getRightPane()
			);
			topPane.setDividerLocation(400);
			topPane.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent evt) {
					updateComponentPopupBounds();
				}
			});
		}

		return topPane;
	}
	
	private BiModalJSplitPane getLeftPane() {
		if (leftPane == null) {
			leftPane = new BiModalJSplitPane(
					JSplitPane.VERTICAL_SPLIT,
					getNorthWestPanel().getThisComponent(),
					getSouthWestPanel().getThisComponent()
			);
			leftPane.setResizeWeight(1.0);
		}
		
		return leftPane;
	}

	private BiModalJSplitPane getRightPane() {
		if (rightPane == null) {
			rightPane = new BiModalJSplitPane(
					JSplitPane.VERTICAL_SPLIT,
					getTopRightPane(),
					getSouthPanel().getThisComponent()
			);
			rightPane.setDividerLocation(DEF_DIVIDER_LOATION);
			rightPane.setResizeWeight(1.0);
		}

		return rightPane;
	}

	private BiModalJSplitPane getTopRightPane() {
		if (topRightPane == null) {
			topRightPane = new BiModalJSplitPane(
					JSplitPane.HORIZONTAL_SPLIT,
					getCenterPanel(),
					getEastPanel().getThisComponent()
			);
			topRightPane.setResizeWeight(1.0);
		}

		return topRightPane;
	}
	
	private JPanel getCenterPanel() {
		if (centerPanel == null) {
			centerPanel = new JPanel();
			centerPanel.setLayout(new CardLayout());
			
			// Null check is just for unit tests, because it's very hard to mock up the UI
			if (netViewMediator.getNetworkViewMainPanel() != null)
				centerPanel.add(netViewMediator.getNetworkViewMainPanel(), NetworkViewMainPanel.NAME);
		}
		
		return centerPanel;
	}
	
	private JPanel getBottomPanel() {
		if (bottomPanel == null) {
			bottomPanel = new JPanel(new BorderLayout());
			bottomPanel.add(getSouthSideBar());
		}
		
		return bottomPanel;
	}
	
	private StarterPanel getStarterPanel() {
		if (starterPanel == null) {
			starterPanel = new StarterPanel(serviceRegistrar);
			starterPanel.getCloseButton().addActionListener(e -> hideStarterPanel());
		}

		return starterPanel;
	}

	private CytoPanelImpl getNorthWestPanel() {
		if (northWestPanel == null) {
			northWestPanel = new CytoPanelImpl(WEST, 0, DOCK, serviceRegistrar);
		}

		return northWestPanel;
	}
	
	private CytoPanelImpl getSouthWestPanel() {
		if (southWestPanel == null) {
			southWestPanel = new CytoPanelImpl(SOUTH_WEST, 1, HIDE, serviceRegistrar);
		}
		
		return southWestPanel;
	}

	private CytoPanelImpl getEastPanel() {
		if (eastPanel == null) {
			eastPanel = new CytoPanelImpl(EAST, 2, HIDE, serviceRegistrar);
		}

		return eastPanel;
	}

	private CytoPanelImpl getSouthPanel() {
		if (southPanel == null) {
			southPanel = new CytoPanelImpl(SOUTH, 3, DOCK, serviceRegistrar);
		}

		return southPanel;
	}

	private CytoPanelImpl getAutomationPanel() {
		if (automationPanel == null) {
			automationPanel = new CytoPanelImpl(BOTTOM, 4, HIDE, serviceRegistrar);
		}
		
		return automationPanel;
	}
	
	private void showComponentPopup(CytoPanelImpl cytoPanel, int index) {
		if (index < 0) // Should not happen!
			return;
		
		if (popup == null) {
			popup = new ComponentPopup(cytoPanel);
			Toolkit.getDefaultToolkit().addAWTEventListener(glassPaneMouseListener, AWTEvent.MOUSE_EVENT_MASK);
		} else {
			popup.setCytoPanel(cytoPanel);
		}
		
		// Adjust button selection
		SideBar bar = getSideBarOf(cytoPanel);
		
		if (!bar.isShowing())
			return;
		
		SideBar.TrimStack trimStack = getTrimStackOf(cytoPanel);
		JToggleButton btn = trimStack.getButton(index);
		
		if (btn != null && !btn.isSelected()) {
			isAdjusting = true;
			btn.setSelected(true);
			isAdjusting = false;
		}
		
		// Show it
		updateComponentPopupBounds();
		((JComponent) getGlassPane()).add(popup);
		
		if (!getGlassPane().isVisible())
			getGlassPane().setVisible(true);
		
		if (!popup.isVisible()) // To avoid flickering
			popup.setVisible(true);
	}
	
	private void updateComponentPopupBounds() {
		if (popup == null)
			return;
		
		CytoPanelImpl cytoPanel = popup.getCytoPanel();
		
		if (cytoPanel == null)
			return;
		
		SideBar bar = getSideBarOf(cytoPanel);
		CytoPanelNameInternal name = cytoPanel.getCytoPanelNameInternal();
		
		try {
			Dimension maxDim = getTopPane().getSize();
			int maxWidth = maxDim.width;
			int maxHeight = maxDim.height;
			
			Dimension newDim = cytoPanel.isMaximized() ? maxDim : popupPreferredSizes.get(name);
			Dimension dim = newDim != null ? newDim : cytoPanel.getThisComponent().getPreferredSize();
			
			if (newDim == null) {
				if (name == SOUTH || name == BOTTOM) {
					dim.width = maxWidth;
					dim.height = (int) (maxHeight * 0.5f);
				} else {
					if (dim.width <= 0)
						dim.width = 200;
					if (dim.height <= 0)
						dim.height = maxHeight;
				}
			}
			
			// Max size
			dim.width = Math.min(dim.width, maxWidth);
			dim.height = Math.min(dim.height, maxHeight);
			// Min size
			dim.width = Math.max(dim.width, ComponentPopup.MIN_SIZE);
			dim.height = Math.max(dim.height, ComponentPopup.MIN_SIZE);
			
			Point p = bar.compassDirection == SwingConstants.SOUTH ? getBottomPanel().getLocation() : bar.getLocation();
			p = SwingUtilities.convertPoint(getMainPanel(), p, getGlassPane());
			
			if (bar.compassDirection == SwingConstants.WEST) {
				p.x += bar.getWidth();
				
				if (name == SOUTH_WEST) 
					p.y += (bar.getHeight() - dim.height);
			} else if (bar.compassDirection == SwingConstants.EAST) {
				p.x -= dim.width;
			} else if (bar.compassDirection == SwingConstants.SOUTH) {
				if (name == SOUTH) {
					p.x += (bar.getWidth() - dim.width);
					
					if (getEastSideBar().isShowing())
						p.x -= getEastSideBar().getPreferredSize().width;
				} else { // BOTTOM
					p.x += getWestSideBar().getPreferredSize().width;
				}
				
				p.y -= dim.height;
			}
			
			popup.setBounds(p.x, p.y, dim.width, dim.height);
			cytoPanel.getThisComponent().revalidate();
		} catch (Exception e) {
			// Just ignore...
		}
	}
	
	private void disposeComponentPopup() {
		if (popup != null) {
			Toolkit.getDefaultToolkit().removeAWTEventListener(glassPaneMouseListener);
			
			((JComponent) getGlassPane()).remove(popup);
			getGlassPane().setVisible(false);
			
			popup.getContentPane().removeAll();
			popup = null;
		}
	}
	
	private class SideBar extends JPanel {
		
		private final Map<CytoPanelImpl, TrimStack> stacks = new LinkedHashMap<>();
		private final int compassDirection;
		
		public SideBar(int compassDirection, CytoPanelImpl... cytoPanels) {
			this.compassDirection = compassDirection;
			
			Color borderColor = UIManager.getColor("Separator.foreground");
			
			if (compassDirection == SwingConstants.WEST)
				setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, borderColor));
			else if (compassDirection == SwingConstants.EAST)
				setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, borderColor));
			else if (compassDirection == SwingConstants.SOUTH)
				setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor));
			
			BoxLayout layout = new BoxLayout(this,
					compassDirection == SwingConstants.SOUTH ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS);
			setLayout(layout);
			
			// Create Trim Stacks
			int southPad = TrimStack.BTN_VPAD * 2 + CytoPanelUtil.BUTTON_SIZE;
			
			if (compassDirection == SwingConstants.SOUTH)
				add(Box.createHorizontalStrut(southPad));
			
			int i = 0;
			
			for (CytoPanelImpl cp : cytoPanels) {
				TrimStack ts = new TrimStack(cp);
				stacks.put(cp, ts);
				add(ts);
				
				if (i == 0)
					add(compassDirection == SwingConstants.SOUTH ? Box.createHorizontalGlue()
							: Box.createVerticalGlue());

				i++;
			}
			
			if (compassDirection == SwingConstants.SOUTH)
				add(Box.createHorizontalStrut(southPad));
			
			addMouseListener(new ContextMenuMouseListener());
		}
		
		TrimStack getStack(CytoPanelImpl cytoPanel) {
			return stacks.get(cytoPanel);
		}
		
		void update() {
			boolean visible = false;
			
			for (TrimStack ts : stacks.values()) {
				if (ts.isVisible()) {
					visible = true;
					break;
				}
			}
			
			setVisible(visible);
			
			if (isVisible())
				updateUI();
		}

		private class TrimStack extends JPanel {
			
			private static final int BTN_HPAD = 8;
			private static final int BTN_VPAD = 4;
			
			private final CytoPanelImpl cytoPanel;
			private final int orientation;
			private final List<SidebarToggleButton> buttons = new ArrayList<>();
			/** Used when the CytoPanel state is DOCK or FLOAT. */
			private final ToggleableButtonGroup buttonGroup;
			
			TrimStack(CytoPanelImpl cytoPanel) {
				this.cytoPanel = cytoPanel;
				this.orientation = compassDirection == SwingConstants.SOUTH ? SwingConstants.HORIZONTAL
						: SwingConstants.VERTICAL;
				
				BoxLayout layout = new BoxLayout(this,
						orientation == SwingConstants.VERTICAL ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS);
				setLayout(layout);
				
				addMouseListener(new ContextMenuMouseListener(cytoPanel));
				
				buttonGroup = new ToggleableButtonGroup() {
					@Override
					public void setSelected(ButtonModel model, boolean selected) {
						if (isAdjusting || selected == isSelected(model))
							return;
						
						isAdjusting = true;
						onSidebarButtonSelected((SidebarToggleButton) getButton(model), selected);
						super.setSelected(model, selected);
						isAdjusting = false;
					}
				};
				
				update();
			}
			
			SidebarToggleButton getButton(int index) {
				return index >= 0 && index < buttons.size() ? buttons.get(index) : null;
			}
			
			List<SidebarToggleButton> getAllButtons() {
				return new ArrayList<>(buttons);
			}
			
			ToggleableButtonGroup getButtonGroup() {
				return buttonGroup;
			}
			
			void update() {
				minimizedButtonGroup.remove(buttons);
				buttonGroup.remove(buttons);
				
				if (cytoPanel.isRemoved()) {
					setVisible(false);
					return;
				}
				
				// Remove all buttons
				removeAll();
				buttons.clear();
				
				// Create new buttons
				JPanel panel = null;
				final boolean showLabels = isShowSideBarLabels();
				int totalEdge = 0; // Total width or height of added buttons
				List<CytoPanelComponent> cpComponents = cytoPanel.getCytoPanelComponents();
				int i = 0;
				
				for (CytoPanelComponent cpc : cpComponents) {
					final int index = i++;
					String title = cpc.getTitle();
					Icon icon = cpc.getIcon();
					
					if ((title == null || title.trim().isEmpty()) && icon == null)
						continue;
					
					Icon buttonIcon = icon;
					
					if (buttonIcon == null) {
						buttonIcon = new TextIcon(
								"" + title.charAt(0),
								UIManager.getFont("Button.font").deriveFont(Font.BOLD),
								CytoPanelUtil.BUTTON_SIZE,
								CytoPanelUtil.BUTTON_SIZE
						);
					} else if (buttonIcon instanceof ImageIcon) {
						if (showLabels && buttonIcon.getIconHeight() > CytoPanelUtil.BUTTON_SIZE)
							buttonIcon = ViewUtil.resizeIcon(buttonIcon, CytoPanelUtil.BUTTON_SIZE);
						else if (!showLabels && (buttonIcon.getIconHeight() > CytoPanelUtil.BUTTON_SIZE
								|| buttonIcon.getIconWidth() > CytoPanelUtil.BUTTON_SIZE))
							buttonIcon = IconManager.resizeIcon(buttonIcon, CytoPanelUtil.BUTTON_SIZE);
					}

					SidebarToggleButton btn = new SidebarToggleButton(cytoPanel, index);
					btn.setIcon(buttonIcon);
					
					if (showLabels) {
						if (orientation == SwingConstants.VERTICAL)
							btn.setUI(new VerticalButtonUI(compassDirection == SwingConstants.EAST));
						else
							btn.setUI(new BasicButtonUI());
					}
					
					btn.setToolTipText(title);
					btn.setAlignmentX(CENTER_ALIGNMENT);
					
					Dimension d = btn.getPreferredSize();
					
					if (showLabels) {
						btn.setText(title);
						btn.setHorizontalAlignment(SwingConstants.CENTER);
						btn.setVerticalAlignment(SwingConstants.CENTER);
						d = btn.getPreferredSize();
					}
					
					if (orientation == SwingConstants.VERTICAL)
						d = new Dimension(d.width + 2 * BTN_VPAD, d.height + 2 * BTN_HPAD);
					else
						d = new Dimension(d.width + 2 * BTN_HPAD, d.height + 2 * BTN_VPAD);
					
					btn.setPreferredSize(d);
					btn.setMinimumSize(d);
					btn.setMaximumSize(d);
					btn.setSize(d);
					btn.addItemListener(evt -> ViewUtil.updateToolBarStyle(btn));
					btn.addMouseListener(new ContextMenuMouseListener(cytoPanel));
					ViewUtil.updateToolBarStyle(btn);
					
					buttons.add(btn);
					
					// Add all buttons to correct button group again
					if (cytoPanel.getState() == HIDE) {
						minimizedButtonGroup.add(btn);
					} else {
						buttonGroup.add(btn);
						
						// Restore button selection
						JToggleButton selBtn = getButton(cytoPanel.getSelectedIndex());
						
						if (selBtn != null)
							buttonGroup.setSelected(selBtn, true);
					}
					
					int buttonEdge = orientation == SwingConstants.VERTICAL ? d.height : d.width;
					
					if (panel != null && SideBar.this.getSize() != null) {
						int barEdge = orientation == SwingConstants.VERTICAL ?
								SideBar.this.getSize().height : SideBar.this.getSize().width;
						
						for (TrimStack ts : stacks.values()) {
							if (ts != this && ts.getSize() != null)
								barEdge -= orientation == SwingConstants.VERTICAL ?
										ts.getSize().height : ts.getSize().width;
						}
								
						if (totalEdge + buttonEdge > barEdge)
							panel = null;
					}
					
					if (panel == null) {
						panel = new JPanel();
						panel.setAlignmentX(cytoPanel.getCytoPanelName() == CytoPanelName.SOUTH ?
								RIGHT_ALIGNMENT : LEFT_ALIGNMENT);
						panel.setAlignmentY(TOP_ALIGNMENT);
						
						BoxLayout layout = new BoxLayout(panel,
								orientation == SwingConstants.VERTICAL ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS);
						panel.setLayout(layout);
						
						// When adding more columns/rows of buttons, the first buttons
						// should be closer to the CytoPanel, which is why the WEST SideBar
						// adds new columns to the left
						if (compassDirection == SwingConstants.WEST)
							add(panel, 0);
						else
							add(panel);
						
						totalEdge = 0;
					}
					
					totalEdge += buttonEdge;
					panel.add(btn);
				}
				
				setVisible(!buttons.isEmpty());
				
				if (isVisible()) {
					if (!showLabels)
						LookAndFeelUtil.equalizeSize(buttons.toArray(new JComponent[buttons.size()]));
					
					// Align columns/rows of buttons accordingly
					if (compassDirection == SwingConstants.WEST)
						add(Box.createHorizontalGlue(), 0);
					else if (compassDirection == SwingConstants.SOUTH)
						add(Box.createVerticalGlue());
					else
						add(Box.createHorizontalGlue());
					
					updateUI();
				}
			}
			
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 11;
				result = prime * result + getOuterType().hashCode();
				result = prime * result + ((cytoPanel == null) ? 0 : cytoPanel.hashCode());
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (!(obj instanceof TrimStack))
					return false;
				TrimStack other = (TrimStack) obj;
				if (!getOuterType().equals(other.getOuterType()))
					return false;
				if (cytoPanel == null) {
					if (other.cytoPanel != null)
						return false;
				} else if (!cytoPanel.equals(other.cytoPanel)) {
					return false;
				}
				return true;
			}

			private SideBar getOuterType() {
				return SideBar.this;
			}
		}
		
		private class ContextMenuMouseListener extends MouseAdapter {
			
			private final CytoPanelImpl cytoPanel;

			public ContextMenuMouseListener() {
				this(null);
			}
			
			public ContextMenuMouseListener(CytoPanelImpl cytoPanel) {
				this.cytoPanel = cytoPanel;
			}

			@Override
			public void mousePressed(MouseEvent evt) {
				maybeShowContextMenu(evt);
			}

			@Override
			public void mouseReleased(final MouseEvent evt) {
				maybeShowContextMenu(evt);
			}
			
			private void maybeShowContextMenu(MouseEvent evt) {
				if (!evt.isPopupTrigger())
					return;
				
				invokeOnEDT(() -> {
					final JPopupMenu menu = new JPopupMenu();
					
					if (cytoPanel != null) {
						final String title = cytoPanel.getTitle();
						{
							JMenuItem mi = new JMenuItem(CytoPanelImpl.TEXT_DOCK + " " + title);
							mi.addActionListener(e -> cytoPanel.setState(DOCK));
							mi.setEnabled(cytoPanel.getState() != DOCK);
							menu.add(mi);
						}
						{
							JMenuItem mi = new JMenuItem(CytoPanelImpl.TEXT_FLOAT + " " + title);
							mi.addActionListener(e -> cytoPanel.setState(FLOAT));
							mi.setEnabled(cytoPanel.getState() != FLOAT);
							menu.add(mi);
						}
						{
							JMenuItem mi = new JMenuItem(CytoPanelImpl.TEXT_HIDE + " " + title);
							mi.addActionListener(e -> cytoPanel.setState(HIDE));
							mi.setEnabled(cytoPanel.getState() != HIDE);
							menu.add(mi);
						}
						{
							JMenuItem mi = new JMenuItem(CytoPanelImpl.TEXT_REMOVE + " " + title);
							mi.addActionListener(e -> removeCytoPanel(cytoPanel));
							mi.setEnabled(!cytoPanel.isRemoved());
							menu.add(mi);
						}
						
						menu.addSeparator();
					}
					{
						JMenuItem mi = new JCheckBoxMenuItem("Show Labels");
						mi.addActionListener(e -> setShowSideBarLabels(mi.isSelected()));
						mi.setSelected(isShowSideBarLabels());
						menu.add(mi);
					}
					
					Component parent = (Component) evt.getSource();
					menu.show(parent, evt.getX(), evt.getY());
				});
			}
		}
	}
	
	private class SidebarToggleButton extends JToggleButton {
		
		private final CytoPanelImpl cytoPanel;
		private final int index;

		public SidebarToggleButton(CytoPanelImpl cytoPanel, int index) {
			this.cytoPanel = cytoPanel;
			this.index = index;
			
			setFont(getFont().deriveFont(getSmallFontSize()));
			setFocusPainted(false);
			setFocusable(false);
			setBorder(BorderFactory.createEmptyBorder());
			setContentAreaFilled(false);
			setOpaque(true);
		}
		
		CytoPanelImpl getCytoPanel() {
			return cytoPanel;
		}
		
		public int getIndex() {
			return index;
		}
		
		@Override
		public String toString() {
			return getText();
		}
	}
	
	private class ComponentPopup extends JRootPane {
		
		private static final int BORDER_WIDTH = 5;
		private static final int MIN_SIZE = 100 + BORDER_WIDTH;
		
		// Border widths: Top, Left, Bottom. Right
		private int tb, lb, bb, rb;
		
		private int[] locations = {
				SwingConstants.NORTH,
				SwingConstants.SOUTH,
				SwingConstants.WEST,
				SwingConstants.EAST,
				SwingConstants.NORTH_WEST,
				SwingConstants.NORTH_EAST,
				SwingConstants.SOUTH_WEST,
				SwingConstants.SOUTH_EAST
		};

		private int[] cursors = {
				Cursor.N_RESIZE_CURSOR,
				Cursor.S_RESIZE_CURSOR,
				Cursor.W_RESIZE_CURSOR,
				Cursor.E_RESIZE_CURSOR,
				Cursor.NW_RESIZE_CURSOR,
				Cursor.NE_RESIZE_CURSOR,
				Cursor.SW_RESIZE_CURSOR,
				Cursor.SE_RESIZE_CURSOR
		};

		private CytoPanelImpl cytoPanel;
		
		private Point dragPoint;
		private int cursor = Cursor.DEFAULT_CURSOR;

		ComponentPopup(CytoPanelImpl cytoPanel) {
			this.cytoPanel = cytoPanel;
			
			addListeners();
			update();
		}
		
		void update() {
			updateBorder();
			getContentPane().removeAll();
			
			if (cytoPanel == null || cytoPanel.getThisComponent() == null)
				return;
			
			Component c = cytoPanel.getThisComponent();
			getContentPane().add(c, BorderLayout.CENTER);
			
			if (!c.isVisible())
				c.setVisible(true);
			
			c.revalidate();
			c.repaint();
		}
		
		void updateBorder() {
			tb = lb = bb = rb = 0;
			CytoPanelNameInternal name = cytoPanel.getCytoPanelNameInternal();
			
			switch (name) {
				case WEST:       rb = bb = BORDER_WIDTH; break;
				case SOUTH_WEST: rb = tb = BORDER_WIDTH; break;
				case EAST:       lb = bb = BORDER_WIDTH; break;
				case SOUTH:      lb = tb = BORDER_WIDTH; break;
				case BOTTOM:     rb = tb = BORDER_WIDTH; break;
			}
			
			getRootPane().setBorder(
					BorderFactory.createMatteBorder(tb, lb, bb, rb, UIManager.getColor("Separator.foreground")));
		}
		
		void setCytoPanel(CytoPanelImpl cytoPanel) {
			if (this.cytoPanel != cytoPanel) {
				this.cytoPanel = cytoPanel;
				update();
			}
		}

		CytoPanelImpl getCytoPanel() {
			return cytoPanel;
		}
		
		private void addListeners() {
			getRootPane().addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent evt) {
		            cursor = getCursor(evt);
		            dragPoint = evt.getPoint();
		            requestFocus();
		            repaint();
				}
				@Override
				public void mouseReleased(MouseEvent evt) {
					cursor = Cursor.DEFAULT_CURSOR;
					dragPoint = null;
					setCursor(Cursor.getDefaultCursor());
					revalidate();
				}
				@Override
				public void mouseExited(MouseEvent evt) {
					setCursor(Cursor.getDefaultCursor());
				}
			});
			getRootPane().addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseMoved(MouseEvent evt) {
					setCursor(Cursor.getPredefinedCursor(getCursor(evt)));
				}
				@Override
				public void mouseDragged(MouseEvent evt) {
					if (dragPoint != null) {
						setCursor(Cursor.getPredefinedCursor(cursor));
						
						int x = getX();
						int y = getY();
						int w = getWidth();
						int h = getHeight();

						int dx = evt.getX() - dragPoint.x;
						int dy = evt.getY() - dragPoint.y;
						
						switch (cursor) {
							case Cursor.N_RESIZE_CURSOR:
								y += dy;
								h -= dy;
								break;
	
							case Cursor.S_RESIZE_CURSOR:
								dragPoint = evt.getPoint();
								h += dy;
								break;
	
							case Cursor.W_RESIZE_CURSOR:
								x += dx;
								w -= dx;
								break;
	
							case Cursor.E_RESIZE_CURSOR:
								w += dx;
								dragPoint = evt.getPoint();
								break;
	
							case Cursor.NW_RESIZE_CURSOR:
								x += dx;
								y += dy;
								w -= dx;
								h -= dy;
								break;
	
							case Cursor.NE_RESIZE_CURSOR:
								y += dy;
								w += dx;
								h -= dy;
								dragPoint = new Point(evt.getX(), dragPoint.y);
								break;
	
							case Cursor.SW_RESIZE_CURSOR:
								x += dx;
								w -= dx;
								h += dy;
								dragPoint = new Point(dragPoint.x, evt.getY());
								break;
	
							case Cursor.SE_RESIZE_CURSOR:
								w += dx;
								h += dy;
								dragPoint = evt.getPoint();
								break;
						}

						final int maxWidth = getTopPane().getWidth();
						final int maxHeight = getTopPane().getHeight();
						
						w = Math.max(w, MIN_SIZE);
						h = Math.max(h, MIN_SIZE);
						w = Math.min(w, maxWidth);
						h = Math.min(h, maxHeight);
						
						final int minX = getMasterPane().getX();
						final int minY = getMasterPane().getY();
						final int maxX = minX + getTopPane().getWidth();
						final int maxY = minY + getTopPane().getHeight();
						
						switch (cursor) {
							case Cursor.N_RESIZE_CURSOR:
							case Cursor.NE_RESIZE_CURSOR:
								y = Math.max(y, minY);
								y = Math.min(y, maxY - h);
								break;
	
							case Cursor.W_RESIZE_CURSOR:
							case Cursor.SW_RESIZE_CURSOR:
								x = Math.max(x, minX);
								x = Math.min(x, maxX - w);
								break;
	
							case Cursor.NW_RESIZE_CURSOR:
								x = Math.max(x, minX);
								x = Math.min(x, maxX - w);
								y = Math.max(y, minY);
								y = Math.min(y, maxY - h);
								break;
						}
						
						switch (cursor) {
							case Cursor.S_RESIZE_CURSOR:
								dragPoint = evt.getPoint();
								break;
	
							case Cursor.E_RESIZE_CURSOR:
								dragPoint = evt.getPoint();
								break;
	
							case Cursor.NE_RESIZE_CURSOR:
								dragPoint = new Point(evt.getX(), dragPoint.y);
								break;
	
							case Cursor.SW_RESIZE_CURSOR:
								dragPoint = new Point(dragPoint.x, evt.getY());
								break;
	
							case Cursor.SE_RESIZE_CURSOR:
								dragPoint = evt.getPoint();
								break;
						}
						
						setBounds(x, y, w, h);
						cytoPanel.setMaximized(false);
						
						if (getParent() != null)
							getParent().revalidate();
						
						if (cytoPanel != null)
							popupPreferredSizes.put(cytoPanel.getCytoPanelNameInternal(), new Dimension(w, h));
					}
				}
			});
		}
		
		private Rectangle getRectangle(int x, int y, int w, int h, int location) {
			CytoPanelNameInternal name = cytoPanel != null ? cytoPanel.getCytoPanelNameInternal() : null;
			
			switch (location) {
				case SwingConstants.NORTH:
					if (name == BOTTOM || name == SOUTH || name == SOUTH_WEST)
						return new Rectangle(x + lb, y, w - rb, BORDER_WIDTH);
					
					break;
				case SwingConstants.SOUTH:
					if (name == WEST || name == EAST)
						return new Rectangle(x + lb, y + h - bb, w - BORDER_WIDTH, BORDER_WIDTH);
					
					break;
				case SwingConstants.WEST:
					if (name == EAST || name == SOUTH)
						return new Rectangle(x, y + tb, BORDER_WIDTH, h - BORDER_WIDTH);
					
					break;
				case SwingConstants.EAST:
					if (name == WEST || name == SOUTH_WEST || name == BOTTOM)
						return new Rectangle(x + w - rb, y + tb, BORDER_WIDTH, h - BORDER_WIDTH);
					
					break;
				case SwingConstants.NORTH_WEST:
					if (name == SOUTH)
						return new Rectangle(x, y, BORDER_WIDTH, BORDER_WIDTH);
					
					break;
				case SwingConstants.NORTH_EAST:
					if (name == SOUTH_WEST || name == BOTTOM)
						return new Rectangle(x + w - rb, y, BORDER_WIDTH, BORDER_WIDTH);
					
					break;
				case SwingConstants.SOUTH_WEST:
					if (name == EAST)
						return new Rectangle(x, y + h - bb, BORDER_WIDTH, BORDER_WIDTH);
					
					break;
				case SwingConstants.SOUTH_EAST:
					if (name == WEST)
						return new Rectangle(x + w - rb, y + h - bb, BORDER_WIDTH, BORDER_WIDTH);
			}
			
			return null;
		}
		
		private int getCursor(MouseEvent evt) {
			Component c = evt.getComponent();
			int w = c.getWidth();
			int h = c.getHeight();

			for (int i = 0; i < locations.length; i++) {
				Rectangle rect = getRectangle(0, 0, w, h, locations[i]);

				if (rect != null && rect.contains(evt.getPoint()))
					return cursors[i];
			}

			return Cursor.DEFAULT_CURSOR;
		}
	}
	
	private class GlassPaneMouseListener implements AWTEventListener {
		
		@Override
		public void eventDispatched(AWTEvent ae) {
			if (popup == null || !popup.isShowing())
				return;
			if (ae instanceof MouseEvent == false || ae.getID() != MouseEvent.MOUSE_PRESSED)
				return;
			
			// Over a Window other than the CytoscapeDesktop?
			KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			Window window = keyboardFocusManager.getActiveWindow();
			
			if (window instanceof CytoscapeDesktop == false)
				return;
			
			// Over a JPopupMenu?
			MenuSelectionManager msm = MenuSelectionManager.defaultManager();
		    MenuElement[] path = msm.getSelectedPath();
		    
		    for (MenuElement me : path) {
		        if (me instanceof JPopupMenu)
		            return;
		    }
			
			// Over the cytopanel popup?
		    Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
		    
			if (mouseLoc.x >= popup.getLocationOnScreen().x
					&& mouseLoc.x <= popup.getLocationOnScreen().x + popup.getWidth()
					&& mouseLoc.y >= popup.getLocationOnScreen().y
					&& mouseLoc.y <= popup.getLocationOnScreen().y + popup.getHeight())
				return; // Mouse pressed on the popup, nothing to do here

			// Over a sidebar button?
			boolean overButton = false;
			
			PANELS_LOOP:
			for (CytoPanelImpl cp : getAllCytoPanels()) {
				if (cp.getState() != HIDE)
					continue; 
				
				SideBar.TrimStack ts = getTrimStackOf(cp);
				
				if (!ts.isShowing())
					continue;
				
				for (SidebarToggleButton btn : ts.getAllButtons()) {
					if (!btn.isShowing())
						continue;
	
					if (mouseLoc.x >= btn.getLocationOnScreen().x
							&& mouseLoc.x <= btn.getLocationOnScreen().x + btn.getWidth()
							&& mouseLoc.y >= btn.getLocationOnScreen().y
							&& mouseLoc.y <= btn.getLocationOnScreen().y + btn.getHeight()) {
						overButton = true;
						break PANELS_LOOP;
					}
				}
			}

			// No need to dispose when over a sidebar button for a hidden panel,
			// since the button click will do it
			if (!overButton) {
				disposeComponentPopup();

				isAdjusting = true;
				minimizedButtonGroup.clearSelection();
				isAdjusting = false;
			}
		}
	}
}
