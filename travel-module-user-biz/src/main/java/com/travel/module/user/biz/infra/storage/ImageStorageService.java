package com.travel.module.user.biz.infra.storage;

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
 * 通用图片存储服务
 * 支持本地上传，自动判断是否为外部 URL
 */
@Component
public class ImageStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String ROOT_NAME = "travel-agent-uploads";

    private Path root() {
        return Paths.get(System.getProperty("user.home"), ROOT_NAME);
    }

    /**
     * 上传图片到本地存储
     * @param file 上传的文件
     * @param category 分类目录，如 "inspiration", "journey", "avatar"
     * @return 可访问的 URL 路径，如 "/uploads/inspiration/xxx.jpg"
     */
    public String store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("图片不能超过 5MB");
        }
        String ext = extOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("仅支持图片格式: jpg/png/gif/webp");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = root().resolve(category);
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("图片保存失败: " + e.getMessage(), e);
        }
        return "/uploads/" + category + "/" + filename;
    }

    /**
     * 判断字符串是否为外部 URL
     * @param value 可能为 URL 或本地路径的字符串
     * @return true 如果是外部 URL
     */
    public boolean isExternalUrl(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.startsWith("http://") || value.startsWith("https://");
    }

    /**
     * 处理图片字段值
     * 如果是外部 URL 直接返回，如果是本地上传则保存并返回路径
     * @param value 可能是外部 URL 或 MultipartFile
     * @param category 分类目录
     * @param <T> 值类型
     * @return 图片 URL
     */
    @SuppressWarnings("unchecked")
    public <T> String processImage(Object value, String category) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (isExternalUrl(str)) {
                return str;
            }
            if (str.isEmpty()) {
                return "";
            }
            // 字符串但不是 URL，当作无效值处理
            return "";
        }
        if (value instanceof MultipartFile) {
            return store((MultipartFile) value, category);
        }
        return "";
    }

    private String extOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
