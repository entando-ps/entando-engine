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
package org.entando.entando.aps.system.services.activitystream;

import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.system.services.keygenerator.IKeyGeneratorManager;
import java.util.List;
import org.aspectj.lang.annotation.Before;
import org.entando.entando.aps.system.services.actionlog.IActionLogManager;
import org.entando.entando.aps.system.services.actionlog.model.ActionLogRecord;
import org.entando.entando.aps.system.services.activitystream.model.ActivityStreamComment;
import org.entando.entando.aps.system.services.activitystream.model.ActivityStreamLikeInfo;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.event.ProfileChangedEvent;
import org.entando.entando.aps.system.services.userprofile.event.ProfileChangedObserver;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

public class SocialActivityStreamManager extends AbstractService implements ISocialActivityStreamManager, ProfileChangedObserver {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(SocialActivityStreamManager.class);

    private static final String LIKE_RECORDS_CACHE_GROUP = "ActivityStreamLikeRecords_cacheGroup";

    private static final String COMMENT_RECORDS_CACHE_GROUP = "ActivityStreamCommentRecords_cacheGroup";

    private IActionLogManager _actionLogManager;

    private ISocialActivityStreamDAO _socialActivityStreamDAO;

    private IKeyGeneratorManager _keyGeneratorManager;

    private IUserProfileManager _userProfileManager;

    private ICacheInfoManager cacheInfoManager;

    public void init() throws Exception {
        _logger.debug("{} ready", this.getClass().getName());
    }

