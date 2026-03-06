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
            "前置条件检查",
            "检测 i主题 .itz 字体包",
            "修改 fonts/ 条目（加空格）",
            "清除旧 i主题字体缓存",
            "触发密钥生成（vivo文档）",
            "重新下载 i主题字体包",
            "注入第三方字体进 .itz 包",
            "修改 hmtx 字段激活字体",
            "触发 i主题应用字体",
            "重启手机"
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
        statusMessage.postValue("✅ 全部完成！手机将在 3 秒后重启...");
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
