package com.travel.module.note.biz.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.core.exception.BusinessException;
import com.travel.module.note.biz.api.dto.*;
import com.travel.module.note.biz.infra.persistence.NoteDocumentMapper;
import com.travel.module.note.biz.infra.persistence.NoteDocumentPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 笔记应用服务：负责文档 CRUD，内容以 Markdown 文本直接存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteApplicationService {

    private final NoteDocumentMapper documentMapper;

    // ---------------- 查询 ----------------

    /**
     * 查询用户的笔记文档列表。
     */
    public List<NoteDocumentResponse> listDocs(String userId) {
        LambdaQueryWrapper<NoteDocumentPO> q = new LambdaQueryWrapper<>();
        q.eq(NoteDocumentPO::getUserId, userId)
         .orderByDesc(NoteDocumentPO::getUpdatedAt);
        return documentMapper.selectList(q).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询单篇笔记完整内容。
     */
    public NoteDocumentResponse getDoc(Long id, String userId) {
        NoteDocumentPO doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (userId != null && !userId.isBlank() && !doc.getUserId().equals(userId)) {
            throw new BusinessException(404, "笔记不存在");
        }
        return toResponse(doc);
    }

    /**
     * 通过分享 token 查看笔记。
     */
    public NoteDocumentResponse getDocByShareToken(String token) {
        LambdaQueryWrapper<NoteDocumentPO> q = new LambdaQueryWrapper<>();
        q.eq(NoteDocumentPO::getShareToken, token)
         .eq(NoteDocumentPO::getVisibility, "link");
        NoteDocumentPO doc = documentMapper.selectOne(q);
        if (doc == null) {
            throw new BusinessException(404, "分享的笔记不存在或已失效");
        }
        return toResponse(doc);
    }

    // ---------------- 写入 ----------------

    /**
     * 创建笔记。
     */
    @Transactional
    public NoteDocumentResponse create(String userId, NoteDocumentRequest request) {
        NoteDocumentPO doc = new NoteDocumentPO();
        doc.setUserId(blankDefault(userId, "user_001"));
        doc.setTitle(blankDefault(request.getTitle(), "未命名笔记"));
        doc.setDestination(request.getDestination());
        doc.setCoverUrl(request.getCoverUrl());
        doc.setVisibility("link".equals(request.getVisibility()) ? "link" : "private");
        doc.setStatus("draft");
        // Only link-visible notes receive a bearer token. Private notes must not
        // accidentally become readable through a previously issued URL.
        if ("link".equals(doc.getVisibility())) {
            doc.setShareToken(newShareToken());
        }
        doc.setThemeJson(request.getThemeJson());
        doc.setContent(request.getContent());
        doc.setSourceSocialNoteId(request.getSourceSocialNoteId());
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(doc.getCreatedAt());
        documentMapper.insert(doc);

        return getDoc(doc.getId(), null);
    }

    /**
     * 更新笔记。
     */
    @Transactional
    public NoteDocumentResponse update(Long id, String userId, NoteDocumentRequest request) {
        NoteDocumentPO doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (!doc.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改该笔记");
        }
        if (request.getTitle() != null) {
            doc.setTitle(request.getTitle().isBlank() ? "未命名笔记" : request.getTitle().trim());
        }
        if (request.getDestination() != null) {
            doc.setDestination(request.getDestination());
        }
        if (request.getCoverUrl() != null) {
            doc.setCoverUrl(request.getCoverUrl());
        }
        if (request.getVisibility() != null) {
            String visibility = "link".equals(request.getVisibility()) ? "link" : "private";
            doc.setVisibility(visibility);
            if ("link".equals(visibility)) {
                if (doc.getShareToken() == null || doc.getShareToken().isBlank()) {
                    doc.setShareToken(newShareToken());
                }
            } else {
                // Revoke the old URL when a note becomes private.
                doc.setShareToken(null);
            }
        }
        if (!"link".equals(doc.getVisibility())) {
            doc.setShareToken(null);
        }
        if (request.getThemeJson() != null) {
            doc.setThemeJson(request.getThemeJson());
        }
        if (request.getContent() != null) {
            doc.setContent(request.getContent());
        }
        if (request.getSourceSocialNoteId() != null) {
            doc.setSourceSocialNoteId(request.getSourceSocialNoteId());
        }
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        return getDoc(id, null);
    }

    /**
     * 删除笔记。
     */
    @Transactional
    public void delete(Long id, String userId) {
        NoteDocumentPO doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (!doc.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该笔记");
        }
        documentMapper.deleteById(id);
    }

    // ---------------- 私有工具 ----------------

    private NoteDocumentResponse toResponse(NoteDocumentPO doc) {
        return NoteDocumentResponse.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .title(doc.getTitle())
                .destination(doc.getDestination())
                .coverUrl(doc.getCoverUrl())
                .visibility(doc.getVisibility())
                .shareToken(doc.getShareToken())
                .status(doc.getStatus())
                .themeJson(doc.getThemeJson())
                .content(doc.getContent())
                .sourceSocialNoteId(doc.getSourceSocialNoteId())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private String blankDefault(String v, String d) {
        return v == null || v.isBlank() ? d : v;
    }

    private String newShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
