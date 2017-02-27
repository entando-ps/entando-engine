/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.agiletec.aps.system.services.page;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.entando.entando.aps.system.init.model.portdb.PageMetadataDraft;
import org.entando.entando.aps.system.init.model.portdb.PageMetadataOnline;
import org.entando.entando.aps.system.init.model.portdb.WidgetConfig;
import org.entando.entando.aps.system.init.model.portdb.WidgetConfigDraft;
import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agiletec.aps.system.common.AbstractDAO;
import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.util.ApsProperties;

/**
 * Data Access Object for the 'page' objects
 * @author M.Diana - E.Santoboni
 */
public class PageDAO extends AbstractDAO implements IPageDAO {
	
	private static final Logger _logger =  LoggerFactory.getLogger(PageDAO.class);
	
	private enum WidgetConfigDest {
		ON_LINE, DRAFT;
	}
	
	/**
	 * Load a sorted list of the pages and the configuration of the widgets 
	 * @return the list of pages
	 */
	@Override
	public List<IPage> loadPages() {
		Connection conn = null;
		Statement stat = null;
		ResultSet res = null;
		List<IPage> pages = null;
		try {
			conn = this.getConnection();
			stat = conn.createStatement();
			res = stat.executeQuery(ALL_PAGES);
			pages = this.createPages(res);
		} catch (Throwable t) {
			_logger.error("Error loading pages",  t);
			throw new RuntimeException("Error loading pages", t);
		} finally {
			closeDaoResources(res, stat, conn);
		}
		return pages;
	}

	/**
	 * Read & create in a single passage, for efficiency reasons, the pages and the 
	 * association of the associated widgets.
	 * @param res the result set where to extract pages information from.
	 * @return The list of the pages defined in the system
	 * @throws Throwable In case of error
	 */
	protected List<IPage> createPages(ResultSet res) throws Throwable {
		int widgetStartOnline = 15;
		int widgetStartDraft = 18;
		List<IPage> pages = new ArrayList<IPage>();
		Page page = null;
		Widget onlineWidgets[] = null;
		Widget draftWidgets[] = null;
		int numFramesOnline = 0;
		int numFramesDraft = 0;
		String prevCode = null;
		while (res.next()) {
			String code = res.getString(3);
			if (prevCode == null || !code.equals(prevCode)) {
				page = this.createPage(code, res);
				pages.add(page);
				
				numFramesOnline = this.getWidgetArrayLength(page.getOnlineMetadata());
				if (numFramesOnline >= 0) {
					onlineWidgets = new Widget[numFramesOnline];
					page.setOnlineWidgets(onlineWidgets);
				}
				numFramesDraft = this.getWidgetArrayLength(page.getDraftMetadata());
				if (numFramesDraft >= 0) {
					draftWidgets = new Widget[numFramesDraft];
					page.setDraftWidgets(draftWidgets);
				}
				prevCode = code;
			}
			this.readWidget(page, numFramesOnline, onlineWidgets, widgetStartOnline, res);
			this.readWidget(page, numFramesDraft, draftWidgets, widgetStartDraft, res);
		}
		return pages;
	}
	
	protected int getWidgetArrayLength(PageMetadata metadata) {
		int numFrames = -1;
		if (metadata != null) {
			PageModel model = metadata.getModel();
			if (model != null) {
				numFrames = model.getFrames().length;
			}
		}
		return numFrames;
	}
	
	protected void readWidget(IPage page, int numOfFrames, Widget widgets[], int startIndex, ResultSet res) throws ApsSystemException, SQLException {
		Object posObj = res.getObject(startIndex);
		if (posObj != null) {
			int pos = res.getInt(startIndex);
			if (pos >= 0 && pos < numOfFrames) {
				Widget widget = this.createWidget(page, pos, res, startIndex+1);
				widgets[pos] = widget;
			} else {
				_logger.warn("The position read from the database exceeds the numer of frames defined in the model of the page {}", page.getCode());
			}
		}
	}
	
	protected Page createPage(String code, ResultSet res) throws Throwable {
		Page page = new Page();
		page.setCode(code);
		page.setParentCode(res.getString(1));
		page.setPosition(res.getInt(2));
		page.setGroup(res.getString(4));
		if (res.getString(5)!=null) {
			page.setOnlineMetadata(this.createPageMetadata(code, res, 5));
		}
		if (res.getString(10)!=null) {
			page.setDraftMetadata(this.createPageMetadata(code, res, 10));
		}
		return page;
	}
	
