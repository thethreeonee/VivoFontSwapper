package com.example.vivofontswapper.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vivofontswapper.R;
import com.example.vivofontswapper.databinding.ActivityMainBinding;
import com.example.vivofontswapper.util.FontSwapHelper;
import com.example.vivofontswapper.util.RealPathUtil;
import com.example.vivofontswapper.util.ShizukuUtils;

public class MainActivity extends AppCompatActivity {

    private static final int SHIZUKU_REQUEST_CODE = 2001;

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private StepAdapter stepAdapter;

    // 选择字体文件的 launcher
    private final ActivityResultLauncher<String> fontPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    String realPath = RealPathUtil.getRealPath(this, uri);
                    if (realPath != null && (realPath.endsWith(".ttf") || realPath.endsWith(".otf"))) {
                        viewModel.setSelectedFontPath(realPath);
                        binding.btnStart.setEnabled(true);
                        updateFontSelectionUi(realPath.substring(realPath.lastIndexOf('/') + 1));
                    } else {
                        Toast.makeText(this, "请选择 .ttf 或 .otf 字体文件", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupRecyclerView();
        setupObservers();
        setupButtons();
        updateFontSelectionUi(null);
        requestPermissions();
    }

    private void setupRecyclerView() {
        stepAdapter = new StepAdapter();
        binding.rvSteps.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSteps.setAdapter(stepAdapter);
    }

    private void setupObservers() {
        viewModel.getSteps().observe(this, steps -> stepAdapter.setSteps(steps));

        viewModel.getIsRunning().observe(this, running -> {
            binding.btnStart.setEnabled(!running && viewModel.getSelectedFontPath() != null);
            binding.btnSelectFont.setEnabled(!running);
        });

        viewModel.getAllDone().observe(this, done -> {
            if (done) {
                binding.cardDone.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupButtons() {
        binding.btnSelectFont.setOnClickListener(v -> fontPickerLauncher.launch("font/*"));

        binding.btnStart.setEnabled(false);
        binding.btnStart.setOnClickListener(v -> startFontSwap());
        binding.btnReset.setOnClickListener(v -> {
            viewModel.resetSteps();
            binding.cardDone.setVisibility(View.GONE);
            if (viewModel.getSelectedFontPath() == null) {
                updateFontSelectionUi(null);
            }
        });
        binding.btnHelp.setOnClickListener(v -> showHelpDialog());
    }

    private void updateFontSelectionUi(String fileName) {
        boolean hasFile = fileName != null && !fileName.isEmpty();
        if (hasFile) {
            binding.tvFontStatus.setText("READY");
            binding.tvFontStatus.setBackgroundResource(R.drawable.pill_status_bg_ready);
            binding.tvFontHint.setText(fileName);
        } else {
            binding.tvFontStatus.setText("NOT READY");
            binding.tvFontStatus.setBackgroundResource(R.drawable.pill_status_bg_not_ready);
            binding.tvFontHint.setText("请选择 .ttf/.otf 字体文件");
        }
    }

    private void startFontSwap() {
        String fontPath = viewModel.getSelectedFontPath();
        if (fontPath == null) {
            Toast.makeText(this, "请先选择字体文件", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ShizukuUtils.isShizukuAvailable()) {
            new AlertDialog.Builder(this)
                    .setTitle("Shizuku 未就绪")
                    .setMessage("请先启动 Shizuku 服务，再重试。")
                    .setPositiveButton("知道了", null)
                    .show();
            return;
        }

        if (!ShizukuUtils.hasPermission()) {
            boolean requested = ShizukuUtils.requestPermission(SHIZUKU_REQUEST_CODE);
            if (requested) {
                Toast.makeText(this, "请在弹窗中授予 Shizuku 权限", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无法请求 Shizuku 权限，请检查 Shizuku 状态", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        runFontSwapFlow(fontPath);
    }

    private void runFontSwapFlow(String fontPath) {
        viewModel.setRunning(true);
        viewModel.resetSteps();
        binding.cardDone.setVisibility(View.GONE);

        FontSwapHelper helper = new FontSwapHelper(this, fontPath, new FontSwapHelper.StepCallback() {
            @Override
            public void onStepStart(int step, String description) {
                runOnUiThread(() -> {
                    viewModel.setStepRunning(step, description);
                    // 滚动到当前步骤
                    binding.rvSteps.smoothScrollToPosition(step);
                });
            }

            @Override
            public void onStepSuccess(int step, String detail) {
                runOnUiThread(() -> viewModel.setStepSuccess(step, detail));
            }

            @Override
            public void onStepFailed(int step, String reason) {
                runOnUiThread(() -> {
                    viewModel.setStepFailed(step, reason);
                    // 显示错误对话框
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("步骤 " + (step + 1) + " 失败")
                            .setMessage(reason)
                            .setPositiveButton("知道了", null)
                            .setNegativeButton("重试", (d, w) -> startFontSwap())
                            .show();
                });
            }

            @Override
            public void onAllDone() {
                runOnUiThread(() -> viewModel.setAllDone());
            }
        });

        // 在子线程执行，避免阻塞 UI
        new Thread(helper::executeFullFlow).start();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("使用说明")
                .setMessage(
                        "本 App 用于简化 vivo 字体替换流程\n\n" +
                        "【前置条件】\n" +
                        "1. 已安装并启动 Shizuku\n" +
                        "2. 准备一个可用的 .ttf/.otf 字体文件\n" +
                        "3. 首次执行会自动释放并安装内置的指定版本 APK\n\n" +
                        "【操作步骤】\n" +
                        "1. 点击“选择字体”选择 .ttf/.otf 文件\n" +
                        "2. 点击“开始执行”，先获取 Shizuku 授权\n" +
                        "3. 自动卸载当前 vivo文档 / i主题，并安装指定版本\n" +
                        "4. 自动拉起 i主题下载“我是一个假黑体”，返回后继续执行\n" +
                        "5. 自动完成 .itz 注入、/data/vfonts 写入和补丁\n" +
                        "6. 按提示在 i主题应用字体后手动重启手机\n\n" +
                        "【注意】\n" +
                        "- 若应用字体时 i主题闪退，请先恢复默认主题再试\n" +
                        "- 如需更换不同字体，重新选择后再次执行即可"
                )
                .setPositiveButton("明白了", null)
                .show();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, 1001);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            if (ShizukuUtils.hasPermission()) {
                Toast.makeText(this, "Shizuku 授权成功，开始执行流程", Toast.LENGTH_SHORT).show();
                startFontSwap();
            } else {
                Toast.makeText(this, "未获得 Shizuku 权限，流程已取消", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
