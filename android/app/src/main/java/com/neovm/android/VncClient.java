package com.neovm.android;

import android.graphics.Bitmap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Minimal RFB/VNC client for NeoVM's local, unauthenticated VNC endpoint. */
final class VncClient {
    interface Listener {
        void onConnected(int width, int height);
        void onFrame(Bitmap bitmap);
        void onError(String message);
    }

    private final Listener listener;
    private final Object writeLock = new Object();
    private volatile boolean closed;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Bitmap framebuffer;
    private int width;
    private int height;

    VncClient(Listener listener) {
        this.listener = listener;
    }

    void start() {
        new Thread(this::run, "NeoVM-VNC-reader").start();
    }

    private void run() {
        try {
            connectWithRetry();
            if (closed) return;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            handshake();
            while (!closed) {
                int messageType = in.readUnsignedByte();
                switch (messageType) {
                    case 0:
                        readFramebufferUpdate();
                        break;
                    case 1:
                        // SetColorMapEntries: padding, first-colour index, number of entries.
                        in.readUnsignedByte();
                        in.readUnsignedShort();
                        int colorCount = in.readUnsignedShort();
                        skipFully((long) colorCount * 6L);
                        break;
                    case 2:
                        // Bell
                        break;
                    case 3:
                        skipFully(3);
                        int clipboardLength = in.readInt();
                        if (clipboardLength < 0 || clipboardLength > 16 * 1024 * 1024) {
                            throw new IOException("VNC 클립보드 메시지 크기가 비정상적입니다.");
                        }
                        skipFully(clipboardLength);
                        break;
                    default:
                        throw new IOException("지원하지 않는 VNC 메시지 유형: " + messageType);
                }
            }
        } catch (Exception e) {
            if (!closed) listener.onError(e.getMessage() == null ? "VNC 연결이 끊어졌습니다." : e.getMessage());
        } finally {
            close();
        }
    }

    private void connectWithRetry() throws IOException, InterruptedException {
        IOException last = null;
        for (int i = 0; i < 24 && !closed; i++) {
            Socket candidate = new Socket();
            try {
                candidate.connect(new InetSocketAddress("127.0.0.1", 5900), 700);
                candidate.setTcpNoDelay(true);
                candidate.setSoTimeout(0);
                socket = candidate;
                return;
            } catch (IOException e) {
                last = e;
                try { candidate.close(); } catch (IOException ignored) {}
                Thread.sleep(250);
            }
        }
        if (closed) throw new IOException("VNC 연결이 취소되었습니다.");
        throw new IOException("로컬 VNC 서버에 연결할 수 없습니다. VM의 QEMU 빌드가 VNC를 지원하는지 확인해 주세요.",
                last);
    }

    private void handshake() throws IOException {
        byte[] serverVersion = new byte[12];
        in.readFully(serverVersion);
        String version = new String(serverVersion, StandardCharsets.US_ASCII);
        if (!version.startsWith("RFB ")) throw new IOException("VNC 서버 버전 응답이 올바르지 않습니다.");
        out.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
        out.flush();

        int securityCount = in.readUnsignedByte();
        if (securityCount == 0) {
            int length = in.readInt();
            if (length > 0 && length < 65536) {
                byte[] reason = new byte[length];
                in.readFully(reason);
                throw new IOException("VNC 연결 거부: " + new String(reason, StandardCharsets.UTF_8));
            }
            throw new IOException("VNC 서버가 연결을 거부했습니다.");
        }

        int selected = -1;
        for (int i = 0; i < securityCount; i++) {
            int type = in.readUnsignedByte();
            if (type == 1) selected = 1; // None; endpoint is loopback-only.
        }
        if (selected != 1) throw new IOException("이 VNC 서버의 인증 방식은 아직 지원하지 않습니다.");
        out.writeByte(selected);
        out.flush();

        int securityResult = in.readInt();
        if (securityResult != 0) throw new IOException("VNC 보안 협상이 실패했습니다.");

        out.writeByte(1); // Shared session
        out.flush();

        width = in.readUnsignedShort();
        height = in.readUnsignedShort();
        if (width < 1 || height < 1 || width > 8192 || height > 8192 ||
                (long) width * height > 16_000_000L) {
            throw new IOException("VNC 화면 크기가 지원 범위를 벗어났습니다: " + width + "x" + height);
        }
        byte[] serverPixelFormat = new byte[16];
        in.readFully(serverPixelFormat);
        int nameLength = in.readInt();
        if (nameLength < 0 || nameLength > 1024 * 1024) throw new IOException("VNC 서버 이름 길이가 비정상적입니다.");
        skipFully(nameLength);
        framebuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Request 32-bit little-endian true-colour pixels (RGB shifts 16/8/0).
        synchronized (writeLock) {
            out.writeByte(0); // SetPixelFormat
            out.write(new byte[3]);
            out.writeByte(32); // bits-per-pixel
            out.writeByte(24); // depth
            out.writeByte(0);  // big-endian flag
            out.writeByte(1);  // true-colour flag
            out.writeShort(255);
            out.writeShort(255);
            out.writeShort(255);
            out.writeByte(16);
            out.writeByte(8);
            out.writeByte(0);
            out.write(new byte[3]);

            out.writeByte(2); // SetEncodings
            out.writeByte(0);
            out.writeShort(2);
            out.writeInt(0);     // Raw
            out.writeInt(-223);  // DesktopSize
            out.flush();
        }
        listener.onConnected(width, height);
        requestUpdate(false);
    }

