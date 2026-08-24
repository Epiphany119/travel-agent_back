package com.travel.module.note.biz.api;

import com.travel.common.core.result.ApiResult;
import com.travel.module.note.biz.api.dto.NoteDocumentRequest;
import com.travel.module.note.biz.api.dto.NoteDocumentResponse;
import com.travel.module.note.biz.application.NoteApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 旅行笔记控制器 - 类飞书文档的在线编辑笔记。
 *
 * <p>路径前缀：{@code /api/notes}。支持文档 CRUD、按分享 token 查看、复制。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteApplicationService noteService;

    /** 查询用户笔记列表（不含内容块） */
    @GetMapping
    public ApiResult<List<NoteDocumentResponse>> list(
            @RequestParam(defaultValue = "user_001") String userId) {
        return ApiResult.success(noteService.listDocs(userId));
    }

    /** 获取单篇笔记完整内容 */
    @GetMapping("/{id}")
    public ApiResult<NoteDocumentResponse> get(
            @PathVariable Long id,
            @RequestParam(required = false) String userId) {
        return ApiResult.success(noteService.getDoc(id, userId));
    }

    /** 通过分享 token 查看笔记 */
    @GetMapping("/share/{token}")
    public ApiResult<NoteDocumentResponse> getByShare(
            @PathVariable String token) {
        return ApiResult.success(noteService.getDocByShareToken(token));
    }

    /** 创建笔记 */
    @PostMapping
    public ApiResult<NoteDocumentResponse> create(
            @Valid @RequestBody NoteDocumentRequest request,
            @RequestParam(defaultValue = "user_001") String userId) {
        return ApiResult.success(noteService.create(userId, request));
    }

    /** 更新笔记（属性 + 内容块整段覆盖） */
    @PutMapping("/{id}")
    public ApiResult<NoteDocumentResponse> update(
            @PathVariable Long id,
            @RequestBody NoteDocumentRequest request,
            @RequestParam(defaultValue = "user_001") String userId) {
        return ApiResult.success(noteService.update(id, userId, request));
    }

    /** 删除笔记 */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(
            @PathVariable Long id,
            @RequestParam(defaultValue = "user_001") String userId) {
        noteService.delete(id, userId);
        return ApiResult.success();
    }
}
