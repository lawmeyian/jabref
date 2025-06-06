package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

/// Replaces and's for & (in case of two authors) and ; (in case
/// of more than two authors).
public class AuthorAndsReplacer implements LayoutFormatter {

    /* (non-Javadoc)
     * @see org.jabref.export.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return null;
        }
        String[] authors = fieldText.split(" and ");
        // CHECKSTYLE:OFF
        String s = switch (authors.length) {
            case 1 -> authors[0]; // just no action
            case 2 -> authors[0] + " & " + authors[1];
            default -> {
                int i;
                int x = authors.length;
                StringBuilder sb = new StringBuilder();
                for (i = 0; i < (x - 2); i++) {
                    sb.append(authors[i]).append("; ");
                }
                sb.append(authors[i]).append(" & ").append(authors[i + 1]);
                yield sb.toString();
            }
        };
        // CHECKSTYLE:ON

        return s;
    }
}