    private void readFramebufferUpdate() throws IOException {
        in.readUnsignedByte(); // Padding
        int rectangleCount = in.readUnsignedShort();
        for (int i = 0; i < rectangleCount; i++) {
            int x = in.readUnsignedShort();
            int y = in.readUnsignedShort();
            int w = in.readUnsignedShort();
            int h = in.readUnsignedShort();
            int encoding = in.readInt();

            if (encoding == -223) { // DesktopSize pseudo-encoding
                if (w < 1 || h < 1 || w > 8192 || h > 8192 ||
                        (long) w * h > 16_000_000L) {
                    throw new IOException("VNC 화면 크기가 지원 범위를 벗어났습니다: " + w + "x" + h);
                }
                width = w;
                height = h;
                framebuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                listener.onConnected(width, height);
            } else if (encoding == -224) { // LastRect
                break;
            } else if (encoding == 0) { // Raw, with the requested 32-bit pixel format
                if (w < 1 || h < 1 || x + w > width || y + h > height ||
                        (long) w * h > 16_000_000L) {
                    throw new IOException("VNC 화면 업데이트 범위가 비정상적입니다.");
                }
                byte[] rowBytes = new byte[w * 4];
                int[] rowPixels = new int[w];
                synchronized (framebuffer) {
                    for (int row = 0; row < h; row++) {
                        in.readFully(rowBytes);
                        for (int col = 0, offset = 0; col < w; col++, offset += 4) {
                            int blue = rowBytes[offset] & 255;
                            int green = rowBytes[offset + 1] & 255;
                            int red = rowBytes[offset + 2] & 255;
                            rowPixels[col] = 0xff000000 | (red << 16) | (green << 8) | blue;
                        }
                        framebuffer.setPixels(rowPixels, 0, w, x, y + row, w, 1);
                    }
                }
            } else if (encoding == 1) { // CopyRect
                int srcX = in.readUnsignedShort();
                int srcY = in.readUnsignedShort();
                if (w > 0 && h > 0 && x + w <= width && y + h <= height &&
                        srcX + w <= width && srcY + h <= height) {
                    synchronized (framebuffer) {
                        int[] copy = new int[w * h];
                        framebuffer.getPixels(copy, 0, w, srcX, srcY, w, h);
                        framebuffer.setPixels(copy, 0, w, x, y, w, h);
                    }
                }
            } else {
                throw new IOException("지원하지 않는 VNC 화면 인코딩: " + encoding);
            }
        }
        listener.onFrame(framebuffer);
        requestUpdate(true);
    }

    private void requestUpdate(boolean incremental) throws IOException {
        synchronized (writeLock) {
            if (closed || out == null) return;
            out.writeByte(3); // FramebufferUpdateRequest
            out.writeByte(incremental ? 1 : 0);
            out.writeShort(0);
            out.writeShort(0);
            out.writeShort(width);
            out.writeShort(height);
            out.flush();
        }
    }

    void sendPointer(int x, int y, int buttons) {
        try {
            synchronized (writeLock) {
                if (closed || out == null) return;
                out.writeByte(5);
                out.writeByte(buttons & 255);
                out.writeShort(clamp(x, 0, Math.max(0, width - 1)));
                out.writeShort(clamp(y, 0, Math.max(0, height - 1)));
                out.flush();
            }
        } catch (IOException e) {
            if (!closed) listener.onError("마우스 입력 전송 실패: " + e.getMessage());
        }
    }

    void sendText(String text) {
        if (text == null) return;
        text.codePoints().forEach(codePoint -> {
            if (codePoint == '\r') return;
            int keySym;
            if (codePoint == '\n') keySym = 0xff0d;
            else if (codePoint == '\t') keySym = 0xff09;
            else if (codePoint == 8) keySym = 0xff08;
            else if (codePoint <= 0xff) keySym = codePoint;
            else keySym = 0x01000000 | codePoint;
            sendKey(keySym, true);
            sendKey(keySym, false);
        });
    }

    void sendKey(int keySym, boolean down) {
        try {
            synchronized (writeLock) {
                if (closed || out == null) return;
                out.writeByte(4); // KeyEvent
                out.writeByte(down ? 1 : 0);
                out.writeShort(0);
                out.writeInt(keySym);
                out.flush();
            }
        } catch (IOException e) {
            if (!closed) listener.onError("키보드 입력 전송 실패: " + e.getMessage());
        }
    }

    private static int clamp(int n, int min, int max) {
        return Math.max(min, Math.min(max, n));
    }

    private void skipFully(long length) throws IOException {
        long remaining = length;
        byte[] buffer = new byte[8192];
        while (remaining > 0) {
            int amount = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (amount < 0) throw new IOException("VNC 메시지가 중간에 끝났습니다.");
            remaining -= amount;
        }
    }

    void close() {
        closed = true;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
