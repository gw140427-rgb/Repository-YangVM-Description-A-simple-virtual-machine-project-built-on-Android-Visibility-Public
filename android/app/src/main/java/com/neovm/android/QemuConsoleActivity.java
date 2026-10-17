package com.neovm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class QemuConsoleActivity extends Activity {
    private final int bg = Color.rgb(11, 16, 32);
    private final int panel = Color.rgb(21, 29, 51);
    private final int white = Color.rgb(240, 244, 255);
    private final int muted = Color.rgb(157, 170, 201);
    private final int accent = Color.rgb(139, 155, 255);
    private final Handler main = new Handler(Looper.getMainLooper());

    private String vmId, vmName, guestOs, isoUri;
    private int ramMb, diskGb;
    private TextView status, console;
    private ScrollView consoleScroll;
    private EditText input;
    private Button startButton, stopButton, sendButton, screenButton;
    private volatile Process qemuProcess;
    private volatile boolean starting = false;
    private volatile boolean cancelStartup = false;
    private File vmDir, isoFile, diskFile;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        vmId = getIntent().getStringExtra("vm_id");
        vmName = getIntent().getStringExtra("vm_name");
        guestOs = getIntent().getStringExtra("vm_os");
        isoUri = getIntent().getStringExtra("vm_iso_uri");
        ramMb = getIntent().getIntExtra("vm_ram_mb", 1024);
        diskGb = getIntent().getIntExtra("vm_disk_gb", 16);
        if (vmId == null || isoUri == null) {
            finish();
            return;
        }
        vmDir = new File(getFilesDir(), "vms/" + vmId);
        isoFile = new File(vmDir, "installer.iso");
        diskFile = new File(vmDir, "disk.qcow2");
        render();
    }

    private void render() {
        ScrollView page = new ScrollView(this);
        page.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(20));
        page.addView(root);

        TextView title = text(vmName == null ? "가상 머신" : vmName, 23, white, true);
        root.addView(title);
        TextView detail = text((guestOs == null ? "운영체제" : guestOs) + "  ·  RAM " + ramMb + "MB  ·  디스크 " + diskGb + "GB", 12, muted, false);
        detail.setPadding(0, dp(6), 0, dp(16));
        root.addView(detail);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(14), dp(13), dp(14), dp(13));
        statusCard.setBackground(round(panel, 14));
        status = text("●  중지됨", 14, muted, true);
        statusCard.addView(status);
        TextView hint = text("VM 화면은 VNC로 열 수 있습니다. 직렬 콘솔도 아래에 계속 표시됩니다.", 12, muted, false);
        hint.setPadding(0, dp(8), 0, 0);
        statusCard.addView(hint);
        root.addView(statusCard);

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(0, dp(12), 0, dp(12));
        startButton = button("▶  VM 시작", true);
        startButton.setOnClickListener(v -> startVm());
        actions.addView(startButton, new LinearLayout.LayoutParams(0, dp(46), 1));
        stopButton = button("■  중지", false);
        stopButton.setEnabled(false);
        LinearLayout.LayoutParams stopP = new LinearLayout.LayoutParams(0, dp(46), 1);
        stopP.leftMargin = dp(8);
        stopButton.setOnClickListener(v -> stopVm());
        actions.addView(stopButton, stopP);
        root.addView(actions);

        screenButton = button("화면 보기 (VNC)", false);
        screenButton.setEnabled(false);
        screenButton.setOnClickListener(v -> {
            Process running = qemuProcess;
            if (running == null || !running.isAlive()) {
                Toast.makeText(this, "먼저 VM을 시작해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent viewIntent = new Intent(this, VncDisplayActivity.class);
            viewIntent.putExtra("vm_name", vmName);
            startActivity(viewIntent);
        });
        LinearLayout.LayoutParams screenP = new LinearLayout.LayoutParams(-1, dp(44));
        screenP.bottomMargin = dp(12);
        root.addView(screenButton, screenP);

        TextView consoleTitle = text("직렬 콘솔 출력", 14, white, true);
        consoleTitle.setPadding(0, dp(5), 0, dp(8));
        root.addView(consoleTitle);

        consoleScroll = new ScrollView(this);
        consoleScroll.setBackground(round(Color.rgb(4, 7, 14), 12));
        consoleScroll.setFillViewport(false);
        console = text("NeoVM QEMU console\nISO 파일은 VM 전용 폴더로 복사되고, 가상 디스크는 QCOW2 형식으로 생성됩니다.\n\n", 12, Color.rgb(189, 219, 192), false);
        console.setTypeface(Typeface.MONOSPACE);
        console.setPadding(dp(12), dp(12), dp(12), dp(12));
        consoleScroll.addView(console);
        LinearLayout.LayoutParams consoleP = new LinearLayout.LayoutParams(-1, dp(280));
        root.addView(consoleScroll, consoleP);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("콘솔 입력 (예: Enter)");
        input.setTextColor(white);
        input.setHintTextColor(muted);
        input.setBackground(round(panel, 10));
        input.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams inputP = new LinearLayout.LayoutParams(-1, dp(48));
        inputP.topMargin = dp(10);
        root.addView(input, inputP);

        sendButton = button("콘솔에 입력 보내기", false);
        sendButton.setOnClickListener(v -> sendInput());
        LinearLayout.LayoutParams sendP = new LinearLayout.LayoutParams(-1, dp(44));
        sendP.topMargin = dp(8);
        root.addView(sendButton, sendP);

        TextView warning = text("주의: ARM64 Linux ISO부터 시험하세요. 그래픽 화면은 QEMU/VNC와 게스트 OS 호환성에 따라 달라집니다. 스냅샷과 하드웨어 가속은 아직 지원하지 않습니다.", 11, muted, false);
        warning.setPadding(0, dp(16), 0, 0);
        root.addView(warning);
        setContentView(page);
    }

    private void startVm() {
        if (starting || (qemuProcess != null && qemuProcess.isAlive())) return;
        starting = true;
        cancelStartup = false;
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        status.setText("●  준비 중 — ISO 및 디스크 확인");
        if (screenButton != null) screenButton.setEnabled(false);
        append("VM 시작 요청: QEMU 런타임 확인 중...\n");
        new Thread(() -> {
            try {
                File qemu = new File(getApplicationInfo().nativeLibraryDir, "libqemu.so");
                File qemuImg = new File(getApplicationInfo().nativeLibraryDir, "libqemu_img.so");
                if (!qemu.isFile() || !qemuImg.isFile()) {
                    throw new IllegalStateException("이 APK에 ARM64 QEMU 런타임이 없습니다. GitHub Actions의 최신 NeoVM-debug-APK 빌드를 사용해 주세요.");
                }
                checkStartupNotCancelled();
                if (!vmDir.mkdirs() && !vmDir.isDirectory()) {
                    throw new IllegalStateException("VM 저장 폴더를 만들 수 없습니다: " + vmDir.getAbsolutePath());
                }
                copyIsoIfNeeded();
                checkStartupNotCancelled();

                File firmwareFile = new File(vmDir, "QEMU_EFI.fd");
                final long expectedFirmwareBytes = 64L * 1024L * 1024L;
                if (!firmwareFile.isFile() || firmwareFile.length() != expectedFirmwareBytes) {
                    File firmwareTemp = new File(vmDir, "QEMU_EFI.fd.tmp");
                    if (firmwareTemp.exists() && !firmwareTemp.delete()) {
                        throw new IllegalStateException("이전 임시 UEFI 파일을 정리할 수 없습니다.");
                    }
                    long copiedFirmwareBytes = 0L;
                    try (InputStream firmwareIn = getAssets().open("firmware/QEMU_EFI.fd");
                         OutputStream firmwareOut = new FileOutputStream(firmwareTemp)) {
                        byte[] firmwareBuffer = new byte[64 * 1024];
                        int firmwareRead;
                        while ((firmwareRead = firmwareIn.read(firmwareBuffer)) != -1) {
                            checkStartupNotCancelled();
                            firmwareOut.write(firmwareBuffer, 0, firmwareRead);
                            copiedFirmwareBytes += firmwareRead;
                        }
                        firmwareOut.flush();
                    } catch (Exception e) {
                        firmwareTemp.delete();
                        throw e;
                    }
                    if (copiedFirmwareBytes != expectedFirmwareBytes || firmwareTemp.length() != expectedFirmwareBytes) {
                        firmwareTemp.delete();
                        throw new IllegalStateException("UEFI 펌웨어 파일 크기가 올바르지 않습니다. 다시 빌드한 APK를 사용해 주세요.");
                    }
                    if (firmwareFile.exists() && !firmwareFile.delete()) {
                        firmwareTemp.delete();
                        throw new IllegalStateException("손상된 임시 UEFI 파일을 교체할 수 없습니다.");
                    }
                    if (!firmwareTemp.renameTo(firmwareFile)) {
                        firmwareTemp.delete();
                        throw new IllegalStateException("UEFI 펌웨어 파일을 최종 위치로 옮길 수 없습니다.");
                    }
                    append("ARM64 UEFI 펌웨어 준비 완료.\n");
                }

                checkStartupNotCancelled();
                if (!diskFile.exists()) {
                    append("가상 디스크 생성 중 (" + diskGb + "GB, QCOW2 sparse image)...\n");
                    // Create a temporary disk first so an interrupted qemu-img command cannot leave a partial disk.qcow2.
                    File diskTemp = new File(vmDir, "disk.qcow2.tmp");
                    if (diskTemp.exists() && !diskTemp.delete()) {
                        throw new IllegalStateException("이전 임시 가상 디스크를 정리할 수 없습니다.");
                    }
                    ProcessBuilder diskBuilder = new ProcessBuilder(qemuImg.getAbsolutePath(), "create", "-f", "qcow2", diskTemp.getAbsolutePath(), diskGb + "G");
                    diskBuilder.environment().put("LD_LIBRARY_PATH", getApplicationInfo().nativeLibraryDir);
                    diskBuilder.redirectErrorStream(true);
                    Process diskProc = diskBuilder.start();
                    String diskOutput = readAll(diskProc.getInputStream());
                    int code = diskProc.waitFor();
                    if (code != 0 || !diskTemp.isFile() || diskTemp.length() == 0L) {
                        diskTemp.delete();
                        throw new IllegalStateException("qemu-img 가상 디스크 생성 실패 (" + code + "): " + diskOutput);
                    }
                    if (!diskTemp.renameTo(diskFile)) {
                        diskTemp.delete();
                        throw new IllegalStateException("가상 디스크 임시 파일을 최종 위치로 옮길 수 없습니다.");
                    }
                    append("가상 디스크 생성 완료.\n");
                }

                // Inspect any existing disk before attaching it; never recreate or overwrite an existing user disk.
                ProcessBuilder infoBuilder = new ProcessBuilder(qemuImg.getAbsolutePath(), "info", diskFile.getAbsolutePath());
                infoBuilder.environment().put("LD_LIBRARY_PATH", getApplicationInfo().nativeLibraryDir);
                infoBuilder.redirectErrorStream(true);
                Process infoProc = infoBuilder.start();
                String diskInfo = readAll(infoProc.getInputStream());
                int infoCode = infoProc.waitFor();
                if (infoCode != 0 || !diskInfo.toLowerCase(java.util.Locale.ROOT).contains("file format: qcow2")) {
                    throw new IllegalStateException("가상 디스크 검증에 실패했습니다. 데이터 보호를 위해 기존 파일을 삭제하거나 덮어쓰지 않았습니다.\n" + diskInfo);
                }

                checkStartupNotCancelled();
                List<String> cmd = new ArrayList<>();
                cmd.add(qemu.getAbsolutePath());
                cmd.addAll(java.util.Arrays.asList(
                        "-machine", "virt",
                        "-cpu", "cortex-a53",
                        "-bios", firmwareFile.getAbsolutePath(),
                        "-smp", "2",
                        "-m", Integer.toString(Math.max(512, ramMb)),
                        "-drive", "if=none,file=" + diskFile.getAbsolutePath() + ",format=qcow2,id=hd0",
                        "-device", "virtio-blk-device,drive=hd0",
                        "-drive", "if=none,file=" + isoFile.getAbsolutePath() + ",media=cdrom,readonly=on,id=cd0",
                        "-device", "virtio-scsi-device,id=scsi0",
                        "-device", "scsi-cd,drive=cd0,bus=scsi0.0",
                        "-boot", "order=d",
                        "-netdev", "user,id=net0",
                        "-device", "virtio-net-device,netdev=net0",
                        "-device", "virtio-gpu-device",
                        "-vnc", "127.0.0.1:0",
                        "-serial", "stdio",
                        "-monitor", "none",
                        "-no-reboot"
                ));
                checkStartupNotCancelled();
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.environment().put("LD_LIBRARY_PATH", getApplicationInfo().nativeLibraryDir);
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                qemuProcess = proc;
                if (cancelStartup) {
                    proc.destroy();
                    if (!proc.waitFor(3, TimeUnit.SECONDS)) proc.destroyForcibly();
                    if (qemuProcess == proc) qemuProcess = null;
                    return;
                }
                main.post(() -> {
                    starting = false;
                    startButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    if (screenButton != null) screenButton.setEnabled(true);
                    status.setText("●  QEMU 실행 중 — 게스트 부팅 대기");
                    append("QEMU 프로세스가 시작됐습니다. '화면 보기 (VNC)'를 눌러 그래픽 화면을 열 수 있습니다.\n");
                });
                Thread reader = new Thread(() -> {
                    try {
                        byte[] buffer = new byte[2048];
                        int count;
                        while ((count = proc.getInputStream().read(buffer)) != -1) {
                            String s = new String(buffer, 0, count, java.nio.charset.StandardCharsets.UTF_8);
                            append(s);
                        }
                    } catch (Exception e) {
                        append("\n[콘솔 읽기 종료: " + e.getMessage() + "]\n");
                    }
                    try {
                        int exit = proc.waitFor();
                        main.post(() -> {
                            if (qemuProcess == proc) qemuProcess = null;
                            starting = false;
                            startButton.setEnabled(true);
                            stopButton.setEnabled(false);
                            if (screenButton != null) screenButton.setEnabled(false);
                            status.setText("●  중지됨 (종료 코드 " + exit + ")");
                            append("\nQEMU가 종료됐습니다. 코드: " + exit + "\n");
                        });
                    } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }, "NeoVM-QEMU-output");
                reader.setDaemon(true);
                reader.start();
            } catch (Exception e) {
                starting = false;
                if (cancelStartup || isFinishing()) {
                    if (!isFinishing()) main.post(() -> {
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        if (screenButton != null) screenButton.setEnabled(false);
                        status.setText("●  시작 취소됨");
                    });
                    return;
                }
                main.post(() -> {
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    if (screenButton != null) screenButton.setEnabled(false);
                    status.setText("●  시작 실패");
                    append("\n오류: " + e.getMessage() + "\n");
                    if (!isFinishing()) {
                        new AlertDialog.Builder(this)
                                .setTitle("VM을 시작하지 못했어요")
                                .setMessage(e.getMessage() == null ? "알 수 없는 오류" : e.getMessage())
                                .setPositiveButton("확인", null)
                                .show();
                    }
                });
            }
        }, "NeoVM-QEMU-start").start();
    }

    private void copyIsoIfNeeded() throws Exception {
        String previous = getSharedPreferences("neovm_runtime", MODE_PRIVATE).getString("iso_" + vmId, "");
        if (isoFile.exists() && isoUri.equals(previous)) {
            append("기존 ISO 복사본 재사용: " + isoFile.getName() + "\n");
            return;
        }
        Uri uri = Uri.parse(isoUri);
        long size = querySize(uri);
        long available = new StatFs(getFilesDir().getAbsolutePath()).getAvailableBytes();
        if (size > 0 && available < size + 128L * 1024L * 1024L) {
            throw new IllegalStateException("ISO 복사 공간이 부족합니다. 필요 공간 약 " + (size / (1024 * 1024)) + "MB, 남은 공간 약 " + (available / (1024 * 1024)) + "MB입니다.");
        }
        append("ISO 파일을 앱 저장 공간으로 복사 중" + (size > 0 ? " (" + (size / (1024 * 1024)) + "MB)" : "") + "...\n");
        File temp = new File(vmDir, "installer.iso.tmp");
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(temp)) {
            if (in == null) throw new IllegalStateException("선택한 ISO 파일을 읽을 수 없습니다.");
            byte[] buf = new byte[1024 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                checkStartupNotCancelled();
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (Exception e) {
            temp.delete();
            throw e;
        }
        if (isoFile.exists() && !isoFile.delete()) throw new IllegalStateException("이전 ISO 복사본을 교체할 수 없습니다.");
        if (!temp.renameTo(isoFile)) throw new IllegalStateException("ISO 임시 파일을 최종 경로로 옮길 수 없습니다.");
        getSharedPreferences("neovm_runtime", MODE_PRIVATE).edit().putString("iso_" + vmId, isoUri).apply();
        append("ISO 복사 완료. 원본 파일은 삭제하지 않았습니다.\n");
    }

    private void checkStartupNotCancelled() throws InterruptedException {
        if (cancelStartup || Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("VM 시작이 취소되었습니다.");
        }
    }

    private long querySize(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getLong(0);
        } catch (Exception ignored) { }
        return -1;
    }

    private String readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toString("UTF-8");
    }

    private void stopVm() {
        cancelStartup = true;
        Process p = qemuProcess;
        if (p == null) {
            starting = false;
            if (!isFinishing()) {
                status.setText("●  중지됨");
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                if (screenButton != null) screenButton.setEnabled(false);
            }
            return;
        }
        status.setText("●  중지 요청 중...");
        new Thread(() -> {
            try {
                p.destroy();
                if (!p.waitFor(4, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                    p.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                append("중지 오류: " + e.getMessage() + "\n");
            }
            main.post(() -> {
                if (qemuProcess == p) qemuProcess = null;
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                if (screenButton != null) screenButton.setEnabled(false);
                status.setText("●  중지됨");
                append("\nVM 중지됨.\n");
            });
        }, "NeoVM-QEMU-stop").start();
    }

    private void sendInput() {
        Process p = qemuProcess;
        if (p == null || !p.isAlive()) {
            Toast.makeText(this, "실행 중인 VM이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String value = input.getText().toString();
        try {
            p.getOutputStream().write((value + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            p.getOutputStream().flush();
            input.setText("");
        } catch (Exception e) {
            Toast.makeText(this, "콘솔 입력 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void append(String value) {
        main.post(() -> {
            if (console == null) return;
            console.append(value);
            if (console.length() > 45000) {
                console.setText(console.getText().subSequence(console.length() - 30000, console.length()));
            }
            consoleScroll.post(() -> consoleScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTextColor(primary ? Color.rgb(13, 19, 39) : white);
        b.setBackground(round(primary ? accent : panel, 11));
        return b;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        // Keep the VM alive only while this console screen is active in this prototype.
        if (isFinishing()) stopVm();
        super.onDestroy();
    }
}