	protected Widget createWidget(IPage page, int pos, ResultSet res, int startIndex) throws ApsSystemException, SQLException {
		String typeCode = res.getString(startIndex++);
		if (null == typeCode) {
			return null;
		}
		Widget widget = new Widget();
		WidgetType type = this.getWidgetTypeManager().getWidgetType(typeCode);
		widget.setType(type);
		ApsProperties config = new ApsProperties();
		String configText = res.getString(startIndex++);
		if (null != configText && configText.trim().length() > 0) {
			try {
				config.loadFromXml(configText);
			} catch (Throwable t) {
				_logger.error("IO error detected while parsing the configuration of the widget in position '{}' of the page '{}'", pos, page.getCode(), t);
				String msg = "IO error detected while parsing the configuration of the widget in position " +pos+ " of the page '"+ page.getCode()+"'";
				throw new ApsSystemException(msg, t);
			}
		} else {
			config = type.getConfig();
		}
		widget.setConfig(config);
		return widget;
	}
	
	/**
	 * Insert a new page.
	 * @param page The new page to insert.
	 */
	@Override
	public void addPage(IPage page) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			String pageCode = page.getCode();
			this.addPageRecord(page, conn);
			
			Date now = new Date();
			PageMetadata onlineMetadata = page.getOnlineMetadata();
			if (onlineMetadata != null) {
				onlineMetadata.setUpdatedAt(now);
				this.addOnlinePageMetadata(pageCode, onlineMetadata, conn);
			}
			
			PageMetadata draftMetadata = page.getDraftMetadata();
			if (draftMetadata != null) {
				draftMetadata.setUpdatedAt(now);
				this.addDraftPageMetadata(pageCode, draftMetadata, conn);
			}
			
