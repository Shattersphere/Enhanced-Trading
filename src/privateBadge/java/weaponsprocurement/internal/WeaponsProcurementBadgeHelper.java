package weaponsprocurement.internal;

public final class WeaponsProcurementBadgeHelper {
    private static final String TOTAL_ERR = "graphics/ui/wp_total_err.png";
    private static final String TOTAL_99PLUS = "graphics/ui/wp_total_green_99plus.png";
    private static final String TOTAL_RED_PREFIX = "graphics/ui/wp_total_red_";
    private static final String TOTAL_YELLOW_PREFIX = "graphics/ui/wp_total_yellow_";
    private static final String TOTAL_GREEN_PREFIX = "graphics/ui/wp_total_green_";
    private static final String TOTAL_SUFFIX = ".png";

    private static final String KEY_READY = "wp.counts.ready";
    private static final String KEY_PATCHED_BADGES_ENABLED = "wp.config.patchedBadgesEnabled";
    private static final String KEY_WEAPON_PREFIX = "wp.weapon.";
    private static final String KEY_FIGHTER_PREFIX = "wp.fighter.";
    private static final String KEY_PLAYER_SUFFIX = ".player";
    private static final String KEY_STORAGE_SUFFIX = ".storage";

    private WeaponsProcurementBadgeHelper() {
    }

    public static String getTotalStatusSpritePath(String weaponId) {
        return getTotalStatusSpritePath("weapon", weaponId);
    }

    public static String getTotalStatusSpritePath(String kind, String id) {
        if (!isPatchedBadgesEnabled() || !isReady() || isEmpty(id)) {
            return null;
        }

        Integer playerCount = readCount(kind, id, true);
        Integer storageCount = readCount(kind, id, false);
        if (playerCount == null || storageCount == null) {
            return null;
        }

        int total = playerCount + storageCount;
        if (total >= 99) {
            return TOTAL_99PLUS;
        }
        if (total >= 0) {
            return toTotalSprite(total);
        }
        return TOTAL_ERR;
    }

    private static boolean isReady() {
        return "true".equalsIgnoreCase(System.getProperty(KEY_READY));
    }

    private static boolean isPatchedBadgesEnabled() {
        return "true".equalsIgnoreCase(System.getProperty(KEY_PATCHED_BADGES_ENABLED));
    }

    private static Integer readCount(String kind, String id, boolean player) {
        String prefix = getPrefix(kind);
        if (prefix == null) {
            return null;
        }
        String raw = System.getProperty(prefix + id + (player ? KEY_PLAYER_SUFFIX : KEY_STORAGE_SUFFIX));
        if (isEmpty(raw)) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String getPrefix(String kind) {
        if ("weapon".equals(kind)) {
            return KEY_WEAPON_PREFIX;
        }
        if ("fighter".equals(kind)) {
            return KEY_FIGHTER_PREFIX;
        }
        return null;
    }

    private static String toTotalSprite(int total) {
        if (total == 0) {
            return TOTAL_RED_PREFIX + "0" + TOTAL_SUFFIX;
        }
        if (total <= 9) {
            return TOTAL_YELLOW_PREFIX + total + TOTAL_SUFFIX;
        }
        return TOTAL_GREEN_PREFIX + total + TOTAL_SUFFIX;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }
}
