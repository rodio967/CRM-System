package com.shift.crm.mapper;

import com.shift.crm.dto.response.SellerResponse;
import com.shift.crm.model.Seller;
import org.springframework.stereotype.Component;

@Component
public class SellerMapper {

    public SellerResponse toResponse(Seller seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getName(),
                seller.getContactInfo(),
                seller.getRegistrationDate()
        );
    }
}
