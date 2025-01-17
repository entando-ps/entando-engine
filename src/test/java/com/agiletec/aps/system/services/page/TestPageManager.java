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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import java.util.Map;
import javax.sql.DataSource;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.services.mock.MockWidgetsDAO;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.IManager;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.util.ApsProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author M.Diana, E.Mezzano
 */
class TestPageManager extends BaseTestCase {

    @Test
    void testGetPage_1() throws Throwable {
        IPage root = _pageManager.getDraftRoot();
        assertNotNull(root);
        assertEquals("homepage", root.getCode());
        assertTrue(root.isOnline());
        assertNotNull(root.getMetadata());
        assertNotNull(root.getMetadata());
        assertEquals(7, root.getChildrenCodes().length);

        assertEquals("service", root.getChildrenCodes()[0]);
        assertEquals("pagina_1", root.getChildrenCodes()[1]);
        assertEquals("pagina_2", root.getChildrenCodes()[2]);
        assertEquals("coach_page", root.getChildrenCodes()[3]);
        assertEquals("customers_page", root.getChildrenCodes()[4]);
        assertEquals("administrators_page", root.getChildrenCodes()[5]);
        assertEquals("pagina_draft", root.getChildrenCodes()[6]);

        assertNotNull(_pageManager.getOnlinePage("homepage"));
        assertNotNull(_pageManager.getDraftPage("homepage"));
    }

    @Test
    void testGetPage_2() throws Throwable {
        IPage page1 = _pageManager.getOnlinePage("pagina_1");
        assertNotNull(page1);
        assertTrue(page1.isOnline());
        assertNotNull(page1.getMetadata());
        assertNotNull(page1.getMetadata());
        assertEquals(2, page1.getPosition());
        assertTrue(page1.getChildrenCodes().length > 1);
        assertTrue(page1.getWidgets().length > 1);
    }

    @Test
    void testGetPage_3() throws Throwable {
        assertNull(_pageManager.getOnlinePage("pagina_draft"));
        IPage draft = _pageManager.getDraftPage("pagina_draft");
        assertFalse(draft.isOnline());
        assertNotNull(draft.getMetadata());
        assertEquals(0, draft.getChildrenCodes().length);
        assertEquals(7, draft.getPosition());
    }

    @Test
    void testAddUpdateMoveDeletePage() throws Throwable {
        try {
            assertNull(this._pageManager.getDraftPage("temp"));
            assertNull(this._pageManager.getDraftPage("temp1"));
            assertNull(this._pageManager.getDraftPage("temp2"));
            this.checkAddPage();
            this.checkUpdatePage();
            this.executeMovePage();
            this.checkPutOnlineOfflinePage();
            this.deletePage();
        } catch (Throwable t) {
            throw t;
        } finally {
            _pageManager.deletePage("temp");
            _pageManager.deletePage("temp1");
            _pageManager.deletePage("temp2");
        }
    }

    private void checkAddPage() throws Throwable {
        IPage parentPage = _pageManager.getDraftPage("service");
        String parentForNewPage = parentPage.getParentCode();
        PageModel pageModel = this._pageModelManager.getPageModel(parentPage.getMetadata().getModelCode());
        PageMetadata metadata = PageTestUtil.createPageMetadata(pageModel,
                true, "pagina temporanea", null, null, false, null, null);
        ApsProperties config = PageTestUtil.createProperties("actionPath", "/myJsp.jsp", "param1", "value1");
        Widget widgetToAdd = PageTestUtil.createWidget("formAction", config);
        Widget[] widgets = {widgetToAdd};
        Page pageToAdd = PageTestUtil.createPage("temp", parentForNewPage, "free", pageModel, metadata, widgets);
        _pageManager.addPage(pageToAdd);

        IPage addedPage = _pageManager.getDraftPage("temp");
        PageTestUtil.comparePages(pageToAdd, addedPage, false);
        PageTestUtil.comparePageMetadata(pageToAdd.getMetadata(), addedPage.getMetadata(), 0);
        assertEquals(widgetToAdd.getConfig(), addedPage.getWidgets()[0].getConfig());
        assertEquals(widgetToAdd.getTypeCode(), addedPage.getWidgets()[0].getTypeCode());
        assertEquals(8, addedPage.getPosition());

        pageToAdd = (Page) pageToAdd.clone();
        pageToAdd.setCode("temp1");
        _pageManager.addPage(pageToAdd);
        addedPage = _pageManager.getDraftPage("temp1");
        PageTestUtil.comparePages(pageToAdd, addedPage, false);
        PageTestUtil.comparePageMetadata(pageToAdd.getMetadata(), addedPage.getMetadata(), 0);
        assertEquals(widgetToAdd.getConfig(), addedPage.getWidgets()[0].getConfig());
        assertEquals(widgetToAdd.getTypeCode(), addedPage.getWidgets()[0].getTypeCode());
        assertEquals(9, addedPage.getPosition());

        pageToAdd = (Page) pageToAdd.clone();
        pageToAdd.setCode("temp2");
        _pageManager.addPage(pageToAdd);
        addedPage = _pageManager.getDraftPage("temp2");
        assertNotNull(_pageManager.getDraftPage(addedPage.getCode()));
        assertNotNull(pageToAdd.getMetadata());
        assertEquals(widgetToAdd.getConfig(), addedPage.getWidgets()[0].getConfig());
        assertEquals(widgetToAdd.getTypeCode(), addedPage.getWidgets()[0].getTypeCode());
        assertEquals(10, addedPage.getPosition());
    }

