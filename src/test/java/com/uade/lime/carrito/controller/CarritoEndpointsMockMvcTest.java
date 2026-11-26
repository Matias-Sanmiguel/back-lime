package com.uade.lime.carrito.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.uade.lime.auth.model.Sex;
import com.uade.lime.auth.model.User;
import com.uade.lime.auth.model.UserRole;
import com.uade.lime.auth.repository.DenylistedTokenRepository;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.auth.security.JwtService;
import com.uade.lime.carrito.model.Carrito;
import com.uade.lime.carrito.repository.CarritoItemRepository;
import com.uade.lime.carrito.repository.CarritoRepository;
import com.uade.lime.property.model.OperationType;
import com.uade.lime.property.model.Property;
import com.uade.lime.property.model.PropertyImage;
import com.uade.lime.property.model.PropertyType;
import com.uade.lime.property.repository.InquiryRepository;
import com.uade.lime.property.repository.PropertyImageRepository;
import com.uade.lime.property.repository.PropertyRepository;

@SpringBootTest
@ActiveProfiles("test")
class CarritoEndpointsMockMvcTest {

    private static final String CART_ITEMS_PATH = "/api/v1/me/cart/items";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private CarritoItemRepository carritoItemRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyImageRepository propertyImageRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private DenylistedTokenRepository denylistedTokenRepository;

    private User usuario;
    private User otroUsuario;
    private Property propiedad;
    private PropertyImage coverImage;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        carritoRepository.deleteAll();
        carritoRepository.flush();
        inquiryRepository.deleteAll();
        inquiryRepository.flush();
        propertyImageRepository.deleteAll();
        propertyImageRepository.flush();
        propertyRepository.deleteAll();
        propertyRepository.flush();
        denylistedTokenRepository.deleteAll();
        denylistedTokenRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();

        usuario = userRepository.save(user("carrito"));
        otroUsuario = userRepository.save(user("otro"));
        propiedad = propertyRepository.save(publishedProperty(usuario, "Departamento para carrito"));
        coverImage = propertyImageRepository.save(
                PropertyImage.of(propiedad, "/uploads/cover.jpg", Instant.now()));
    }

    @AfterEach
    void cleanUp() {
        carritoRepository.deleteAll();
        carritoRepository.flush();
    }

    @Test
    void addAndGet_returnsPropertyItemAndReusesUsersCart() throws Exception {
        mockMvc.perform(post(CART_ITEMS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyId": %d }
                                """.formatted(propiedad.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.addedAt").isNotEmpty())
                .andExpect(jsonPath("$.property.id", is(propiedad.getId().intValue())))
                .andExpect(jsonPath("$.property.title", is("Departamento para carrito")))
                .andExpect(jsonPath("$.property.owner.id", is(usuario.getId().intValue())))
                .andExpect(jsonPath("$.property.coverImage.id", is(coverImage.getId().intValue())))
                .andExpect(jsonPath("$.property.coverImage.url", is("/uploads/cover.jpg")));

        Property secondProperty = propertyRepository.save(publishedProperty(usuario, "Otra propiedad"));
        mockMvc.perform(post(CART_ITEMS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyId": %d }
                                """.formatted(secondProperty.getId())))
                .andExpect(status().isCreated());

        assertThat(carritoRepository.findByUsuario_Id(usuario.getId())).isPresent();
        mockMvc.perform(get(CART_ITEMS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    void getAll_returnsOnlyCurrentUsersItems() throws Exception {
        createItem(usuario, propiedad);
        createItem(otroUsuario, propiedad);

        mockMvc.perform(get(CART_ITEMS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].property.id", is(propiedad.getId().intValue())));
    }

    @Test
    void add_duplicateProperty_returnsConflict() throws Exception {
        createItem(usuario, propiedad);

        mockMvc.perform(post(CART_ITEMS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyId": %d }
                                """.formatted(propiedad.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void add_unpublishedProperty_returnsConflict() throws Exception {
        Property draft = propertyRepository.save(draftProperty(usuario));

        mockMvc.perform(post(CART_ITEMS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyId": %d }
                                """.formatted(draft.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_itemOwnedByAnotherUser_returnsNotFound() throws Exception {
        var item = createItem(otroUsuario, propiedad);

        mockMvc.perform(delete(CART_ITEMS_PATH + "/{itemId}", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario)))
                .andExpect(status().isNotFound());

        assertThat(carritoItemRepository.findById(item.getId())).isPresent();
    }

    @Test
    void delete_ownedItem_returnsNoContent() throws Exception {
        var item = createItem(usuario, propiedad);

        mockMvc.perform(delete(CART_ITEMS_PATH + "/{itemId}", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario)))
                .andExpect(status().isNoContent());

        assertThat(carritoItemRepository.findById(item.getId())).isEmpty();
    }

    @Test
    void add_nonPositivePropertyId_returnsBadRequest() throws Exception {
        mockMvc.perform(post(CART_ITEMS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "propertyId": 0 }
                                """))
                .andExpect(status().isBadRequest());
    }

    private com.uade.lime.carrito.model.CarritoItem createItem(User owner, Property property) {
        Carrito carrito = carritoRepository.findByUsuario_Id(owner.getId())
                .orElseGet(() -> carritoRepository.saveAndFlush(Carrito.create(owner, Instant.now())));
        return carritoItemRepository.saveAndFlush(carrito.addItem(property, Instant.now()));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtService.issue(user, UUID.randomUUID().toString());
    }

    private User user(String prefix) {
        return User.register(
                "%s-%s@example.com".formatted(prefix, UUID.randomUUID()),
                "encoded-password",
                prefix,
                UserRole.USER,
                null,
                LocalDate.of(1995, 1, 1),
                Sex.OTHER,
                Instant.now());
    }

    private Property publishedProperty(User owner, String title) {
        Property property = Property.draft(
                title,
                "Descripcion",
                PropertyType.APARTMENT,
                OperationType.RENT,
                BigDecimal.valueOf(450000),
                "ARS",
                "Av. Siempre Viva 742",
                "Buenos Aires",
                "Buenos Aires",
                2,
                1,
                BigDecimal.valueOf(55),
                BigDecimal.valueOf(65),
                owner,
                Instant.now());
        property.publish(Instant.now());
        return property;
    }

    private Property draftProperty(User owner) {
        return Property.draft(
                "Departamento borrador",
                "Descripcion",
                PropertyType.APARTMENT,
                OperationType.RENT,
                BigDecimal.valueOf(450000),
                "ARS",
                "Av. Siempre Viva 742",
                "Buenos Aires",
                "Buenos Aires",
                2,
                1,
                BigDecimal.valueOf(55),
                BigDecimal.valueOf(65),
                owner,
                Instant.now());
    }
}
