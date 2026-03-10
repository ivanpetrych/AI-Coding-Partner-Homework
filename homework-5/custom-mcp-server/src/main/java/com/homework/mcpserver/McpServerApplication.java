package com.homework.mcpserver;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.function.BiFunction;

/**
 * MCP Server entry point.
 *
 * <p>Spring AI MCP auto-configuration picks up two kinds of beans:
 * <ul>
 *   <li>{@link ToolCallbackProvider} — turns {@code @Tool}-annotated methods into
 *       MCP tools that a client (Claude / Copilot) can invoke.</li>
 *   <li>{@code List<McpServerFeatures.SyncResourceRegistration>} — registers URI
 *       resources that the client can read via the {@code resources/read} RPC call.</li>
 * </ul>
 *
 * <h2>MCP Concepts</h2>
 * <ul>
 *   <li><b>Resources</b>: read-only URIs the client can subscribe to and read
 *       (analogous to files or API endpoints).  Clients discover them via
 *       {@code resources/list} and fetch content via {@code resources/read}.</li>
 *   <li><b>Tools</b>: callable actions with typed parameters.  The client calls
 *       {@code tools/call} and receives a structured result.  Tools can perform
 *       side-effects (write a file, run a build, etc.) or simply return data.</li>
 * </ul>
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    // -----------------------------------------------------------------------
    // Tools
    // -----------------------------------------------------------------------

    /**
     * Registers every {@code @Tool}-annotated method in {@link LoremIpsumService}
     * as an MCP tool.  The auto-configuration converts the provider into a
     * {@code tools/list} response automatically.
     */
    @Bean
    public ToolCallbackProvider loremIpsumTools(LoremIpsumService service) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(service)
                .build();
    }

    // -----------------------------------------------------------------------
    // Resources
    // -----------------------------------------------------------------------

    /**
     * Registers the {@code lorem://content{?word_count}} resource URI.
     *
     * <p>Clients can read this resource with an optional {@code word_count} query
     * parameter: e.g. {@code lorem://content?word_count=50}.  When omitted the
     * server returns 30 words (the default).
     */
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> loremIpsumResources(LoremIpsumService service) {
        var resource = new McpSchema.Resource(
                "lorem://content{?word_count}",
                "Lorem Ipsum Content",
                "Returns lorem ipsum words from the lorem-ipsum.md file. "
                        + "Expand the URI template with word_count to control how many words "
                        + "are returned, e.g. lorem://content?word_count=50. Default is 30.",
                "text/plain",
                null
        );

        BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler =
                (exchange, request) -> {
                    String uri = request.uri();
                    int wordCount = parseWordCount(uri);
                    String content = service.getWords(wordCount);
                    return new McpSchema.ReadResourceResult(
                            List.of(new McpSchema.TextResourceContents(uri, "text/plain", content))
                    );
                };

        return List.of(new McpServerFeatures.SyncResourceSpecification(resource, handler));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Extracts the {@code word_count} value from an RFC 6570-expanded URI such as
     * {@code lorem://content?word_count=50}, returning the default (30) on any parse
     * failure or when the parameter is absent.
     */
    private static int parseWordCount(String uri) {
        int markerIdx = uri.indexOf("word_count=");
        if (markerIdx < 0) {
            return LoremIpsumService.DEFAULT_WORD_COUNT;
        }
        String raw = uri.substring(markerIdx + "word_count=".length());
        // Trim any subsequent query params or fragments
        int end = raw.indexOf('&');
        if (end >= 0) {
            raw = raw.substring(0, end);
        }
        end = raw.indexOf('#');
        if (end >= 0) {
            raw = raw.substring(0, end);
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : LoremIpsumService.DEFAULT_WORD_COUNT;
        } catch (NumberFormatException e) {
            return LoremIpsumService.DEFAULT_WORD_COUNT;
        }
    }
}
