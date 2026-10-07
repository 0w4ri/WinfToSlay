package pp.winf2slay.view.ui;

import com.simsilica.lemur.text.DefaultDocumentModel;

import java.util.function.Predicate;

/**
 * Textmodell für Eingabefelder mit Höchstlänge und erlaubten Zeichen.
 */
public class LimitedDocument extends DefaultDocumentModel {

    private final int maxLength;
    private final Predicate<Character> allowed;

    /**
     * @param text      Startwert
     * @param maxLength Höchstlänge
     * @param allowed   erlaubte Zeichen
     */
    public LimitedDocument(String text, int maxLength, Predicate<Character> allowed) {
        super(text.length() > maxLength ? text.substring(0, maxLength) : text);
        this.maxLength = maxLength;
        this.allowed = allowed;
    }

    @Override
    public void insert(char c) {
        if (getText().length() < maxLength && allowed.test(c)) super.insert(c);
    }

    @Override
    public void insert(String text) {
        for (char c : text.toCharArray()) insert(c);
    }

    @Override
    public void insertNewLine() {
        // einzeilig
    }
}
