package com.travel.module.note.biz.api;

import com.travel.common.core.result.ApiResult;
import com.travel.module.note.biz.api.dto.NoteDocumentRequest;
import com.travel.module.note.biz.api.dto.NoteDocumentResponse;
import com.travel.module.note.biz.application.NoteApplicationService;
import com.travel.module.note.biz.infra.storage.NoteImageStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 旅行笔记控制器 - 类飞书文档的在线编辑笔记。
 *
 * <p>路径前缀：{@code /api/notes}。支持文档 CRUD、按分享 token 查看、复制、图片上传。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NoteController {

    private final NoteApplicationService noteService;
    private final NoteImageStorageService imageStorageService;

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

    /**
     * 上传笔记图片（粘贴 / 拖拽 / 选择上传统一入口）。
     *
     * <p>图片保存到用户本地目录 {@code ~/travel-agent-uploads/note/{userId}/}，
     * 数据库只记录访问路径，前端以 Markdown 图片语法写入笔记内容。</p>
     *
     * @param file   图片文件（jpg/jpeg/png/gif）
     * @param userId 所属用户
     * @return {"url": "/uploads/note/{userId}/xxx.jpg"}
     */
    @PostMapping("/upload")
    public ApiResult<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "user_001") String userId) {
        try {
            String url = imageStorageService.store(file, userId);
            return ApiResult.success(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            return ApiResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("笔记图片上传失败", e);
            return ApiResult.error("上传失败: " + e.getMessage());
        }
    }
}
