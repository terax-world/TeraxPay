package world.terax.pay.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import world.terax.pay.listeners.WebhookListener;
import world.terax.pay.model.Payment;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class PaymentService {
    private static final String API_URL = "https://api.terax.world";
    private MapService mapService;
    private WebhookListener webhookListener;

    public PaymentService() {
        this.mapService = new MapService();
        this.webhookListener = new WebhookListener();
    }

    public Payment generateLink(String slug) {
        Payment payment = new Payment();
        payment.setSlug(slug);

        try {
            String url = API_URL + "/pay/link";
            HttpURLConnection conn = createConnection(url, "POST");
            String requestBody = "{\"slug\": \"" + slug + "\"}";
            sendRequestBody(conn, requestBody);

            String response = getResponse(conn);
            payment.setQrCode(extractLinkFromResponse(response));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return payment;
    }

    public Payment generatePix(String slug) {
        Payment payment = new Payment();
        payment.setSlug(slug);

        try {
            String url = API_URL + "/pay/pix";
            HttpURLConnection conn = createConnection(url, "POST");
            String requestBody = "{\"slug\": \"" + slug + "\"}";
            sendRequestBody(conn, requestBody);

            String response = getResponse(conn);
            payment.setQrCode(parseQRCodeFromResponse(response));
            payment.setPaymentId(parsePaymentIdFromResponse(response));
            payment.setStatus(parseStatusFromResponse(response));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return payment;
    }

    private HttpURLConnection createConnection(String url, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        return conn;
    }

    private void sendRequestBody(HttpURLConnection conn, String body) throws Exception {
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = body.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
    }

    private String getResponse(HttpURLConnection conn) throws Exception {
        try (Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    public void sendLinkToPlayer(Player player, String link) {
        player.sendMessage("Clique neste link para pagar: " + link);
    }

    private String parseQRCodeFromResponse(String response) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

            if (jsonResponse.has("qr_code")) {
                return jsonResponse.get("qr_code").getAsString();
            } else {
                System.out.println("QR Code not found in the response.");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String parsePaymentIdFromResponse(String response) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

            if (jsonResponse.has("payment_id")) {
                return jsonResponse.get("payment_id").getAsString();
            } else {
                System.out.println("Payment ID not found in the response.");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String parseStatusFromResponse(String response) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

            if (jsonResponse.has("status")) {
                return jsonResponse.get("status").getAsString();
            } else {
                System.out.println("Status not found in the response.");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void sendQRCodeToPlayer(Player player, String qrCode) {
        mapService.createQRCodeMap(player, qrCode);
    }

    public void sendApprovalToPlayer(Player player) {
        mapService.updateMapForPaymentApproval(player);
    }

    private String extractLinkFromResponse(String response) {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(response);
        JsonObject jsonResponse = jsonElement.getAsJsonObject();
        return jsonResponse.get("link").getAsString();
    }
}
