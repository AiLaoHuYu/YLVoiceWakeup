package com.yl.ylvoicewakeup.creteVoice;

import java.util.List;

public class CreateLogotype {

    private List<String>  featureInfo; //特征描述
    private List<String>  featureId;//特征唯一标识
    private List<String>  groupName;//声纹分组名称
    private List<String>  groupInfo;//分组描述信息
    private List<String> groupId;// 分组标识

    public List<String> getFeatureInfo() {
        return featureInfo;
    }

    public void setFeatureInfo(List<String> featureInfo) {
        this.featureInfo = featureInfo;
    }

    public List<String> getFeatureId() {
        return featureId;
    }

    public void setFeatureId(List<String> featureId) {
        this.featureId = featureId;
    }

    public List<String> getGroupName() {
        return groupName;
    }

    public void setGroupName(List<String> groupName) {
        this.groupName = groupName;
    }

    public List<String> getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfo(List<String> groupInfo) {
        this.groupInfo = groupInfo;
    }

    public List<String> getGroupId() {
        return groupId;
    }

    public void setGroupId(List<String> groupId) {
        this.groupId = groupId;
    }

    @Override
    public String toString() {
        return "CreateLogotype{" +
                "featureInfo=" + featureInfo +
                ", featureId=" + featureId +
                ", groupName=" + groupName +
                ", groupInfo=" + groupInfo +
                ", groupId=" + groupId +
                '}';
    }
    public void clear() {
        if (groupId != null) groupId.clear();
        if (groupName != null) groupName.clear();
        if (groupInfo != null) groupInfo.clear();
        if (featureId != null) featureId.clear();
        if (featureInfo != null) featureInfo.clear();
    }
}
