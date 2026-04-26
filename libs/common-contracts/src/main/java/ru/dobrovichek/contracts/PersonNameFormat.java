package ru.dobrovichek.contracts;

/**
 * Форматы отображения ФИО для API и клиентов.
 */
public final class PersonNameFormat {

    private PersonNameFormat() {
    }

    /** Полное ФИО: «Имя Отчество Фамилия» (пустые части пропускаются). */
    public static String fullFormal(String firstName, String patronymic, String lastName) {
        String f = blankToNull(firstName);
        String p = blankToNull(patronymic);
        String l = blankToNull(lastName);
        StringBuilder sb = new StringBuilder();
        appendPart(sb, f);
        appendPart(sb, p);
        appendPart(sb, l);
        return sb.toString();
    }

    /** Только имя (кратко на карте у волонтёра). */
    public static String firstNameOnly(String firstName) {
        String f = blankToNull(firstName);
        return f != null ? f : "";
    }

    /** Имя и фамилия — как показываем волонтёра подопечному. */
    public static String firstAndLast(String firstName, String lastName) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, blankToNull(firstName));
        appendPart(sb, blankToNull(lastName));
        return sb.toString();
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (part == null) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(part);
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
