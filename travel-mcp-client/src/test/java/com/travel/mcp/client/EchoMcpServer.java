package com.travel.mcp.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * 内存中的 Echo MCP Server，用于测试客户端协议。
 * 
 * <p>模拟天气 MCP Server，返回硬编码的天气数据。</p>
 */
public class EchoMcpServer {

    private static final Logger log = LoggerFactory.getLogger(EchoMcpServer.class);

    private final int port;
    private HttpServer server;

    public EchoMcpServer(int port) {
        this.port = port;
    }

    /**
     * 启动 Echo Server
     *
     * @throws IOException 如果启动失败
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Handler for /mcp/info - returns server info
        server.createContext("/mcp/info", exchange -> {
            log.info("Received request: {}", exchange.getRequestURI());
            String resp = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "name": "echo-weather",
                        "version": "1.0.0",
                        "tools": [
                            {
                                "name": "weather.get_forecast",
                                "description": "获取天气预报",
                                "inputSchema": {
                                    "type": "object",
                                    "properties": {
                                        "city": {
                                            "type": "string",
                                            "description": "城市名称"
                                        }
                                    }
                                }
                            },
                            {
                                "name": "weather.get_current",
                                "description": "获取当前天气",
                                "inputSchema": {
                                    "type": "object",
                                    "properties": {
                                        "city": {
                                            "type": "string",
                                            "description": "城市名称"
                                        }
                                    }
                                }
                            }
                        ]
                    }
                }
                """;
            sendJson(exchange, resp);
        });

        // Handler for /mcp/call - returns tool call result
        server.createContext("/mcp/call", exchange -> {
            log.info("Received call request: {}", exchange.getRequestURI());
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            log.debug("Request body: {}", body);

            // Echo back with weather data
            String resp = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "success": true,
                        "data": {
                            "city": "杭州",
                            "forecast": [
                                {
                                    "date": "2026-08-12",
                                    "tempMin": 26,
                                    "tempMax": 34,
                                    "text": "晴"
                                },
                                {
                                    "date": "2026-08-13",
                                    "tempMin": 27,
                                    "tempMax": 35,
                                    "text": "多云"
                                }
                            ]
                        }
                    }
                }
                """;
            sendJson(exchange, resp);
        });

        // Handler for /mcp/stream/{taskId} - returns SSE stream
        server.createContext("/mcp/stream/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String taskId = path.substring("/mcp/stream/".length());
            log.info("Starting SSE stream for taskId: {}", taskId);

            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");

            // Send SSE events
            try {
                // Send token event
                String tokenEvent = "event: token\ndata: {\"type\":\"token\",\"data\":\"正在查询天气...\"}\n\n";
                exchange.getResponseBody().write(tokenEvent.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();

                Thread.sleep(100);

                // Send tool call event
                String toolEvent = "event: tool_call\ndata: {\"type\":\"tool_call\",\"data\":{\"name\":\"weather.get_forecast\",\"arguments\":{\"city\":\"杭州\"}}}\n\n";
                exchange.getResponseBody().write(toolEvent.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();

                Thread.sleep(100);

                // Send final result
                String finalEvent = "event: task_done\ndata: {\"type\":\"final\",\"data\":{\"city\":\"杭州\",\"temp\":32,\"text\":\"晴\"}}\n\n";
                exchange.getResponseBody().write(finalEvent.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();

            } catch (IOException | InterruptedException e) {
                log.warn("SSE stream error", e);
            } finally {
                exchange.close();
            }
        });

        server.setExecutor(null);
        server.start();
        log.info("Echo MCP Server started on port {}", port);
    }

    /**
     * 停止 Echo Server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("Echo MCP Server stopped");
        }
    }

    /**
     * 发送 JSON 响应
     */
    private void sendJson(HttpExchange exchange, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /**
     * 获取服务器 URL
     *
     * @return URL
     */
    public String getUrl() {
        return "http://localhost:" + port;
    }
}
