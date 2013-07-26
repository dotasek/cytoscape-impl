package org.cytoscape.task.internal.table;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
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

import static org.cytoscape.work.TunableValidator.ValidationState.OK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator;
import org.cytoscape.work.TunableValidator.ValidationState;
import org.cytoscape.work.util.ListMultipleSelection;
import org.cytoscape.work.util.ListSingleSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeDataTableTask extends AbstractTask implements TunableValidator {
	
	enum TableType {
		NODE_ATTR("Node Table Columns", CyNode.class),
		EDGE_ATTR("Edge Table Columns", CyEdge.class),
		NETWORK_ATTR("Network Table Columns", CyNetwork.class);

		private final String name;
		private final Class<? extends CyIdentifiable> type;

		private TableType(final String name, Class<? extends CyIdentifiable> type) {
			this.name = name;
			this.type = type;
		}

		public Class<? extends CyIdentifiable> getType() {
			return this.type;
		}

		@Override
		public String toString() {
			return name;
		}
	};
	

	private static final Logger logger = LoggerFactory.getLogger(MapTableToNetworkTablesTask.class);
	private static final String NO_NETWORKS = "No Networks Found";
	public static final String NO_TABLES = "No Tables Found";
	
	public static final String NETWORK_COLLECTION = "To a Network Collection";
	public static final String NETWORK_SELECTION = "To selected networks only";
	public static final String UNASSIGNED_TABLE = "To an unassigned table";
	
	private CyRootNetworkManager rootNetworkManager;
	private CyNetworkManager networkManager;
	private CyTableManager tabelMgr;
	private Map<String, CyNetwork> name2NetworkMap;
	private Map<String, CyRootNetwork> name2RootMap;
	private Map<String, String> source2targetColumnMap;
	
	public ListSingleSelection<CyTable> globalTable;
	@Tunable(description="Source table to merge",gravity=0.1, groups={"Source Data Table"})
	
	public ListSingleSelection<CyTable> getGlobalTable() {
		return globalTable;
	}
	
	public void setGlobalTable(ListSingleSelection<CyTable> table) {
		
		ListMultipleSelection<String> tempList = getColumns(table.getSelectedValue());
		if(!mergeColumns.getPossibleValues().containsAll(tempList.getPossibleValues()) 
				|| mergeColumns.getPossibleValues().size() != tempList.getPossibleValues().size())
		{
			mergeColumns = tempList;
			mergeKey = getColumnsWithNames(table.getSelectedValue());
			if(selectAllColumns)
				mergeColumns.setSelectedValues(mergeColumns.getPossibleValues());
			
		}
		globalTable = table;
	}
	
	public ListMultipleSelection<String> mergeColumns;
	@Tunable(description="List of Columns to Merge",gravity=0.2, groups={"Source Data Table","Data Columns To Merge"},listenForChange={"GlobalTable","SelectAllColumns"})
	public ListMultipleSelection<String> getMergeColumns(){
		
		return mergeColumns;
	}
	
	public void setMergeColumns(ListMultipleSelection<String> columns){
		
		mergeColumns = columns;
	}
	
	public boolean selectAllColumns = false;
	@Tunable(description="Select All Columns",gravity=0.3, groups={"Source Data Table","Data Columns To Merge"})
	
	public boolean getSelectAllColumns (){
		return selectAllColumns;
	}
	
	public void setSelectAllColumns ( boolean selected){
		
		if(selected != selectAllColumns)
		{
			selectAllColumns = selected;
			if(selectAllColumns)
				mergeColumns.setSelectedValues(mergeColumns.getPossibleValues());
			else
				mergeColumns.setSelectedValues(new ArrayList<String>());
		}
	}
	
	public ListSingleSelection<String> mergeKey;
	@Tunable(description = "Key column to merge", groups={"Source Data Table"},gravity=0.4, listenForChange={"GlobalTable"})
	public ListSingleSelection<String> getMergeKey() {
		return mergeKey;
	}

	public void setMergeKey(ListSingleSelection<String> key) {
		this.mergeKey = key;
	}
	
	@Tunable(description="Apply a soft merge",gravity=0.5, groups={"Source Data Table"})
	public boolean mergeDataType = false;

	
	public ListSingleSelection<String> importTypeChooser ;
	@Tunable(description="Where to merge the Data Table",gravity=1.0, groups={"Target Data Table"}, xorChildren=true)
	
	public ListSingleSelection<String> getImportTypeChooser() {
		return importTypeChooser;
	}

	public void setImportTypeChooser(ListSingleSelection<String> chooser) {
		this.importTypeChooser = chooser;
	}

	public ListSingleSelection<String> rootNetworkList;
	@Tunable(description = "Network Collection", groups = {"Target Data Table","Select a Network Collection"},gravity=2.0,  xorKey=NETWORK_COLLECTION)
	public ListSingleSelection<String> getRootNetworkList() {
		return rootNetworkList;
	}

	public void setRootNetworkList(ListSingleSelection<String> roots) {
		ListSingleSelection<String> tempList = getColumns(name2RootMap.get(rootNetworkList.getSelectedValue()),
				dataTypeOptions1.getSelectedValue(), CyRootNetwork.SHARED_ATTRS);
		if(!columnList.getPossibleValues().containsAll(tempList.getPossibleValues())
				|| columnList.getPossibleValues().size() != tempList.getPossibleValues().size())
			columnList = tempList;
	}

	public ListSingleSelection<String> columnList;
	@Tunable(description = "Key Column for Network", groups = {"Target Data Table","Select a Network Collection"},gravity=3.0, xorKey=NETWORK_COLLECTION, listenForChange = {
			"DataTypeOptions1", "RootNetworkList" })
	public ListSingleSelection<String> getColumnList() {
		return columnList;
	}

	public void setColumnList(ListSingleSelection<String> colList) {
		this.columnList = colList;
	}
	
	public ListSingleSelection<TableType> dataTypeOptions1;

	@Tunable(description = "Merge Data in", gravity=4.0, groups={"Target Data Table","Select a Network Collection"}, xorKey=NETWORK_COLLECTION)
	public ListSingleSelection<TableType> getDataTypeOptions1() {
		return dataTypeOptions1;
	}

	public void setDataTypeOptions1(ListSingleSelection<TableType> options) {
		ListSingleSelection<String> tempList = getColumns(name2RootMap.get(rootNetworkList.getSelectedValue()),
				dataTypeOptions1.getSelectedValue(), CyRootNetwork.SHARED_ATTRS);
		if(!columnList.getPossibleValues().containsAll(tempList.getPossibleValues()) 
				|| columnList.getPossibleValues().size() != tempList.getPossibleValues().size())
			columnList = tempList;
	}

	public ListMultipleSelection<String> networkList;
	@Tunable(description = "Network List", groups = {"Target Data Table","Select Networks"},gravity=5.0, xorKey=NETWORK_SELECTION, params = "displayState=uncollapsed")
	public ListMultipleSelection<String> getNetworkList() {
		return networkList;
	}

	public void setNetworkList(ListMultipleSelection<String> list) {
		this.networkList = list;
	}

	@Tunable(description = "Merge Data in", gravity=6.0, groups={"Target Data Table","Select Networks"}, xorKey=NETWORK_SELECTION)
	public ListSingleSelection<TableType> dataTypeOptions2;
	
	
	public ListSingleSelection<Object> unassignedTables;
	@Tunable(description = "Unassigned Tables", groups = {"Target Data Table","Select the Unassigned Table"},gravity=7.0, xorKey=UNASSIGNED_TABLE)
	public ListSingleSelection<Object> getUnassignedTables() {
		return unassignedTables;
	}

	public void setUnassignedTables(ListSingleSelection<Object> tables) {
		if(tables.getSelectedValue() instanceof CyTable)
		{
			ListSingleSelection<String> tempList = getColumnsWithNames((CyTable)tables.getSelectedValue());
			if(!targetMergeKey.getPossibleValues().containsAll(tempList.getPossibleValues())
					|| targetMergeKey.getPossibleValues().size() != tempList.getPossibleValues().size())
			{
				targetMergeKey = tempList;
				targetMergeKey = getColumnsWithNames((CyTable)tables.getSelectedValue());
				
			}
		}
		this.unassignedTables = tables;
	}
	
	public ListSingleSelection<String> targetMergeKey;
	@Tunable(description = "Key Column To Merge", groups={"Target Data Table","Select the Unassigned Table"},gravity=8.0, listenForChange={"UnassignedTables"})
	public ListSingleSelection<String> getTargetMergeKey() {
		return targetMergeKey;
	}

	public void setTargetMergeKey(ListSingleSelection<String> key) {
		this.targetMergeKey = key;
	}

	@ProvidesTitle
	public String getTitle() {
		return "Merge Data Table";
	}



	public MergeDataTableTask( CyTableManager tabelMgr,CyRootNetworkManager rootNetworkManeger, CyNetworkManager networkManager) {
		

		init(tabelMgr,rootNetworkManeger, networkManager);
	}
	
	private final void init( CyTableManager tabelMgr,CyRootNetworkManager rootNetworkManeger, CyNetworkManager networkManager) {
		this.rootNetworkManager = rootNetworkManeger;
		this.networkManager = networkManager;
		this.name2NetworkMap = new HashMap<String, CyNetwork>();
		this.name2RootMap = new HashMap<String, CyRootNetwork>();
		this.source2targetColumnMap = new HashMap<String, String>();
		this.tabelMgr =  tabelMgr;

		initTunable(tabelMgr,networkManager);
	}

	private final void initTunable(CyTableManager tabelMgr,CyNetworkManager networkManager) {
		
		importTypeChooser = new ListSingleSelection<String>(NETWORK_COLLECTION,NETWORK_SELECTION, UNASSIGNED_TABLE);
		importTypeChooser.setSelectedValue(NETWORK_COLLECTION);

		final List<TableType> options = new ArrayList<TableType>();
		for (TableType type : TableType.values())
			options.add(type);
		dataTypeOptions1 = new ListSingleSelection<TableType>(options);
		dataTypeOptions1.setSelectedValue(TableType.NODE_ATTR);
		dataTypeOptions2 = new ListSingleSelection<TableType>(options);
		dataTypeOptions2.setSelectedValue(TableType.NODE_ATTR);

		for (CyNetwork net : networkManager.getNetworkSet()) {
			String netName = net.getRow(net).get(CyNetwork.NAME, String.class);
			name2NetworkMap.put(netName, net);
		}
		List<String> names = new ArrayList<String>();
		names.addAll(name2NetworkMap.keySet());
		if (names.isEmpty())
			networkList = new ListMultipleSelection<String>(NO_NETWORKS);
		else
			networkList = new ListMultipleSelection<String>(names);

		for (CyNetwork net : networkManager.getNetworkSet()) {
			final CyRootNetwork rootNet = rootNetworkManager.getRootNetwork(net);
			if (!name2RootMap.containsValue(rootNet))
				name2RootMap.put(rootNet.getRow(rootNet).get(CyRootNetwork.NAME, String.class), rootNet);
		}
		List<String> rootNames = new ArrayList<String>();
		rootNames.addAll(name2RootMap.keySet());
		rootNetworkList = new ListSingleSelection<String>(rootNames);
		if(!rootNames.isEmpty())
		{
			rootNetworkList.setSelectedValue(rootNames.get(0));

			columnList = getColumns(name2RootMap.get(rootNetworkList.getSelectedValue()),
					dataTypeOptions1.getSelectedValue(), CyRootNetwork.SHARED_ATTRS);
		}
		
		List<CyTable> listOfTables = new ArrayList<CyTable>();
		List<Object> listOfUTables = new ArrayList<Object>();
		for ( CyTable tempTable : tabelMgr.getGlobalTables()) 
		{
			if(tempTable.isPublic())
			{
				listOfTables.add(tempTable);
				listOfUTables.add(tempTable);
			}
		}
		
		for ( CyNetwork network : networkManager.getNetworkSet()) 
		{
			listOfTables.add(network.getDefaultNodeTable());
			listOfTables.add(network.getDefaultEdgeTable());
		}
		if(listOfUTables.size()>0)
		{
			unassignedTables = new ListSingleSelection<Object>(listOfUTables);
			targetMergeKey = getColumnsWithNames((CyTable)unassignedTables.getSelectedValue());
		}
		else
		{
			listOfUTables.add(NO_TABLES);
			unassignedTables = new ListSingleSelection<Object>(listOfUTables);
		}
			
		globalTable = new ListSingleSelection<CyTable>(listOfTables);
		mergeColumns = getColumns(globalTable.getSelectedValue());
		mergeKey = getColumnsWithNames(globalTable.getSelectedValue());
	}

	public ListSingleSelection<String> getColumns(CyNetwork network, TableType tableType, String namespace) {
		CyTable selectedTable = getTable(network, tableType, CyRootNetwork.SHARED_ATTRS);

		List<String> colNames = new ArrayList<String>();
		for (CyColumn col : selectedTable.getColumns())
			colNames.add(col.getName());

		ListSingleSelection<String> columns = new ListSingleSelection<String>(colNames);
		columns.setSelectedValue(CyRootNetwork.SHARED_NAME);
		return columns;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		TableType tableType = getDataTypeOptions();
		
		//If we are here and there no networks loaded, we could only continue if the merge is on 
		// an unassigned table
		if(!importTypeChooser.getSelectedValue().matches(UNASSIGNED_TABLE))
		{
			if(name2RootMap.isEmpty())
				return;
		}

		if (!checkKeys()) {
			throw new IllegalArgumentException("Types of keys selected for tables are not matching.");
		}

		if(importTypeChooser.getSelectedValue().matches(NETWORK_COLLECTION))
			mapTableToDefaultAttrs(tableType);
		if(importTypeChooser.getSelectedValue().matches(NETWORK_SELECTION))
			mapTableToLocalAttrs(tableType);
		if(importTypeChooser.getSelectedValue().matches(UNASSIGNED_TABLE))
			mapTableToUnassignedTable();

	}
	
	private void mapTableToUnassignedTable() {
		
		if(!unassignedTables.getSelectedValue().toString().matches(NO_TABLES))
		{
			CyTable tableChosen = (CyTable)unassignedTables.getSelectedValue();
			if(!tableChosen.equals(globalTable))
			{
				applyMapping(tableChosen);
			}
			
		}
		
	}

	private void mapTableToLocalAttrs(TableType tableType) {
		List<CyNetwork> networks = new ArrayList<CyNetwork>();

		if (networkList.getSelectedValues().isEmpty())
			return;

		if (!networkList.getSelectedValues().get(0).equals(NO_NETWORKS))
			for (String netName : networkList.getSelectedValues())
				networks.add(name2NetworkMap.get(netName));

		for (CyNetwork network : networks) {
			CyTable targetTable = getTable(network, tableType, CyNetwork.LOCAL_ATTRS);
			if (targetTable != null)
				applyMapping(targetTable);
		}
	}

	private void mapTableToDefaultAttrs(TableType tableType) {
		CyTable targetTable = getTable(name2RootMap.get(rootNetworkList.getSelectedValue()), tableType,
				CyRootNetwork.SHARED_DEFAULT_ATTRS);
		if (targetTable != null) {
			applyMapping(targetTable);
		}
	}

	private CyTable getTable(CyNetwork network, TableType tableType, String namespace) {
		if (tableType == TableType.NODE_ATTR)
			return network.getTable(CyNode.class, namespace);
		if (tableType == TableType.EDGE_ATTR)
			return network.getTable(CyEdge.class, namespace);
		if (tableType == TableType.NETWORK_ATTR)
			return network.getTable(CyNetwork.class, namespace);

		logger.warn("The selected table type is not valie. \nTable needs to be one of these types: "
				+ TableType.NODE_ATTR + ", " + TableType.EDGE_ATTR + ", " + TableType.NETWORK_ATTR + ".");
		return null;
	}

	private void applyMapping(CyTable targetTable) {
		ArrayList<CyColumn> columns = new ArrayList<CyColumn>();
		CyColumn tempCol;
		
		for(String colName :mergeColumns.getSelectedValues())
		{
		    tempCol = globalTable.getSelectedValue().getColumn(colName);
			if(tempCol != null)
				columns.add(tempCol);
		}
		copyColumns(globalTable.getSelectedValue(),columns, targetTable,isNewColumnVirtual());
		if(!isNewColumnVirtual())
			copyRows(globalTable.getSelectedValue(),columns, targetTable);
			

	}

	private CyColumn getJoinTargetColumn(CyTable targetTable) {
		String joinKeyName = CyNetwork.NAME;
		if(importTypeChooser.getSelectedValue().matches(NETWORK_COLLECTION))
			joinKeyName = columnList.getSelectedValue();
		if(importTypeChooser.getSelectedValue().matches(UNASSIGNED_TABLE))
			joinKeyName = targetMergeKey.getSelectedValue();//targetTable.getPrimaryKey().getName();
		return targetTable.getColumn(joinKeyName);
	}

	private void copyRows(CyTable sourceTable, List<CyColumn> sourceColumns, CyTable targetTable) {
		CyRow sourceRow;
		CyColumn targetKeyColumn = getJoinTargetColumn(targetTable);

		for (CyRow targetRow : targetTable.getAllRows()) {
			Object key = targetRow.get(targetKeyColumn.getName(), targetKeyColumn.getType());

			if(isMergeColumnKeyColumn())
			{
				if(!sourceTable.rowExists(key))
					continue;
				
				sourceRow = sourceTable.getRow(key);
			}
			else
			{
				if (sourceTable.getMatchingRows(getMergeKeyColumn().getName(), key).isEmpty())
					continue;
	
				sourceRow = sourceTable.getMatchingRows(getMergeKeyColumn().getName(), key).iterator().next();
			}

			if (sourceRow == null)
				continue;

			for (CyColumn col : sourceColumns) {
				
				if (col == getMergeKeyColumn())
					continue;
				

				String targetColName = source2targetColumnMap.get(col.getName());
				
				if (targetColName == null)
					continue;  // skip this column
				
				if (col.getType() == List.class)
					targetRow.set(targetColName, sourceRow.getList(col.getName(), col.getListElementType()));
				else
					targetRow.set(targetColName, sourceRow.get(col.getName(), col.getType()));

			}
		}

	}

	private void copyColumns(CyTable sourceTable, List<CyColumn> sourceColumns,CyTable targetTable, boolean addVirtual) {

		for (CyColumn col : sourceColumns) {
			
			if (col == getMergeKeyColumn())
				continue;
			
			// This is a bad idea!  It prevents users from updating data in existing
			// columns, which is a common case
			// String targetColName = getUniqueColumnName(targetTable, col.getName());
			String targetColName = col.getName();
			
			if ( !isMergeColumnKeyColumn())
				addVirtual = false;
			
			if (targetTable.getColumn(targetColName) == null) {
				if(!addVirtual)
				{
					if (col.getType() == List.class)
						targetTable.createListColumn(targetColName, col.getListElementType(), col.isImmutable());
					else
						targetTable.createColumn(targetColName, col.getType(), col.isImmutable(), col.getDefaultValue());
				}
				else
				{
					targetTable.addVirtualColumn(targetColName, col.getName(), sourceTable, getJoinTargetColumn(targetTable).getName(), false);
				}
			} else {
				CyColumn targetCol = targetTable.getColumn(targetColName);
				if ((targetCol.getType() != col.getType()) ||
				    (col.getType() == List.class && (targetCol.getListElementType() != col.getListElementType()))) {
					logger.error("Column '"+targetColName+"' has a different type in the target table -- skipping column");
					continue;
				}
			}

			source2targetColumnMap.put(col.getName(), targetColName);
		}
	}

	public boolean checkKeys() {

		Class<?> joinTargetColumnType = String.class;
		if(importTypeChooser.getSelectedValue().matches(NETWORK_COLLECTION))
		{
			joinTargetColumnType = getJoinTargetColumn(
					getTable(name2RootMap.get(rootNetworkList.getSelectedValue()), getDataTypeOptions(),
							CyNetwork.DEFAULT_ATTRS)).getType();
		}
		if(importTypeChooser.getSelectedValue().matches(UNASSIGNED_TABLE))
		{
			if(!unassignedTables.getSelectedValue().toString().matches(NO_TABLES))
				joinTargetColumnType = getJoinTargetColumn((CyTable)unassignedTables.getSelectedValue()).getType();
		}
			
		if (getMergeKeyColumn().getType() != joinTargetColumnType)
			return false;
			

		return true;
	}
	
	private ListMultipleSelection<String> getColumns(CyTable selectedTable) {
		String tempName;
		List<String> colNames = new ArrayList<String>();
		for (CyColumn col : selectedTable.getColumns())
		{
			tempName = col.getName();
			if(!tempName.matches(CyRootNetwork.SHARED_NAME) && !tempName.matches(CyRootNetwork.NAME) 
					&& !tempName.matches(CyRootNetwork.SUID)  && !tempName.matches(CyRootNetwork.SELECTED))
				colNames.add(tempName);
		}

		ListMultipleSelection<String> columns = new ListMultipleSelection<String>(colNames);
		
		return columns;
	}
	
	private ListSingleSelection<String> getColumnsWithNames(CyTable selectedTable) {
		String tempName;
		
		List<String> colNames = new ArrayList<String>();
		for (CyColumn col : selectedTable.getColumns())
		{
			tempName = col.getName();
			if( !tempName.matches(CyRootNetwork.SUID) && !tempName.matches(CyRootNetwork.SELECTED))
				colNames.add(tempName);
		}

		ListSingleSelection<String> columns = new ListSingleSelection<String>(colNames);
		if( selectedTable.getColumn(CyRootNetwork.NAME) != null)
			columns.setSelectedValue(CyRootNetwork.NAME);
		else if(!selectedTable.getPrimaryKey().getName().matches(CyRootNetwork.SUID))
			columns.setSelectedValue(selectedTable.getPrimaryKey().getName());
		
		return columns;
	}
	
	private boolean isNewColumnVirtual ()
	{
		return mergeDataType;
	}
	
	private boolean isMergeColumnKeyColumn ()
	{
		return globalTable.getSelectedValue().getPrimaryKey() == getMergeKeyColumn();
	}
	
	private CyColumn getMergeKeyColumn ()
	{
		return globalTable.getSelectedValue().getColumn(mergeKey.getSelectedValue());
	}

	private TableType getDataTypeOptions()
	{
		if(importTypeChooser.getSelectedValue().matches(NETWORK_COLLECTION))
			return dataTypeOptions1.getSelectedValue();
		else
			return dataTypeOptions2.getSelectedValue();
		
	}
	@Override
	public ValidationState getValidationState(Appendable errMsg) {
		if ( !isMergeColumnKeyColumn() && isNewColumnVirtual()) {
			try{
				mergeDataType = false;
				errMsg.append("Source Key column needs to be the key column of source table to apply a soft merge.\n");
			} catch (IOException e) {
				e.printStackTrace();
				return ValidationState.INVALID;
			}
			return ValidationState.INVALID;
		}
		
		
		return OK;
	}
}
