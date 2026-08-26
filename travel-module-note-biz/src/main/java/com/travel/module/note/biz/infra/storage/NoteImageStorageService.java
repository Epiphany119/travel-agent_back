package com.travel.module.note.biz.infra.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 笔记图片存储服务。
 *
 * <p>上传的图片保存到用户本地 {@code ~/travel-agent-uploads/note/{userId}/} 目录，
 * 数据库只记录文件访问路径（如 {@code /uploads/note/user_001/xxx.jpg}），
 * 由 WebMvcConfig 的静态资源映射对外提供访问。</p>
 *
 * <p>支持 jpg/jpeg/png/gif（gif 可用于表情包），单文件上限 5MB。</p>
 */
@Component
public class NoteImageStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String ROOT_NAME = "travel-agent-uploads";

    /**
     * 保存笔记图片。
     *
     * @param file   上传的图片文件
     * @param userId 所属用户，构造独立文件夹
     * @return 可访问的 URL 路径，如 /uploads/note/user_001/xxx.jpg
     */
    public String store(MultipartFile file, String userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("图片不能超过 5MB");
        }
        String ext = extOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("仅支持图片格式: jpg/jpeg/png/gif");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = root().resolve("note").resolve(safeUserId(userId));
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("图片保存失败: " + e.getMessage(), e);
        }
        return "/uploads/note/" + safeUserId(userId) + "/" + filename;
    }

    private Path root() {
        return Paths.get(System.getProperty("user.home"), ROOT_NAME);
    }

    /** 防止用户 ID 携带路径分隔符 */
    private String safeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "user_001";
        }
        return userId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private String extOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}