package world.terax.pay.listeners;

import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.util.UUID;

public class WebhookListener extends WebSocketClient {

    private final WebhookListener webhookListener;

    public WebhookListener(URI serverUri, WebhookListener listener) {
        super(serverUri);
        this.webhookListener = listener;
    }

    @Override
    public void onOpen(WebSocketServerProtocolHandler.ServerHandshakeStateEvent handshakedata) {
        System.out.println("Conectado ao WebSocket da API");
    }

    @Override
    public void onMessage(String message) {
        JSONObject json = new JSONObject(message);
        String invoiceId = json.getString("invoiceId");
        String status = json.getString("status");

        // Repasse para WebhookListener
        Bukkit.getScheduler().runTask(TeraxPay.getInstance(), () -> {
            webhookListener.handleWebhook(invoiceId, status);
        });
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket desconectado: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}
