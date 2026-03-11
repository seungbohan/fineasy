package com.fineasy.external.coingecko;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CoinDef {

    BTC("bitcoin", "BTC", "Bitcoin"),
    ETH("ethereum", "ETH", "Ethereum"),
    XRP("ripple", "XRP", "XRP"),
    BNB("binancecoin", "BNB", "BNB"),
    SOL("solana", "SOL", "Solana"),
    ADA("cardano", "ADA", "Cardano"),
    DOGE("dogecoin", "DOGE", "Dogecoin"),
    AVAX("avalanche-2", "AVAX", "Avalanche"),
    DOT("polkadot", "DOT", "Polkadot"),
    LINK("chainlink", "LINK", "Chainlink");

    private final String coingeckoId;
    private final String symbol;
    private final String name;

    private static final Map<String, CoinDef> BY_SYMBOL = Arrays.stream(values())
            .collect(Collectors.toMap(
                    def -> def.symbol.toUpperCase(),
                    Function.identity()
            ));

    private static final Map<String, CoinDef> BY_COINGECKO_ID = Arrays.stream(values())
            .collect(Collectors.toMap(
                    CoinDef::coingeckoId,
                    Function.identity()
            ));

    CoinDef(String coingeckoId, String symbol, String name) {
        this.coingeckoId = coingeckoId;
        this.symbol = symbol;
        this.name = name;
    }

    public String coingeckoId() { return coingeckoId; }
    public String symbol() { return symbol; }
    public String displayName() { return name; }

    public static String allCoingeckoIds() {
        return Arrays.stream(values())
                .map(CoinDef::coingeckoId)
                .collect(Collectors.joining(","));
    }

    public static CoinDef findBySymbol(String symbol) {
        if (symbol == null) return null;
        return BY_SYMBOL.get(symbol.toUpperCase());
    }

    public static CoinDef findByCoingeckoId(String coingeckoId) {
        if (coingeckoId == null) return null;
        return BY_COINGECKO_ID.get(coingeckoId);
    }
}
