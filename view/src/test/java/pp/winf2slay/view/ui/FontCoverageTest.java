package pp.winf2slay.view.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Die Bitmap-Schriften enthalten nur einen Teil von Unicode. Fehlt ein Zeichen,
 * zeichnet jME es unsichtbar (Breite 0) – aus „−4“ würde unbemerkt „4“. Dieser Test
 * prüft alle Zeichenketten im Quelltext aller Module gegen alle Schriften.
 */
class FontCoverageTest {

    private static final String[] FONTS = {"fonts/title.fnt", "fonts/heading.fnt", "fonts/ui.fnt", "fonts/body.fnt"};
    private static final Pattern CHAR = Pattern.compile("^char id=(\\d+)", Pattern.MULTILINE);
    private static final Pattern STRING = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");

    private static Set<Integer> glyphsOf(String font) throws IOException {
        try (InputStream in = FontCoverageTest.class.getClassLoader().getResourceAsStream(font)) {
            assertTrue(in != null, "Schrift fehlt: " + font);
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Set<Integer> ids = new HashSet<>();
            Matcher m = CHAR.matcher(text);
            while (m.find()) ids.add(Integer.parseInt(m.group(1)));
            return ids;
        }
    }

    @Test
    void allTextsCanBeDrawnWithEveryFont() throws IOException {
        Set<Integer> common = null;
        for (String font : FONTS) {
            Set<Integer> glyphs = glyphsOf(font);
            if (common == null) common = glyphs;
            else common.retainAll(glyphs);
        }
        assertFalse(common.isEmpty());

        // Arbeitsverzeichnis der Tests ist das Modul „view“
        List<Path> roots = List.of(Path.of("src/main/java"), Path.of("../model/src/main/java"),
                                   Path.of("../controller/src/main/java"));
        List<String> problems = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file, StandardCharsets.UTF_8);
                    source = BLOCK_COMMENT.matcher(source).replaceAll("");
                    source = LINE_COMMENT.matcher(source).replaceAll("");
                    Matcher m = STRING.matcher(source);
                    while (m.find()) {
                        String literal = m.group(1);
                        for (int i = 0; i < literal.length(); i++) {
                            char c = literal.charAt(i);
                            if (c >= 32 && !common.contains((int) c))
                                problems.add(file.getFileName() + ": '" + c + "' (U+"
                                             + Integer.toHexString(c).toUpperCase() + ") in \"" + literal + "\"");
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), "Zeichen ohne Glyphe in den Schriften:\n" + String.join("\n", problems));
    }
}
