import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class SearchResult {
    private final int start;
    private final int end;
    private final boolean found;

    private SearchResult(int start, int end, boolean found) {
        this.start = start;
        this.end = end;
        this.found = found;
    }

    public static SearchResult found(int start, int end) {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Must be performed: 0 <= `start` <= `end`.");
        }
        return new SearchResult(start, end, true);
    }

    public static SearchResult notFound() {
        return new SearchResult(-1, -1, false);
    }

    public boolean found() {
        return this.found;
    }

    public int start() {
        if (!this.found) {
            throw new IllegalStateException("Nothing was found.");
        }
        return start;
    }

    public int end() {
        if (!this.found) {
            throw new IllegalStateException("Nothing was found.");
        }
        return end;
    }
}

public class Scanner {
    public static final Pattern DEFAULT_DELIMITER = Pattern.compile("\\p{javaWhitespace}+");
    private static final int BASE_BUFFER_CAPACITY = 1024;
    private final Reader reader;
    private final HashMap<String, SearchResult> cache;
    private Pattern delimiter;
    private boolean canRead;
    private CharBuffer buffer;
    private IOException lastIOException;

    public Scanner(Reader reader) {
        this.reader = reader;
        this.buffer = CharBuffer.allocate(0);
        this.canRead = true;
        this.cache = new HashMap<>();
        this.delimiter = DEFAULT_DELIMITER;
        this.lastIOException = null;
    }

    public Scanner(String source) {
        this(new StringReader(source));
    }

    public Scanner(InputStream source) {
        this(new InputStreamReader(source));
    }

    public Scanner(InputStream source, Charset charset) {
        this(new InputStreamReader(source));
    }

    public Scanner(File source) throws FileNotFoundException {
        this(new FileReader(source));
    }

    public Scanner(File source, Charset charset) throws IOException {
        this(new FileReader(source, charset));
    }

    // TODO delete this after debug
    public void dump() {
        cache.clear();
        do {
            System.out.print(buffer);
            buffer.position(0).limit(0);
        } while (extendBuffer());
        System.out.println();
    }

    public Scanner useDelimiter(String pattern) throws PatternSyntaxException {
        return useDelimiter(Pattern.compile(pattern));
    }

    public Scanner useDelimiter(Pattern pattern) {
        this.delimiter = pattern;
        return this;
    }

    public boolean hasNext() {
        SearchResult firstDelimiter = find(delimiter);
        if (!firstDelimiter.found()) {
            return buffer.hasRemaining();
        } else if (firstDelimiter.start() == 0) {
            return firstDelimiter.end() != buffer.length();
        } else {
            return true;
        }
    }

    public String next() throws NoSuchElementException {
        SearchResult firstDelimiter = find(delimiter);
        String result;
        if (!firstDelimiter.found()) {
            result = buffer.toString();
            buffer.position(buffer.limit());
        } else if (firstDelimiter.start() != 0) {
            result = buffer.subSequence(0, firstDelimiter.start()).toString();
            buffer.position(buffer.position() + firstDelimiter.end());
        } else if (firstDelimiter.end() != buffer.length()) {
            buffer.position(buffer.position() + firstDelimiter.end());
            SearchResult secondDelimiter = find(delimiter);
            if (secondDelimiter.found()) {
                result = buffer.subSequence(0, secondDelimiter.start()).toString();
                buffer.position(buffer.position() + secondDelimiter.end());
            } else {
                result = buffer.toString();
                buffer.position(buffer.limit());
            }
            return result;
        } else {
            throw new NoSuchElementException();
        }
        cache.clear();
        reduceBuffer();
        return result;
    }

    public Scanner skip(String pattern) throws NoSuchElementException, PatternSyntaxException {
        return skip(Pattern.compile(pattern));
    }

    public Scanner skip(Pattern pattern) throws NoSuchElementException {
        SearchResult res = find(pattern);
        if (!res.found() || res.start() != 0) {
            throw new NoSuchElementException();
        }
        buffer.position(buffer.position() + res.end());
        cache.clear();
        reduceBuffer();
        return this;
    }

    private SearchResult find(Pattern pattern) {
        SearchResult result = cache.get(pattern.pattern());
        if (result != null) {
            return result;
        }
        Matcher matcher;
        boolean found;
        do {
            matcher = pattern.matcher(buffer);
            found = matcher.find();
        } while (matcher.hitEnd() && extendBuffer());
        result = found
                ? SearchResult.found(matcher.start(), matcher.end())
                : SearchResult.notFound();
        cache.put(pattern.pattern(), result);
        return result;
    }

    /* Tries to extend the buffer. Returns `canRead`. */
    private boolean extendBuffer() {
        if (!canRead) {
            return false;
        }
        int newBufferCapacity = Math.max(buffer.length() * 2, BASE_BUFFER_CAPACITY);
        CharBuffer newBuffer = CharBuffer.allocate(newBufferCapacity);
        newBuffer.position(buffer.length());
        int read;
        try {
            read = reader.read(newBuffer);
        } catch (IOException readEx) {
            lastIOException = readEx;
            System.err.println("An error occurred while extending the buffer: " + readEx.getMessage());
            try {
                reader.close();
            } catch (IOException closeEx) {
                System.err.println("An error occurred while closing the reader: " + closeEx.getMessage());
            }
            read = -2;
        }
        canRead = read >= 0;
        if (canRead) {
            buffer = newBuffer.flip().append(buffer).position(0);
        }
        return canRead;
    }

    private void reduceBuffer() {
        if (buffer.position() >= buffer.length()) {
            buffer = CharBuffer.allocate(Math.max(buffer.length() * 2, BASE_BUFFER_CAPACITY)).put(buffer).flip();
        }
    }

    public IOException ioException() {
        return lastIOException;
    }
}