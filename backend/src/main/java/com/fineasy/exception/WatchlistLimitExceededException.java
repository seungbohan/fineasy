package com.fineasy.exception;

public class WatchlistLimitExceededException extends BusinessException {

    private static final int MAX_WATCHLIST_SIZE = 30;

    public WatchlistLimitExceededException() {
        super("WATCHLIST_LIMIT_EXCEEDED",
                "Watchlist limit of " + MAX_WATCHLIST_SIZE + " items exceeded");
    }
}