    private void checkUpdatePage() throws Exception {
        Page dbPage = (Page) _pageManager.getDraftPage("temp");
        PageModel pageModel = this._pageModelManager.getPageModel(dbPage.getMetadata().getModelCode());
        Page pageToUpdate = PageTestUtil.createPage("temp", dbPage.getParentCode(), "free", pageModel, dbPage.getMetadata().clone(), PageTestUtil
                .copyArray(dbPage.getWidgets()));
        pageToUpdate.setPosition(dbPage.getPosition());
        PageMetadata onlineMetadata = pageToUpdate.getMetadata();
        onlineMetadata.setTitle("en", "temptitle1");
        onlineMetadata.setShowable(true);

        ApsProperties config = PageTestUtil.createProperties("actionPath", "/myJsp.jsp", "param1", "value1");
        Widget widgetToAdd = PageTestUtil.createWidget("formAction", config);
        pageToUpdate.getWidgets()[0] = widgetToAdd;
        _pageManager.setPageOnline(pageToUpdate.getCode());

        IPage updatedPage = _pageManager.getOnlinePage(dbPage.getCode());
        pageToUpdate = (Page) _pageManager.getOnlinePage(pageToUpdate.getCode());

        assertNotNull(updatedPage);
        PageTestUtil.comparePages(pageToUpdate, updatedPage, false);
        PageTestUtil.comparePageMetadata(pageToUpdate.getMetadata(), updatedPage.getMetadata(), 0);

        assertEquals(1, pageToUpdate.getMetadata().getTitles().size());
        PageTestUtil.compareWidgets(pageToUpdate.getWidgets(), updatedPage.getWidgets());
    }

    private void checkPutOnlineOfflinePage() throws Exception {
        String pageCode = "temp2";
        assertNull(_pageManager.getOnlinePage(pageCode));
        Page draftPage = (Page) _pageManager.getDraftPage(pageCode);
        assertNotNull(draftPage);
        assertFalse(draftPage.isOnline());
        assertFalse(draftPage.isChanged());

        _pageManager.setPageOnline(pageCode);
        Page onlinePage = (Page) _pageManager.getOnlinePage(pageCode);
        assertNotNull(onlinePage);
        assertTrue(onlinePage.isOnline());
        assertFalse(onlinePage.isChanged());
        PageTestUtil.comparePageMetadata(onlinePage.getMetadata(), onlinePage.getMetadata(), 0);
        PageTestUtil.compareWidgets(onlinePage.getWidgets(), onlinePage.getWidgets());

        _pageManager.setPageOffline(pageCode);
        assertNull(_pageManager.getOnlinePage(pageCode));
        Page offlinePage = (Page) _pageManager.getOnlinePage(pageCode);
        assertNull(offlinePage);
    }

    private void executeMovePage() throws Exception {
        int firstPos = 8;
        assertEquals(firstPos, _pageManager.getDraftPage("temp").getPosition());
        assertEquals(firstPos + 1, _pageManager.getDraftPage("temp1").getPosition());
        assertEquals(firstPos + 2, _pageManager.getDraftPage("temp2").getPosition());

        _pageManager.deletePage("temp");
        assertNull(_pageManager.getDraftPage("temp"));

        IPage temp1 = _pageManager.getDraftPage("temp1");
        IPage temp2 = _pageManager.getDraftPage("temp2");
        assertEquals(firstPos, temp1.getPosition());
        assertEquals(firstPos + 1, temp2.getPosition());

        _pageManager.movePage("temp2", true);
        IPage movedTemp1 = _pageManager.getDraftPage("temp1");
        IPage movedTemp2 = _pageManager.getDraftPage("temp2");
        IPage movedTemp2Parent = _pageManager.getDraftPage(movedTemp2.getParentCode());
        assertEquals(firstPos, movedTemp2.getPosition());
        assertEquals(firstPos + 1, movedTemp1.getPosition());

        String[] pages = movedTemp2Parent.getChildrenCodes();
        assertEquals("temp2", pages[pages.length - 2]);
        assertEquals("temp1", pages[pages.length - 1]);

        _pageManager.movePage("temp2", false);
        movedTemp1 = _pageManager.getDraftPage("temp1");
        movedTemp2 = _pageManager.getDraftPage("temp2");
        assertEquals(firstPos, movedTemp1.getPosition());
        assertEquals(firstPos + 1, movedTemp2.getPosition());
        movedTemp2Parent = _pageManager.getDraftPage(movedTemp2.getParentCode());
        pages = movedTemp2Parent.getChildrenCodes();
        assertEquals("temp1", pages[pages.length - 2]);
        assertEquals("temp2", pages[pages.length - 1]);
    }

