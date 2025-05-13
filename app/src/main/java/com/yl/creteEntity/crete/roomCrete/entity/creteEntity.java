package com.yl.creteEntity.crete.roomCrete.entity;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "crete_entity")
public class creteEntity implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;//自增id
    @ColumnInfo(name = "featureInfo")
    private String featureInfo; //特征描述
    @ColumnInfo(name = "featureId")
    private String featureId;//特征唯一标识
    @ColumnInfo(name = "groupName")
    private String groupName;//声纹分组名称
    @ColumnInfo(name = "groupInfo")
    private String groupInfo;//分组描述信息
    @ColumnInfo(name = "groupId")
    private String groupId;// 分组标识

    public creteEntity() {

    }

        public creteEntity(String featureInfo, String featureId, String groupName, String groupInfo, String groupId) {
        this.featureInfo = featureInfo;
        this.featureId = featureId;
        this.groupName = groupName;
        this.groupInfo = groupInfo;
        this.groupId = groupId;
    }
    public static final Creator<creteEntity> CREATOR = new Creator<creteEntity>() {
        @Override
        public creteEntity createFromParcel(Parcel source) {
            return new creteEntity(source);
        }

        @Override
        public creteEntity[] newArray(int size) {
            return new creteEntity[size];
        }
    };

    protected creteEntity(Parcel in) {
        id = in.readInt();
        featureInfo = in.readString();
        featureId = in.readString();
        groupName = in.readString();
        groupInfo = in.readString();
        groupId = in.readString();
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupInfo() {
        return groupInfo;
    }

    public void setGroupInfo(String groupInfo) {
        this.groupInfo = groupInfo;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public String getFeatureInfo() {
        return featureInfo;
    }

    public void setFeatureInfo(String featureInfo) {
        this.featureInfo = featureInfo;
    }


    @Override
    public String toString() {
        return "creteEntity{" +
                "id=" + id +
                ", featureInfo='" + featureInfo + '\'' +
                ", featureId='" + featureId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", groupInfo='" + groupInfo + '\'' +
                ", groupId='" + groupId + '\'' +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(featureInfo);
        dest.writeString(featureId);
        dest.writeString(groupName);
        dest.writeString(groupInfo);
        dest.writeString(groupId);
    }
}
