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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.lime.auth.model.Sex;
import com.uade.lime.auth.model.User;
import com.uade.lime.auth.model.UserRole;
import com.uade.lime.auth.repository.DenylistedTokenRepository;
import com.uade.lime.auth.repository.UserRepository;
import com.uade.lime.auth.security.JwtService;
import com.uade.lime.carrito.model.Carrito;
import com.uade.lime.carrito.repository.CarritoRepository;
import com.uade.lime.property.model.OperationType;
import com.uade.lime.property.model.Property;
import com.uade.lime.property.model.PropertyType;
import com.uade.lime.property.repository.InquiryRepository;
import com.uade.lime.property.repository.PropertyImageRepository;
import com.uade.lime.property.repository.PropertyRepository;

@SpringBootTest
@ActiveProfiles("test")
class CarritoEndpointsMockMvcTest {

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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        propiedad = propertyRepository.save(publishedProperty(usuario));
    }

    @AfterEach
    void cleanUp() {
        carritoRepository.deleteAll();
        carritoRepository.flush();
    }

    @Test
    void addAndGet_returnsCartWithPropertyAndQuantity() throws Exception {
        String response = mockMvc.perform(post("/api/v1/carritos")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "propiedadId": %d,
                                      "cantidad": 2
                                    }
                                  ]
                                }
                                """.formatted(propiedad.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId", is(usuario.getId().intValue())))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.items[0].propiedadId", is(propiedad.getId().intValue())))
                .andExpect(jsonPath("$.items[0].propiedadTitulo", is("Departamento para carrito")))
                .andExpect(jsonPath("$.items[0].cantidad", is(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long carritoId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/carritos/{id}", carritoId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(carritoId.intValue())))
                .andExpect(jsonPath("$.items[0].cantidad", is(2)));
    }

    @Test
    void getAll_returnsOnlyCurrentUsersCarts() throws Exception {
        createCart(usuario);
        createCart(otroUsuario);

        mockMvc.perform(get("/api/v1/carritos")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].usuarioId", is(usuario.getId().intValue())));
    }

    @Test
    void getByIdAndDelete_hideOtherUsersCart() throws Exception {
        var carrito = createCart(usuario);

        mockMvc.perform(get("/api/v1/carritos/{id}", carrito.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otroUsuario)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/carritos/{id}", carrito.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(otroUsuario)))
                .andExpect(status().isNotFound());

        assertThat(carritoRepository.findById(carrito.getId())).isPresent();
    }

    @Test
    void add_withNonPositiveQuantity_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/carritos")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "propiedadId": %d,
                                      "cantidad": 0
                                    }
                                  ]
                                }
                                """.formatted(propiedad.getId())))
                .andExpect(status().isBadRequest());
    }

    private Carrito createCart(User owner) {
        var carrito = Carrito.create(owner, Instant.now());
        carrito.addItem(propiedad, 1);
        return carritoRepository.saveAndFlush(carrito);
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

    private Property publishedProperty(User owner) {
        Property property = Property.draft(
                "Departamento para carrito",
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
}
