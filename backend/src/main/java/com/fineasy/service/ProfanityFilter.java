package com.fineasy.service;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ProfanityFilter {

    private static final Set<String> BLOCKED_WORDS = Set.of(
            "씨발", "시발", "ㅅㅂ", "ㅆㅂ", "개새끼", "병신", "ㅂㅅ",
            "지랄", "ㅈㄹ", "좆", "닥쳐", "꺼져", "미친놈", "미친년",
            "느금마", "니애미", "니엄마", "fuck", "shit", "asshole", "dick"
    );

    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        return BLOCKED_WORDS.stream().anyMatch(lowerText::contains);
    }
}
