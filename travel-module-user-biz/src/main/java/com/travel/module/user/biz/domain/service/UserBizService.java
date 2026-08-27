package com.travel.module.user.biz.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.module.user.biz.infra.persistence.*;
import com.travel.module.user.biz.infra.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserBizService {

    private final InspirationMapper inspirationMapper;
    private final JourneyMapper journeyMapper;
    private final JourneyPointMapper journeyPointMapper;
    private final JourneyImageMapper journeyImageMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserTravelPreferenceMapper userTravelPreferenceMapper;
    private final TravelNoteMapper travelNoteMapper;
    private final ImageStorageService imageStorageService;
    private final JdbcTemplate jdbcTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<TravelNotePO> listTravelNotes(String userId) {
        LambdaQueryWrapper<TravelNotePO> w = new LambdaQueryWrapper<>();
        w.eq(TravelNotePO::getUserId, userId).orderByDesc(TravelNotePO::getUpdatedAt);
        return travelNoteMapper.selectList(w);
    }

    public TravelNotePO getTravelNote(Long id) { return travelNoteMapper.selectById(id); }

    public TravelNotePO getSharedTravelNote(String token) {
        return travelNoteMapper.selectOne(new LambdaQueryWrapper<TravelNotePO>().eq(TravelNotePO::getShareToken, token).eq(TravelNotePO::getVisibility, "link"));
    }

    @Transactional
    public TravelNotePO saveTravelNote(TravelNotePO note) {
        if (note.getUserId() == null) note.setUserId("user_001");
        if (note.getTemplateVersion() == null) note.setTemplateVersion(1);
        if (note.getNoteType() == null) note.setNoteType("inspiration");
        if (note.getSourceType() == null) note.setSourceType("manual");
        if (note.getStatus() == null) note.setStatus("draft");
        if (note.getVisibility() == null) note.setVisibility("private");
        if (note.getContentJson() == null || note.getContentJson().isBlank()) note.setContentJson("{\"overview\":{},\"days\":[],\"budget\":{\"items\":[]},\"strategies\":[],\"reminders\":[]}");
        if (note.getShareToken() == null && "link".equals(note.getVisibility())) note.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        if (note.getId() == null) travelNoteMapper.insert(note); else travelNoteMapper.updateById(note);
        return note;
    }

    @Transactional
    public TravelNotePO copyTravelNote(Long id, String userId) {
        TravelNotePO src = travelNoteMapper.selectById(id);
        if (src == null) return null;
        src.setId(null); src.setUserId(userId == null ? "user_001" : userId); src.setSourceType("copy"); src.setStatus("draft"); src.setVisibility("private"); src.setShareToken(null); src.setTitle(src.getTitle() + " · 副本");
        return saveTravelNote(src);
    }

    public void deleteTravelNote(Long id) { travelNoteMapper.deleteById(id); }

    public List<Map<String,Object>> searchUsers(String q) {
        String key = q == null ? "" : q.trim();
        return jdbcTemplate.queryForList("SELECT public_id, nickname, avatar FROM user_profile WHERE public_id LIKE ? OR nickname LIKE ? LIMIT 20", "%" + key + "%", "%" + key + "%");
    }
    public List<Map<String,Object>> listPublicNotes(int page, int size) { return listPublicNotes(page, size, null, null); }
    public List<Map<String,Object>> listPublicNotes(int page, int size, String q, String tag) {
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = Math.max(0, page) * safeSize;
        StringBuilder sql = new StringBuilder("SELECT n.id,n.user_id,n.title,n.content,n.cover_url,n.destination,n.tags,n.like_count,n.comment_count,n.favorite_count,n.created_at, COALESCE(NULLIF(n.author_name,''),p.nickname,n.user_id) author, COALESCE(NULLIF(n.author_avatar,''),p.avatar,'') author_avatar FROM social_note n LEFT JOIN user_profile p ON p.public_id=n.user_id WHERE n.visibility='public' AND n.status='published'");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (q != null && !q.isBlank()) { sql.append(" AND (n.title LIKE ? OR n.content LIKE ? OR n.destination LIKE ?)"); String key="%"+q.trim()+"%"; args.add(key); args.add(key); args.add(key); }
        if (tag != null && !tag.isBlank()) { sql.append(" AND JSON_CONTAINS(n.tags, JSON_QUOTE(?))"); args.add(tag.trim()); }
        sql.append(" ORDER BY n.created_at DESC LIMIT ? OFFSET ?"); args.add(safeSize); args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }
    public Map<String,Object> getPublicNote(Long id) {
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT * FROM social_note WHERE id=? AND visibility='public'", id);
        return rows.isEmpty() ? null : rows.get(0);
    }
    @Transactional
    public Map<String,Object> updateSocialNote(Long id, String userId, Map<String,Object> body) {
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT user_id FROM social_note WHERE id=?", id);
        if (rows.isEmpty()) return Map.of("updated", false);
        String owner = String.valueOf(rows.get(0).get("user_id"));
        if (!Objects.equals(owner, userId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "无权修改该帖子");
        String tags = body.get("tags") instanceof Collection ? new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(body.get("tags")).toString() : String.valueOf(body.getOrDefault("tags", "[]"));
        jdbcTemplate.update("UPDATE social_note SET title=?,content=?,cover_url=?,destination=?,tags=?,author_name=?,author_avatar=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                String.valueOf(body.getOrDefault("title", "旅行笔记")), String.valueOf(body.getOrDefault("content", "")), String.valueOf(body.getOrDefault("coverUrl", "")), String.valueOf(body.getOrDefault("destination", "")), tags,
                String.valueOf(body.getOrDefault("authorName", "")), String.valueOf(body.getOrDefault("authorAvatar", "")), id);
        return getPublicNote(id);
    }
    @Transactional
    public TravelNotePO copySocialNote(Long id, String userId) {
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT * FROM social_note WHERE id=? AND visibility='public'", id);
        if (rows.isEmpty()) return null;
        Map<String,Object> source = rows.get(0);
        Object linkedId = source.get("travel_note_id");
        if (linkedId instanceof Number) {
            TravelNotePO linked = copyTravelNote(((Number) linkedId).longValue(), userId);
            if (linked != null) return linked;
        }
        TravelNotePO copy = new TravelNotePO();
        copy.setUserId(userId == null || userId.isBlank() ? "user_001" : userId);
        copy.setTitle(String.valueOf(source.getOrDefault("title", "旅行笔记")) + " · 副本");
        copy.setDestination(String.valueOf(source.getOrDefault("destination", "")));
        copy.setNoteType("inspiration");
        copy.setSourceType("copy");
        copy.setStatus("draft");
        copy.setVisibility("private");
        copy.setCoverUrl(String.valueOf(source.getOrDefault("cover_url", "")));
        try {
            Map<String,Object> document = new LinkedHashMap<>();
            document.put("version", 1);
            document.put("format", "html");
            document.put("origin", "copy");
            document.put("blocks", List.of(Map.of("id", "body", "type", "rich-text", "html", String.valueOf(source.getOrDefault("content", "")))));
            copy.setContentJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(document));
        } catch (Exception e) {
            copy.setContentJson("{\"version\":1,\"format\":\"html\",\"blocks\":[{\"id\":\"body\",\"type\":\"rich-text\",\"html\":\"\"}]}");
        }
        return saveTravelNote(copy);
    }
    @Transactional public void reactNote(Long noteId, String userId, String type) {
        int exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM social_reaction WHERE note_id=? AND user_id=? AND reaction_type=?", Integer.class, noteId, userId, type);
        String count = "like".equals(type) ? "like_count" : "favorite_count";
        if (exists == 0) { jdbcTemplate.update("INSERT INTO social_reaction(note_id,user_id,reaction_type) VALUES(?,?,?)", noteId,userId,type); jdbcTemplate.update("UPDATE social_note SET " + count + "=" + count + "+1 WHERE id=?", noteId); }
        else { jdbcTemplate.update("DELETE FROM social_reaction WHERE note_id=? AND user_id=? AND reaction_type=?", noteId,userId,type); jdbcTemplate.update("UPDATE social_note SET " + count + "=GREATEST(" + count + "-1,0) WHERE id=?", noteId); }
    }
    public void addComment(Long noteId, String userId, String content) { jdbcTemplate.update("INSERT INTO social_comment(note_id,user_id,content) VALUES(?,?,?)", noteId,userId,content); jdbcTemplate.update("UPDATE social_note SET comment_count=comment_count+1 WHERE id=?", noteId); }
    public List<Map<String,Object>> listComments(Long noteId) { return jdbcTemplate.queryForList("SELECT * FROM social_comment WHERE note_id=? ORDER BY created_at ASC", noteId); }
    public void requestFriend(String from, String to, String message) { jdbcTemplate.update("INSERT INTO social_friend_request(requester_id,receiver_id,message) VALUES(?,?,?) ON DUPLICATE KEY UPDATE status='pending',message=VALUES(message)", from,to,message == null ? "" : message); }
    public Map<String,Object> publishSocialNote(Map<String,Object> body) {
        String tags = body.get("tags") instanceof java.util.Collection ? new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(body.get("tags")).toString() : String.valueOf(body.getOrDefault("tags", "[]"));
        jdbcTemplate.update("INSERT INTO social_note(user_id,travel_note_id,title,content,cover_url,destination,tags,author_name,author_avatar) VALUES(?,?,?,?,?,?,?,?,?)", body.getOrDefault("userId","user_001"), body.get("travelNoteId"), body.getOrDefault("title","旅行笔记"), body.getOrDefault("content",""), body.getOrDefault("coverUrl",""), body.getOrDefault("destination", ""), tags, body.getOrDefault("authorName", ""), body.getOrDefault("authorAvatar", ""));
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("published", true);
        result.put("id", jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        return result;
    }

    @Value("${travel.amap.api-key:}")
    private String amapApiKey;

    // ========== 地理编码（景点名称 → 经纬度） ==========
    public Map<String, BigDecimal> geocodeAddress(String address) {
        if (amapApiKey == null || amapApiKey.isBlank()) {
            // API Key 未配置时返回空结果
            return Map.of();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://restapi.amap.com/v3/geocode/geo")
                    .queryParam("key", amapApiKey)
                    .queryParam("address", address)
                    .build()
                    .toUriString();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return Map.of();
            String status = String.valueOf(response.get("status"));
            if (!"1".equals(status)) return Map.of();
            var geocodes = (java.util.List<Map<String, Object>>) response.get("geocodes");
            if (geocodes == null || geocodes.isEmpty()) return Map.of();
            String location = (String) geocodes.get(0).get("location");
            if (location == null || location.isBlank()) return Map.of();
            String[] parts = location.split(",");
            if (parts.length != 2) return Map.of();
            BigDecimal lng = new BigDecimal(parts[0].trim());
            BigDecimal lat = new BigDecimal(parts[1].trim());
            return Map.of("longitude", lng, "latitude", lat);
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ========== 用户偏好 ==========
    public UserPreferencePO getPreference(String userId) {
        LambdaQueryWrapper<UserPreferencePO> w = new LambdaQueryWrapper<>();
        w.eq(UserPreferencePO::getUserId, userId);
        w.eq(UserPreferencePO::getPreferenceType, "default");
        UserPreferencePO existing = userPreferenceMapper.selectOne(w);
        if (existing == null) {
            // 首次访问自动创建一条默认记录，保证 nickname 有持久化位置
            existing = new UserPreferencePO();
            existing.setUserId(userId);
            existing.setPreferenceType("default");
            userPreferenceMapper.insert(existing);
        }
        return existing;
    }

    @Transactional
    public void savePreference(UserPreferencePO po) {
        UserPreferencePO existing = getPreference(po.getUserId());
        if (existing != null) {
            po.setId(existing.getId());
            po.setCreatedAt(existing.getCreatedAt());
            userPreferenceMapper.updateById(po);
        } else {
            po.setPreferenceType("default");
            userPreferenceMapper.insert(po);
        }
    }

    // ========== 灵感目的地 ==========
    public List<InspirationPO> listInspirations(String userId) {
        LambdaQueryWrapper<InspirationPO> w = new LambdaQueryWrapper<>();
        w.eq(InspirationPO::getUserId, userId);
        w.orderByDesc(InspirationPO::getSortOrder).orderByDesc(InspirationPO::getCreatedAt);
        return inspirationMapper.selectList(w);
    }

    @Transactional
    public InspirationPO addInspiration(InspirationPO po) {
        if (po.getUserId() == null || po.getUserId().isBlank()) po.setUserId("user_001");
        if (po.getName() == null) po.setName("");
        if (po.getImageUrl() == null) po.setImageUrl("");
        if (po.getQuote() == null) po.setQuote("");
        if (po.getBestSeason() == null) po.setBestSeason("");
        if (po.getStatus() == null) po.setStatus(1);
        if (po.getPriority() == null) po.setPriority(0);
        if (po.getEstimatedBudget() == null) po.setEstimatedBudget(0);
        if (po.getSortOrder() == null) po.setSortOrder(0);
        inspirationMapper.insert(po);
        return po;
    }

    @Transactional
    public void updateInspiration(InspirationPO po) {
        inspirationMapper.updateById(po);
    }

    @Transactional
    public void deleteInspiration(Long id) {
        inspirationMapper.deleteById(id);
    }

    // ========== 旅程记录 ==========
    public List<JourneyPO> listJourneys(String userId) {
        LambdaQueryWrapper<JourneyPO> w = new LambdaQueryWrapper<>();
        w.eq(JourneyPO::getUserId, userId);
        w.orderByDesc(JourneyPO::getStartDate);
        return journeyMapper.selectList(w);
    }

    public JourneyPO getJourney(Long id) {
        return journeyMapper.selectById(id);
    }

    /**
     * 获取用户所有的旅程进度（含途经地点与图片），供前端详情展示
     */
    public List<JourneyDetailVO> listJourneyDetails(String userId) {
        List<JourneyPO> journeys = listJourneys(userId);
        return journeys.stream().map(j -> buildDetail(j.getId())).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取单个旅程完整详情（含途经地点与图片）
     */
    public JourneyDetailVO getJourneyDetail(Long id) {
        JourneyPO j = journeyMapper.selectById(id);
        if (j == null) return null;
        return buildDetail(id);
    }

    private JourneyDetailVO buildDetail(Long id) {
        return JourneyDetailVO.builder()
                .journey(journeyMapper.selectById(id))
                .points(listJourneyPoints(id))
                .images(listJourneyImages(id))
                .build();
    }

    @Transactional
    public JourneyPO addJourney(JourneyPO po) {
        // 为必填字段设置默认值，避免 NOT NULL 报错
        if (po.getSummary() == null) po.setSummary("");
        if (po.getTravelType() == null) po.setTravelType("");
        if (po.getCompanions() == null) po.setCompanions("");
        if (po.getWeatherInfo() == null) po.setWeatherInfo("");
        if (po.getStatus() == null) po.setStatus(1);
        if (po.getStartDate() == null) po.setStartDate(LocalDate.now());
        if (po.getEndDate() == null) po.setEndDate(LocalDate.now());
        if (po.getTotalDays() == null) po.setTotalDays(0);
        if (po.getTotalCost() == null) po.setTotalCost(0);
        if (po.getRating() == null) po.setRating(0);
        journeyMapper.insert(po);
        return po;
    }

    @Transactional
    public void updateJourney(JourneyPO po) {
        journeyMapper.updateById(po);
    }

    @Transactional
    public void deleteJourney(Long id) {
        journeyPointMapper.delete(new LambdaQueryWrapper<JourneyPointPO>().eq(JourneyPointPO::getJourneyId, id));
        journeyImageMapper.delete(new LambdaQueryWrapper<JourneyImagePO>().eq(JourneyImagePO::getJourneyId, id));
        journeyMapper.deleteById(id);
    }

    // ========== 途经地点 ==========
    public List<JourneyPointPO> listJourneyPoints(Long journeyId) {
        LambdaQueryWrapper<JourneyPointPO> w = new LambdaQueryWrapper<>();
        w.eq(JourneyPointPO::getJourneyId, journeyId);
        w.orderByAsc(JourneyPointPO::getSortOrder);
        return journeyPointMapper.selectList(w);
    }

    @Transactional
    public void saveJourneyPoints(Long journeyId, List<JourneyPointPO> points) {
        journeyPointMapper.delete(new LambdaQueryWrapper<JourneyPointPO>().eq(JourneyPointPO::getJourneyId, journeyId));
        for (JourneyPointPO p : points) {
            p.setJourneyId(journeyId);
            // 为必填字段设置默认值，避免 NOT NULL 报错
            if (p.getLatitude() == null) p.setLatitude(java.math.BigDecimal.ZERO);
            if (p.getLongitude() == null) p.setLongitude(java.math.BigDecimal.ZERO);
            if (p.getVisitDate() == null) p.setVisitDate(LocalDate.now());
            if (p.getDescription() == null) p.setDescription("");
            if (p.getSortOrder() == null) p.setSortOrder(0);
            journeyPointMapper.insert(p);
        }
    }

    // ========== 旅程照片 ==========
    public List<JourneyImagePO> listJourneyImages(Long journeyId) {
        LambdaQueryWrapper<JourneyImagePO> w = new LambdaQueryWrapper<>();
        w.eq(JourneyImagePO::getJourneyId, journeyId);
        w.orderByAsc(JourneyImagePO::getSortOrder);
        return journeyImageMapper.selectList(w);
    }

    @Transactional
    public void saveJourneyImages(Long journeyId, List<JourneyImagePO> images) {
        journeyImageMapper.delete(new LambdaQueryWrapper<JourneyImagePO>().eq(JourneyImagePO::getJourneyId, journeyId));
        for (JourneyImagePO img : images) {
            img.setJourneyId(journeyId);
            journeyImageMapper.insert(img);
        }
    }

    // ========== 用户头像 ==========
    public String getUserProfile(String userId) {
        UserProfilePO profile = userProfileMapper.findByUserId(userId);
        return profile != null ? profile.getAvatar() : null;
    }

    public String uploadAvatar(MultipartFile file, String userId) throws Exception {
        // 保存文件到用户家目录下的 travel-agent-uploads 目录
        String fileName = file.getOriginalFilename();
        String ext = fileName != null && fileName.contains(".") 
            ? fileName.substring(fileName.lastIndexOf(".")) 
            : ".jpg";
        String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;
        
        String uploadDir = System.getProperty("user.home") + "/travel-agent-uploads/avatar/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File dest = new File(uploadDir + newFileName);
        file.transferTo(dest);
        
        // 生成访问URL
        String avatarUrl = "/uploads/avatar/" + newFileName;
        
        // 更新数据库
        UserProfilePO profile = new UserProfilePO();
        profile.setUserId(userId);
        profile.setUsername(userId);
        profile.setAvatar(avatarUrl);
        profile.setAvatarUrl(avatarUrl);
        saveUserProfile(profile);
        
        return avatarUrl;
    }

    /**
     * 通用图片上传
     * @param file 上传的文件
     * @param category 分类目录: inspiration, journey, general
     * @return 图片访问 URL
     */
    public String uploadImage(MultipartFile file, String category) {
        return imageStorageService.store(file, category);
    }

    public void saveUserProfile(UserProfilePO po) {
        UserProfilePO existing = userProfileMapper.findByUserId(po.getUserId());
        if (existing != null) {
            po.setId(existing.getId());
            po.setCreatedAt(existing.getCreatedAt());
            userProfileMapper.updateByUserId(po);
        } else {
            userProfileMapper.insert(po);
        }
    }

    // ========== 用户昵称（从 user_travel_preference.name 读取） ==========
    public String getUserNickname(String userId) {
        UserTravelPreferencePO pref = userTravelPreferenceMapper.findByUserIdAndType(userId, "default");
        if (pref != null && pref.getName() != null && !pref.getName().isBlank()) {
            return pref.getName();
        }
        return "旅人";
    }

    /**
     * 更新 user_travel_preference.name（用于侧边栏昵称持久化）
     */
    public void updateNickname(String userId, String nickname) {
        UserTravelPreferencePO pref = userTravelPreferenceMapper.findByUserIdAndType(userId, "default");
        if (pref == null) {
            pref = new UserTravelPreferencePO();
            pref.setUserId(userId);
            pref.setPreferenceType("default");
            pref.setName(nickname);
            userTravelPreferenceMapper.insert(pref);
        } else {
            pref.setName(nickname);
            userTravelPreferenceMapper.update(pref);
        }
    }
}
