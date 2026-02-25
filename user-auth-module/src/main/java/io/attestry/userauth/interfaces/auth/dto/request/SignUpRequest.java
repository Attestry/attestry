package io.attestry.userauth.interfaces.auth.dto.request;

// TODO("valid check")
// TODO("email 형식")
// TODO("password 는 대문자 포함 8자리 이상")
// TODO("phone 은 010-0000-0000" 형식)
public record SignUpRequest(String email, String password, String phone) {
}