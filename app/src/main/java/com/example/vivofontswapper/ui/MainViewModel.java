package com.example.vivofontswapper.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.vivofontswapper.model.Step;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<List<Step>> steps = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("选择字体文件后点击开始");
    private final MutableLiveData<Boolean> allDone = new MutableLiveData<>(false);

    private String selectedFontPath;

    // 定义流程步骤
    private static final String[] STEP_TITLES = {
            "获取 Shizuku 授权",
            "前置条件检查（Shizuku + 内置安装包）",
            "卸载 vivo文档",
            "卸载 i主题",
            "安装 vivo文档 12.2.3",
            "安装 i主题 12.1.5.1",
            "拉起 i主题下载假黑体",
            "检查假字体 .itz 包",
            "注入字体到 .itz/fonts",
            "写入 /data/vfonts 目标字体",
            "修改 hmtx 后缀空格",
            "拉起文档打开 .itz",
            "拉起文档打开 /data/vfonts",
            "再次拉起 i主题应用字体",
            "完成（请手动重启）"
    };

    public MainViewModel() {
        resetSteps();
    }

    public void resetSteps() {
        List<Step> list = new ArrayList<>();
        for (int i = 0; i < STEP_TITLES.length; i++) {
            list.add(new Step(i, STEP_TITLES[i]));
        }
        steps.postValue(list);
        isRunning.postValue(false);
        allDone.postValue(false);
        statusMessage.postValue("选择字体文件后点击开始");
    }

    public void setStepRunning(int index, String description) {
        updateStep(index, Step.Status.RUNNING, description);
        statusMessage.postValue("步骤 " + (index + 1) + "/" + STEP_TITLES.length + "：" + STEP_TITLES[index]);
    }

    public void setStepSuccess(int index, String detail) {
        updateStep(index, Step.Status.SUCCESS, detail);
    }

    public void setStepFailed(int index, String reason) {
        updateStep(index, Step.Status.FAILED, reason);
        isRunning.postValue(false);
        statusMessage.postValue("❌ 步骤 " + (index + 1) + " 失败：" + STEP_TITLES[index]);
    }

    public void setAllDone() {
        allDone.postValue(true);
        isRunning.postValue(false);
        statusMessage.postValue("✅ 全部完成，请到 i主题确认应用后手动重启手机");
    }

    public void setRunning(boolean running) {
        isRunning.postValue(running);
    }

    private void updateStep(int index, Step.Status status, String detail) {
        List<Step> list = steps.getValue();
        if (list == null || index >= list.size()) return;
        Step step = list.get(index);
        step.setStatus(status);
        step.setDetail(detail != null ? detail : "");
        steps.postValue(new ArrayList<>(list));
    }

    public void setSelectedFontPath(String path) {
        this.selectedFontPath = path;
        statusMessage.postValue("已选择字体：" + path.substring(path.lastIndexOf('/') + 1));
    }

    public String getSelectedFontPath() { return selectedFontPath; }

    public LiveData<List<Step>> getSteps() { return steps; }
    public LiveData<Boolean> getIsRunning() { return isRunning; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getAllDone() { return allDone; }
}
