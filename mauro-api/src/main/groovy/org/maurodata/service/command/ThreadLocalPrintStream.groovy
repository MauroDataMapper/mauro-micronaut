package org.maurodata.service.command

class ThreadLocalPrintStream extends PrintStream {

    ThreadLocal<PrintStream> threadLocalWithPrintStream
    PrintStream fallback

    ThreadLocalPrintStream(final ThreadLocal<PrintStream> threadLocalWithPrintStream, final PrintStream fallback) {
        super(fallback)
        this.threadLocalWithPrintStream = threadLocalWithPrintStream
        this.fallback = fallback
    }

    private PrintStream getPrintStream() {
        PrintStream ps = threadLocalWithPrintStream.get()
        return ps != null ? ps : fallback
    }

    @Override
    void flush() {
        getPrintStream().flush()
    }

    @Override
    void close() {
        getPrintStream().close()
    }

    @Override
    boolean checkError() {
        return getPrintStream().checkError()
    }

    @Override
    protected void setError() {
        getPrintStream().setError()
    }

    @Override
    protected void clearError() {
        getPrintStream().clearError()
    }

    @Override
    void write(int b) {
        getPrintStream().write(b)
    }

    @Override
    void write(byte[] buf, int off, int len) {
        getPrintStream().write(buf, off, len)
    }

    @Override
    void write(byte[] buf) throws IOException {
        getPrintStream().write(buf)
    }

    @Override
    void writeBytes(byte[] buf) {
        getPrintStream().writeBytes(buf)
    }

    @Override
    void print(boolean b) {
        getPrintStream().print(b)
    }

    @Override
    void print(char c) {
        getPrintStream().print(c)
    }

    @Override
    void print(int i) {
        getPrintStream().print(i)
    }

    @Override
    void print(long l) {
        getPrintStream().print(l)
    }

    @Override
    void print(float f) {
        getPrintStream().print(f)
    }

    @Override
    void print(double d) {
        getPrintStream().print(d)
    }

    @Override
    void print(char[] s) {
        getPrintStream().print(s)
    }

    @Override
    void print(String s) {
        getPrintStream().print(s)
    }

    @Override
    void print(Object obj) {
        getPrintStream().print(obj)
    }

    @Override
    void println() {
        getPrintStream().println()
    }

    @Override
    void println(boolean x) {
        getPrintStream().println(x)
    }

    @Override
    void println(char x) {
        getPrintStream().println(x)
    }

    @Override
    void println(int x) {
        getPrintStream().println(x)
    }

    @Override
    void println(long x) {
        getPrintStream().println(x)
    }

    @Override
    void println(float x) {
        getPrintStream().println(x)
    }

    @Override
    void println(double x) {
        getPrintStream().println(x)
    }

    @Override
    void println(char[] x) {
        getPrintStream().println(x)
    }

    @Override
    void println(String x) {
        getPrintStream().println(x)
    }

    @Override
    void println(Object x) {
        getPrintStream().println(x)
    }

    @Override
    PrintStream printf(String format, Object... args) {
        return getPrintStream().printf(format, args)
    }

    @Override
    PrintStream printf(Locale l, String format, Object... args) {
        return getPrintStream().printf(l, format, args)
    }

    @Override
    PrintStream format(String format, Object... args) {
        return getPrintStream().format(format, args)
    }

    @Override
    PrintStream format(Locale l, String format, Object... args) {
        return getPrintStream().format(l, format, args)
    }

    @Override
    PrintStream append(CharSequence csq) {
        return getPrintStream().append(csq)
    }

    @Override
    PrintStream append(CharSequence csq, int start, int end) {
        return getPrintStream().append(csq, start, end)
    }

    @Override
    PrintStream append(char c) {
        return getPrintStream().append(c)
    }

}
