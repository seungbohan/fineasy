package com.fineasy.service;

public interface TokenProvider {

    String generateAccessToken(long userId, String email);

    String generateRefreshToken(long userId, String email);

    long getUserIdFromToken(String token);

    String getEmailFromToken(String token);

    boolean validateToken(String token);

    boolean isRefreshToken(String token);
}
