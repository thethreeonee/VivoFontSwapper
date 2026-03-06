package com.example.vivofontswapper.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
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

public class MainActivity extends AppCompatActivity {

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
                        binding.tvFontName.setText("字体：" + realPath.substring(realPath.lastIndexOf('/') + 1));
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
        requestPermissions();
    }

    private void setupRecyclerView() {
        stepAdapter = new StepAdapter();
        binding.rvSteps.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSteps.setAdapter(stepAdapter);
    }

    private void setupObservers() {
        viewModel.getSteps().observe(this, steps -> stepAdapter.setSteps(steps));

        viewModel.getStatusMessage().observe(this, msg -> binding.tvStatus.setText(msg));

        viewModel.getIsRunning().observe(this, running -> {
            binding.btnStart.setEnabled(!running && viewModel.getSelectedFontPath() != null);
            binding.btnSelectFont.setEnabled(!running);
            binding.progressMain.setVisibility(running ? View.VISIBLE : View.GONE);
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
        });

        binding.btnHelp.setOnClickListener(v -> showHelpDialog());
    }

    private void startFontSwap() {
        String fontPath = viewModel.getSelectedFontPath();
        if (fontPath == null) {
            Toast.makeText(this, "请先选择字体文件", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        "本 App 自动化执行 vivo 自定义字体流程（无需 ATools）\n\n" +
                        "【前置条件】\n" +
                        "1. 手机已获取 Root 权限\n" +
                        "2. 已安装 vivo文档 12.2.3\n" +
                        "3. 已安装 i主题 12.1.5.1\n" +
                        "4. 已下载好目标 .ttf 字体文件\n\n" +
                        "【操作步骤】\n" +
                        "1. 点击"选择字体"选择 .ttf/.otf 文件\n" +
                        "2. 点击"开始执行"等待流程完成\n" +
                        "3. 如遇步骤失败，按提示操作后重试\n" +
                        "4. 流程完成后手机自动重启\n\n" +
                        "【注意】\n" +
                        "- 若应用字体时 i主题闪退，请先在系统主题设置中恢复默认主题后重试\n" +
                        "- 如需更换不同字体，直接重新执行即可"
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
}