    private void deletePage() throws Throwable {
        DataSource dataSource = (DataSource) this.getApplicationContext().getBean("portDataSource");
        MockWidgetsDAO mockWidgetsDAO = new MockWidgetsDAO();
        mockWidgetsDAO.setDataSource(dataSource);
        _pageManager.deletePage("temp");
        _pageManager.deletePage("temp2");
        IPage page = _pageManager.getDraftPage("temp");
        assertNull(page);
        boolean exists = true;
        try {
            exists = mockWidgetsDAO.exists("temp");
            assertEquals(false, exists);
            exists = mockWidgetsDAO.exists("temp2");
            assertEquals(false, exists);
        } catch (Throwable e) {
            throw e;
        }
    }

    @Test
    void testAddPublishPage() throws Throwable {
        try {
            this.addPagesForTest("test_add_", "pagina_11", 4);
            this.checkOrderAndPos("pagina_11", Arrays.asList("test_add_1", "test_add_2", "test_add_3", "test_add_4"));
            for (int i = 0; i < 4; i++) {
                IPage pageStartDraft = this._pageManager.getDraftPage("test_add_" + (i + 1));
                assertEquals(i + 1, pageStartDraft.getPosition());
                IPage pageStartOnline = this._pageManager.getOnlinePage("test_add_" + (i + 1));
                assertNull(pageStartOnline);
            }
            IPage onlineParent = this._pageManager.getOnlinePage("pagina_11");
            assertNotNull(onlineParent);
            assertEquals(0, onlineParent.getChildrenCodes().length);

            this._pageManager.setPageOnline("test_add_3");
            assertNotNull(this._pageManager.getOnlinePage("test_add_3"));
            onlineParent = this._pageManager.getOnlinePage("pagina_11");
            assertNotNull(onlineParent);
            assertEquals(1, onlineParent.getChildrenCodes().length);
            assertEquals("test_add_3", onlineParent.getChildrenCodes()[0]);

            assertNull(this._pageManager.getOnlinePage("test_add_1"));
            this._pageManager.setPageOnline("test_add_1");
            assertNotNull(this._pageManager.getOnlinePage("test_add_1"));
            onlineParent = this._pageManager.getOnlinePage("pagina_11");
            assertNotNull(onlineParent);
            assertEquals(2, onlineParent.getChildrenCodes().length);
            assertEquals("test_add_1", onlineParent.getChildrenCodes()[0]);
            assertEquals("test_add_3", onlineParent.getChildrenCodes()[1]);
            ((IManager) this._pageManager).refresh(); // to test cache refresh
            assertNotNull(this._pageManager.getOnlinePage("test_add_1"));
            onlineParent = this._pageManager.getOnlinePage("pagina_11");
            assertNotNull(onlineParent);
            assertEquals(2, onlineParent.getChildrenCodes().length);
            assertEquals("test_add_1", onlineParent.getChildrenCodes()[0]);
            assertEquals("test_add_3", onlineParent.getChildrenCodes()[1]);
        } catch (Exception e) {
            throw e;
        } finally {
            for (int i = 0; i < 4; i++) {
                this._pageManager.setPageOffline("test_add_" + (i + 1));
                this._pageManager.deletePage("test_add_" + (i + 1));
                this._pageManager.deletePage("dt_move_" + (i + 1));
            }
            for (int i = 0; i < 4; i++) {
                assertNull(this._pageManager.getDraftPage("test_add_" + (i + 1)));
            }
            IPage extractedParent = _pageManager.getDraftPage("pagina_11");
            assertEquals(0, extractedParent.getChildrenCodes().length);
        }
        ((IManager) this._pageManager).refresh();
        IPage extractedParent = _pageManager.getDraftPage("pagina_11");
        assertEquals(0, extractedParent.getChildrenCodes().length);
    }