    @Override
    @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'ActivityStreamLikeRecords_id_'.concat(#id)")
    public void editActionLikeRecord(int id, String username, boolean add) throws EntException {
        try {
            this.getSocialActivityStreamDAO().editActionLikeRecord(id, username, add);
            this.getActionLogManager().updateRecordDate(id);
        } catch (Throwable t) {
            _logger.error("Error editing activity stream like records", t);
            throw new EntException("Error editing activity stream like records", t);
        }
    }

    @Override
    @Cacheable(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'ActivityStreamLikeRecords_id_'.concat(#id)")
    public List<ActivityStreamLikeInfo> getActionLikeRecords(int id) throws EntException {
        List<ActivityStreamLikeInfo> infos = null;
        try {
            infos = this.getSocialActivityStreamDAO().getActionLikeRecords(id);
            if (null != infos) {
                for (int i = 0; i < infos.size(); i++) {
                    ActivityStreamLikeInfo asli = infos.get(i);
                    String username = asli.getUsername();
                    IUserProfile profile = this.getUserProfileManager().getProfile(username);
                    String displayName = (null != profile) ? profile.getDisplayName() : username;
                    asli.setDisplayName(displayName);
                }
                String cacheKey = "ActivityStreamLikeRecords_id_" + id;
                this.getCacheInfoManager().putInGroup(ICacheInfoManager.DEFAULT_CACHE_NAME, cacheKey, new String[]{LIKE_RECORDS_CACHE_GROUP});
            }
        } catch (Throwable t) {
            _logger.error("Error extracting activity stream like records", t);
            throw new EntException("Error extracting activity stream like records", t);
        }
        return infos;
    }

    @Override
    public void updateFromProfileChanged(ProfileChangedEvent event) {
        try {
            this.getCacheInfoManager().flushGroup(ICacheInfoManager.DEFAULT_CACHE_NAME, LIKE_RECORDS_CACHE_GROUP);
            this.getCacheInfoManager().flushGroup(ICacheInfoManager.DEFAULT_CACHE_NAME, COMMENT_RECORDS_CACHE_GROUP);
        } catch (Throwable t) {
            _logger.error("Error flushing cache group", t);
        }
    }

    @Override
    @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'ActivityStreamCommentRecords_id_'.concat(#streamId)")
    public void addActionCommentRecord(String username, String commentText, int streamId) throws EntException {
        try {
            Integer key = null;
            ActionLogRecord record = null;
            do {
                key = this.getKeyGeneratorManager().getUniqueKeyCurrentValue();
                record = this.getActionLogManager().getActionRecord(key);
            } while (null != record);
            this.getSocialActivityStreamDAO().addActionCommentRecord(key, streamId, username, commentText);
            this.getActionLogManager().updateRecordDate(streamId);
        } catch (Throwable t) {
            _logger.error("Error adding a comment record to stream with id:{}", streamId, t);
            throw new EntException("Error adding a comment record", t);
        }
    }

    @Override
    @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'ActivityStreamCommentRecords_id_'.concat(#streamId)")
    public void deleteActionCommentRecord(int id, int streamId) throws EntException {
        try {
            this.getSocialActivityStreamDAO().deleteActionCommentRecord(id);
            this.getActionLogManager().updateRecordDate(streamId);
        } catch (Throwable t) {
            _logger.error("Error deleting comment with id {} from stream with id {}", id, streamId, t);
            throw new EntException("Error deleting comment", t);
        }
    }

    @Override
    @Cacheable(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'ActivityStreamCommentRecords_id_'.concat(#id)")
    public List<ActivityStreamComment> getActionCommentRecords(int id) throws EntException {
        List<ActivityStreamComment> infos = null;
        try {
            infos = this.getSocialActivityStreamDAO().getActionCommentRecords(id);
            if (null != infos) {
                for (int i = 0; i < infos.size(); i++) {
                    ActivityStreamComment comment = infos.get(i);
                    String username = comment.getUsername();
                    IUserProfile profile = this.getUserProfileManager().getProfile(username);
                    String displayName = (null != profile) ? profile.getDisplayName() : username;
                    comment.setDisplayName(displayName);
                }
                String cacheKey = "ActivityStreamCommentRecords_id_" + id;
                this.getCacheInfoManager().putInGroup(ICacheInfoManager.DEFAULT_CACHE_NAME, cacheKey, new String[]{COMMENT_RECORDS_CACHE_GROUP});
            }
        } catch (Throwable t) {
            _logger.error("Error extracting activity stream like records for stream with id {}", id, t);
            throw new EntException("Error extracting activity stream like records", t);
        }
        return infos;
    }

    @Before("execution(* org.entando.entando.aps.system.services.actionlog.IActionLogManager.deleteActionRecord(..)) && args(id,..)")
    public void listenDeleteActionRecord(Integer id) {
        try {
            this.getCacheInfoManager().flushEntry(ICacheInfoManager.DEFAULT_CACHE_NAME, "ActivityStreamCommentRecords_id_" + id);
            this.getCacheInfoManager().flushEntry(ICacheInfoManager.DEFAULT_CACHE_NAME, "ActivityStreamLikeRecords_id_" + id);
            this.getSocialActivityStreamDAO().deleteSocialRecordsRecord(id);
        } catch (Throwable t) {
            _logger.error("Error deleting action record", t);
        }
    }

    protected IActionLogManager getActionLogManager() {
        return _actionLogManager;
    }

    public void setActionLogManager(IActionLogManager actionLogManager) {
        this._actionLogManager = actionLogManager;
    }

    protected ISocialActivityStreamDAO getSocialActivityStreamDAO() {
        return _socialActivityStreamDAO;
    }

    public void setSocialActivityStreamDAO(ISocialActivityStreamDAO socialActivityStreamDAO) {
        this._socialActivityStreamDAO = socialActivityStreamDAO;
    }

    protected IKeyGeneratorManager getKeyGeneratorManager() {
        return _keyGeneratorManager;
    }

    public void setKeyGeneratorManager(IKeyGeneratorManager keyGeneratorManager) {
        this._keyGeneratorManager = keyGeneratorManager;
    }

    protected IUserProfileManager getUserProfileManager() {
        return _userProfileManager;
    }

    public void setUserProfileManager(IUserProfileManager userProfileManager) {
        this._userProfileManager = userProfileManager;
    }

    protected ICacheInfoManager getCacheInfoManager() {
        return this.cacheInfoManager;
    }
    @Autowired
    public void setCacheInfoManager(ICacheInfoManager cacheInfoManager) {
        this.cacheInfoManager = cacheInfoManager;
    }
    
}
