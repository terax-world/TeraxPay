package world.terax.pay.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Payment {
    private String slug;
    private String qrCode;
    private String paymentId;
    private String status;
}
