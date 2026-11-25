package com.uade.lime.property.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.uade.lime.common.exception.ArgumentInvalidException;

@Component
public class FileStorageService {

    // Tipos permitidos -> extensión con la que se guarda el archivo. La extensión sale de acá y
    // nunca del nombre original: si no, "foto.png.html" se serviría como HTML desde /uploads/**.
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final int SIGNATURE_BYTES = 12; // lo que hace falta leer para reconocer los 3 formatos

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de uploads", e);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ArgumentInvalidException("El archivo está vacío");
        }

        // El content-type es solo un header que manda el cliente (y puede venir null):
        // sirve para elegir el formato esperado, pero el contenido se verifica más abajo.
        String contentType = normalizeContentType(file.getContentType());
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new ArgumentInvalidException("Tipo de archivo no permitido: solo se aceptan JPG, PNG o WEBP");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ArgumentInvalidException("El archivo supera el tamaño máximo permitido (5 MB)");
        }
        if (!matchesSignature(contentType, readHeader(file))) {
            throw new ArgumentInvalidException("El contenido del archivo no coincide con una imagen JPG, PNG o WEBP");
        }

        String filename = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }

        return filename;
    }

    public void delete(String filename) {
        try {
            Files.deleteIfExists(uploadDir.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo borrar el archivo", e);
        }
    }

    // null -> "" (no está en el mapa, así que se rechaza con 400 en vez de explotar con un NPE).
    // Se descartan parámetros ("image/png; charset=...") y se pasa a minúsculas ("IMAGE/PNG").
    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int params = contentType.indexOf(';');
        String type = params >= 0 ? contentType.substring(0, params) : contentType;
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(SIGNATURE_BYTES);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo", e);
        }
    }

    // Magic bytes: JPEG = FF D8 FF · PNG = 89 "PNG" 0D 0A 1A 0A · WEBP = "RIFF" .... "WEBP"
    private static boolean matchesSignature(String contentType, byte[] h) {
        return switch (contentType) {
            case "image/jpeg" -> h.length >= 3
                    && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "image/png" -> h.length >= 8
                    && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
                    && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A;
            case "image/webp" -> h.length >= 12
                    && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            default -> false;
        };
    }
}