    @Test
    void testChangeParent() throws Throwable {
        try {
            this.addPagesForTest("st_move_", "pagina_11", 4);
            this.addPagesForTest("dt_move_", "pagina_12", 4);
            for (int i = 0; i < 4; i++) {
                if (i % 2 == 1) {
                    this._pageManager.setPageOnline("st_move_" + (i + 1));
                    this._pageManager.setPageOnline("dt_move_" + (i + 1));
                }
            }
            this.checkOrderAndPos("pagina_11", Arrays.asList("st_move_1", "st_move_2", "st_move_3", "st_move_4"));
            this.checkOrderAndPos("pagina_12", Arrays.asList("dt_move_1", "dt_move_2", "dt_move_3", "dt_move_4"));
            for (int i = 0; i < 4; i++) {
                IPage pageStartDraft = this._pageManager.getDraftPage("st_move_" + (i + 1));
                IPage pageDestDraft = this._pageManager.getDraftPage("st_move_" + (i + 1));
                assertEquals(i + 1, pageStartDraft.getPosition());
                assertEquals(i + 1, pageDestDraft.getPosition());
                IPage pageStartOnline = this._pageManager.getOnlinePage("st_move_" + (i + 1));
                IPage pageDestOnline = this._pageManager.getOnlinePage("st_move_" + (i + 1));
                if (i % 2 == 1) {
                    assertEquals(i + 1, pageStartOnline.getPosition());
                    assertEquals(i + 1, pageDestOnline.getPosition());
                } else {
                    assertNull(pageStartOnline);
                    assertNull(pageDestOnline);
                }
            }

            boolean result = this._pageManager.movePage("pagina_1", "xxxxxx");
            assertFalse(result);

            result = this._pageManager.movePage("pagina_1", "pagina_12");
            assertFalse(result);

            result = this._pageManager.movePage("st_move_2", "pagina_12");
            assertTrue(result);
            for (int i = 0; i < 2; i++) {
                this.checkOrderAndPos("pagina_11", Arrays.asList("st_move_1", "st_move_3", "st_move_4"));
                this.checkOrderAndPos("pagina_12", Arrays.asList("dt_move_1", "dt_move_2", "dt_move_3", "dt_move_4", "st_move_2"));
                ((IManager) this._pageManager).refresh(); // to check the same values after cache refresh
            }

            result = this._pageManager.movePage("st_move_1", "pagina_12");
            assertTrue(result);
            for (int i = 0; i < 2; i++) {
                this.checkOrderAndPos("pagina_11", Arrays.asList("st_move_3", "st_move_4"));
                this.checkOrderAndPos("pagina_12", Arrays.asList("dt_move_1", "dt_move_2", "dt_move_3", "dt_move_4", "st_move_2", "st_move_1"));
                ((IManager) this._pageManager).refresh(); // to check the same values after cache refresh
            }

            result = this._pageManager.movePage("dt_move_3", "pagina_11");
            assertTrue(result);
            for (int i = 0; i < 2; i++) {
                this.checkOrderAndPos("pagina_11", Arrays.asList("st_move_3", "st_move_4", "dt_move_3"));
                this.checkOrderAndPos("pagina_12", Arrays.asList("dt_move_1", "dt_move_2", "dt_move_4", "st_move_2", "st_move_1"));
                ((IManager) this._pageManager).refresh(); // to check the same values after cache refresh
            }

        } catch (Exception e) {
            throw e;
        } finally {
            for (int i = 0; i < 4; i++) {
                this._pageManager.setPageOffline("st_move_" + (i + 1));
                this._pageManager.setPageOffline("dt_move_" + (i + 1));
                this._pageManager.deletePage("st_move_" + (i + 1));
                this._pageManager.deletePage("dt_move_" + (i + 1));
            }
            for (int i = 0; i < 4; i++) {
                assertNull(this._pageManager.getDraftPage("st_move_" + (i + 1)));
                assertNull(this._pageManager.getDraftPage("dt_move_" + (i + 1)));
            }
            IPage extractedParent = _pageManager.getDraftPage("pagina_11");
            assertEquals(0, extractedParent.getChildrenCodes().length);
        }
        ((IManager) this._pageManager).refresh();
        IPage extractedParent = _pageManager.getDraftPage("pagina_11");
        assertEquals(0, extractedParent.getChildrenCodes().length);
    }

