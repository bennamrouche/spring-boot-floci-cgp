package net.alphaben.floci.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

@Controller
public class FileController {
    private final Storage storage;
    private final String bucketName;

    public FileController(Storage storage, @Value("${app.storage.bucket}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<StoredFile> files = StreamSupport.stream(storage.list(bucketName).iterateAll().spliterator(), false)
                .map(blob -> new StoredFile(blob.getName(), blob.getSize(), blob.getContentType()))
                .toList();
        model.addAttribute("files", files);
        return "index";
    }

    @PostMapping("/files")
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException {
        if (file.isEmpty() || file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Choose a file before uploading.");
            return "redirect:/";
        }

        String filename = file.getOriginalFilename().replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, filename))
                .setContentType(file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());
        redirectAttributes.addFlashAttribute("message", "Uploaded " + filename);
        return "redirect:/";
    }

    @GetMapping("/files/{name:.+}")
    public void download(@PathVariable String name, HttpServletResponse response) throws IOException {
        Blob blob = storage.get(BlobId.of(bucketName, name));
        if (blob == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(blob.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : blob.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20"));
        response.setContentLengthLong(blob.getSize());
        blob.downloadTo(response.getOutputStream());
    }

    @PostMapping("/files/{name:.+}/delete")
    public String delete(@PathVariable String name, RedirectAttributes redirectAttributes) {
        boolean deleted = storage.delete(BlobId.of(bucketName, name));
        if (deleted) {
            redirectAttributes.addFlashAttribute("message", "Deleted " + name);
        } else {
            redirectAttributes.addFlashAttribute("error", "File not found: " + name);
        }
        return "redirect:/";
    }

    public record StoredFile(String name, long size, String contentType) {
        public String icon() {
            String lowerName = name.toLowerCase();
            if (contentType != null && contentType.startsWith("image/")) return "▧";
            if (contentType != null && contentType.startsWith("video/")) return "▶";
            if (contentType != null && contentType.startsWith("audio/")) return "♫";
            if (lowerName.endsWith(".pdf")) return "▤";
            if (lowerName.endsWith(".zip") || lowerName.endsWith(".gz") || lowerName.endsWith(".tar")) return "▦";
            if (lowerName.endsWith(".java") || lowerName.endsWith(".kt") || lowerName.endsWith(".js") || lowerName.endsWith(".ts")) return "⌘";
            if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) return "≡";
            return "▧";
        }

        public String displayType() {
            if (contentType != null && !contentType.isBlank()) {
                return contentType;
            }
            int dot = name.lastIndexOf('.');
            return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1).toUpperCase() + " file" : "File";
        }

        public String displaySize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            return String.format("%.1f MB", size / (1024.0 * 1024));
        }
    }
}
