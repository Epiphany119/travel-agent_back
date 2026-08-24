package com.travel.module.note.biz.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.core.exception.BusinessException;
import com.travel.module.note.biz.api.dto.*;
import com.travel.module.note.biz.infra.persistence.NoteBlockMapper;
import com.travel.module.note.biz.infra.persistence.NoteDocumentMapper;
import com.travel.module.note.biz.infra.persistence.NoteBlockPO;
import com.travel.module.note.biz.infra.persistence.NoteDocumentPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 笔记应用服务：负责文档与内容块的 CRUD，以及分享、复制等业务编排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteApplicationService {

    private final NoteDocumentMapper documentMapper;
    private final NoteBlockMapper blockMapper;

    // ---------------- 查询 ----------------

    /**
     * 查询用户的笔记文档列表（不含内容块）。
     */
    public List<NoteDocumentResponse> listDocs(String userId) {
        LambdaQueryWrapper<NoteDocumentPO> q = new LambdaQueryWrapper<>();
        q.eq(NoteDocumentPO::getUserId, userId)
         .orderByDesc(NoteDocumentPO::getUpdatedAt);
        return documentMapper.selectList(q).stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询单篇笔记完整内容（含内容块）。
     */
    public NoteDocumentResponse getDoc(Long id, String userId) {
        NoteDocumentPO doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (userId != null && !userId.isBlank() && !doc.getUserId().equals(userId)) {
            throw new BusinessException(404, "笔记不存在");
        }
        List<NoteBlockResponse> blocks = findBlocks(id);
        return toDetailResponse(doc, blocks);
    }

    /**
     * 通过分享 token 查看笔记（无需登录权限校验，仅供分享浏览）。
     */
    public NoteDocumentResponse getDocByShareToken(String token) {
        LambdaQueryWrapper<NoteDocumentPO> q = new LambdaQueryWrapper<>();
        q.eq(NoteDocumentPO::getShareToken, token);
        NoteDocumentPO doc = documentMapper.selectOne(q);
        if (doc == null) {
            throw new BusinessException(404, "分享的笔记不存在或已失效");
        }
        return toDetailResponse(doc, findBlocks(doc.getId()));
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
        doc.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(doc.getCreatedAt());
        documentMapper.insert(doc);

        saveBlocks(doc.getId(), request.getBlocks());
        return getDoc(doc.getId(), null);
    }

    /**
     * 更新笔记文档属性与内容块（整段覆盖 blocks）。
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
        doc.setDestination(request.getDestination());
        doc.setCoverUrl(request.getCoverUrl());
        if (request.getVisibility() != null) {
            doc.setVisibility("link".equals(request.getVisibility()) ? "link" : "private");
        }
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);

        saveBlocks(id, request.getBlocks());
        return getDoc(id, null);
    }

    /**
     * 删除笔记（连同内容块）。
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
        blockMapper.delete(new LambdaQueryWrapper<NoteBlockPO>().eq(NoteBlockPO::getDocumentId, id));
        documentMapper.deleteById(id);
    }

    // ---------------- 私有工具 ----------------

    private List<NoteBlockResponse> findBlocks(Long documentId) {
        LambdaQueryWrapper<NoteBlockPO> q = new LambdaQueryWrapper<>();
        q.eq(NoteBlockPO::getDocumentId, documentId)
         .orderByAsc(NoteBlockPO::getSortOrder);
        return blockMapper.selectList(q).stream()
                .map(b -> NoteBlockResponse.builder()
                        .id(b.getId())
                        .type(b.getType())
                        .text(b.getText())
                        .sortOrder(b.getSortOrder())
                        .attrsJson(b.getAttrsJson())
                        .build())
                .collect(Collectors.toList());
    }

    /** 整段覆盖内容块：先删后插，并按传入顺序重排 sortOrder。 */
    private void saveBlocks(Long documentId, List<NoteBlockRequest> blocks) {
        blockMapper.delete(new LambdaQueryWrapper<NoteBlockPO>().eq(NoteBlockPO::getDocumentId, documentId));
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        int order = 0;
        for (NoteBlockRequest req : blocks) {
            NoteBlockPO po = new NoteBlockPO();
            po.setDocumentId(documentId);
            po.setType(req.getType() == null ? "p" : req.getType());
            po.setText(req.getText() == null ? "" : req.getText());
            po.setSortOrder(order++);
            po.setAttrsJson(req.getAttrsJson());
            blockMapper.insert(po);
        }
    }

    private NoteDocumentResponse toListResponse(NoteDocumentPO doc) {
        return NoteDocumentResponse.builder()
                .id(doc.getId())
                .userId(doc.getUserId())
                .title(doc.getTitle())
                .destination(doc.getDestination())
                .coverUrl(doc.getCoverUrl())
                .visibility(doc.getVisibility())
                .shareToken(doc.getShareToken())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private NoteDocumentResponse toDetailResponse(NoteDocumentPO doc, List<NoteBlockResponse> blocks) {
        NoteDocumentResponse resp = toListResponse(doc);
        resp.setBlocks(blocks);
        return resp;
    }

    private String blankDefault(String v, String d) {
        return v == null || v.isBlank() ? d : v;
    }
}