    @Test
    void testMoveUpDown() throws Throwable {
        try {
            this.addPagesForTest("move_", "pagina_11", 6);
            for (int i = 0; i < 4; i++) {
                if (i % 2 == 1) {
                    this._pageManager.setPageOnline("move_" + (i + 1));
                }
            }
            this.checkOrderAndPos("pagina_11", Arrays.asList("move_1", "move_2", "move_3", "move_4", "move_5", "move_6"));
            for (int i = 0; i < 4; i++) {
                IPage pageStartDraft = this._pageManager.getDraftPage("move_" + (i + 1));
                assertEquals(i + 1, pageStartDraft.getPosition());
                IPage pageStartOnline = this._pageManager.getOnlinePage("move_" + (i + 1));
                if (i % 2 == 1) {
                    assertEquals(i + 1, pageStartOnline.getPosition());
                } else {
                    assertNull(pageStartOnline);
                }
            }

            boolean result = this._pageManager.movePage("move_2", false);
            assertTrue(result);
            this.checkOrderAndPos("pagina_11", Arrays.asList("move_1", "move_3", "move_2", "move_4", "move_5", "move_6"));
            ((IManager) this._pageManager).refresh();
            this.checkOrderAndPos("pagina_11", Arrays.asList("move_1", "move_3", "move_2", "move_4", "move_5", "move_6"));

            result = this._pageManager.movePage("move_3", true);
            assertTrue(result);
            this.checkOrderAndPos("pagina_11", Arrays.asList("move_3", "move_1", "move_2", "move_4", "move_5", "move_6"));
            ((IManager) this._pageManager).refresh();
            this.checkOrderAndPos("pagina_11", Arrays.asList("move_3", "move_1", "move_2", "move_4", "move_5", "move_6"));

            result = this._pageManager.movePage("move_3", true);
            assertFalse(result);
            result = this._pageManager.movePage("move_6", false);
            assertFalse(result);

            result = this._pageManager.movePage("move_5", false);
            assertTrue(result);
            this.checkOrderAndPos("pagina_11", Arrays.asList("move_3", "move_1", "move_2", "move_4", "move_6", "move_5"));
            ((IManager) this._pageManager).refresh();
            this.checkOrderAndPos("pagina_11", Arrays.asList("move_3", "move_1", "move_2", "move_4", "move_6", "move_5"));
        } catch (Exception e) {
            throw e;
        } finally {
            for (int i = 0; i < 6; i++) {
                this._pageManager.setPageOffline("move_" + (i + 1));
                this._pageManager.deletePage("move_" + (i + 1));
            }
            for (int i = 0; i < 6; i++) {
                assertNull(this._pageManager.getDraftPage("move_" + (i + 1)));
            }
            IPage extractedParent = _pageManager.getDraftPage("pagina_11");
            assertEquals(0, extractedParent.getChildrenCodes().length);
        }
        ((IManager) this._pageManager).refresh();
        IPage extractedParent = _pageManager.getDraftPage("pagina_11");
        assertEquals(0, extractedParent.getChildrenCodes().length);
    }

