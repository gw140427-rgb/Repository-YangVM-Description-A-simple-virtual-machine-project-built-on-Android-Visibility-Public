package com.neovm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_ISO = 431;
    private static final String PREFS = "neovm_config";
    private static final String KEY_VMS = "virtual_machines";
    private final ArrayList<VmConfig> machines = new ArrayList<>();
    private LinearLayout machineList;
    private TextView summary;
    private String pendingIsoVmId;
    private final int bg = Color.rgb(11, 16, 32);
    private final int panel = Color.rgb(21, 29, 51);
    private final int panel2 = Color.rgb(28, 38, 65);
    private final int white = Color.rgb(240, 244, 255);
    private final int muted = Color.rgb(157, 170, 201);
    private final int accent = Color.rgb(139, 155, 255);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        loadMachines();
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));
        scroll.addView(root);

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("N", 25, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(accent, 16));
        brandRow.addView(logo, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setPadding(dp(13), 0, 0, 0);
        brandText.addView(text("NeoVM", 27, white, true));
        TextView subtitle = text("VIRTUAL MACHINE MANAGER", 10, muted, true);
        subtitle.setLetterSpacing(0.1f);
        brandText.addView(subtitle);
        brandRow.addView(brandText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView alpha = text("ALPHA", 10, accent, true);
        alpha.setPadding(dp(10), dp(6), dp(10), dp(6));
        alpha.setBackground(round(Color.rgb(41, 48, 91), 30));
        brandRow.addView(alpha);
        root.addView(brandRow);

        TextView headline = text("내 가상 컴퓨터,\n한곳에서 관리하기.", 28, white, true);
        headline.setPadding(0, dp(30), 0, dp(9));
        root.addView(headline);
        TextView desc = text("VM 설정을 만들고 ISO 이미지를 연결해 보세요.\n실제 운영체제 부팅 엔진은 아직 연결되지 않았어요.", 14, muted, false);
        desc.setLineSpacing(dp(4), 1.0f);
        root.addView(desc);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(16), dp(15), dp(16), dp(15));
        statusCard.setBackground(round(panel, 18));
        LinearLayout statusLine = new LinearLayout(this);
        statusLine.setGravity(Gravity.CENTER_VERTICAL);
        statusLine.addView(text("●", 13, Color.rgb(255, 197, 93), true));
        statusLine.addView(text("  가상화 엔진 연결 대기", 14, white, true));
        statusCard.addView(statusLine);
        TextView engineBody = text("현재 버전: VM 프로필 저장 · RAM/디스크 설정 · ISO 파일 선택", 12, muted, false);
        engineBody.setPadding(0, dp(8), 0, 0);
        statusCard.addView(engineBody);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(22);
        root.addView(statusCard, statusParams);

        LinearLayout sectionRow = new LinearLayout(this);
        sectionRow.setGravity(Gravity.CENTER_VERTICAL);
        sectionRow.setPadding(0, dp(28), 0, dp(14));
        sectionRow.addView(text("가상 머신", 19, white, true), new LinearLayout.LayoutParams(0, -2, 1));
        summary = text("0개", 13, muted, false);
        sectionRow.addView(summary);
        root.addView(sectionRow);

        Button add = button("＋  새 가상 머신 만들기", true);
        add.setOnClickListener(v -> showCreateDialog());
        root.addView(add, fullWidth());

        machineList = new LinearLayout(this);
        machineList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(-1, -2);
        listParams.topMargin = dp(14);
        root.addView(machineList, listParams);
        renderMachines();

        TextView foot = text("NeoVM · Android edition · 0.1.0\n설정 데이터는 이 기기에 저장됩니다.", 11, muted, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(27), 0, dp(4));
        root.addView(foot);
        setContentView(scroll);
    }

    private void renderMachines() {
        if (machineList == null) return;
        machineList.removeAllViews();
        summary.setText(machines.size() + "개");
        if (machines.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setGravity(Gravity.CENTER);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            empty.setBackground(round(panel, 18));
            empty.addView(text("▣", 34, muted, true));
            TextView noVm = text("아직 만든 가상 머신이 없어요", 15, white, true);
            noVm.setPadding(0, dp(12), 0, dp(5));
            empty.addView(noVm);
            empty.addView(text("위의 버튼을 눌러 첫 VM을 추가하세요.", 12, muted, false));
            machineList.addView(empty, fullWidth());
            return;
        }
        for (VmConfig vm : new ArrayList<>(machines)) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(16), dp(16), dp(15));
            card.setBackground(round(panel, 18));
            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(text("▣", 24, accent, true), new LinearLayout.LayoutParams(dp(35), -2));
            LinearLayout names = new LinearLayout(this);
            names.setOrientation(LinearLayout.VERTICAL);
            names.addView(text(vm.name, 17, white, true));
            TextView os = text(vm.guestOs, 12, muted, false);
            os.setPadding(0, dp(3), 0, 0);
            names.addView(os);
            top.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
            TextView stopped = text("중지됨", 11, Color.rgb(255, 208, 129), true);
            stopped.setPadding(dp(9), dp(6), dp(9), dp(6));
            stopped.setBackground(round(Color.rgb(69, 52, 35), 20));
            top.addView(stopped);
            card.addView(top);

            LinearLayout specs = new LinearLayout(this);
            specs.setPadding(0, dp(16), 0, dp(14));
            specs.addView(specChip("RAM", vm.ramMb + " MB"), new LinearLayout.LayoutParams(0, -2, 1));
            specs.addView(specChip("디스크", vm.diskGb + " GB"), new LinearLayout.LayoutParams(0, -2, 1));
            specs.addView(specChip("ISO", vm.isoUri == null ? "미연결" : "연결됨"), new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(specs);

            LinearLayout buttons = new LinearLayout(this);
            Button start = button("▶  실행", true);
            start.setOnClickListener(v -> tryStart(vm));
            buttons.addView(start, new LinearLayout.LayoutParams(0, dp(45), 1));
            Button iso = button("ISO 선택", false);
            LinearLayout.LayoutParams isoP = new LinearLayout.LayoutParams(0, dp(45), 1);
            isoP.leftMargin = dp(8);
            iso.setOnClickListener(v -> chooseIso(vm.id));
            buttons.addView(iso, isoP);
            Button del = button("삭제", false);
            LinearLayout.LayoutParams delP = new LinearLayout.LayoutParams(dp(70), dp(45));
            delP.leftMargin = dp(8);
            del.setOnClickListener(v -> confirmDelete(vm));
            buttons.addView(del, delP);
            card.addView(buttons);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.bottomMargin = dp(12);
            machineList.addView(card, p);
        }
    }

    private ViewGroup.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private android.view.View specChip(String label, String value) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setPadding(dp(9), dp(10), dp(5), dp(10));
        chip.setBackground(round(panel2, 12));
        chip.addView(text(label, 10, muted, true));
        TextView val = text(value, 12, white, true);
        val.setPadding(0, dp(4), 0, 0);
        chip.addView(val);
        return chip;
    }

    private void showCreateDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("예: Debian 테스트");
        name.setTextColor(white);
        name.setHintTextColor(muted);
        name.setBackgroundTintList(ColorStateList.valueOf(accent));
        form.addView(formLabel("가상 머신 이름"));
        form.addView(name, fullWidth());
        form.addView(formLabel("운영체제 종류"));
        Spinner os = spinner(new String[]{"Debian GNU/Linux", "Ubuntu Linux", "기타 Linux", "Windows (ARM64 ISO)"});
        form.addView(os, fullWidth());
        form.addView(formLabel("RAM 메모리"));
        Spinner ram = spinner(new String[]{"512 MB", "1024 MB", "2048 MB", "3072 MB", "4096 MB"});
        ram.setSelection(1);
        form.addView(ram, fullWidth());
        form.addView(formLabel("가상 디스크 용량"));
        Spinner disk = spinner(new String[]{"4 GB", "8 GB", "16 GB", "32 GB", "64 GB"});
        disk.setSelection(2);
        form.addView(disk, fullWidth());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("새 가상 머신")
                .setView(form)
                .setNegativeButton("취소", null)
                .setPositiveButton("만들기", null)
                .create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) {
                name.setError("이름을 입력해 주세요.");
                return;
            }
            VmConfig vm = new VmConfig();
            vm.id = UUID.randomUUID().toString();
            vm.name = n;
            vm.guestOs = (String) os.getSelectedItem();
            vm.ramMb = parseLeadingNumber((String) ram.getSelectedItem());
            vm.diskGb = parseLeadingNumber((String) disk.getSelectedItem());
            machines.add(vm);
            saveMachines();
            dialog.dismiss();
            render();
            Toast.makeText(this, "가상 머신 설정을 저장했어요.", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private TextView formLabel(String s) {
        TextView t = text(s, 13, muted, true);
        t.setPadding(0, dp(12), 0, dp(2));
        return t;
    }

    private Spinner spinner(String[] options) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        return s;
    }

    private int parseLeadingNumber(String value) {
        String digits = value.replaceAll("[^0-9].*$", "");
        try { return Integer.parseInt(digits); } catch (NumberFormatException e) { return 0; }
    }

    private void chooseIso(String vmId) {
        pendingIsoVmId = vmId;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "ISO 이미지 선택");
        try {
            startActivityForResult(intent, REQUEST_ISO);
        } catch (Exception e) {
            Toast.makeText(this, "파일 선택기를 열 수 없어요.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_ISO || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // Some document providers do not provide persistable grants.
        }
        for (VmConfig vm : machines) {
            if (vm.id.equals(pendingIsoVmId)) {
                vm.isoUri = uri.toString();
                saveMachines();
                break;
            }
        }
        pendingIsoVmId = null;
        render();
        Toast.makeText(this, "ISO 파일 위치를 저장했어요. 아직 부팅한 것은 아니에요.", Toast.LENGTH_LONG).show();
    }

    private void tryStart(VmConfig vm) {
        if (vm.isoUri == null || vm.isoUri.trim().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("ISO 이미지가 필요해요")
                    .setMessage("운영체제 설치 ISO를 먼저 선택해 주세요. 다만 현재 NeoVM 버전에는 실제 부팅 엔진이 아직 포함되지 않았어요.")
                    .setNegativeButton("닫기", null)
                    .setPositiveButton("ISO 선택", (d, w) -> chooseIso(vm.id))
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("가상화 엔진이 아직 없어요")
                .setMessage("'" + vm.name + "'의 설정과 ISO 경로는 저장되어 있어요. 하지만 이 빌드에는 QEMU 등 실제 게스트 운영체제를 실행하는 엔진이 연결되지 않아 아직 부팅할 수 없습니다. 다음 단계에는 ARM64용 엔진 통합이 필요해요.")
                .setPositiveButton("확인", null)
                .show();
    }

    private void confirmDelete(VmConfig vm) {
        new AlertDialog.Builder(this)
                .setTitle("가상 머신 삭제")
                .setMessage("'" + vm.name + "'의 NeoVM 설정을 삭제할까요? 선택한 원본 ISO 파일은 삭제하지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (d, w) -> {
                    machines.remove(vm);
                    saveMachines();
                    render();
                }).show();
    }

    private void loadMachines() {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_VMS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                VmConfig vm = new VmConfig();
                vm.id = o.optString("id", UUID.randomUUID().toString());
                vm.name = o.optString("name", "새 가상 머신");
                vm.guestOs = o.optString("guestOs", "기타 Linux");
                vm.ramMb = o.optInt("ramMb", 1024);
                vm.diskGb = o.optInt("diskGb", 16);
                vm.isoUri = o.isNull("isoUri") ? null : o.optString("isoUri", null);
                machines.add(vm);
            }
        } catch (Exception e) {
            machines.clear();
        }
    }

    private void saveMachines() {
        JSONArray array = new JSONArray();
        try {
            for (VmConfig vm : machines) {
                JSONObject o = new JSONObject();
                o.put("id", vm.id);
                o.put("name", vm.name);
                o.put("guestOs", vm.guestOs);
                o.put("ramMb", vm.ramMb);
                o.put("diskGb", vm.diskGb);
                o.put("isoUri", vm.isoUri == null ? JSONObject.NULL : vm.isoUri);
                array.put(o);
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_VMS, array.toString()).apply();
        } catch (Exception e) {
            Toast.makeText(this, "설정을 저장하지 못했어요.", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String value, boolean primary) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.rgb(13, 19, 39) : white);
        b.setPadding(dp(7), 0, dp(7), 0);
        b.setBackground(round(primary ? accent : panel2, 12));
        return b;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class VmConfig {
        String id;
        String name;
        String guestOs;
        int ramMb;
        int diskGb;
        String isoUri;
    }
}
