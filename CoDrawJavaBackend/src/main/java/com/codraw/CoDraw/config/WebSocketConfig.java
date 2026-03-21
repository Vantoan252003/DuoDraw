package com.codraw.CoDraw.config;

import com.codraw.CoDraw.handler.DrawingWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DrawingWebSocketHandler drawingWebSocketHandler;
    private final com.codraw.CoDraw.handler.GlobalWebSocketHandler globalWebSocketHandler;

    public WebSocketConfig(DrawingWebSocketHandler drawingWebSocketHandler, com.codraw.CoDraw.handler.GlobalWebSocketHandler globalWebSocketHandler) {
        this.drawingWebSocketHandler = drawingWebSocketHandler;
        this.globalWebSocketHandler = globalWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(drawingWebSocketHandler, "/ws/draw")
                .setAllowedOrigins("*");
        registry.addHandler(globalWebSocketHandler, "/ws/global")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Tăng giới hạn tin nhắn text lên 5MB (mặc định chỉ là 8KB) để chứa các nét vẽ (strokes)
        container.setMaxTextMessageBufferSize(5 * 1024 * 1024);
        // Tăng giới hạn binary lên 5MB
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024);
        return container;
    }
}

