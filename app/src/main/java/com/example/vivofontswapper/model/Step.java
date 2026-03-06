package com.example.vivofontswapper.model;

/**
 * 代表换字体流程中的一个步骤
 */
public class Step {

    public enum Status {
        PENDING,    // 等待
        RUNNING,    // 进行中
        SUCCESS,    // 成功
        FAILED,     // 失败
        SKIPPED     // 跳过
    }

    private final int index;
    private final String title;
    private String detail;
    private Status status;

    public Step(int index, String title) {
        this.index = index;
        this.title = title;
        this.status = Status.PENDING;
        this.detail = "";
    }

    public int getIndex() { return index; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public Status getStatus() { return status; }

    public void setStatus(Status status) { this.status = status; }
    public void setDetail(String detail) { this.detail = detail; }
}
