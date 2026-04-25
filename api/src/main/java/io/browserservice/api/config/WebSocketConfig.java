package io.browserservice.api.config;

import io.browserservice.api.ws.CallerIdHandshakeInterceptor;
import io.browserservice.api.ws.SessionWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final EngineProperties props;
    private final SessionWebSocketHandler handler;
    private final CallerIdHandshakeInterceptor interceptor;

    public WebSocketConfig(EngineProperties props,
                           SessionWebSocketHandler handler,
                           CallerIdHandshakeInterceptor interceptor) {
        this.props = props;
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, props.webSocket().path())
                .addInterceptors(interceptor)
                .setAllowedOriginPatterns("*");
    }
}
