package io.attestry.product.application.port.query;

import io.attestry.product.application.dto.view.MyPassportView;
import java.util.List;

public interface MyPassportQueryPort {

    List<MyPassportView> findByOwnerId(String ownerId);
}
