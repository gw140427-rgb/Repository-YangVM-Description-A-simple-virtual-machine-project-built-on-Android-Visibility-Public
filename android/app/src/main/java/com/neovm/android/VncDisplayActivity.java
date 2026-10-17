package com.neovm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class VncDisplayActivity extends Activity {
    private static final int BG = Color.rgb(11, 16, 32);
    private static final int PANEL = Color.rgb(21, 29, 51);
    private static final int TEXT = Color.rgb(240, 244, 255);
    private static final int MUTED = Color.rgb(157, 170, 201);

    private final Handler main = new Handler(Looper.getMainLooper());
    private VncClient client;
    private CanvasView canvasView;
    private TextView status;
    private String vmName;
    private int connectionGeneration = 0;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        vmName = getIntent().getStringExtra("vm_name");
        buildUi();
        connect();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(8), dp(6), dp(8), dp(6));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText((vmName == null ? "NeoVM" : vmName) + " · 그래픽 화면");
        title.setTextColor(TEXT);
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button keyboard = button("키보드", false);
        keyboard.setOnClickListener(v -> showKeyboardDialog());
        toolbar.addView(keyboard, new LinearLayout.LayoutParams(dp(90), dp(42)));
        Button reconnect = button("다시 연결", false);
        reconnect.setOnClickListener(v -> connect());
        LinearLayout.LayoutParams reconnectP = new LinearLayout.LayoutParams(dp(98), dp(42));
        reconnectP.leftMargin = dp(6);
        toolbar.addView(reconnect, reconnectP);
        root.addView(toolbar);

        status = new TextView(this);
        status.setText("VNC 연결 중...");
        status.setTextColor(MUTED);
        status.setTextSize(11);
        status.setPadding(dp(4), dp(4), dp(4), dp(4));
        root.addView(status);

        canvasView = new CanvasView();
        LinearLayout.LayoutParams screenP = new LinearLayout.LayoutParams(-1, 0, 1);
        screenP.topMargin = dp(4);
        screenP.bottomMargin = dp(4);
        root.addView(canvasView, screenP);

        TextView help = new TextView(this);
        help.setText("화면을 터치하면 마우스 클릭으로 전달됩니다. 키보드 버튼으로 글자를 입력하세요.");
        help.setTextColor(MUTED);
        help.setTextSize(10);
        help.setGravity(Gravity.CENTER);
        root.addView(help);
        setContentView(root);
    }

    private void connect() {
        final int generation = ++connectionGeneration;
        if (client != null) client.close();
        status.setText("VNC 연결 중...");
        canvasView.setFrame(null);
        VncClient next = new VncClient(new VncClient.Listener() {
            @Override public void onConnected(int width, int height) {
                main.post(() -> {
                    if (generation != connectionGeneration || isFinishing()) return;
                    status.setText("연결됨 · " + width + " × " + height + " · 터치=마우스");
                });
            }

            @Override public void onFrame(Bitmap bitmap) {
                main.post(() -> {
                    if (generation == connectionGeneration && canvasView != null && !isFinishing()) {
                        canvasView.setFrame(bitmap);
                    }
                });
            }

            @Override public void onError(String message) {
                main.post(() -> {
                    if (generation != connectionGeneration || isFinishing()) return;
                    if (status != null) status.setText("VNC 오류: " + message);
                    Toast.makeText(VncDisplayActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
        client = next;
        next.start();
    }

    private void showKeyboardDialog() {
        if (client == null) {
            Toast.makeText(this, "VNC 연결을 먼저 확인해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText editor = new EditText(this);
        editor.setSingleLine(false);
        editor.setMinLines(2);
        editor.setTextColor(TEXT);
        editor.setHintTextColor(MUTED);
        editor.setHint("가상 머신에 보낼 텍스트");
        editor.setBackgroundColor(PANEL);
        editor.setPadding(dp(12), dp(8), dp(12), dp(8));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("가상 머신 키보드")
                .setView(editor)
                .setNegativeButton("취소", null)
                .setPositiveButton("입력 보내기", null)
                .create();
        dialog.setOnShowListener(v -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String value = editor.getText().toString();
                VncClient active = client;
                if (active != null) active.sendText(value);
                dialog.dismiss();
            });
        });
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        editor.requestFocus();
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.rgb(13, 19, 39) : TEXT);
        button.setBackgroundColor(primary ? Color.rgb(139, 155, 255) : PANEL);
        return button;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        connectionGeneration++;
        if (client != null) client.close();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();
    }

    private final class CanvasView extends View {
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private Bitmap frame;
        private final RectF destination = new RectF();

        CanvasView() {
            super(VncDisplayActivity.this);
            setBackgroundColor(Color.BLACK);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }

        void setFrame(Bitmap bitmap) {
            frame = bitmap;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Bitmap current = frame;
            if (current == null) return;
            synchronized (current) {
                if (current.isRecycled()) return;
                float scale = Math.min(getWidth() / (float) current.getWidth(), getHeight() / (float) current.getHeight());
                float dw = current.getWidth() * scale;
                float dh = current.getHeight() * scale;
                float left = (getWidth() - dw) / 2f;
                float top = (getHeight() - dh) / 2f;
                destination.set(left, top, left + dw, top + dh);
                canvas.drawBitmap(current, null, destination, paint);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            Bitmap current = frame;
            VncClient active = client;
            if (current == null || active == null) return true;
            float scale = Math.min(getWidth() / (float) current.getWidth(), getHeight() / (float) current.getHeight());
            if (scale <= 0) return true;
            float dw = current.getWidth() * scale;
            float dh = current.getHeight() * scale;
            float left = (getWidth() - dw) / 2f;
            float top = (getHeight() - dh) / 2f;
            int x = Math.round((event.getX() - left) / scale);
            int y = Math.round((event.getY() - top) / scale);
            int buttonMask = event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL ? 0 : 1;
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN ||
                    event.getActionMasked() == MotionEvent.ACTION_MOVE ||
                    event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                active.sendPointer(x, y, buttonMask);
                return true;
            }
            return true;
        }
    }
}