    private List<IPage> addPagesForTest(String codePrefix, String parentCode, int count) throws Throwable {
        List<IPage> pages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            IPage page = this.createPageForTest(codePrefix + (i + 1), parentCode);
            this._pageManager.addPage(page);
        }
        return pages;
    }

    private void checkOrderAndPos(String code, List<String> childOrder) throws Throwable {
        IPage extractedParent = _pageManager.getDraftPage(code);
        assertEquals(childOrder.size(), extractedParent.getChildrenCodes().length);
        assertEquals(childOrder, Arrays.asList(extractedParent.getChildrenCodes()));
        List<String> onlineCodes = new ArrayList<>();
        for (int i = 0; i < childOrder.size(); i++) {
            String childCode = childOrder.get(i);
            IPage draftPage = _pageManager.getDraftPage(childCode);
            assertEquals(code, draftPage.getParentCode());
            assertEquals(childOrder.indexOf(childCode) + 1, draftPage.getPosition());
            IPage onlinePage = _pageManager.getOnlinePage(childCode);
            if (null != onlinePage) {
                assertEquals(code, onlinePage.getParentCode());
                assertEquals(childOrder.indexOf(childCode) + 1, onlinePage.getPosition());
                onlineCodes.add(childCode);
            }
        }
        IPage extractedOnlineParent = _pageManager.getOnlinePage(code);
        if (null != extractedOnlineParent) {
            assertEquals(onlineCodes, Arrays.asList(extractedOnlineParent.getChildrenCodes()));
        }
    }

    private IPage createPageForTest(String code, String parentCode) throws Throwable {
        IPage prototype = _pageManager.getDraftPage("service");
        PageModel pageModel = this._pageModelManager.getPageModel(prototype.getMetadata().getModelCode());
        PageMetadata metadata = PageTestUtil.createPageMetadata(pageModel,
                true, "pagina temporanea", null, null, false, null, null);
        Widget[] widgets = new Widget[pageModel.getFrames().length];
        return PageTestUtil.createPage(code, parentCode, Group.FREE_GROUP_NAME, pageModel, metadata, widgets);
    }

    @Test
    void testFailureJoinWidget_1() {
        String pageCode = "wrongPageCode";
        int frame = 2;
        Assertions.assertThrows(EntException.class, () -> {
            Widget widget = this.getWidgetForTest("login", null);
            this._pageManager.joinWidget(pageCode, widget, frame);
        });
    }
    
    @Test
    void testFailureJoinWidget_2() throws Throwable {
        String pageCode = "pagina_1";
        int frame = 6;
        IPage pagina_1 = this._pageManager.getDraftPage(pageCode);
        assertTrue(pagina_1.getWidgets().length <= frame);
        Assertions.assertThrows(EntException.class, () -> {
            Widget widget = this.getWidgetForTest("login", null);
            this._pageManager.joinWidget(pageCode, widget, frame);
        });
        this._pageManager.updatePage(pagina_1);
    }

    @Test
    void testFailureRemoveWidget_1() throws Throwable {
        String pageCode = "wrongPageCode";
        int frame = 2;
        try {
            this._pageManager.removeWidget(pageCode, frame);
            fail();
        } catch (EntException e) {
            // Errore per pagina inesistente
        } catch (Throwable t) {
            throw t;
        }
    }

    @Test
    void testFailureRemoveWidget_2() throws Throwable {
        String pageCode = "pagina_1";
        int frame = 6;
        IPage pagina_1 = this._pageManager.getDraftPage(pageCode);
        assertTrue(pagina_1.getWidgets().length <= frame);
        try {
            this._pageManager.removeWidget(pageCode, frame);
            fail();
        } catch (EntException e) {
            // Errore per frame errato in modello
        } catch (Throwable t) {
            throw t;
        }
    }

    @Test
    void testJoinMoveRemoveWidget() throws Throwable {
        String pageCode = "pagina_1";
        int frame = 1;
        IPage pagina_1 = this._pageManager.getDraftPage(pageCode);
        assertNull(pagina_1.getWidgets()[frame]);
        try {

            Widget[] onlineWidgets = this._pageManager.getOnlinePage(pageCode).getWidgets();
            Widget[] draftWidgets = this._pageManager.getDraftPage(pageCode).getWidgets();
            onlineWidgets = PageTestUtil.getValuedWidgets(onlineWidgets);
            draftWidgets = PageTestUtil.getValuedWidgets(draftWidgets);
            assertEquals(onlineWidgets.length, draftWidgets.length);

            Widget widget = this.getWidgetForTest("login_form", null);
            this._pageManager.joinWidget(pageCode, widget, frame);

            pagina_1 = this._pageManager.getDraftPage(pageCode);
            assertTrue(pagina_1.isChanged());

            onlineWidgets = this._pageManager.getOnlinePage(pageCode).getWidgets();
            draftWidgets = this._pageManager.getDraftPage(pageCode).getWidgets();
            onlineWidgets = PageTestUtil.getValuedWidgets(onlineWidgets);
            draftWidgets = PageTestUtil.getValuedWidgets(draftWidgets);

            assertEquals(onlineWidgets.length + 1, draftWidgets.length);
            Widget extracted = pagina_1.getWidgets()[frame];
            assertNotNull(extracted);
            assertEquals("login_form", extracted.getTypeCode());

            this._pageManager.moveWidget(pageCode, frame, frame - 1);
            pagina_1 = this._pageManager.getDraftPage(pageCode);
            assertTrue(pagina_1.isChanged());

            onlineWidgets = this._pageManager.getOnlinePage(pageCode).getWidgets();
            draftWidgets = this._pageManager.getDraftPage(pageCode).getWidgets();
            onlineWidgets = PageTestUtil.getValuedWidgets(onlineWidgets);
            draftWidgets = PageTestUtil.getValuedWidgets(draftWidgets);

            assertEquals(onlineWidgets.length + 1, draftWidgets.length);
            assertNull(pagina_1.getWidgets()[frame]);
            extracted = pagina_1.getWidgets()[frame - 1];
            assertNotNull(extracted);
            assertEquals("login_form", extracted.getTypeCode());

            this._pageManager.moveWidget(pageCode, frame - 1, frame);
            pagina_1 = this._pageManager.getDraftPage(pageCode);
            assertTrue(pagina_1.isChanged());

            onlineWidgets = this._pageManager.getOnlinePage(pageCode).getWidgets();
            draftWidgets = this._pageManager.getDraftPage(pageCode).getWidgets();
            onlineWidgets = PageTestUtil.getValuedWidgets(onlineWidgets);
            draftWidgets = PageTestUtil.getValuedWidgets(draftWidgets);

            assertEquals(onlineWidgets.length + 1, draftWidgets.length);
            assertNull(pagina_1.getWidgets()[frame - 1]);
            extracted = pagina_1.getWidgets()[frame];
            assertNotNull(extracted);
            assertEquals("login_form", extracted.getTypeCode());

            this._pageManager.removeWidget(pageCode, frame);
            pagina_1 = this._pageManager.getDraftPage(pageCode);
            assertFalse(pagina_1.isChanged());

            onlineWidgets = this._pageManager.getOnlinePage(pageCode).getWidgets();
            draftWidgets = this._pageManager.getDraftPage(pageCode).getWidgets();
            onlineWidgets = PageTestUtil.getValuedWidgets(onlineWidgets);
            draftWidgets = PageTestUtil.getValuedWidgets(draftWidgets);

            assertEquals(onlineWidgets.length, draftWidgets.length);
            extracted = pagina_1.getWidgets()[frame];
            assertNull(extracted);
        } catch (Throwable t) {
            pagina_1.getWidgets()[frame] = null;
            this._pageManager.updatePage(pagina_1);
            throw t;
        }
    }

    @Test
    void testSearchPage() throws Throwable {
        List<String> allowedGroupCodes = new ArrayList<>();
        allowedGroupCodes.add(Group.ADMINS_GROUP_NAME);
        try {
            List<IPage> pagesFound = this._pageManager.searchPages("aGIna_", null, allowedGroupCodes);
            assertNotNull(pagesFound);
            assertEquals(5, pagesFound.size());
            String pageCodeToken = "agina";
            pagesFound = this._pageManager.searchPages(pageCodeToken, null, allowedGroupCodes);
            // verify the result found
            assertNotNull(pagesFound);
            Iterator<IPage> itr = pagesFound.iterator();
            assertEquals(6, pagesFound.size());
            while (itr.hasNext()) {
                IPage currentCode = itr.next();
                assertTrue(currentCode.getCode().contains(pageCodeToken));
            }
            pagesFound = this._pageManager.searchPages("", null, allowedGroupCodes);
            assertNotNull(pagesFound);
            assertEquals(17, pagesFound.size());
            pagesFound = this._pageManager.searchPages(null, null, allowedGroupCodes);
            assertNotNull(pagesFound);
            assertEquals(17, pagesFound.size());
        } catch (Throwable t) {
            throw t;
        }
    }

    @Test
    void testGetWidgetUtilizers() throws Throwable {
        List<IPage> pageUtilizers1 = this._pageManager.getDraftWidgetUtilizers(null);
        assertNotNull(pageUtilizers1);
        assertEquals(0, pageUtilizers1.size());

        List<IPage> pageUtilizers2 = this._pageManager.getDraftWidgetUtilizers("login_form");
        assertNotNull(pageUtilizers2);
        assertEquals(1, pageUtilizers2.size());

        List<IPage> pageUtilizers3 = this._pageManager.getDraftWidgetUtilizers("leftmenu");
        assertNotNull(pageUtilizers3);
        assertEquals(3, pageUtilizers3.size());
        assertEquals("pagina_1", pageUtilizers3.get(0).getCode());

        pageUtilizers3 = this._pageManager.getOnlineWidgetUtilizers("leftmenu");
        assertNotNull(pageUtilizers3);
        assertEquals(1, pageUtilizers3.size());
        assertEquals("pagina_1", pageUtilizers3.get(0).getCode());
    }

    @Test
    void testPageStatus() throws EntException {
        String testCode = "testcode";
        PagesStatus status = this._pageManager.getPagesStatus();
        try {
            IPage parentPage = _pageManager.getDraftRoot();
            PageModel pageModel = this._pageModelManager.getPageModel(parentPage.getMetadata().getModelCode());
            PageMetadata draftMeta = PageTestUtil.createPageMetadata(pageModel, true, "pagina temporanea", null, null, false, null, null);
            Page pageToAdd = PageTestUtil.createPage(testCode, parentPage.getCode(), "free", pageModel, draftMeta, new Widget[pageModel.getFrames().length]);
            _pageManager.addPage(pageToAdd);
            PagesStatus newStatus = this._pageManager.getPagesStatus();
            assertEquals(status.getOnline(), newStatus.getOnline());
            assertEquals(status.getOnlineWithChanges(), newStatus.getOnlineWithChanges());
            assertEquals(status.getUnpublished() + 1, newStatus.getUnpublished());
            assertEquals(status.getTotal() + 1, newStatus.getTotal());

            this._pageManager.setPageOnline(testCode);
            newStatus = this._pageManager.getPagesStatus();
            assertEquals(status.getOnline() + 1, newStatus.getOnline());
            assertEquals(status.getOnlineWithChanges(), newStatus.getOnlineWithChanges());
            assertEquals(status.getUnpublished(), newStatus.getUnpublished());
            assertEquals(status.getTotal() + 1, newStatus.getTotal());

            IPage test = this._pageManager.getDraftPage(testCode);
            test.getMetadata().setTitle("it", "modxxxx");

            this._pageManager.updatePage(test);
            newStatus = this._pageManager.getPagesStatus();
            assertEquals(status.getOnline(), newStatus.getOnline());
            assertEquals(status.getOnlineWithChanges() + 1, newStatus.getOnlineWithChanges());
            assertEquals(status.getUnpublished(), newStatus.getUnpublished());
            assertEquals(status.getTotal() + 1, newStatus.getTotal());

            this._pageManager.setPageOffline(testCode);
            newStatus = this._pageManager.getPagesStatus();
            assertEquals(status.getOnline(), newStatus.getOnline());
            assertEquals(status.getOnlineWithChanges(), newStatus.getOnlineWithChanges());
            assertEquals(status.getUnpublished() + 1, newStatus.getUnpublished());
            assertEquals(status.getTotal() + 1, newStatus.getTotal());

            this._pageManager.deletePage(testCode);
            newStatus = this._pageManager.getPagesStatus();
            assertEquals(status.getOnline(), newStatus.getOnline());
            assertEquals(status.getOnlineWithChanges(), newStatus.getOnlineWithChanges());
            assertEquals(status.getUnpublished(), newStatus.getUnpublished());
            assertEquals(status.getTotal(), newStatus.getTotal());
        } finally {
            this._pageManager.deletePage(testCode);
        }
    }

    private Widget getWidgetForTest(String widgetTypeCode, ApsProperties config) throws Throwable {
        Widget widget = new Widget();
        widget.setTypeCode(widgetTypeCode);
        if (null != config) {
            widget.setConfig(config);
        }
        return widget;
    }

    @Test
    void testGetDraftPage_should_load_draftPages() {
        String onlyDraftPageCode = "pagina_draft";
        IPage page = this._pageManager.getDraftPage(onlyDraftPageCode);
        assertNotNull(page);
        assertFalse(page.isOnline());
    }

    @Test
    void testGetDraftPage_should_load_onlinePages() {
        String onlinePageCode = "pagina_1";
        IPage page = this._pageManager.getDraftPage(onlinePageCode);
        assertNotNull(page);
        assertTrue(page.isOnline());
    }

    @Test
    void testGetOnlinePage_should_ignore_draftPages() {
        String onlyDraftPageCode = "pagina_draft";
        IPage page = this._pageManager.getOnlinePage(onlyDraftPageCode);
        assertNull(page);
    }

    @Test
    void testGetOnlinePage_should_load_onlinePages() {
        String onlinePageCode = "pagina_1";
        IPage page = this._pageManager.getOnlinePage(onlinePageCode);
        assertNotNull(page);
        assertTrue(page.isOnline());
        List<String> childs = Arrays.asList(page.getChildrenCodes());
        assertEquals(2, childs.size());
    }

    @Test
    void testGetOnlinePage_should_ignore_draftPageChildren() {
        String onlyDraftPageCode = "pagina_draft";
        String onlinePageCode = "homepage";
        IPage page = this._pageManager.getOnlinePage(onlinePageCode);
        assertNotNull(page);
        assertTrue(page.isOnline());
        List<String> childs = Arrays.asList(page.getChildrenCodes());
        for (String child : childs) {
            String code = child;
            assertFalse(code.equalsIgnoreCase(onlyDraftPageCode));
            IPage childPage = this._pageManager.getOnlinePage(code);
            assertTrue(childPage.isOnlineInstance());
        }
    }

    @Test
    void testGetDraftPage_should_load_draftPageChildren() {
        String onlyDraftPageCode = "pagina_draft";
        String onlinePageCode = "homepage";
        IPage page = this._pageManager.getDraftPage(onlinePageCode);
        assertNotNull(page);
        assertTrue(page.isOnline());
        List<String> childs = Arrays.asList(page.getChildrenCodes());
        boolean found = false;
        for (String child : childs) {
            String code = child;
            found = code.equals(onlyDraftPageCode);
        }
        assertTrue(found);
    }

    @Test
    void testUpdateParams() throws Throwable {
        ConfigInterface configManager = getApplicationContext().getBean(ConfigInterface.class);
        String value = this._pageManager.getConfig(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
        assertEquals("notfound", value);
        assertEquals(value, configManager.getParam(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE));

        Map<String, String> map = new HashMap<>();
        map.put(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE, "newValue");
        this._pageManager.updateParams(map);
        value = this._pageManager.getConfig(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
        assertEquals("newValue", value);
        assertEquals(value, configManager.getParam(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE));

        map.put(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE, "notfound");
        this._pageManager.updateParams(map);
        value = this._pageManager.getConfig(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
        assertEquals("notfound", value);
        assertEquals(value, configManager.getParam(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE));

        map.put("invalidKey", "value");
        this._pageManager.updateParams(map);
        assertNull(this._pageManager.getConfig("invalidKey"));
        assertNull(configManager.getParam("invalidKey"));
    }

    @BeforeEach
    private void init() {
        this._pageManager = (IPageManager) this.getService(SystemConstants.PAGE_MANAGER);
        this._pageModelManager = (IPageModelManager) this.getService(SystemConstants.PAGE_MODEL_MANAGER);
    }

    private IPageManager _pageManager = null;
    private IPageModelManager _pageModelManager = null;

}
