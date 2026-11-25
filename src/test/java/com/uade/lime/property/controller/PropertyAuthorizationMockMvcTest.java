package com.uade.lime.property.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import com.uade.lime.auth.model.Sex;
import com.uade.lime.auth.model.User;
import com.uade.lime.auth.model.UserRole;
import com.uade.lime.auth.repository.DenylistedTokenRepository;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.auth.security.JwtService;
import com.uade.lime.property.model.OperationType;
import com.uade.lime.property.model.Property;
import com.uade.lime.property.model.PropertyImage;
import com.uade.lime.property.model.PropertyType;
import com.uade.lime.property.repository.InquiryRepository;
import com.uade.lime.property.repository.PropertyImageRepository;
import com.uade.lime.property.repository.PropertyRepository;

@SpringBootTest
@ActiveProfiles("test")
class PropertyAuthorizationMockMvcTest {

    private static final String PATCH_BODY = """
            {
              "title": "Departamento editado"
            }
            """;

    private static final String IMAGES_URL = "/api/v1/properties/{id}/images";
    private static final String IMAGE_URL = "/api/v1/properties/{id}/images/{imageId}";
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final String EXISTING_IMAGE_URL = "/uploads/existing.png";

    // Primeros bytes (magic bytes) de cada formato permitido, rellenados hasta 12 bytes.
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private DenylistedTokenRepository denylistedTokenRepository;

    private User owner;
    private User differentUser;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        denylistedTokenRepository.deleteAll();
        inquiryRepository.deleteAll();
        inquiryRepository.flush();
        propertyImageRepository.deleteAll();
        propertyImageRepository.flush();
        propertyRepository.deleteAll();
        propertyRepository.flush();
        denylistedTokenRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();