			this.addWidgetForPage(page, WidgetConfigDest.DRAFT, conn);
			if (onlineMetadata != null) {
				this.addWidgetForPage(page, WidgetConfigDest.ON_LINE, conn);
			}
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error while adding a new page",  t);
			throw new RuntimeException("Error while adding a new page", t);
		} finally {
			closeConnection(conn);
		}
	}
	
	protected void addPageRecord(IPage page, Connection conn) throws ApsSystemException {
		int position = 1;
		IPage[] sisters = page.getParent().getAllChildren();
		if (null != sisters && sisters.length > 0) {
			IPage last = sisters[sisters.length - 1];
			if (null != last) {
				position = last.getPosition() + 1;
			} else {
				position = sisters.length + 1;
			}
		}
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(ADD_PAGE);
			stat.setString(1, page.getCode());
			stat.setString(2, page.getParent().getCode());
			stat.setInt(3, position);
			stat.setString(4, page.getGroup());
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error adding a new page record",  t);
			throw new RuntimeException("Error adding a new page record", t);
		} finally {
			closeDaoResources(null, stat);
		}
		if (page instanceof Page) {
			((Page) page).setPosition(position);
		}
	}
	
	/**
	 * Delete the page identified by the given code.
	 * @param page The page to delete.
	 */
	@Override
	public void deletePage(IPage page) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			String pageCode = page.getCode();
			this.deleteOnlineWidgets(pageCode, conn);
			this.deleteDraftWidgets(pageCode, conn);
			this.deleteOnlinePageMetadata(pageCode, conn);
			this.deleteDraftPageMetadata(pageCode, conn);
			this.deletePageRecord(pageCode, conn);
			this.shiftPages(page.getParentCode(), page.getPosition(), conn);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error deleting page",  t);
			throw new RuntimeException("Error deleting page", t);
		} finally {
			closeConnection(conn);
		}
	}

	protected void deletePageRecord(String pageCode, Connection conn) throws ApsSystemException {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(DELETE_PAGE);
			stat.setString(1, pageCode);
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error deleting a page record",  t);
			throw new RuntimeException("Error deleting a page record", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	/**
	 * Delete the widget associated to a page.
	 * @param pageCode The code of the page containing the widget to delete.
	 * @param conn The database connection
	 * @throws ApsSystemException In case of database error
	 */
	protected void deleteOnlineWidgets(String pageCode, Connection conn) throws ApsSystemException {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(DELETE_WIDGETS_FOR_PAGE_ONLINE);
			stat.setString(1, pageCode);
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error while deleting widgets for page '{}'", pageCode,  t);
			throw new RuntimeException("Error while deleting widgets", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}

	/**
	 * Delete the widget associated to the draft version of a page.
	 * @param pageCode The code of the page containing the widget to delete.
	 * @param conn The database connection
	 * @throws ApsSystemException In case of database error
	 */
	protected void deleteDraftWidgets(String pageCode, Connection conn) throws ApsSystemException {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(DELETE_WIDGETS_FOR_PAGE_DRAFT);
			stat.setString(1, pageCode);
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error while deleting  draft widgets for page '{}'", pageCode,  t);
			throw new RuntimeException("Error while deleting draft widgets", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	

	/**
	 * Decrement by one the position of a group of pages to compact the positions after a deletion
	 * @param parentCode the code of the parent of the pages to compact.
	 * @param position The empty position which needs to be compacted.
	 * @param conn The connection to the database
	 * @throws ApsSystemException In case of database error
	 */
	protected void shiftPages(String parentCode, int position, Connection conn) throws ApsSystemException {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(SHIFT_PAGE);
			stat.setString(1, parentCode);
			stat.setInt(2, position);
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error moving page position",  t);
			throw new RuntimeException("Error moving page position", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}

	/**
	 * Updates the position for the page movement
	 * @param pageDown The page to move downwards
	 * @param pageUp The page to move upwards
	 */
	@Override
	public void updatePosition(IPage pageDown, IPage pageUp) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			
			this.updatePosition(pageDown.getCode(), MOVE_DOWN, conn);
			this.updatePosition(pageUp.getCode(), MOVE_UP, conn);
			
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error detected while updating positions",  t);
			throw new RuntimeException("Error detected while updating positions", t);
		} finally {
			closeConnection(conn);
		}
	}
	
	private void updatePosition(String pageCode, String query, Connection conn) {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(query);
			stat.setString(1, pageCode);
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error detected while updating position for page {}", pageCode,  t);
			throw new RuntimeException("Error detected while updating position for page " + pageCode, t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	@Override
	public void updateWidgetPosition(String pageCode, Integer frameToMove, Integer destFrame) {
		Connection conn = null;
		int TEMP_FRAME_POSITION = -9999;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			
			this.updateWidgetPosition(pageCode, frameToMove, TEMP_FRAME_POSITION, conn);
			this.updateWidgetPosition(pageCode, destFrame, frameToMove, conn);
			this.updateWidgetPosition(pageCode, TEMP_FRAME_POSITION, destFrame, conn);
			
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error while updating WidgetPosition. page: {} from position: {} to position {}", pageCode, frameToMove, destFrame,  t);
			throw new RuntimeException("Error while updating widget position", t);
		} finally {
			closeConnection(conn);
		}
	}
	
	private void updateWidgetPosition(String pageCode, int frameToMove, int destFrame, Connection conn) {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(MOVE_WIDGET);
			stat.setInt(1, destFrame);
			stat.setString(2, pageCode);
			stat.setInt(3, frameToMove);
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error while updating WidgetPosition. page: {} from position: {} to position {}", pageCode, frameToMove, destFrame,  t);
			throw new RuntimeException("Error while updating widget position", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	/**
	 * Updates a page record in the database.
	 * @param page The page to update
	 */
	@Override
	public void updatePage(IPage page) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			String pageCode = page.getCode();
			
			this.deleteOnlineWidgets(pageCode, conn);
			this.deleteDraftWidgets(pageCode, conn);
			this.deleteOnlinePageMetadata(pageCode, conn);
			this.deleteDraftPageMetadata(pageCode, conn);
			
			this.updatePageRecord(page, conn);
			
			this.addDraftPageMetadata(pageCode, page.getDraftMetadata(), conn);
			this.addOnlinePageMetadata(pageCode, page.getOnlineMetadata(), conn);
			this.addWidgetForPage(page, WidgetConfigDest.DRAFT, conn);
			if (page.isOnline()) {
				this.addWidgetForPage(page, WidgetConfigDest.ON_LINE, conn);
			}
			
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error while updating the page",  t);
			throw new RuntimeException("Error while updating the page", t);
		} finally {
			closeConnection(conn);
		}
	}
	
	protected void updatePageRecord(IPage page, Connection conn) throws ApsSystemException {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(UPDATE_PAGE);
			stat.setString(1, page.getParentCode());
			stat.setString(2, page.getGroup());
			stat.setString(3, page.getCode());
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error while updating the page record",  t);
			throw new RuntimeException("Error while updating the page record", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	@Override
	public void setPageOnline(String pageCode) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			this.deleteOnlineWidgets(pageCode, conn);
			this.deleteOnlinePageMetadata(pageCode, conn);
			
			this.executeQueryWithoutResultset(conn, SET_ONLINE_METADATA, pageCode);
			this.executeQueryWithoutResultset(conn, SET_ONLINE_WIDGETS, pageCode);
			
			Date now = new Date();
			this.updatePageMetadataDraftLastUpdate(pageCode, now , conn);
			this.updatePageMetadataOnlineLastUpdate(pageCode, now , conn);
			
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error while setting page '{}' as online", pageCode,  t);
			throw new RuntimeException("Error while setting page " + pageCode + " as online", t);
		} finally {
			closeConnection(conn);
		}
	}
	
	// TODO Verificare quali servono
	private void saveOnlinePageMetadata(String pageCode, PageMetadata pageMetadata, Connection conn) throws ApsSystemException {
		String tableName = PageMetadataOnline.TABLE_NAME;
		boolean isAdd = this.existsPageMetadata(pageCode, tableName, conn);
		this.savePageMetadata(pageCode, pageMetadata, isAdd, tableName, conn);
	}
	private void saveDraftPageMetadata(String pageCode, PageMetadata pageMetadata, Connection conn) throws ApsSystemException {
		String tableName = PageMetadataDraft.TABLE_NAME;
		boolean isAdd = this.existsPageMetadata(pageCode, tableName, conn);
		this.savePageMetadata(pageCode, pageMetadata, isAdd, tableName, conn);
	}
	
	private void addOnlinePageMetadata(String pageCode, PageMetadata pageMetadata, Connection conn) throws ApsSystemException {
		this.savePageMetadata(pageCode, pageMetadata, true, PageMetadataOnline.TABLE_NAME, conn);
	}
	private void addDraftPageMetadata(String pageCode, PageMetadata pageMetadata, Connection conn) throws ApsSystemException {
		this.savePageMetadata(pageCode, pageMetadata, true, PageMetadataDraft.TABLE_NAME, conn);
	}
	
	private void updateOnlinePageMetadata(String pageCode, PageMetadata pageMetadata, Connection conn) throws ApsSystemException {
		this.savePageMetadata(pageCode, pageMetadata, false, PageMetadataOnline.TABLE_NAME, conn);
	}
	private void updateDraftPageMetadata(String pageCode, PageMetadata pageMetadata, Connection conn) throws ApsSystemException {
		this.savePageMetadata(pageCode, pageMetadata, false, PageMetadataDraft.TABLE_NAME, conn);
	}
	
	protected void deleteOnlinePageMetadata(String pageCode, Connection conn) throws ApsSystemException {
		this.executeQueryWithoutResultset(conn,  DELETE_ONLINE_PAGE_METADATA, pageCode);
	}
	protected void deleteDraftPageMetadata(String pageCode, Connection conn) throws ApsSystemException {
		this.executeQueryWithoutResultset(conn,  DELETE_DRAFT_PAGE_METADATA, pageCode);
	}
	
	protected void savePageMetadata(String pageCode, PageMetadata pageMetadata, boolean isAdd, String tableName, Connection conn) throws ApsSystemException {
		if (pageMetadata!=null) {
			PreparedStatement stat = null;
			try {
				StringBuilder query = new StringBuilder(isAdd ? ADD_PAGE_METADATA_START : UPDATE_PAGE_METADATA_START);
				query.append(tableName).append(isAdd ? ADD_PAGE_METADATA_END : UPDATE_PAGE_METADATA_END);
				stat = conn.prepareStatement(query.toString());
				int index = 1;
				if (isAdd) {
					stat.setString(index++, pageCode);
				}
				stat.setString(index++, pageMetadata.getTitles().toXml());
				stat.setString(index++, pageMetadata.getModel().getCode());
				if (pageMetadata.isShowable()) {
					stat.setInt(index++, 1);
				} else {
					stat.setInt(index++, 0);
				}
				String extraConfig = this.getExtraConfig(pageMetadata);
				stat.setString(index++, extraConfig);
				Date updatedAt = pageMetadata.getUpdatedAt();
				stat.setTimestamp(index++, updatedAt!=null ? new java.sql.Timestamp(updatedAt.getTime()) : null);
				if (!isAdd) {
					stat.setString(index++, pageCode);
				}
				stat.executeUpdate();
			} catch (Throwable t) {
				_logger.error("Error while saving the page metadata record for table {}", tableName,  t);
				throw new RuntimeException("Error while saving the page metadata record for table " + tableName, t);
			} finally {
				closeDaoResources(null, stat);
			}
		}
	}
	
	protected boolean existsPageMetadata(String pageCode, String tableName, Connection conn) throws ApsSystemException {
		boolean exists = false;
		PreparedStatement stat = null;
		ResultSet res = null;
		try {
			StringBuilder query = new StringBuilder(EXISTS_PAGE_METADATA_PREFIX).append(tableName).append(PAGE_METADATA_WHERE_CODE);
			stat = conn.prepareStatement(query.toString());
			stat.setString(1, pageCode);
			res = stat.executeQuery();
			if (res.next()) {
				exists = true;
			}
		} catch (Throwable t) {
			_logger.error("Error while checking the page metadata existance for table {} and page ", tableName, pageCode,  t);
			throw new RuntimeException("Error while checking the page metadata existance for table " + tableName + " and page " + pageCode, t);
		} finally {
			closeDaoResources(res, stat);
		}
		return exists;
	}
	
	protected PageMetadata createPageMetadata(String code, ResultSet res, int startIndex) throws Throwable {
		PageMetadata pageMetadata = new PageMetadata();
		int index = startIndex;
		String titleText = res.getString(index++);
		ApsProperties titles = new ApsProperties();
		try {
			titles.loadFromXml(titleText);
		} catch (Throwable t) {
			_logger.error("IO error detected while parsing the titles of the page {}", code, t);
			String msg = "IO error detected while parsing the titles of the page '" + code + "'";
			throw new ApsSystemException(msg, t);
		}
		pageMetadata.setTitles(titles);
		
		pageMetadata.setModel(this.getPageModelManager().getPageModel(res.getString(index++)));
		
		Integer showable = new Integer (res.getInt(index++));
		pageMetadata.setShowable(showable.intValue() == 1);
		
		String extraConfig = res.getString(index++);
		if (null != extraConfig && extraConfig.trim().length() > 0) {
			try {
				PageExtraConfigDOM configDom = new PageExtraConfigDOM();
				configDom.addExtraConfig(pageMetadata, extraConfig);
			} catch (Throwable t) {
				_logger.error("IO error detected while parsing the extra config of the page {}", code, t);
				String msg = "IO error detected while parsing the extra config of the page '" + code + "'";
				throw new ApsSystemException(msg, t);
			}
		}
		Timestamp date = res.getTimestamp(index++);
		pageMetadata.setUpdatedAt(date != null ? new Date(date.getTime()) : null);
		return pageMetadata;
	}
	
	protected String getExtraConfig(PageMetadata pageMetadata) {
		PageExtraConfigDOM dom = this.getExtraConfigDOM();
		return dom.extractXml(pageMetadata);
	}
	
	protected PageExtraConfigDOM getExtraConfigDOM() {
		return new PageExtraConfigDOM();
	}
	
	protected void addWidgetForPage(IPage page, WidgetConfigDest dest, Connection conn) throws ApsSystemException {
		PreparedStatement stat = null;
		try {
			Widget[] widgets = null;
			String query = "";
			if (dest == WidgetConfigDest.ON_LINE) {
				query = ADD_WIDGET_FOR_PAGE;
				widgets = page.getOnlineWidgets();
			} else if(dest == WidgetConfigDest.DRAFT) {
				query = ADD_WIDGET_FOR_PAGE_DRAFT;
				widgets = page.getDraftWidgets();
			}
			if (null == widgets) return;
			
			String pageCode = page.getCode();
			stat = conn.prepareStatement(query);
			for (int i = 0; i < widgets.length; i++) {
				Widget widget = widgets[i];
				if (widget != null) {
					if (null == widget.getType()) {
						_logger.error("Widget Type null when adding widget on frame '{}' of page '{}'", i, page.getCode());
						continue;
					}
					this.valueAddWidgetStatement(pageCode, i, widget, stat);
					stat.addBatch();
					stat.clearParameters();
				}
			}
			stat.executeBatch();
		} catch (Throwable t) {
			_logger.error("Error while inserting the widgets in a page",  t);
			throw new RuntimeException("Error while inserting the widgets in a page", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	@Override
	public void removeWidget(IPage page, int pos) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			
			this.removeWidget(page.getCode(), pos, conn);
			Date now = new Date();
			this.updatePageMetadataDraftLastUpdate(page.getCode(), now, conn);
			page.getDraftMetadata().setUpdatedAt(now);
			
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error removing the widget from page '{}', frame {}", page.getCode(), pos,  t);
			throw new RuntimeException("Error removing the widget from page '" + page.getCode() + "', frame " + pos, t);
		} finally {
			closeConnection(conn);
		}
	}
	
	protected void removeWidget(String pageCode, int pos, Connection conn) {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(DELETE_WIDGET_FOR_PAGE_DRAFT);
			stat.setString(1, pageCode);
			stat.setInt(2, pos);
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error removing the widget from page '{}', frame {}", pageCode, pos,  t);
			throw new RuntimeException("Error removing the widget from page '" + pageCode + "', frame " + pos, t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	@Override
	public void joinWidget(IPage page, Widget widget, int pos) {
		String pageCode = page.getCode();
		Connection conn = null;
		PreparedStatement stat = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			
			this.removeWidget(pageCode, pos, conn);
			stat = conn.prepareStatement(ADD_WIDGET_FOR_PAGE_DRAFT);
			this.valueAddWidgetStatement(pageCode, pos, widget, stat);
			stat.executeUpdate();
			
			Date now = new Date();
			this.updatePageMetadataDraftLastUpdate(pageCode, now, conn);
			page.getDraftMetadata().setUpdatedAt(now);
			
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error adding a widget in the frame '{}' of the page '{}'", pos, pageCode, t);
			throw new RuntimeException("Error adding a widget in the frame " +pos+" of the page '"+pageCode+"'", t);
		} finally {
			closeDaoResources(null, stat, conn);
		}
	}
	
	private void valueAddWidgetStatement(String pageCode, int pos, Widget widget, PreparedStatement stat) throws Throwable {
		stat.setString(1, pageCode);
		stat.setInt(2, pos);
		stat.setString(3, widget.getType().getCode());
		if (!widget.getType().isLogic()) {
			String config = null;
			if (null != widget.getConfig()) {
				config = widget.getConfig().toXml();
			}
			stat.setString(4, config);
		} else {
			stat.setNull(4, Types.VARCHAR);
		}
	}
	
	@Override
	public void movePage(IPage currentPage, IPage newParent) {
		Connection conn = null;
		PreparedStatement stat = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			//position
			int pos = 1;
			IPage[] sisters = newParent.getAllChildren();
			if (null != sisters && sisters.length > 0) {
				IPage last = sisters[sisters.length - 1];
				if (null != last) {
					pos = last.getPosition() + 1;
				} else {
					pos = sisters.length + 1;
				}
			}
			stat = conn.prepareStatement(UPDATE_PAGE_TREE_POSITION);
			stat.setString(1, newParent.getCode());
			stat.setInt(2, pos);
			stat.setString(3, currentPage.getCode());
			stat.executeUpdate();
			this.shiftPages(currentPage.getParentCode(), currentPage.getPosition(), conn);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error while moving the page {} under {}" + newParent, currentPage, newParent, t);
			throw new RuntimeException("Error while moving the page " + currentPage + " under " + newParent, t);
		} finally {
			this.closeDaoResources(null, stat, conn);
		}
	}
	
	private void updatePageMetadataDraftLastUpdate(String pageCode, Date date, Connection conn) throws SQLException {
		this.updatePageMetatataUpdate(pageCode, date, PageMetadataDraft.TABLE_NAME, conn);
	}
	private void updatePageMetadataOnlineLastUpdate(String pageCode, Date date, Connection conn) throws SQLException {
		this.updatePageMetatataUpdate(pageCode, date, PageMetadataOnline.TABLE_NAME, conn);
	}
	private void updatePageMetatataUpdate(String pageCode, Date date, String tablename, Connection conn) throws SQLException {
		PreparedStatement stat = null;
		try {
			StringBuilder query = new StringBuilder("UPDATE ").append(tablename).append( " SET updatedat = ? WHERE code = ?");
			stat = conn.prepareStatement(query.toString());
			stat.setTimestamp(1, new Timestamp(date.getTime()));
			stat.setString(2, pageCode);
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error while updating the page metadata record for table {} and page {}", PageMetadataDraft.TABLE_NAME, pageCode,  t);
			throw new RuntimeException("Error while updating the page metadata record for table " + PageMetadataDraft.TABLE_NAME + " and page " + pageCode, t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	protected IPageModelManager getPageModelManager() {
		return _pageModelManager;
	}
	public void setPageModelManager(IPageModelManager pageModelManager) {
		this._pageModelManager = pageModelManager;
	}

	public IWidgetTypeManager getWidgetTypeManager() {
		return _widgetTypeManager;
	}
	
	public void setWidgetTypeManager(IWidgetTypeManager widgetTypeManager) {
		this._widgetTypeManager = widgetTypeManager;
	}

	private IPageModelManager _pageModelManager;
	private IWidgetTypeManager _widgetTypeManager;
	
	// attenzione: l'ordinamento deve rispettare prima l'ordine delle pagine
	// figlie nelle pagine madri, e poi l'ordine dei widget nella pagina.
	private static final String ALL_PAGES = 
		"SELECT p.parentcode, p.pos, p.code, p.groupcode, "
		+ "onl.titles, onl.modelcode, onl.showinmenu, onl.extraconfig, onl.updatedat, "
		+ "drf.titles, drf.modelcode, drf.showinmenu, drf.extraconfig, drf.updatedat, "
		+ "wonl.framepos, wonl.widgetcode, wonl.config, "
		+ "wdrf.framepos, wdrf.widgetcode, wdrf.config FROM pages p "
		+ "LEFT JOIN " + PageMetadataOnline.TABLE_NAME + " onl ON p.code = onl.code "
		+ "LEFT JOIN " + PageMetadataDraft.TABLE_NAME + " drf ON p.code = drf.code "
		+ "LEFT JOIN " + WidgetConfig.TABLE_NAME + " wonl ON p.code = wonl.pagecode "
		+ "LEFT JOIN " + WidgetConfigDraft.TABLE_NAME + " wdrf ON p.code = wdrf.pagecode "
		+ "ORDER BY p.parentcode, p.pos, p.code, wonl.framepos, wdrf.framepos ";
	
	private static final String ADD_PAGE = 
		"INSERT INTO pages(code, parentcode, pos, groupcode) VALUES ( ? , ? , ? , ? )";

	private static final String DELETE_PAGE = 
		"DELETE FROM pages WHERE code = ? ";

	private static final String DELETE_WIDGETS_FOR_PAGE_ONLINE = 
		"DELETE FROM " + WidgetConfig.TABLE_NAME + " WHERE pagecode = ? ";

//	private static final String DELETE_WIDGET_FOR_PAGE_ONLINE = 
//		DELETE_WIDGETS_FOR_PAGE + " AND framepos = ? ";

	private static final String DELETE_WIDGETS_FOR_PAGE_DRAFT = 
			"DELETE FROM " + WidgetConfigDraft.TABLE_NAME + " WHERE pagecode = ? ";

	private static final String DELETE_WIDGET_FOR_PAGE_DRAFT  = 
			DELETE_WIDGETS_FOR_PAGE_DRAFT + " AND framepos = ? ";

	
	
	private static final String MOVE_UP = 
		"UPDATE pages SET pos = (pos - 1) WHERE code = ? ";

	private static final String MOVE_DOWN = 
		"UPDATE pages SET pos = (pos + 1) WHERE code = ? ";

	private static final String UPDATE_PAGE = 
		"UPDATE pages SET parentcode = ?, groupcode = ? WHERE code = ? ";

	private static final String SHIFT_PAGE = 
		"UPDATE pages SET pos = (pos - 1) WHERE parentcode = ? AND pos > ? ";

	private static final String ADD_WIDGET_FOR_PAGE = 
		"INSERT INTO " + WidgetConfig.TABLE_NAME + " (pagecode, framepos, widgetcode, config) VALUES ( ? , ? , ? , ? )";

	private static final String ADD_WIDGET_FOR_PAGE_DRAFT = 
		"INSERT INTO " + WidgetConfigDraft.TABLE_NAME + " (pagecode, framepos, widgetcode, config) VALUES ( ? , ? , ? , ? )";

	private static final String MOVE_WIDGET =
		"UPDATE " + WidgetConfigDraft.TABLE_NAME + " SET framepos = ? WHERE pagecode = ? and framepos = ? ";

	private static final String UPDATE_PAGE_TREE_POSITION = 
			"UPDATE pages SET parentcode = ? , pos =?  WHERE code = ? ";

	private static final String EXISTS_PAGE_METADATA_PREFIX = "SELECT code FROM ";

	private static final String LOAD_PAGE_METADATA_PREFIX = 
			"SELECT titles, modelcode, showinmenu, extraconfig, updatedat FROM ";

	private static final String PAGE_METADATA_WHERE_CODE = " WHERE code = ?";

	private static final String ADD_PAGE_METADATA_END = 
			" (code, titles, modelcode, showinmenu, extraconfig, updatedat) VALUES (?, ?, ?, ?, ?, ?) ";

//	private static final String ADD_ONLINE_PAGE_METADATA = "INSERT INTO " + PageMetadataOnline.TABLE_NAME + ADD_PAGE_METADATA_END;
//	private static final String ADD_DRAFT_PAGE_METADATA  = "INSERT INTO " + PageMetadataDraft.TABLE_NAME  + ADD_PAGE_METADATA_END;
	
	private static final String UPDATE_PAGE_METADATA_END = 
			"SET titles = ?, modelcode = ?, showinmenu = ?, extraconfig = ?, updatedat = ? " + PAGE_METADATA_WHERE_CODE;
	
//	private static final String UPDATE_ONLINE_PAGE_METADATA = "UPDATE " + PageMetadataOnline.TABLE_NAME + UPDATE_PAGE_METADATA_END;
//	private static final String UPDATE_DRAFT_PAGE_METADATA  = "UPDATE " + PageMetadataDraft.TABLE_NAME  + UPDATE_PAGE_METADATA_END;
	
	private static final String ADD_PAGE_METADATA_START = "INSERT INTO ";
	
	private static final String UPDATE_PAGE_METADATA_START = "UPDATE ";
	
	private static final String DELETE_ONLINE_PAGE_METADATA = "DELETE FROM " + PageMetadataOnline.TABLE_NAME + PAGE_METADATA_WHERE_CODE;
	private static final String DELETE_DRAFT_PAGE_METADATA  = "DELETE FROM " + PageMetadataDraft.TABLE_NAME  + PAGE_METADATA_WHERE_CODE;
	
	private static final String SET_ONLINE_METADATA = 
			"INSERT INTO " + PageMetadataOnline.TABLE_NAME
			+ " (code, titles, modelcode, showinmenu, extraconfig, updatedat) SELECT code, titles, modelcode, showinmenu, extraconfig, updatedat FROM "
			+ PageMetadataDraft.TABLE_NAME + " WHERE code = ?";
	
	private static final String SET_ONLINE_WIDGETS = 
			"INSERT INTO " + WidgetConfig.TABLE_NAME
			+ " (pagecode, framepos, widgetcode, config) SELECT pagecode, framepos, widgetcode, config FROM "
			+ WidgetConfigDraft.TABLE_NAME + " WHERE pagecode = ?";
	
	private static final String UPDATE_ONLINE_DATE = "UPDATE " + PageMetadataOnline.TABLE_NAME + " SET updatedat = ?" + PAGE_METADATA_WHERE_CODE;
	private static final String UPDATE_DRAFT_DATE  = "UPDATE " + PageMetadataDraft.TABLE_NAME  + " SET updatedat = ?" + PAGE_METADATA_WHERE_CODE;
	
}