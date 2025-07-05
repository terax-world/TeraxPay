package world.terax.service;

import com.google.gson.Gson;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import world.terax.model.CreatePurchaseResponse;
import world.terax.util.MapQRCodeUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class PurchaseService {

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private final Map<UUID, Short> playerMapIds = new HashMap<>();

    public void createPurchase(Player player, String productSlug, String method) {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{" +
                        "\"playerNick\":\"" + player.getName() + "\"," +
                        "\"productSlug\":\"" + productSlug + "\"," +
                        "\"paymentMethod\":\"" + method + "\"" +
                        "}"
        );

        Request request = new Request.Builder()
                .url("http://localhost:8080/purchases/by-slug")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                player.sendMessage("§cErro ao tentar iniciar a compra: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        player.sendMessage("§cErro ao criar compra: " + response.code());
                        return;
                    }

                    String responseBodyString = responseBody.string();
                    CreatePurchaseResponse purchase = gson.fromJson(responseBodyString, CreatePurchaseResponse.class);

                    if (method.equalsIgnoreCase("pix")) {
                        Bukkit.getScheduler().runTask(world.terax.TeraxPay.getInstance(), () -> {
                            short mapId = MapQRCodeUtil.giveMapWithQRCode(player, purchase.qrcode);
                            playerMapIds.put(player.getUniqueId(), mapId);
                            player.sendMessage("§aAbra o mapa para escanear o QR Code do Pix!");
                        });
                    } else {
                        player.sendMessage("§aClique no link para pagar com cartão: " + purchase.paylink);
                    }

                }
            }

        });
    }

    public void markPaymentApproved(Player player) {
        Short mapId = playerMapIds.get(player.getUniqueId());
        if (mapId == null) {
            player.sendMessage("§cNenhum mapa salvo para atualizar.");
            return;
        }

        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("TeraxPlugin"), () -> {
            MapQRCodeUtil.updateMapToV(player, mapId);
            player.sendMessage("§aPagamento aprovado! Veja o V verde no mapa.");
        });
    }

}
