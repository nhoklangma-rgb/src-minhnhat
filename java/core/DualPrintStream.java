package core;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DualPrintStream extends PrintStream {
    private final PrintStream second;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("[yyyy-MM-ddHH:mm:ss] ");

    public DualPrintStream(PrintStream main, String fileName) throws FileNotFoundException {
        super(main);
        FileOutputStream fos = new FileOutputStream(fileName, true);
        this.second = new PrintStream(fos);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);
        if (core.Manager.gI().write_log_to_file) {
            second.write(buf, off, len);
            second.flush();
        }
    }

    @Override
    public void println(String x) {
        super.println(x);
        if (core.Manager.gI().write_log_to_file) {
            String timestamp = dateFormat.format(new Date());
            second.println(timestamp + x);
            second.flush();
        }
    }

    @Override
    public void print(String s) {
        super.print(s);
        if (core.Manager.gI().write_log_to_file) {
            second.print(s);
            second.flush();
        }
    }

    @Override
    public void close() {
        super.close();
        second.close();
    }
}