        Instant now = Instant.now();
        LocalDate birthDate = LocalDate.of(1995, 1, 1);
        owner = userRepository.save(User.register(
                "owner@example.com",
                "encoded-password",
                "Owner User",
                UserRole.AGENCY,
                "Lime Test Agency",
                birthDate,
                Sex.MALE,
                now));
        differentUser = userRepository.save(User.register(
                "other@example.com",
                "encoded-password",
                "Other User",
                UserRole.AGENCY,
                "Otra Agencia",
                birthDate,
                Sex.FEMALE,
                now));
    }

    @Test
    void create_asUserRole_returns403() throws Exception {
        User buyer = userRepository.save(User.register(
                "buyer@example.com",
                "encoded-password",
                "Buyer User",
                UserRole.USER,
                null,
                LocalDate.of(1995, 1, 1),
                Sex.OTHER,
                Instant.now()));

        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "No deberia crearse"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void patch_asOwner_returns200() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Departamento editado")));
    }

    @Test
    void patch_asDifferentUser_returns403() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void patch_asDifferentUser_doesNotModifyProperty() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        String originalTitle = propertyRepository.findById(property.getId()).orElseThrow().getTitle();

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_BODY))
                .andExpect(status().isForbidden());

        String titleAfterForbiddenPatch = propertyRepository.findById(property.getId()).orElseThrow().getTitle();
        org.assertj.core.api.Assertions.assertThat(titleAfterForbiddenPatch).isEqualTo(originalTitle);
    }

    @Test
    void patch_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(patch("/api/v1/properties/{id}", property.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PATCH_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_asOwner_returns204() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asDifferentUser_returns403() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publish_asOwner_returns200() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/publish", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PUBLISHED")));
    }

    @Test
    void publish_asDifferentUser_returns403() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/publish", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void publish_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/publish", property.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pause_asOwner_returns200() throws Exception {
        Property property = propertyRepository.save(publishedProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/pause", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAUSED")));
    }

    @Test
    void pause_asDifferentUser_returns403() throws Exception {
        Property property = propertyRepository.save(publishedProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/pause", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pause_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(publishedProperty(owner, "Departamento original"));

        mockMvc.perform(post("/api/v1/properties/{id}/pause", property.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------
    // Imágenes: POST / PATCH / DELETE /api/v1/properties/{id}/images
    // DoD: sin token -> 401 · archivo inválido -> 400 · dueño ajeno -> 403
    // ---------------------------------------------------------------------

    // ---- POST: alta ----

    @Test
    void addImage_asOwner_returns201() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        upload(HttpMethod.POST, png("foto.png"), owner, IMAGES_URL, property.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", startsWith("/uploads/")))
                .andExpect(jsonPath("$.url", endsWith(".png")));

        assertThat(propertyImageRepository.count()).isEqualTo(1);
    }

    @Test
    void addImage_asOwner_acceptsJpegAndWebp() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        upload(HttpMethod.POST, new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG),
                owner, IMAGES_URL, property.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", endsWith(".jpg")));
        upload(HttpMethod.POST, new MockMultipartFile("file", "a.webp", "image/webp", WEBP),
                owner, IMAGES_URL, property.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", endsWith(".webp")));
    }

    @Test
    void addImage_atMaxSize_returns201() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", pngOfSize(MAX_IMAGE_BYTES));

        upload(HttpMethod.POST, file, owner, IMAGES_URL, property.getId())
                .andExpect(status().isCreated());
    }

    // La extensión del archivo guardado sale del tipo validado, no del nombre que manda el cliente
    // (si no, "foto.png.html" se serviría como HTML desde /uploads/**).
    @Test
    void addImage_ignoresClientFilenameExtension() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        upload(HttpMethod.POST, png("foto.png.html"), owner, IMAGES_URL, property.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", endsWith(".png")));
    }

    // ---- POST: 401 ----

    @Test
    void addImage_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        upload(HttpMethod.POST, png("a.png"), null, IMAGES_URL, property.getId())
                .andExpect(status().isUnauthorized());

        assertThat(propertyImageRepository.count()).isZero();
    }

    @Test
    void addImage_withInvalidToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(multipart(HttpMethod.POST, IMAGES_URL, property.getId())
                        .file(png("a.png"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // La seguridad corta antes de mirar el archivo: sin token manda el 401, no el 400.
    @Test
    void addImage_withoutTokenAndInvalidFile_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile invalid = new MockMultipartFile("file", "a.txt", "text/plain", "hola".getBytes());

        upload(HttpMethod.POST, invalid, null, IMAGES_URL, property.getId())
                .andExpect(status().isUnauthorized());
    }

    // ---- POST: 403 ----

    @Test
    void addImage_asDifferentUser_returns403() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        upload(HttpMethod.POST, png("a.png"), differentUser, IMAGES_URL, property.getId())
                .andExpect(status().isForbidden());
    }

    @Test
    void addImage_asDifferentUser_doesNotCreateImage() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        upload(HttpMethod.POST, png("a.png"), differentUser, IMAGES_URL, property.getId())
                .andExpect(status().isForbidden());

        assertThat(propertyImageRepository.count()).isZero();
    }

    @Test
    void addImage_asUserRole_returns403() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        upload(HttpMethod.POST, png("a.png"), buyer(), IMAGES_URL, property.getId())
                .andExpect(status().isForbidden());
    }

    // El dueño se valida antes que el archivo: un ajeno con un archivo inválido recibe 403, no 400.
    @Test
    void addImage_asDifferentUserWithInvalidFile_returns403() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile invalid = new MockMultipartFile("file", "a.txt", "text/plain", "hola".getBytes());

        upload(HttpMethod.POST, invalid, differentUser, IMAGES_URL, property.getId())
                .andExpect(status().isForbidden());
    }

    // ---- POST: 400 ----

    @Test
    void addImage_invalidContentType_returns400() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hola".getBytes());

        upload(HttpMethod.POST, file, owner, IMAGES_URL, property.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("JPG, PNG o WEBP")));

        assertThat(propertyImageRepository.count()).isZero();
    }

    @Test
    void addImage_svgContentType_returns400() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile svg = new MockMultipartFile(
                "file", "a.svg", "image/svg+xml", "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes());

        upload(HttpMethod.POST, svg, owner, IMAGES_URL, property.getId())
                .andExpect(status().isBadRequest());
    }

    @Test
    void addImage_withoutContentType_returns400() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile file = new MockMultipartFile("file", "a.png", null, PNG);

        upload(HttpMethod.POST, file, owner, IMAGES_URL, property.getId())
                .andExpect(status().isBadRequest());
    }

    // El content-type es solo un header que manda el cliente: el contenido tiene que coincidir.
    @Test
    void addImage_contentDoesNotMatchDeclaredType_returns400() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile textAsPng = new MockMultipartFile(
                "file", "a.png", "image/png", "<html>no soy una imagen</html>".getBytes());

        upload(HttpMethod.POST, textAsPng, owner, IMAGES_URL, property.getId())
                .andExpect(status().isBadRequest());

        assertThat(propertyImageRepository.count()).isZero();
    }

    @Test
    void addImage_jpegDeclaredAsPng_returns400() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", JPEG);

        upload(HttpMethod.POST, file, owner, IMAGES_URL, property.getId())
                .andExpect(status().isBadRequest());
    }

    @Test
    void addImage_emptyFile_returns400() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        upload(HttpMethod.POST, empty, owner, IMAGES_URL, property.getId())
                .andExpect(status().isBadRequest());
    }

    @Test
    void addImage_overMaxSize_returns400() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        MockMultipartFile tooBig = new MockMultipartFile(
                "file", "a.png", "image/png", pngOfSize(MAX_IMAGE_BYTES + 1));

        upload(HttpMethod.POST, tooBig, owner, IMAGES_URL, property.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("5 MB")));

        assertThat(propertyImageRepository.count()).isZero();
    }

    @Test
    void addImage_withoutFilePart_returns400() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(multipart(HttpMethod.POST, IMAGES_URL, property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isBadRequest());
    }

    // ---- POST: 404 ----

    @Test
    void addImage_propertyNotFound_returns404() throws Exception {
        upload(HttpMethod.POST, png("a.png"), owner, IMAGES_URL, 999_999L)
                .andExpect(status().isNotFound());
    }

    // ---- PATCH: reemplazo ----

    @Test
    void replaceImage_asOwner_returns200() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        PropertyImage image = existingImage(property);

        upload(HttpMethod.PATCH, png("nueva.png"), owner, IMAGE_URL, property.getId(), image.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(image.getId().intValue())))
                .andExpect(jsonPath("$.url", not(is(EXISTING_IMAGE_URL))))
                .andExpect(jsonPath("$.url", startsWith("/uploads/")));
    }

    @Test
    void replaceImage_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        PropertyImage image = existingImage(property);

        upload(HttpMethod.PATCH, png("nueva.png"), null, IMAGE_URL, property.getId(), image.getId())
                .andExpect(status().isUnauthorized());

        assertThat(urlOf(image)).isEqualTo(EXISTING_IMAGE_URL);
    }

    @Test
    void replaceImage_asDifferentUser_returns403AndKeepsImage() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        PropertyImage image = existingImage(property);

        upload(HttpMethod.PATCH, png("nueva.png"), differentUser, IMAGE_URL, property.getId(), image.getId())
                .andExpect(status().isForbidden());

        assertThat(urlOf(image)).isEqualTo(EXISTING_IMAGE_URL);
    }

    @Test
    void replaceImage_invalidContentType_returns400AndKeepsImage() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        PropertyImage image = existingImage(property);
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hola".getBytes());

        upload(HttpMethod.PATCH, file, owner, IMAGE_URL, property.getId(), image.getId())
                .andExpect(status().isBadRequest());

        assertThat(urlOf(image)).isEqualTo(EXISTING_IMAGE_URL);
    }

    @Test
    void replaceImage_contentDoesNotMatchDeclaredType_returns400AndKeepsImage() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        PropertyImage image = existingImage(property);
        MockMultipartFile textAsPng = new MockMultipartFile(
                "file", "a.png", "image/png", "<html>no soy una imagen</html>".getBytes());

        upload(HttpMethod.PATCH, textAsPng, owner, IMAGE_URL, property.getId(), image.getId())
                .andExpect(status().isBadRequest());

        assertThat(urlOf(image)).isEqualTo(EXISTING_IMAGE_URL);
    }

    @Test
    void replaceImage_unknownImage_returns404() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        upload(HttpMethod.PATCH, png("nueva.png"), owner, IMAGE_URL, property.getId(), 999_999L)
                .andExpect(status().isNotFound());
    }

    // ---- DELETE ----

    @Test
    void deleteImage_asOwner_returns204() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        PropertyImage image = existingImage(property);

        mockMvc.perform(delete(IMAGE_URL, property.getId(), image.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isNoContent());

        assertThat(propertyImageRepository.existsById(image.getId())).isFalse();
    }

    @Test
    void deleteImage_withoutToken_returns401() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        PropertyImage image = existingImage(property);

        mockMvc.perform(delete(IMAGE_URL, property.getId(), image.getId()))
                .andExpect(status().isUnauthorized());

        assertThat(propertyImageRepository.existsById(image.getId())).isTrue();
    }

    @Test
    void deleteImage_asDifferentUser_returns403AndKeepsImage() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));
        PropertyImage image = existingImage(property);

        mockMvc.perform(delete(IMAGE_URL, property.getId(), image.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(differentUser)))
                .andExpect(status().isForbidden());

        assertThat(propertyImageRepository.existsById(image.getId())).isTrue();
    }

    @Test
    void deleteImage_unknownImage_returns404() throws Exception {
        Property property = propertyRepository.save(draftProperty(owner, "Departamento original"));

        mockMvc.perform(delete(IMAGE_URL, property.getId(), 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(owner)))
                .andExpect(status().isNotFound());
    }

    // ---- GET sigue siendo público (la mejora es solo sobre las mutaciones) ----

    @Test
    void getImages_withoutToken_onPublishedProperty_returns200() throws Exception {
        Property property = propertyRepository.save(publishedProperty(owner, "Departamento original"));
        existingImage(property);

        mockMvc.perform(get(IMAGES_URL, property.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtService.issue(user, UUID.randomUUID().toString());
    }

    private Property publishedProperty(User owner, String title) {
        Property property = draftProperty(owner, title);
        property.publish(Instant.now());
        return property;
    }

    private Property draftProperty(User owner, String title) {
        return Property.draft(
                title,
                "Descripcion de prueba",
                PropertyType.APARTMENT,
                OperationType.SALE,
                BigDecimal.valueOf(100000),
                "USD",
                "Av. Siempre Viva 742",
                "Buenos Aires",
                "Buenos Aires",
                2,
                1,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(60),
                owner,
                Instant.now());
    }

    // Sube (o reemplaza) un archivo; uploader == null significa "sin token".
    private ResultActions upload(
            HttpMethod method, MockMultipartFile file, User uploader, String url, Object... uriVars)
            throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart(method, url, uriVars).file(file);
        if (uploader != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearerToken(uploader));
        }
        return mockMvc.perform(request);
    }

    private MockMultipartFile png(String filename) {
        return new MockMultipartFile("file", filename, "image/png", PNG);
    }

    // Un "PNG" del tamaño pedido: encabezado válido y relleno de ceros.
    private byte[] pngOfSize(int size) {
        byte[] bytes = new byte[size];
        System.arraycopy(PNG, 0, bytes, 0, PNG.length);
        return bytes;
    }

    private PropertyImage existingImage(Property property) {
        return propertyImageRepository.save(PropertyImage.of(property, EXISTING_IMAGE_URL, Instant.now()));
    }

    private String urlOf(PropertyImage image) {
        return propertyImageRepository.findById(image.getId()).orElseThrow().getUrl();
    }

    private User buyer() {
        return userRepository.save(User.register(
                "buyer@example.com",
                "encoded-password",
                "Buyer User",
                UserRole.USER,
                null,
                LocalDate.of(1995, 1, 1),
                Sex.OTHER,
                Instant.now()));
    }
}
