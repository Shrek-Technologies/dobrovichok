package ru.dobrovichek.contracts;

public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    /**
     * Same rules as identity login/register lookup: digits and leading '+' only.
     */
    public static String normalize(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9+]", "");
    }
}
