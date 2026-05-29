package com.nova.ledger.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String remark;
}
