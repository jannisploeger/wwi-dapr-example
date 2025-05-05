//package dev.ploeger.dapr.warehouse;
//
//import dev.ploeger.dapr.warehouse.adapter.KvStoreAdapter;
//import dev.ploeger.dapr.warehouse.model.Sweet;
//import io.dapr.testcontainers.DaprContainer;
//import io.dapr.testcontainers.DaprLogLevel;
//import io.dapr.testcontainers.Component;
//import org.junit.jupiter.api.*; // Added AfterEach
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.Network;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.utility.DockerImageName;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.TimeUnit;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
//@Import(WarehouseIntegrationTest.TestContainersConfiguration.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Keep PER_CLASS for performance, requires cleanup
//class WarehouseIntegrationTest {
//
//    // Define initial state for cleanup/reference if needed
//    private static final Sweet INITIAL_CHOCOLATE = new Sweet("Chocolate", 1.5, 100); // Example initial state
//    private static final Sweet INITIAL_LOLLYPOP = new Sweet("Lollypop", 0.5, 200); // Example initial state
//    private static final Sweet INITIAL_GUMMI = new Sweet("Gummi", 1.0, 150); // Example initial state
//
//    @Autowired
//    private KvStoreAdapter kvStoreAdapter;
//
//    // No need to Autowire DaprContainer if not used directly in tests/setup
//
//    @TestConfiguration
//    static class TestContainersConfiguration {
//        @Bean
//        Network network() {
//            return Network.newNetwork();
//        }
//
//        @Bean
//        @ServiceConnection
//        PostgreSQLContainer<?> postgreSQLContainer(Network network) {
//            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
//                    .withNetwork(network)
//                    .withNetworkAliases("postgres")
//                    .withDatabaseName("dapr")
//                    .withUsername("postgres")
//                    .withPassword("postgres")
//                    // Ensure the database is fully ready before proceeding
//                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 1)
//                            .withStartupTimeout(Duration.ofMinutes(1)));
//        }
//
//        @Bean
//        DaprContainer daprContainer(Network network, PostgreSQLContainer<?> postgresContainer) {
//            // Give Dapr more time, especially on slower systems or first pull
//            Duration daprStartupTimeout = Duration.ofMinutes(3);
//
//            return new DaprContainer(DockerImageName.parse("daprio/daprd"))
//                    .withAppName("warehouse-test")
//                    .withAppPort(8080)
//                    .withDaprLogLevel(DaprLogLevel.INFO) // Use DEBUG for troubleshooting
//                    .withLogConsumer(outputFrame -> System.out.println("Dapr: " + outputFrame.getUtf8StringWithoutLineEnding())) // Cleaned up log output
//                    .withNetwork(network)
//                    .dependsOn(postgresContainer)
//                    .withComponent(new Component("kvstore", "state.postgresql", "v1",
//                            Map.of(
//                                    "connectionString",
//                                    // Ensure spaces are handled if any, use recommended format
//                                    String.format("host=%s user=%s password=%s port=5432 connect_timeout=10 database=%s sslmode=disable",
//                                            "postgres", // Network alias
//                                            postgresContainer.getUsername(),
//                                            postgresContainer.getPassword(),
//                                            postgresContainer.getDatabaseName()),
//                                    "tableName", "kvstore",
//                                    "metadataTableName", "kvstore_metadata",
//                                    "actorStateStore", "false" // Typically false unless using Dapr Actors specifically with this store
//                            )))
//                    // Wait strategy: wait for component to load *and* app health check (if app provides /health endpoint)
//                    // Or stick to application discovered if no health endpoint is available/reliable
//                    .waitingFor(
//                            Wait.forLogMessage(".*Component loaded: kvstore \\(state.postgresql/v1\\).*", 1)
//                                    .withStartupTimeout(daprStartupTimeout) // Apply timeout here too
//                    )
//                    // Add this if your app has a health endpoint Dapr checks:
//                    // .waitingFor(Wait.forLogMessage(".*Health check passed.*", 1).withStartupTimeout(daprStartupTimeout))
//                    .withStartupTimeout(daprStartupTimeout); // Overall container timeout
//        }
//
//        @DynamicPropertySource
//        static void daprProperties(DynamicPropertyRegistry registry) {
//            // Use a random port to avoid conflicts if DEFINED_PORT isn't strictly necessary
//            // registry.add("server.port", () -> 0); // Use random port
//            // Or keep fixed if required by external dependencies/config
//            registry.add("server.port", () -> 8080);
//        }
//    }
//
//    // @BeforeAll removed - Rely on TestContainers 'waitingFor'
//
//    @AfterEach
//    void resetState() {
//        // Reset state modified by tests back to a known baseline
//        // This is crucial when using TestInstance.Lifecycle.PER_CLASS
//        System.out.println("Resetting state after test...");
//        kvStoreAdapter.addOrUpdateSweet(INITIAL_CHOCOLATE).block();
//        kvStoreAdapter.addOrUpdateSweet(INITIAL_LOLLYPOP).block();
//        kvStoreAdapter.addOrUpdateSweet(INITIAL_GUMMI).block();
//
//        // Attempt to remove items added by tests (like "IceCream")
//        // Requires a delete method or a convention (e.g., quantity 0 means delete)
//        // If KvStoreAdapter doesn't support delete, this state leakage is a downside of PER_CLASS.
//        // Example: if addOrUpdate with quantity <= 0 effectively deletes:
//        // kvStoreAdapter.addOrUpdateSweet(new Sweet("IceCream", 0, 0)).block();
//        // Or if a specific delete method exists:
//        // kvStoreAdapter.deleteSweet("IceCream").block();
//
//        // For this example, we assume added items might persist if no delete is available.
//        System.out.println("State reset attempted.");
//    }
//
//
//    @Test
//    void shouldRetrieveInitialInventory() {
//        // Give Dapr/Store a moment to be consistent after potential reset/init
//        await().atMost(15, TimeUnit.SECONDS)
//                .pollInterval(1, TimeUnit.SECONDS)
//                .untilAsserted(() -> {
//                    List<Sweet> inventory = kvStoreAdapter.getInventory().block();
//
//                    assertThat(inventory).isNotNull();
//                    // Check size *after* potential cleanup/resets
//                    // This assertion depends heavily on the effectiveness of resetState()
//                    assertThat(inventory).hasSize(3);
//
//                    // Check for the core items
//                    assertThat(inventory)
//                            .extracting(Sweet::name)
//                            .containsExactlyInAnyOrder("Chocolate", "Lollypop", "Gummi");
//
//                    // Optionally verify initial quantities if needed and resetState guarantees them
//                    assertThat(findSweet(inventory, "Chocolate").quantity()).isEqualTo(INITIAL_CHOCOLATE.quantity());
//                    assertThat(findSweet(inventory, "Lollypop").quantity()).isEqualTo(INITIAL_LOLLYPOP.quantity());
//                    assertThat(findSweet(inventory, "Gummi").quantity()).isEqualTo(INITIAL_GUMMI.quantity());
//                });
//    }
//
//    @Test
//    void shouldUpdateSweetQuantity() {
//        String sweetName = "Chocolate";
//        int newQuantity = 50;
//
//        // Ensure the sweet exists before update attempt (optional, good practice)
//        await().atMost(5, TimeUnit.SECONDS).until(() -> findSweetOptional(kvStoreAdapter.getInventory().block(), sweetName).isPresent());
//
//        // Perform Action
//        boolean updateResult = kvStoreAdapter.updateSweetQuantity(sweetName, newQuantity).block();
//        assertThat(updateResult).isTrue();
//
//        // Verify Result with Awaitility
//        await().atMost(10, TimeUnit.SECONDS)
//                .pollInterval(500, TimeUnit.MILLISECONDS)
//                .untilAsserted(() -> {
//                    List<Sweet> inventory = kvStoreAdapter.getInventory().block();
//                    Sweet updatedSweet = findSweet(inventory, sweetName);
//                    assertThat(updatedSweet.quantity()).isEqualTo(newQuantity);
//                });
//    }
//
//    @Test
//    void shouldAddNewSweet() {
//        // Define new sweet
//        Sweet newSweet = new Sweet("IceCream", 2.0, 75);
//
//        // Perform Action
//        boolean addResult = kvStoreAdapter.addOrUpdateSweet(newSweet).block();
//        assertThat(addResult).isTrue();
//
//        // Verify Result with Awaitility
//        await().atMost(10, TimeUnit.SECONDS)
//                .pollInterval(500, TimeUnit.MILLISECONDS)
//                .untilAsserted(() -> {
//                    List<Sweet> inventory = kvStoreAdapter.getInventory().block();
//
//                    // Check if the new sweet exists
//                    Sweet addedSweet = findSweet(inventory, newSweet.name());
//
//                    // Verify properties
//                    assertThat(addedSweet.price()).isEqualTo(newSweet.price());
//                    assertThat(addedSweet.quantity()).isEqualTo(newSweet.quantity());
//
//                    // Also check that the list size might have increased (if cleanup doesn't remove it)
//                    // This depends on the resetState logic effectiveness
//                    // assertThat(inventory.size()).isGreaterThanOrEqualTo(4);
//                });
//    }
//
//    @Test
//    void shouldHandleNonExistentSweetUpdate() { // Renamed for clarity
//        // Perform Action on a non-existent item
//        boolean updateResult = kvStoreAdapter.updateSweetQuantity("NonExistentSweet", 10).block();
//
//        // Verify Result
//        assertThat(updateResult).isFalse();
//
//        // Verify that the state wasn't accidentally changed (optional sanity check)
//        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
//            List<Sweet> inventory = kvStoreAdapter.getInventory().block();
//            Optional<Sweet> nonExistent = findSweetOptional(inventory, "NonExistentSweet");
//            assertThat(nonExistent).isNotPresent();
//        });
//    }
//
//    // --- Helper Methods ---
//    private Optional<Sweet> findSweetOptional(List<Sweet> inventory, String name) {
//        if (inventory == null) return Optional.empty();
//        return inventory.stream()
//                .filter(s -> s.name().equals(name))
//                .findFirst();
//    }
//
//    private Sweet findSweet(List<Sweet> inventory, String name) {
//        return findSweetOptional(inventory, name)
//                .orElseThrow(() -> new AssertionError("Sweet not found in inventory: " + name));
//    }
//}